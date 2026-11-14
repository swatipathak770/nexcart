package com.nexcart.recovery.service;

import com.nexcart.entity.*;
import com.nexcart.recovery.ai.RecoveryDecisionService;
import com.nexcart.recovery.dto.*;
import com.nexcart.recovery.entity.*;
import com.nexcart.recovery.enums.*;
import com.nexcart.recovery.repository.*;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional @Slf4j
public class RecoveryService {
 private static final Set<RecoveryStatus> TERMINAL_STATUSES = EnumSet.of(RecoveryStatus.RECOVERED, RecoveryStatus.CUSTOMER_CANCELLED, RecoveryStatus.EXHAUSTED, RecoveryStatus.FAILED, RecoveryStatus.NO_ACTION);
 private final RecoveryCaseRepository cases; private final RecoveryActionRepository actions; private final RecoveryAuditRepository audits; private final PaymentRepository payments; private final PaymentAttemptRepository paymentAttempts; private final OrderRepository orders; private final RecoveryDecisionService decisions; private final RazorpayClient razorpay;
 @Value("${recovery.max-attempts:3}") private int maxAttempts;

 public void detectFailedPayment(Payment payment) {
  if (payment == null || payment.getOrder() == null || payment.getPaymentStatus() == PaymentStatus.SUCCESS || latestOrderIsCancelled(payment.getOrder().getId())) return;
  RecoveryCase existing=cases.findFirstByOrderIdOrderByCreatedAtDesc(payment.getOrder().getId()).orElse(null);
  if(existing != null && existing.getStatus()!=RecoveryStatus.RECOVERED) return;
  RecoveryCase c=cases.save(RecoveryCase.builder().order(payment.getOrder()).user(payment.getOrder().getUser()).razorpayPaymentId(payment.getRazorpayPaymentId()).amount(payment.getAmount()).type(RecoveryType.PAYMENT_FAILED).failureReason(payment.getFailureReason()).status(RecoveryStatus.DETECTED).build());
  audit(c,"PAYMENT_FAILED", payment.getFailureReason()); audit(c,"RECOVERY_CASE_CREATED","Created from NexCart payment failure"); analyze(c.getId());
 }
 public RecoveryCase analyze(Long id) {
  RecoveryCase c=getForUpdate(id); if(isCancelled(c)) return markCustomerCancelled(c, "Customer cancelled this order."); ensureNotTerminal(c, "analyzed"); c.setStatus(RecoveryStatus.ANALYZING); audit(c,"AI_ANALYSIS_STARTED","Recovery context prepared; AI recommendation is validated before guardrails.");
  RecoveryDecisionResult result=decisions.decide(buildContext(c)); RecoveryDecision d=result.decision(); c.setDecisionSource(result.source());
  if("DETERMINISTIC_FALLBACK".equals(result.source())) audit(c,"AI_FALLBACK_USED","Decision source: DETERMINISTIC_FALLBACK; reason: "+result.fallbackReason());
  else audit(c,"AI_DECISION_CREATED",d.recommendedAction()+" | probability: "+d.recoveryProbability()+" | confidence: "+d.confidence()+" | risk: "+d.riskLevel());
  audit(c,"AI_DECISION_VALIDATED","Validated bounded action and decision schema."); RecoveryActionType allowed=guard(c,d.recommendedAction());
  c.setRecoveryProbability(d.recoveryProbability()); c.setExpectedRecoveryAmount(c.getAmount().multiply(d.recoveryProbability()).setScale(2, RoundingMode.HALF_UP)); c.setRecommendedAction(allowed); c.setDecisionReason(d.reason()); c.setConfidence(d.confidence().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)+"%"); c.setRiskLevel(d.riskLevel()); c.setRecoveryScore(d.recoveryProbability().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue()); c.setGuardrailResult(allowed==d.recommendedAction() ? "APPROVED: " + allowed : "REJECTED: " + d.recommendedAction() + "; allowed " + allowed); c.setStatus(allowed==RecoveryActionType.NO_ACTION?RecoveryStatus.NO_ACTION:RecoveryStatus.ACTION_RECOMMENDED); cases.save(c);
  log.info("RecoverAI decision recorded; caseId={} source={} action={} simulated={}", c.getId(), c.getDecisionSource(), allowed, c.isSimulated());
  audit(c,"DECISION_CREATED",d.recommendedAction()+" | source: "+c.getDecisionSource()+" | "+d.reason()); audit(c,allowed==d.recommendedAction()?"GUARDRAIL_APPROVED":"GUARDRAIL_REJECTED",c.getGuardrailResult()); return c;
 }
 public RecoveryCase execute(Long id) {
  // Keep lock order consistent with customer cancellation and payment capture:
  // order row first, then recovery case/payment rows.
  cases.findOrderIdById(id).ifPresent(orders::findByIdForUpdate);
  RecoveryCase c=getForUpdate(id); if(isCancelled(c)) return markCustomerCancelled(c, "Customer cancelled this order."); ensureNotTerminal(c, "executed");
  RecoveryActionType action=guard(c,c.getRecommendedAction()); if(action==RecoveryActionType.NO_ACTION){c.setStatus(RecoveryStatus.NO_ACTION); c.setGuardrailResult("REJECTED: no safe recovery action available"); audit(c,"NO_ACTION","No safe recovery action available"); return cases.save(c);}
  if ((action == RecoveryActionType.CREATE_PAYMENT_LINK && hasEquivalentAction(c, action))
          || (action == RecoveryActionType.RETRY_PAYMENT && hasActiveRetryAttempt(c))) {
   audit(c,"ACTION_SKIPPED", action == RecoveryActionType.RETRY_PAYMENT
           ? "A usable pending payment attempt already exists; no duplicate checkout was created."
           : "Equivalent payment-link action is already pending or completed; no duplicate link was created.");
   return cases.save(c);
  }
  if(action==RecoveryActionType.CREATE_PAYMENT_LINK && hasActivePaymentLink(c)) { audit(c,"ACTION_SKIPPED","A secure payment link already exists; no duplicate Razorpay link was created."); return cases.save(c); }
  c.setStatus(RecoveryStatus.ACTION_PENDING); c.setActionAttempts(c.getActionAttempts()+1); c.setExecutedAction(action); RecoveryAction event=RecoveryAction.builder().recoveryCase(c).action(action).build(); audit(c,"ACTION_STARTED",action.name());
  log.info("RecoverAI action started; caseId={} action={} simulated={}", c.getId(), action, c.isSimulated());
  if(c.isSimulated()) return executeSimulated(c, action, event);
  try { if(action==RecoveryActionType.CREATE_PAYMENT_LINK){ JSONObject o=new JSONObject(); o.put("amount",c.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact()); o.put("currency",c.getCurrency()); o.put("reference_id","REC-"+c.getId()); o.put("description","NexCart RecoverAI payment recovery"); com.razorpay.PaymentLink link=razorpay.paymentLink.create(o); event.setRazorpayPaymentLinkId(link.get("id")); event.setPaymentLink(link.get("short_url")); audit(c,"PAYMENT_LINK_CREATED",event.getPaymentLink()); }
   else if(action==RecoveryActionType.SEND_RECOVERY_MESSAGE){ event.setMessage("Your NexCart order worth ₹"+c.getAmount()+" is waiting. Complete payment using your secure payment link."); audit(c,"RECOVERY_MESSAGE_GENERATED",event.getMessage()); }
   else if(action==RecoveryActionType.RETRY_PAYMENT) createRetryPayment(c, event);
  event.setSuccessful(true); c.setStatus(RecoveryStatus.ACTION_PENDING);
  } catch(Exception ex){event.setFailureDetail("Razorpay action unavailable: "+ex.getMessage()); c.setStatus(c.getActionAttempts()>=maxAttempts?RecoveryStatus.EXHAUSTED:RecoveryStatus.ACTION_RECOMMENDED); audit(c,"ACTION_FAILED",event.getFailureDetail());}
  actions.save(event); return cases.save(c);
 }
 private void createRetryPayment(RecoveryCase c, RecoveryAction event) throws Exception {
  if (c.getOrder() == null) throw new IllegalStateException("A payment retry requires an existing order.");
  Order order = orders.findById(c.getOrder().getId()).orElseThrow(() -> new NoSuchElementException("Order not found for payment retry."));
  if (order.getStatus() != OrderStatus.PENDING) throw new IllegalStateException("Only a pending order can be retried.");
  Payment payment = payments.findByOrderIdForUpdate(order.getId()).orElseThrow(() -> new NoSuchElementException("Payment was not initiated for this order."));
  if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) throw new IllegalStateException("This order has already been paid.");

  // The case and payment row locks serialize retries. If an active attempt already
  // exists, keep it and let the customer resume that checkout instead of charging twice.
  PaymentAttempt active = ensureActiveRetryAttempt(c, order, payment);
  event.setMessage("A secure payment checkout is ready for order #" + order.getId() + ".");
  audit(c, "RETRY_CHECKOUT_READY", "Customer can resume the secure checkout from the order details page.");
 }

 private PaymentAttempt ensureActiveRetryAttempt(RecoveryCase c, Order order, Payment payment) throws Exception {
  PaymentAttempt active = paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(payment.getId(), PaymentStatus.PENDING)
          .stream().findFirst().orElse(null);
  if (active != null) return active;
  int attemptNumber = paymentAttempts.findByPaymentIdOrderByAttemptNumberAsc(payment.getId()).stream()
          .mapToInt(PaymentAttempt::getAttemptNumber).max().orElse(0) + 1;
  String idempotencyKey = "recovery-" + c.getId() + "-attempt-" + attemptNumber;
  JSONObject options = new JSONObject();
  options.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact());
  options.put("currency", c.getCurrency() == null ? "INR" : c.getCurrency());
  String receipt = "NC_REC_" + c.getId() + "_" + attemptNumber;
  options.put("receipt", receipt.substring(0, Math.min(40, receipt.length())));
  options.put("notes", new JSONObject().put("nexcart_order_id", order.getId()).put("recovery_case_id", c.getId()).put("idempotency_key", idempotencyKey));
  com.razorpay.Order razorpayOrder = razorpay.orders.create(options);
  active = paymentAttempts.save(PaymentAttempt.builder().payment(payment).attemptNumber(attemptNumber)
          .amount(payment.getAmount()).status(PaymentStatus.PENDING).razorpayOrderId(razorpayOrder.get("id"))
          .recoveryCaseId(c.getId()).idempotencyKey(idempotencyKey).build());
  payment.setPaymentStatus(PaymentStatus.PENDING);
  payment.setRazorpayOrderId(active.getRazorpayOrderId());
  payment.setRazorpayPaymentId(null);
  payment.setRazorpaySignature(null);
  payment.setFailureReason(null);
  payment.setPaidAt(null);
  payments.save(payment);
  audit(c, "PAYMENT_ATTEMPT_CREATED", "Attempt " + attemptNumber + " created for order #" + order.getId() + ".");
  return active;
 }

 public boolean retryCheckoutAvailable(Order order) {
  if (order == null || order.getStatus() != OrderStatus.PENDING) return false;
  return payments.findByOrderId(order.getId()).filter(p -> p.getPaymentStatus() != PaymentStatus.SUCCESS).isPresent()
          && cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
          .filter(c -> c.getStatus() == RecoveryStatus.ACTION_PENDING && c.getExecutedAction() == RecoveryActionType.RETRY_PAYMENT)
          .filter(c -> actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream()
                  .anyMatch(a -> a.getAction() == RecoveryActionType.RETRY_PAYMENT && a.isSuccessful())).isPresent();
 }

 public PaymentAttempt prepareRetryAttemptForOrder(Order order) throws Exception {
  if (order == null || order.getStatus() != OrderStatus.PENDING) throw new IllegalStateException("Payment retry is not available for this order.");
  RecoveryCase c = cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
          .filter(found -> found.getStatus() == RecoveryStatus.ACTION_PENDING && found.getExecutedAction() == RecoveryActionType.RETRY_PAYMENT)
          .filter(found -> actions.findByRecoveryCaseIdOrderByCreatedAtAsc(found.getId()).stream()
                  .anyMatch(a -> a.getAction() == RecoveryActionType.RETRY_PAYMENT && a.isSuccessful()))
          .orElseThrow(() -> new IllegalStateException("A payment retry is not approved for this order."));
  Payment payment = payments.findByOrderIdForUpdate(order.getId()).orElseThrow(() -> new NoSuchElementException("Payment was not initiated for this order."));
  if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) throw new IllegalStateException("This order has already been paid.");
  PaymentAttempt current = paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(payment.getId(), PaymentStatus.PENDING).stream().findFirst().orElse(null);
  if (current == null) {
   audit(c, "RETRY_REQUESTED", "Customer requested a new checkout for order #" + order.getId() + ".");
   current = ensureActiveRetryAttempt(c, order, payment);
  }
  audit(c, "CHECKOUT_INITIATED", "Checkout attempt " + current.getAttemptNumber() + " is available to the order owner.");
  return current;
 }
 public void markRecovered(Payment payment){ if(payment==null||payment.getOrder()==null||payment.getPaymentStatus()!= PaymentStatus.SUCCESS)return; cases.findFirstByOrderIdOrderByCreatedAtDesc(payment.getOrder().getId()).map(c -> getForUpdate(c.getId())).ifPresent(c->{if(latestOrderIsCancelled(payment.getOrder().getId())) { markCustomerCancelled(c, "Customer cancelled this order."); return; } if(c.getStatus()!=RecoveryStatus.RECOVERED){c.setStatus(RecoveryStatus.RECOVERED);c.setRecoveredAmount(payment.getAmount());c.setResolvedAt(LocalDateTime.now());cases.save(c);audit(c,"PAYMENT_SUCCESSFUL",payment.getRazorpayPaymentId());audit(c,"RECOVERY_COMPLETED","Recovered ₹"+payment.getAmount());}}); }
 public void recordRetryAttemptEnded(Payment payment, PaymentStatus status) {
  if (payment == null || payment.getOrder() == null || (status != PaymentStatus.FAILED && status != PaymentStatus.CANCELLED)) return;
  cases.findFirstByOrderIdOrderByCreatedAtDesc(payment.getOrder().getId())
          .filter(c -> c.getStatus() == RecoveryStatus.ACTION_PENDING && c.getExecutedAction() == RecoveryActionType.RETRY_PAYMENT)
          .ifPresent(c -> audit(c, status == PaymentStatus.CANCELLED ? "RETRY_ATTEMPT_CANCELLED" : "RETRY_ATTEMPT_FAILED",
                  "Payment attempt ended with status " + status + "; the pending order remains eligible for another approved checkout."));
 }
 public void recordCaptureReconciliation(Order order, PaymentAttempt attempt, String reason) {
  if (order == null) return;
  cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
          .ifPresent(c -> audit(c, "CAPTURE_REQUIRES_REFUND_RECONCILIATION", reason));
 }
 public Page<RecoveryCase> list(String mode, Pageable pageable){return "REAL".equalsIgnoreCase(mode)?cases.findBySimulated(false,pageable):"SIMULATED".equalsIgnoreCase(mode)?cases.findBySimulated(true,pageable):cases.findAll(pageable);}
 public Map<String,Object> metrics(String mode){
  boolean simulatedOnly="SIMULATED".equalsIgnoreCase(mode);
  boolean realOnly="REAL".equalsIgnoreCase(mode);
  RecoveryMetricsProjection displayed=simulatedOnly||realOnly
    ? cases.aggregateMetrics(simulatedOnly,RecoveryStatus.RECOVERED,RecoveryStatus.CUSTOMER_CANCELLED,TERMINAL_STATUSES)
    : cases.aggregateAllMetrics(RecoveryStatus.RECOVERED,RecoveryStatus.CUSTOMER_CANCELLED,TERMINAL_STATUSES);
  RecoveryMetricsProjection financial=simulatedOnly?displayed:realOnly?displayed:cases.aggregateMetrics(false,RecoveryStatus.RECOVERED,RecoveryStatus.CUSTOMER_CANCELLED,TERMINAL_STATUSES);
  RecoveryMetricsProjection simulated=simulatedOnly?displayed:cases.aggregateMetrics(true,RecoveryStatus.RECOVERED,RecoveryStatus.CUSTOMER_CANCELLED,TERMINAL_STATUSES);
  List<Object[]> distribution=simulatedOnly||realOnly?cases.countActionsByRecommendation(simulatedOnly):cases.countAllActionsByRecommendation();
  Map<String,Object> result=metricValues(financial,displayed,distribution);
  Map<String,Object> simulatedValues=simulatedOnly?result:metricValues(simulated,simulated,cases.countActionsByRecommendation(true));
  result.put("simulatedRevenueAtRisk",simulatedValues.get("revenueAtRisk"));
  result.put("simulatedRevenueRecovered",simulatedValues.get("revenueRecovered"));
  result.put("simulatedExpectedRecoverable",simulatedValues.get("expectedRecoverable"));
  result.put("simulatedRecoveredCases",simulatedValues.get("recoveredCases"));
  return result;
 }
 private Map<String,Object> metricValues(RecoveryMetricsProjection financial,RecoveryMetricsProjection displayed,List<Object[]> actionCounts){
  BigDecimal risk=financial.getRevenueAtRisk()==null?BigDecimal.ZERO:financial.getRevenueAtRisk();
  BigDecimal recovered=financial.getRevenueRecovered()==null?BigDecimal.ZERO:financial.getRevenueRecovered();
  BigDecimal expected=financial.getExpectedRecoverable()==null?BigDecimal.ZERO:financial.getExpectedRecoverable();
  BigDecimal eligibleRevenue=risk.add(recovered);
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("revenueAtRisk",risk);result.put("revenueRecovered",recovered);result.put("expectedRecoverable",expected);
  result.put("casesDetected",displayed.getTotalCases()==null?0:displayed.getTotalCases());
  result.put("recoveredCases",financial.getRecoveredCases()==null?0:financial.getRecoveredCases());
  result.put("recoveryRate",eligibleRevenue.signum()==0?BigDecimal.ZERO:recovered.multiply(BigDecimal.valueOf(100)).divide(eligibleRevenue,2,RoundingMode.HALF_UP));
  Map<String,Long> distribution=new LinkedHashMap<>();
  for(Object[] row:actionCounts) distribution.put(((RecoveryActionType)row[0]).name(),((Number)row[1]).longValue());
  result.put("actionDistribution",distribution);
  return result;
 }
 public RecoveryCase simulate(){ RecoveryCase c=cases.save(RecoveryCase.builder().amount(new BigDecimal("4999.00")).type(RecoveryType.PAYMENT_FAILED).failureReason("Temporary payment timeout (DEMO / SIMULATED)").status(RecoveryStatus.DETECTED).simulated(true).build()); audit(c,"RECOVERY_CASE_CREATED","DEMO / SIMULATED scenario"); return analyze(c.getId()); }
 public RecoveryCase get(Long id){return cases.findById(id).orElseThrow(()->new NoSuchElementException("Recovery case not found"));}
 private RecoveryCase getForUpdate(Long id){return cases.findByIdForUpdate(id).orElseThrow(()->new NoSuchElementException("Recovery case not found"));}
 public List<RecoveryAction> actionList(Long id){return actions.findByRecoveryCaseIdOrderByCreatedAtAsc(id);} public List<RecoveryAuditLog> auditList(Long id){return audits.findByRecoveryCaseIdOrderByCreatedAtAsc(id);}
 public Page<RecoveryAuditLog> auditPage(Long id, Pageable pageable){get(id); return audits.findByRecoveryCaseIdOrderByCreatedAtDesc(id,pageable);}
 public Optional<String> activePaymentLinkFor(Order order){
  if(order == null || latestOrderIsCancelled(order.getId())) return Optional.empty();
  return cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).filter(c -> c.getStatus() == RecoveryStatus.ACTION_PENDING).flatMap(c -> actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().filter(a -> a.getAction() == RecoveryActionType.CREATE_PAYMENT_LINK && a.isSuccessful() && a.getPaymentLink() != null).reduce((first, second) -> second).map(RecoveryAction::getPaymentLink));
 }
 public void syncRecoveryForOrder(Order order){
  if(order == null || latestOrderIsCancelled(order.getId())) return;
  cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).filter(c -> c.getStatus() == RecoveryStatus.ACTION_PENDING).ifPresent(c -> actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().filter(a -> a.getAction() == RecoveryActionType.CREATE_PAYMENT_LINK && a.isSuccessful()).reduce((first, second) -> second).ifPresent(this::syncPaymentLink));
 }
 public void cancelForOrder(Order order, String reason){
  if(order == null) return;
  cases.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).ifPresent(c -> {
   if(c.getStatus() == RecoveryStatus.RECOVERED || isCancellationRecorded(c)) return;
   markCustomerCancelled(c, reason);
  });
 }
 public void handlePaymentLinkPaid(String linkId, String paymentId, String referenceId){
  Optional<RecoveryAction> action = actions.findByRazorpayPaymentLinkId(linkId);
  if(action.isEmpty() && referenceId != null && referenceId.startsWith("REC-")) try { action = actions.findByRecoveryCaseIdOrderByCreatedAtAsc(Long.valueOf(referenceId.substring(4))).stream().filter(a -> a.getAction()==RecoveryActionType.CREATE_PAYMENT_LINK).reduce((first, second) -> second); } catch(NumberFormatException ignored) { }
  action.ifPresent(found -> completePaymentLink(found, paymentId));
 }
 private void syncPaymentLink(RecoveryAction action){
  try {
   com.razorpay.PaymentLink link = action.getRazorpayPaymentLinkId() == null ? findPaymentLinkByReference(action) : razorpay.paymentLink.fetch(action.getRazorpayPaymentLinkId());
   if(link == null) return;
   if(action.getRazorpayPaymentLinkId() == null) { action.setRazorpayPaymentLinkId(link.get("id")); actions.save(action); }
   if("paid".equalsIgnoreCase(link.get("status"))) completePaymentLink(action, link.get("payment_id"));
  } catch(Exception ignored) { }
 }
 private RecoveryCase executeSimulated(RecoveryCase c, RecoveryActionType action, RecoveryAction event){
  audit(c,"SIMULATED_ACTION_PENDING","SIMULATED action is pending; no Razorpay transaction is created.");
  if(action==RecoveryActionType.CREATE_PAYMENT_LINK){ event.setPaymentLink("simulated://payment-links/REC-"+c.getId()); event.setMessage("SIMULATED / DEMO payment link. No Razorpay payment is required or created."); audit(c,"SIMULATED_PAYMENT_LINK_CREATED",event.getPaymentLink()); }
  else if(action==RecoveryActionType.SEND_RECOVERY_MESSAGE){ event.setMessage("SIMULATED / DEMO recovery reminder. No customer notification was sent."); audit(c,"SIMULATED_RECOVERY_MESSAGE_GENERATED",event.getMessage()); }
  else audit(c,"SIMULATED_RETRY_PAYMENT","SIMULATED retry completed; no payment gateway was contacted.");
  event.setSuccessful(true); actions.save(event);
  c.setStatus(RecoveryStatus.RECOVERED); c.setRecoveredAmount(c.getAmount()); c.setResolvedAt(LocalDateTime.now());
  audit(c,"SIMULATED_PAYMENT_SUCCESSFUL","SIMULATED payment successful for ₹"+c.getAmount()+"; no Razorpay transaction occurred.");
  audit(c,"SIMULATED_RECOVERY_COMPLETED","SIMULATED recovered amount ₹"+c.getAmount()+"; excluded from REAL recovered revenue.");
  return cases.save(c);
 }
 private com.razorpay.PaymentLink findPaymentLinkByReference(RecoveryAction action) throws Exception {
  String reference = "REC-" + action.getRecoveryCase().getId();
  for(com.razorpay.PaymentLink link : razorpay.paymentLink.fetchAll()) if(reference.equals(link.get("reference_id"))) return link;
  return null;
 }
 private void completePaymentLink(RecoveryAction action, String razorpayPaymentId){ RecoveryCase c=action.getRecoveryCase(); if(c==null||c.getOrder()==null||latestOrderIsCancelled(c.getOrder().getId())) { if(c != null) markCustomerCancelled(c, "Customer cancelled this order."); return; } if(c.getStatus()==RecoveryStatus.RECOVERED)return; payments.findByOrderId(c.getOrder().getId()).ifPresent(payment->{payment.setPaymentStatus(PaymentStatus.SUCCESS);payment.setRazorpayPaymentId(razorpayPaymentId);payment.setPaidAt(LocalDateTime.now());payment.setFailureReason(null);Order latest=orders.findById(c.getOrder().getId()).orElse(c.getOrder());latest.setStatus(OrderStatus.CONFIRMED);orders.save(latest);markRecovered(payments.save(payment));}); }
 private boolean isCancelled(RecoveryCase c) { return c.getOrder() != null && latestOrderIsCancelled(c.getOrder().getId()); }
 private RecoveryContext buildContext(RecoveryCase c) {
  List<RecoveryAction> prior=actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()); Payment payment=c.getOrder()==null?null:payments.findByOrderId(c.getOrder().getId()).orElse(null);
  long minutes=c.getCreatedAt()==null?0:Math.max(0, java.time.Duration.between(c.getCreatedAt(), LocalDateTime.now()).toMinutes());
  return new RecoveryContext(c.getId(), c.getOrder()==null?null:c.getOrder().getId(), c.getAmount(), c.getCurrency(), payment==null?null:String.valueOf(payment.getPaymentStatus()), c.getFailureReason(), payment==null?0:1, c.getActionAttempts()==null?0:c.getActionAttempts(), prior.stream().map(a->a.getAction()+":"+(a.isSuccessful()?"COMPLETED":"PENDING")).toList(), String.valueOf(c.getStatus()), c.getOrder()==null?null:String.valueOf(c.getOrder().getStatus()), hasActivePaymentLink(c), minutes, c.isSimulated());
 }
 private boolean latestOrderIsCancelled(Long orderId) { return orders.findById(orderId).map(order -> order.getStatus() == OrderStatus.CANCELLED).orElse(true); }
 private boolean hasActivePaymentLink(RecoveryCase c) { return actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().anyMatch(action -> action.getAction() == RecoveryActionType.CREATE_PAYMENT_LINK && action.isSuccessful() && action.getPaymentLink() != null); }
 private boolean hasActiveRetryAttempt(RecoveryCase c) {
  if (c.getOrder() == null) return false;
  return payments.findByOrderId(c.getOrder().getId())
          .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PENDING)
          .map(payment -> paymentAttempts.existsByPaymentIdAndStatus(payment.getId(), PaymentStatus.PENDING))
          .orElse(false);
 }
 private boolean hasEquivalentAction(RecoveryCase c, RecoveryActionType action) {
  if (action != RecoveryActionType.CREATE_PAYMENT_LINK) return false;
  return (c.getStatus()==RecoveryStatus.ACTION_PENDING && c.getExecutedAction()==action)
          || actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().anyMatch(existing -> existing.getAction()==action && existing.isSuccessful());
 }
 private boolean isCancellationRecorded(RecoveryCase c) {
  return c.getStatus() == RecoveryStatus.CUSTOMER_CANCELLED
          || (c.getStatus() == RecoveryStatus.NO_ACTION && "REJECTED: order is CANCELLED".equals(c.getGuardrailResult()));
 }
 private RecoveryCase markCustomerCancelled(RecoveryCase c, String reason) {
  if(isCancellationRecorded(c)) return c;
  for(RecoveryAction action : actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId())) if(action.getRazorpayPaymentLinkId() != null) try { razorpay.paymentLink.cancel(action.getRazorpayPaymentLinkId()); audit(c,"PAYMENT_LINK_CANCELLED","Cancelled recovery payment link "+action.getRazorpayPaymentLinkId()); } catch(Exception ex) { audit(c,"PAYMENT_LINK_CANCELLATION_FAILED","Could not cancel recovery payment link "+action.getRazorpayPaymentLinkId()+": "+ex.getMessage()); }
  // The live original MySQL schema predates CUSTOMER_CANCELLED in this enum. NO_ACTION
  // is an existing terminal enum value; the explicit audit event retains the reason.
  c.setStatus(RecoveryStatus.NO_ACTION);
  c.setRecommendedAction(RecoveryActionType.NO_ACTION);
  c.setExecutedAction(RecoveryActionType.NO_ACTION);
  c.setGuardrailResult("REJECTED: order is CANCELLED");
  c.setDecisionReason(reason + " Recovery stopped; any payment link is unavailable.");
  c.setResolvedAt(LocalDateTime.now());
  audit(c,"CUSTOMER_CANCELLED",c.getDecisionReason());
  return cases.save(c);
 }
 private RecoveryActionType guard(RecoveryCase c, RecoveryActionType a){ if(a==null||c.getActionAttempts()>=maxAttempts)return RecoveryActionType.NO_ACTION; if(a==RecoveryActionType.RETRY_PAYMENT&&c.getActionAttempts()>=2)return RecoveryActionType.CREATE_PAYMENT_LINK; return a; }
 private void ensureNotTerminal(RecoveryCase c, String operation){ if(TERMINAL_STATUSES.contains(c.getStatus())) throw new IllegalStateException("Recovery case is terminal ("+c.getStatus()+") and cannot be "+operation+"."); }
 private void audit(RecoveryCase c,String type,String meta){audits.save(RecoveryAuditLog.builder().recoveryCase(c).eventType(type).metadata(meta).build());}
}
