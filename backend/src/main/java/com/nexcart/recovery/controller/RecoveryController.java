package com.nexcart.recovery.controller;
import com.nexcart.recovery.entity.*;
import com.nexcart.recovery.enums.RecoveryStatus;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.util.PageableFactory;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/recovery") @RequiredArgsConstructor
public class RecoveryController {
 private final RecoveryService service;
 @GetMapping("/cases") public PagedResponse<Map<String,Object>> list(@RequestParam(defaultValue="ALL") String mode, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="createdAt") String sortBy, @RequestParam(defaultValue="desc") String direction){Pageable pageable=PageableFactory.create(page,size,sortBy,direction,Set.of("id","createdAt","amount","status"));return PagedResponse.from(service.list(mode,pageable).map(this::caseDto));}
 @GetMapping("/cases/{id}") public Map<String,Object> detail(@PathVariable Long id){RecoveryCase c=service.get(id);Map<String,Object> r=caseDto(c);r.put("actions",service.actionList(id).stream().map(this::actionDto).toList());return r;}
 @GetMapping("/cases/{id}/audit") public PagedResponse<Map<String,Object>> audit(@PathVariable Long id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size){Pageable pageable=PageableFactory.create(page,size,"createdAt","desc",Set.of("createdAt"));return PagedResponse.from(service.auditPage(id,pageable).map(this::auditDto));}
 @PostMapping("/cases/{id}/analyze") public Map<String,Object> analyze(@PathVariable Long id){return caseDto(service.analyze(id));}
 @PostMapping("/cases/{id}/execute") public Map<String,Object> execute(@PathVariable Long id){return caseDto(service.execute(id));}
 @PostMapping("/simulate") public Map<String,Object> simulate(){return caseDto(service.simulate());}
 @GetMapping("/metrics") public Map<String,Object> metrics(@RequestParam(defaultValue="ALL") String mode){return service.metrics(mode);}
 private Map<String,Object> caseDto(RecoveryCase c){Map<String,Object> r=new LinkedHashMap<>();r.put("id",c.getId());r.put("caseCode","REC-"+c.getId());r.put("orderId",c.getOrder()==null?null:c.getOrder().getId());r.put("orderStatus",c.getOrder()==null?null:c.getOrder().getStatus());r.put("amount",c.getAmount());r.put("type",c.getType());r.put("failureReason",c.getFailureReason());r.put("status",c.getStatus());r.put("recommendedAction",c.getRecommendedAction());r.put("executedAction",c.getExecutedAction());r.put("recoveryProbability",c.getRecoveryProbability());r.put("expectedRecoveryAmount",c.getExpectedRecoveryAmount());r.put("recoveredAmount",c.getRecoveredAmount());r.put("decisionReason",c.getDecisionReason());r.put("confidence",c.getConfidence());r.put("riskLevel",c.getRiskLevel());r.put("decisionSource",c.getDecisionSource());r.put("guardrailResult",c.getGuardrailResult());r.put("actionAttempts",c.getActionAttempts());r.put("simulated",c.isSimulated());r.put("createdAt",c.getCreatedAt());return r;}
 private Map<String,Object> actionDto(RecoveryAction action){Map<String,Object> r=new LinkedHashMap<>();r.put("id",action.getId());r.put("action",action.getAction());r.put("paymentLink",action.getPaymentLink());r.put("message",action.getMessage());r.put("successful",action.isSuccessful());r.put("failureDetail",action.getFailureDetail());r.put("createdAt",action.getCreatedAt());return r;}
 private Map<String,Object> auditDto(RecoveryAuditLog audit){Map<String,Object> r=new LinkedHashMap<>();r.put("id",audit.getId());r.put("eventType",audit.getEventType());r.put("metadata",audit.getMetadata());r.put("createdAt",audit.getCreatedAt());return r;}
}
