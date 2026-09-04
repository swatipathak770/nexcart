package com.nexcart.recovery.service;

import com.nexcart.entity.*;
import com.nexcart.recovery.ai.AIRecoveryService;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.entity.*;
import com.nexcart.recovery.enums.*;
import com.nexcart.recovery.repository.*;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class RecoveryService {
 private static final Set<RecoveryStatus> TERMINAL_STATUSES = EnumSet.of(RecoveryStatus.RECOVERED, RecoveryStatus.CUSTOMER_CANCELLED, RecoveryStatus.EXHAUSTED, RecoveryStatus.FAILED, RecoveryStatus.NO_ACTION);
 private final RecoveryCaseRepository cases; private final RecoveryActionRepository actions; private final RecoveryAuditRepository audits; private final PaymentRepository payments; private final OrderRepository orders; private final AIRecoveryService ai; private final RazorpayClient razorpay;
 @Value("${recovery.max-attempts:3}") private int maxAttempts;

 public void detectFailedPayment(Payment payment) {
  if (payment == null || payment.getOrder() == null || payment.getPaymentStatus() == PaymentStatus.SUCCESS || latestOrderIsCancelled(payment.getOrder().getId())) return;
  RecoveryCase existing=cases.findFirstByOrderIdOrderByCreatedAtDesc(payment.getOrder().getId()).orElse(null);
  if(existing != null && existing.getStatus()!=RecoveryStatus.RECOVERED) return;
  RecoveryCase c=cases.save(RecoveryCase.builder().order(payment.getOrder()).user(payment.getOrder().getUser()).razorpayPaymentId(payment.getRazorpayPaymentId()).amount(payment.getAmount()).type(RecoveryType.PAYMENT_FAILED).failureReason(payment.getFailureReason()).status(RecoveryStatus.DETECTED).build());
  audit(c,"PAYMENT_FAILED", payment.getFailureReason()); audit(c,"RECOVERY_CASE_CREATED","Created from NexCart payment failure"); analyze(c.getId());
 }
 public RecoveryCase analyze(Long id) {
  RecoveryCase c=getForUpdate(id); if(isCancelled(c)) return markCustomerCancelled(c, "Customer cancelled this order."); ensureNotTerminal(c, "analyzed"); c.setStatus(RecoveryStatus.ANALYZING); c.setDecisionSource("DETERMINISTIC_FALLBACK"); audit(c,"RECOVERY_ANALYSIS_STARTED","Decision source: DETERMINISTIC_FALLBACK");
  RecoveryDecision d=ai.decide(c); RecoveryActionType allowed=guard(c,d.recommendedAction());
  c.setRecoveryProbability(d.recoveryProbability()); c.setExpectedRecoveryAmount(d.expectedRecoveryAmount()); c.setRecommendedAction(allowed); c.setDecisionReason(d.reason()); c.setConfidence(d.confidence()); c.setRiskLevel(d.riskLevel()); c.setRecoveryScore(d.recoveryProbability().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue()); c.setGuardrailResult(allowed==d.recommendedAction() ? "APPROVED: " + allowed : "REJECTED: " + d.recommendedAction() + "; allowed " + allowed); c.setStatus(allowed==RecoveryActionType.NO_ACTION?RecoveryStatus.NO_ACTION:RecoveryStatus.ACTION_RECOMMENDED); cases.save(c);
  audit(c,"DETERMINISTIC_DECISION_CREATED",d.recommendedAction()+" | source: "+c.getDecisionSource()+" | "+d.reason()); audit(c,allowed==d.recommendedAction()?"GUARDRAIL_APPROVED":"GUARDRAIL_REJECTED",c.getGuardrailResult()); return c;
 }
 public RecoveryCase execute(Long id) {
  RecoveryCase c=getForUpdate(id); if(isCancelled(c)) return markCustomerCancelled(c, "Customer cancelled this order."); ensureNotTerminal(c, "executed");
  RecoveryActionType action=guard(c,c.getRecommendedAction()); if(action==RecoveryActionType.NO_ACTION){c.setStatus(RecoveryStatus.NO_ACTION); c.setGuardrailResult("REJECTED: no safe recovery action available"); audit(c,"NO_ACTION","No safe recovery action available"); return cases.save(c);}
  if(hasEquivalentAction(c, action)) { audit(c,"ACTION_SKIPPED","Equivalent "+action+" action is already pending or completed; no duplicate action was executed."); return cases.save(c); }
  if(action==RecoveryActionType.CREATE_PAYMENT_LINK && hasActivePaymentLink(c)) { audit(c,"ACTION_SKIPPED","A secure payment link already exists; no duplicate Razorpay link was created."); return cases.save(c); }
  c.setStatus(RecoveryStatus.ACTION_PENDING); c.setActionAttempts(c.getActionAttempts()+1); c.setExecutedAction(action); RecoveryAction event=RecoveryAction.builder().recoveryCase(c).action(action).build(); audit(c,"ACTION_STARTED",action.name());
  if(c.isSimulated()) return executeSimulated(c, action, event);
  try { if(action==RecoveryActionType.CREATE_PAYMENT_LINK){ JSONObject o=new JSONObject(); o.put("amount",c.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact()); o.put("currency",c.getCurrency()); o.put("reference_id","REC-"+c.getId()); o.put("description","NexCart RecoverAI payment recovery"); com.razorpay.PaymentLink link=razorpay.paymentLink.create(o); event.setRazorpayPaymentLinkId(link.get("id")); event.setPaymentLink(link.get("short_url")); audit(c,"PAYMENT_LINK_CREATED",event.getPaymentLink()); }
   else if(action==RecoveryActionType.SEND_RECOVERY_MESSAGE){ event.setMessage("Your NexCart order worth ₹"+c.getAmount()+" is waiting. Complete payment using your secure payment link."); audit(c,"RECOVERY_MESSAGE_GENERATED",event.getMessage()); }
   else audit(c,"RETRY_PAYMENT","Retry approved; customer can restart the existing secure checkout.");
  event.setSuccessful(true); c.setStatus(RecoveryStatus.ACTION_PENDING);
  } catch(Exception ex){event.setFailureDetail("Razorpay action unavailable: "+ex.getMessage()); c.setStatus(c.getActionAttempts()>=maxAttempts?RecoveryStatus.EXHAUSTED:RecoveryStatus.FAILED); audit(c,"ACTION_FAILED",event.getFailureDetail());}
  actions.save(event); return cases.save(c);
 }
 public void markRecovered(Payment payment){ if(payment==null||payment.getOrder()==null||payment.getPaymentStatus()!= PaymentStatus.SUCCESS)return; cases.findFirstByOrderIdOrderByCreatedAtDesc(payment.getOrder().getId()).map(c -> getForUpdate(c.getId())).ifPresent(c->{if(latestOrderIsCancelled(payment.getOrder().getId())) { markCustomerCancelled(c, "Customer cancelled this order."); return; } if(c.getStatus()!=RecoveryStatus.RECOVERED){c.setStatus(RecoveryStatus.RECOVERED);c.setRecoveredAmount(payment.getAmount());c.setResolvedAt(LocalDateTime.now());cases.save(c);audit(c,"PAYMENT_SUCCESSFUL",payment.getRazorpayPaymentId());audit(c,"RECOVERY_COMPLETED","Recovered ₹"+payment.getAmount());}}); }
 public List<RecoveryCase> list(String mode){return "REAL".equalsIgnoreCase(mode)?cases.findBySimulated(false):"SIMULATED".equalsIgnoreCase(mode)?cases.findBySimulated(true):cases.findAll();}
 public RecoveryCase simulate(){ RecoveryCase c=cases.save(RecoveryCase.builder().amount(new BigDecimal("4999.00")).type(RecoveryType.PAYMENT_FAILED).failureReason("Temporary payment timeout (DEMO / SIMULATED)").status(RecoveryStatus.DETECTED).simulated(true).build()); audit(c,"RECOVERY_CASE_CREATED","DEMO / SIMULATED scenario"); return analyze(c.getId()); }
 public RecoveryCase get(Long id){return cases.findById(id).orElseThrow(()->new NoSuchElementException("Recovery case not found"));}
 private RecoveryCase getForUpdate(Long id){return cases.findByIdForUpdate(id).orElseThrow(()->new NoSuchElementException("Recovery case not found"));}
 public List<RecoveryAction> actionList(Long id){return actions.findByRecoveryCaseIdOrderByCreatedAtAsc(id);} public List<RecoveryAuditLog> auditList(Long id){return audits.findByRecoveryCaseIdOrderByCreatedAtAsc(id);}
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
   if(c.getStatus() == RecoveryStatus.RECOVERED || c.getStatus() == RecoveryStatus.CUSTOMER_CANCELLED) return;
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
 private boolean latestOrderIsCancelled(Long orderId) { return orders.findById(orderId).map(order -> order.getStatus() == OrderStatus.CANCELLED).orElse(true); }
 private boolean hasActivePaymentLink(RecoveryCase c) { return actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().anyMatch(action -> action.getAction() == RecoveryActionType.CREATE_PAYMENT_LINK && action.isSuccessful() && action.getPaymentLink() != null); }
 private boolean hasEquivalentAction(RecoveryCase c, RecoveryActionType action) { if(action != RecoveryActionType.RETRY_PAYMENT && action != RecoveryActionType.CREATE_PAYMENT_LINK) return false; return (c.getStatus()==RecoveryStatus.ACTION_PENDING && c.getExecutedAction()==action) || actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId()).stream().anyMatch(existing -> existing.getAction()==action && existing.isSuccessful()); }
 private RecoveryCase markCustomerCancelled(RecoveryCase c, String reason) { if(c.getStatus()==RecoveryStatus.CUSTOMER_CANCELLED) return c; for(RecoveryAction action : actions.findByRecoveryCaseIdOrderByCreatedAtAsc(c.getId())) if(action.getRazorpayPaymentLinkId() != null) try { razorpay.paymentLink.cancel(action.getRazorpayPaymentLinkId()); audit(c,"PAYMENT_LINK_CANCELLED","Cancelled recovery payment link "+action.getRazorpayPaymentLinkId()); } catch(Exception ex) { audit(c,"PAYMENT_LINK_CANCELLATION_FAILED","Could not cancel recovery payment link "+action.getRazorpayPaymentLinkId()+": "+ex.getMessage()); } c.setStatus(RecoveryStatus.CUSTOMER_CANCELLED); c.setRecommendedAction(RecoveryActionType.NO_ACTION); c.setExecutedAction(RecoveryActionType.NO_ACTION); c.setGuardrailResult("REJECTED: order is CANCELLED"); c.setDecisionReason(reason + " Recovery stopped; any payment link is unavailable."); c.setResolvedAt(LocalDateTime.now()); audit(c,"CUSTOMER_CANCELLED",c.getDecisionReason()); return cases.save(c); }
 private RecoveryActionType guard(RecoveryCase c, RecoveryActionType a){ if(a==null||c.getActionAttempts()>=maxAttempts)return RecoveryActionType.NO_ACTION; if(a==RecoveryActionType.RETRY_PAYMENT&&c.getActionAttempts()>=2)return RecoveryActionType.CREATE_PAYMENT_LINK; return a; }
 private void ensureNotTerminal(RecoveryCase c, String operation){ if(TERMINAL_STATUSES.contains(c.getStatus())) throw new IllegalStateException("Recovery case is terminal ("+c.getStatus()+") and cannot be "+operation+"."); }
 private void audit(RecoveryCase c,String type,String meta){audits.save(RecoveryAuditLog.builder().recoveryCase(c).eventType(type).metadata(meta).build());}
}
