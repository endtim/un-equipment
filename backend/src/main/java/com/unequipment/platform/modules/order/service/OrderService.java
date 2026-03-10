package com.unequipment.platform.modules.order.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.dto.MachineReservationRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
import com.unequipment.platform.modules.order.dto.SampleReservationRequest;
import com.unequipment.platform.modules.order.entity.AuditRecord;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.entity.SampleOrder;
import com.unequipment.platform.modules.order.entity.UsageRecord;
import com.unequipment.platform.modules.order.repository.AuditRecordRepository;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.repository.SampleOrderRepository;
import com.unequipment.platform.modules.order.repository.UsageRecordRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ReservationOrderRepository orderRepository;
    private final SampleOrderRepository sampleOrderRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final InstrumentService instrumentService;
    private final FinanceService financeService;
    private final MessageService messageService;
    private final OperationLogService operationLogService;

    @Transactional
    public ReservationOrder createMachineOrder(SysUser user, MachineReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        if (!request.getReservedEnd().isAfter(request.getReservedStart())) {
            throw new BizException("reserved time range is invalid");
        }
        if (orderRepository.countMachineConflict(instrument.getId(), request.getReservedEnd(), request.getReservedStart()) > 0) {
            throw new BizException("reservation time conflicts with existing order");
        }

        BigDecimal hours = BigDecimal.valueOf(Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal amount = nullSafe(instrument.getPriceInternal()).multiply(hours);
        financeService.ensureEnoughBalance(user, amount);

        ReservationOrder order = buildBaseOrder(user, instrument, "MACHINE");
        order.setReserveStart(request.getReservedStart());
        order.setReserveEnd(request.getReservedEnd());
        order.setReserveMinutes((int) Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes());
        order.setEstimatedAmount(amount);
        order.setFinalAmount(amount);
        order.setRemark(request.getRemark());
        orderRepository.insert(order);
        appendAudit(order, user, "PENDING", "SUBMIT", "Machine order submitted");
        messageService.send(user, "Reservation submitted", "Machine reservation submitted and waiting for audit.");
        operationLogService.save(user, "ORDER", "CREATE_MACHINE_ORDER", "orderId:" + order.getId());
        return getOrder(order.getId());
    }

    @Transactional
    public ReservationOrder createSampleOrder(SysUser user, SampleReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        BigDecimal amount = nullSafe(instrument.getPriceExternal()).multiply(BigDecimal.valueOf(request.getSampleCount()));
        financeService.ensureEnoughBalance(user, amount);

        ReservationOrder order = buildBaseOrder(user, instrument, "SAMPLE");
        order.setEstimatedAmount(amount);
        order.setFinalAmount(amount);
        order.setRemark(request.getRemark());
        orderRepository.insert(order);

        SampleOrder sampleOrder = new SampleOrder();
        sampleOrder.setOrderId(order.getId());
        sampleOrder.setSampleName(request.getSampleName());
        sampleOrder.setSampleCount(request.getSampleCount());
        sampleOrder.setReceiveStatus("WAITING_RECEIVE");
        sampleOrder.setTestingStatus("WAITING_RECEIVE");
        sampleOrder.setDangerFlag(0);
        sampleOrder.setCreateTime(LocalDateTime.now());
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.insert(sampleOrder);

        appendAudit(order, user, "PENDING", "SUBMIT", "Sample order submitted");
        messageService.send(user, "Sample reservation submitted", "Sample reservation submitted and waiting for audit.");
        operationLogService.save(user, "ORDER", "CREATE_SAMPLE_ORDER", "orderId:" + order.getId());
        return getOrder(order.getId());
    }

    @Transactional
    public ReservationOrder audit(Long orderId, SysUser auditor, AuditRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, auditor);
        if (!"PENDING_AUDIT".equals(order.getOrderStatus())) {
            throw new BizException("order is not pending audit");
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            order.setAuditStatus("PASS");
            order.setOrderStatus("MACHINE".equals(order.getOrderType()) ? "WAITING_USE" : "WAITING_RECEIVE");
            order.setApproveTime(LocalDateTime.now());
            messageService.send(simpleUser(order.getUserId()), "Order approved", "Your order was approved.");
        } else {
            order.setAuditStatus("REJECT");
            order.setOrderStatus("REJECTED");
            messageService.send(simpleUser(order.getUserId()), "Order rejected", "Your order was rejected.");
        }
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, auditor, order.getAuditStatus(), request.getAction().toUpperCase(), request.getComment());
        operationLogService.save(auditor, "ORDER", "AUDIT_ORDER", "orderId:" + orderId + ":" + request.getAction());
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder checkIn(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        if (!"MACHINE".equals(order.getOrderType()) || !"WAITING_USE".equals(order.getOrderStatus())) {
            throw new BizException("order cannot check in");
        }
        order.setOrderStatus("IN_USE");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);

        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        if (usage == null) {
            usage = new UsageRecord();
            usage.setOrderId(orderId);
            usage.setInstrumentId(order.getInstrumentId());
            usage.setOperatorUserId(order.getUserId());
            usage.setActualMinutes(0);
            usage.setAbnormalFlag(0);
            usage.setCreateTime(LocalDateTime.now());
        }
        usage.setCheckinTime(LocalDateTime.now());
        usage.setStartTime(LocalDateTime.now());
        usage.setUpdateTime(LocalDateTime.now());
        if (usage.getId() == null) {
            usageRecordRepository.insert(usage);
        } else {
            usageRecordRepository.update(usage);
        }

        appendAudit(order, operator, "PASS", "CHECK_IN", "Machine usage started");
        operationLogService.save(operator, "ORDER", "CHECK_IN_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder receiveSample(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        if (!"SAMPLE".equals(order.getOrderType()) || !"WAITING_RECEIVE".equals(order.getOrderStatus())) {
            throw new BizException("order cannot receive sample");
        }
        order.setOrderStatus("TESTING");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);

        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        if (sampleOrder == null) {
            throw new BizException("sample detail missing");
        }
        sampleOrder.setReceiveStatus("RECEIVED");
        sampleOrder.setTestingStatus("TESTING");
        sampleOrder.setReceivedTime(LocalDateTime.now());
        sampleOrder.setReceiverUserId(operator.getId());
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);
        appendAudit(order, operator, "PASS", "RECEIVE_SAMPLE", "Sample received");
        operationLogService.save(operator, "ORDER", "RECEIVE_SAMPLE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder uploadSampleResult(Long orderId, SysUser operator, OrderActionRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        if (!"SAMPLE".equals(order.getOrderType()) || !"TESTING".equals(order.getOrderStatus())) {
            throw new BizException("order cannot upload result");
        }
        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        if (sampleOrder == null) {
            throw new BizException("sample detail missing");
        }
        sampleOrder.setResultSummary(request.getComment());
        sampleOrder.setTestingStatus("WAITING_RESULT");
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);
        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "UPLOAD_RESULT", "Sample result uploaded");
        operationLogService.save(operator, "ORDER", "UPLOAD_SAMPLE_RESULT", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder finishMachine(Long orderId, SysUser operator, OrderActionRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        if (!"MACHINE".equals(order.getOrderType()) || !"IN_USE".equals(order.getOrderStatus())) {
            throw new BizException("order cannot finish");
        }
        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        if (usage == null) {
            throw new BizException("usage record missing");
        }
        usage.setEndTime(LocalDateTime.now());
        if (usage.getStartTime() != null) {
            usage.setActualMinutes((int) Duration.between(usage.getStartTime(), usage.getEndTime()).toMinutes());
        }
        usage.setAbnormalDesc(request.getComment());
        usage.setUpdateTime(LocalDateTime.now());
        usageRecordRepository.update(usage);

        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "FINISH_USE", "Machine usage finished");
        operationLogService.save(operator, "ORDER", "FINISH_MACHINE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder settle(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        if (!"WAITING_SETTLEMENT".equals(order.getOrderStatus())) {
            throw new BizException("order is not waiting settlement");
        }
        financeService.deductForOrder(order);
        order.setSettlementStatus("CONFIRMED");
        order.setPayStatus("PAID");
        order.setOrderStatus("COMPLETED");
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "SETTLE", "Order settled");
        messageService.send(simpleUser(order.getUserId()), "Order completed", "Your order has been settled.");
        operationLogService.save(operator, "ORDER", "SETTLE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder cancel(Long orderId, SysUser user) {
        ReservationOrder order = getOrder(orderId);
        if (!order.getUserId().equals(user.getId())) {
            throw new BizException("cannot cancel others order");
        }
        if ("COMPLETED".equals(order.getOrderStatus()) || "CANCELED".equals(order.getOrderStatus())) {
            throw new BizException("order cannot be canceled");
        }
        order.setOrderStatus("CANCELED");
        order.setCancelReason("Canceled by user");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, user, "PASS", "CANCEL", "Order canceled by user");
        messageService.send(user, "Order canceled", "Your order has been canceled.");
        operationLogService.save(user, "ORDER", "CANCEL_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    public List<Map<String, Object>> myOrders(SysUser user) {
        return orderRepository.findByUserId(user.getId()).stream().map(this::toView).collect(Collectors.toList());
    }

    public Map<String, Object> detail(Long orderId, SysUser user) {
        ReservationOrder order = getOrder(orderId);
        assertVisible(order, user);
        Map<String, Object> result = toView(order);
        result.put("auditRecords", auditRecordRepository.findByOrderId(orderId));
        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        if (usage != null) {
            result.put("usageRecord", usage);
        }
        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        if (sampleOrder != null) {
            result.put("sampleDetail", sampleOrder);
        }
        return result;
    }

    public List<Map<String, Object>> allOrders(SysUser user) {
        return orderRepository.findAll().stream()
            .filter(order -> canView(order, user))
            .map(this::toView)
            .collect(Collectors.toList());
    }

    public ReservationOrder getOrder(Long orderId) {
        ReservationOrder order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException("order not found");
        }
        return order;
    }

    private ReservationOrder buildBaseOrder(SysUser user, Instrument instrument, String orderType) {
        ReservationOrder order = new ReservationOrder();
        order.setOrderNo(orderType + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setOrderType(orderType);
        order.setUserId(user.getId());
        order.setInstrumentId(instrument.getId());
        order.setDepartmentId(user.getDepartmentId());
        order.setOwnerUserId(instrument.getOwnerUserId());
        order.setContactName(user.getRealName());
        order.setContactPhone(user.getPhone() == null ? "-" : user.getPhone());
        order.setOrderStatus("PENDING_AUDIT");
        order.setAuditStatus("PENDING");
        order.setPayStatus("UNPAID");
        order.setSettlementStatus("PENDING");
        order.setEstimatedAmount(BigDecimal.ZERO);
        order.setFinalAmount(BigDecimal.ZERO);
        order.setSource("WEB");
        order.setSubmitTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }

    private void appendAudit(ReservationOrder order, SysUser operator, String result, String action, String opinion) {
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setOrderId(order.getId());
        Integer maxNode = auditRecordRepository.findMaxNodeNo(order.getId());
        auditRecord.setNodeNo(maxNode == null ? 1 : maxNode + 1);
        auditRecord.setAuditorId(operator.getId());
        auditRecord.setAuditorRole(operator.getPrimaryRoleCode());
        auditRecord.setAuditResult(result);
        auditRecord.setAuditOpinion(action + (opinion == null ? "" : ": " + opinion));
        auditRecord.setAuditTime(LocalDateTime.now());
        auditRecord.setCreateTime(LocalDateTime.now());
        auditRecordRepository.insert(auditRecord);
    }

    private Map<String, Object> toView(ReservationOrder order) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("orderType", order.getOrderType());
        result.put("status", order.getOrderStatus());
        result.put("instrumentName", order.getInstrumentName());
        result.put("userName", order.getUserName());
        result.put("reservedStart", order.getReserveStart());
        result.put("reservedEnd", order.getReserveEnd());
        result.put("amount", order.getFinalAmount());
        result.put("remark", order.getRemark());
        result.put("createdAt", order.getCreateTime());
        return result;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private SysUser simpleUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }

    private void assertManageable(ReservationOrder order, SysUser user) {
        if (!canManage(order, user)) {
            throw new BizException("permission denied for this order");
        }
    }

    private void assertVisible(ReservationOrder order, SysUser user) {
        if (!canView(order, user)) {
            throw new BizException("cannot access this order");
        }
    }

    private boolean canManage(ReservationOrder order, SysUser user) {
        if (hasRole(user, "ADMIN")) {
            return true;
        }
        if (hasRole(user, "INSTRUMENT_OWNER")) {
            return order.getOwnerUserId() != null && order.getOwnerUserId().equals(user.getId());
        }
        if (hasRole(user, "DEPT_MANAGER")) {
            return order.getDepartmentId() != null && order.getDepartmentId().equals(user.getDepartmentId());
        }
        return false;
    }

    private boolean canView(ReservationOrder order, SysUser user) {
        if (order.getUserId() != null && order.getUserId().equals(user.getId())) {
            return true;
        }
        return canManage(order, user);
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
