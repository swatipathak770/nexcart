package com.nexcart.recovery.controller;
import com.nexcart.recovery.entity.*;
import com.nexcart.recovery.enums.RecoveryStatus;
import com.nexcart.recovery.service.RecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/recovery") @RequiredArgsConstructor
public class RecoveryController {
 private final RecoveryService service;
 @GetMapping("/cases") public List<Map<String,Object>> list(@RequestParam(defaultValue="ALL") String mode){return service.list(mode).stream().map(this::caseDto).toList();}
 @GetMapping("/cases/{id}") public Map<String,Object> detail(@PathVariable Long id){RecoveryCase c=service.get(id);Map<String,Object> r=caseDto(c);r.put("actions",service.actionList(id).stream().map(this::actionDto).toList());r.put("audit",service.auditList(id).stream().map(this::auditDto).toList());return r;}
 @PostMapping("/cases/{id}/analyze") public Map<String,Object> analyze(@PathVariable Long id){return caseDto(service.analyze(id));}
 @PostMapping("/cases/{id}/execute") public Map<String,Object> execute(@PathVariable Long id){return caseDto(service.execute(id));}
 @PostMapping("/simulate") public Map<String,Object> simulate(){return caseDto(service.simulate());}
 @GetMapping("/metrics") public Map<String,Object> metrics(@RequestParam(defaultValue="ALL") String mode){
  List<RecoveryCase> displayed="SIMULATED".equalsIgnoreCase(mode)?service.list("SIMULATED"):"REAL".equalsIgnoreCase(mode)?service.list("REAL"):service.list("ALL");
  List<RecoveryCase> financial="SIMULATED".equalsIgnoreCase(mode)?displayed:service.list("REAL");
  List<RecoveryCase> simulated=service.list("SIMULATED");
  Map<String,Object> r=metricsFor(financial, displayed); Map<String,Object> demo=metricsFor(simulated, simulated);
  r.put("simulatedRevenueAtRisk",demo.get("revenueAtRisk")); r.put("simulatedRevenueRecovered",demo.get("revenueRecovered")); r.put("simulatedExpectedRecoverable",demo.get("expectedRecoverable")); r.put("simulatedRecoveredCases",demo.get("recoveredCases")); return r;
 }
 private Map<String,Object> metricsFor(List<RecoveryCase> financial, List<RecoveryCase> displayed){List<RecoveryCase> eligible=financial.stream().filter(c->c.getStatus()!=RecoveryStatus.CUSTOMER_CANCELLED).toList();BigDecimal risk=eligible.stream().filter(c->c.getStatus()!=RecoveryStatus.RECOVERED).map(RecoveryCase::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal recovered=eligible.stream().map(c->c.getRecoveredAmount()==null?BigDecimal.ZERO:c.getRecoveredAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal expected=eligible.stream().filter(c->c.getStatus()!=RecoveryStatus.RECOVERED).map(c->c.getExpectedRecoveryAmount()==null?BigDecimal.ZERO:c.getExpectedRecoveryAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal eligibleRevenue=risk.add(recovered);Map<String,Object> r=new LinkedHashMap<>();r.put("revenueAtRisk",risk);r.put("revenueRecovered",recovered);r.put("expectedRecoverable",expected);r.put("casesDetected",displayed.size());r.put("recoveredCases",eligible.stream().filter(c->c.getStatus()==RecoveryStatus.RECOVERED).count());r.put("recoveryRate",eligibleRevenue.signum()==0?BigDecimal.ZERO:recovered.multiply(BigDecimal.valueOf(100)).divide(eligibleRevenue,2,RoundingMode.HALF_UP));r.put("actionDistribution",displayed.stream().filter(c->c.getRecommendedAction()!=null).collect(java.util.stream.Collectors.groupingBy(c->c.getRecommendedAction().name(),java.util.stream.Collectors.counting())));return r;}
 private Map<String,Object> caseDto(RecoveryCase c){Map<String,Object> r=new LinkedHashMap<>();r.put("id",c.getId());r.put("caseCode","REC-"+c.getId());r.put("orderId",c.getOrder()==null?null:c.getOrder().getId());r.put("orderStatus",c.getOrder()==null?null:c.getOrder().getStatus());r.put("amount",c.getAmount());r.put("type",c.getType());r.put("failureReason",c.getFailureReason());r.put("status",c.getStatus());r.put("recommendedAction",c.getRecommendedAction());r.put("executedAction",c.getExecutedAction());r.put("recoveryProbability",c.getRecoveryProbability());r.put("expectedRecoveryAmount",c.getExpectedRecoveryAmount());r.put("recoveredAmount",c.getRecoveredAmount());r.put("decisionReason",c.getDecisionReason());r.put("confidence",c.getConfidence());r.put("riskLevel",c.getRiskLevel());r.put("decisionSource",c.getDecisionSource());r.put("guardrailResult",c.getGuardrailResult());r.put("actionAttempts",c.getActionAttempts());r.put("simulated",c.isSimulated());r.put("createdAt",c.getCreatedAt());return r;}
 private Map<String,Object> actionDto(RecoveryAction action){Map<String,Object> r=new LinkedHashMap<>();r.put("id",action.getId());r.put("action",action.getAction());r.put("paymentLink",action.getPaymentLink());r.put("message",action.getMessage());r.put("successful",action.isSuccessful());r.put("failureDetail",action.getFailureDetail());r.put("createdAt",action.getCreatedAt());return r;}
 private Map<String,Object> auditDto(RecoveryAuditLog audit){Map<String,Object> r=new LinkedHashMap<>();r.put("id",audit.getId());r.put("eventType",audit.getEventType());r.put("metadata",audit.getMetadata());r.put("createdAt",audit.getCreatedAt());return r;}
}
