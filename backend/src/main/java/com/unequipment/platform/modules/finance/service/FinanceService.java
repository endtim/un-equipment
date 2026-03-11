package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.repository.RechargeOrderRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.repository.TransactionRecordRepository;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final AccountRepository accountRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final MessageService messageService;
    private final OperationLogService operationLogService;
    private final SysUserRepository userRepository;

    public Map<String, Object> accountInfo(SysUser user) {
        Account account = getAccount(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("id", account.getId());
        result.put("balance", account.getBalance());
        result.put("status", account.getStatus());
        result.put("totalRecharge", account.getTotalRecharge());
        result.put("totalConsume", account.getTotalConsume());
        result.put("transactions", transactionRecordRepository.findByUserId(user.getId()));
        result.put("recharges", rechargeOrderRepository.findByUserId(user.getId()));
        return result;
    }

    @Transactional
    public RechargeOrder submitRecharge(SysUser user, RechargeRequest request) {
        RechargeOrder order = new RechargeOrder();
        order.setRechargeNo(BizNoGenerator.next("RCG"));
        order.setUserId(user.getId());
        order.setAmount(request.getAmount());
        order.setPayMethod("OFFLINE");
        order.setVoucherUrl(request.getProofUrl());
        order.setStatus("PENDING");
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        rechargeOrderRepository.insert(order);
        operationLogService.save(user, "FINANCE", "SUBMIT_RECHARGE", "recharge:" + order.getRechargeNo());
        return order;
    }

    @Transactional
    public RechargeOrder auditRecharge(Long id, RechargeAuditRequest request, SysUser auditor) {
        RechargeOrder order = rechargeOrderRepository.findById(id);
        if (order == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "recharge order not found");
        }
        if (!canManageUserResource(order.getUserId(), auditor)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied for this recharge order");
        }
        SysUser notifyUser = new SysUser();
        notifyUser.setId(order.getUserId());
        LocalDateTime now = LocalDateTime.now();
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            Account account = getAccount(order.getUserId());
            BigDecimal before = account.getBalance();
            int updated = accountRepository.increaseBalanceForRecharge(account.getId(), order.getAmount(), now);
            if (updated <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "account update failed");
            }
            recordTransaction(order.getUserId(), null, order.getId(), order.getAmount(), "RECHARGE", "IN",
                before, before.add(order.getAmount()), "Recharge approved");
            order.setStatus("PASS");
            messageService.send(notifyUser, "Recharge approved", "Your recharge request has been approved.");
        } else {
            order.setStatus("REJECT");
            order.setRemark(request.getComment());
            messageService.send(notifyUser, "Recharge rejected", "Your recharge request was rejected.");
        }

        order.setAuditUserId(auditor == null ? null : auditor.getId());
        order.setAuditTime(now);
        order.setUpdateTime(now);
        int changed = rechargeOrderRepository.updateIfPending(
            order.getId(),
            order.getStatus(),
            order.getRemark(),
            order.getAuditUserId(),
            order.getAuditTime(),
            order.getUpdateTime()
        );
        if (changed <= 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "recharge order already processed");
        }
        operationLogService.save(auditor, "FINANCE", "AUDIT_RECHARGE", "rechargeId:" + id + ":" + order.getStatus());
        return rechargeOrderRepository.findById(id);
    }

    @Transactional
    public void deductForOrder(ReservationOrder order) {
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        BigDecimal before = account.getBalance();
        int changed = accountRepository.decreaseBalanceForConsume(account.getId(), amount, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient balance");
        }

        recordTransaction(order.getUserId(), order.getId(), null, amount.negate(), "CONSUME", "OUT",
            before, before.subtract(amount), "Order settlement");

        SettlementRecord settlement = new SettlementRecord();
        settlement.setSettlementNo(BizNoGenerator.next("SET"));
        settlement.setOrderId(order.getId());
        settlement.setUserId(order.getUserId());
        settlement.setInstrumentId(order.getInstrumentId());
        settlement.setBillType("INTERNAL");
        settlement.setPriceDesc("Auto generated from reservation order");
        settlement.setEstimatedAmount(nullSafe(order.getEstimatedAmount()));
        settlement.setDiscountAmount(BigDecimal.ZERO);
        settlement.setFinalAmount(amount);
        settlement.setSettleStatus("CONFIRMED");
        settlement.setSettledTime(LocalDateTime.now());
        settlement.setCreateTime(LocalDateTime.now());
        settlementRecordRepository.insert(settlement);
        operationLogService.save(null, "FINANCE", "SETTLE_ORDER", "orderId:" + order.getId());
    }

    @Transactional
    public void refundForOrder(ReservationOrder order, String reason) {
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        BigDecimal before = account.getBalance();
        int changed = accountRepository.increaseBalanceForRefund(account.getId(), amount, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "refund failed");
        }
        recordTransaction(order.getUserId(), order.getId(), null, amount, "REFUND", "IN",
            before, before.add(amount), reason);
        operationLogService.save(null, "FINANCE", "REFUND_ORDER", "orderId:" + order.getId());
    }

    public List<RechargeOrder> listRecharges(SysUser requester) {
        return rechargeOrderRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .collect(Collectors.toList());
    }

    public Account getAccount(SysUser user) {
        return getAccount(user.getId());
    }

    public Account getAccount(Long userId) {
        Account account = accountRepository.findByUserId(userId);
        if (account == null) {
            throw new BizException(ErrorCodes.FINANCE_ACCOUNT_NOT_FOUND, "account not found");
        }
        return account;
    }

    public void ensureEnoughBalance(SysUser user, BigDecimal amount) {
        if (getAccount(user).getBalance().compareTo(amount) < 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient balance");
        }
    }

    private void recordTransaction(Long userId, Long orderId, Long rechargeId, BigDecimal amount, String txnType,
                                   String inoutType, BigDecimal before, BigDecimal after, String remark) {
        TransactionRecord record = new TransactionRecord();
        record.setTxnNo(BizNoGenerator.next("TXN"));
        record.setUserId(userId);
        record.setOrderId(orderId);
        record.setRechargeId(rechargeId);
        record.setTxnType(txnType);
        record.setInoutType(inoutType);
        record.setAmount(amount);
        record.setBalanceBefore(before);
        record.setBalanceAfter(after);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        transactionRecordRepository.insert(record);
    }

    private BigDecimal settlementAmount(ReservationOrder order) {
        BigDecimal finalAmount = order.getFinalAmount();
        if (finalAmount != null && finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return finalAmount;
        }
        return nullSafe(order.getEstimatedAmount());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean canManageUserResource(Long targetUserId, SysUser operator) {
        if (operator == null) {
            return false;
        }
        if (hasRole(operator, "ADMIN")) {
            return true;
        }
        if (targetUserId != null && targetUserId.equals(operator.getId())) {
            return true;
        }
        if (hasRole(operator, "DEPT_MANAGER")) {
            SysUser target = userRepository.findById(targetUserId);
            return target != null && target.getDepartmentId() != null
                && target.getDepartmentId().equals(operator.getDepartmentId());
        }
        return false;
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
