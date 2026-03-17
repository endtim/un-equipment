package com.unequipment.platform.modules.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.repository.RechargeOrderRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.repository.TransactionRecordRepository;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RechargeOrderRepository rechargeOrderRepository;
    @Mock
    private TransactionRecordRepository transactionRecordRepository;
    @Mock
    private SettlementRecordRepository settlementRecordRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private SysUserRepository userRepository;
    @Mock
    private ReservationOrderRepository orderRepository;

    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        financeService = new FinanceService(
            accountRepository,
            rechargeOrderRepository,
            transactionRecordRepository,
            settlementRecordRepository,
            messageService,
            operationLogService,
            userRepository,
            orderRepository
        );
    }

    @Test
    void refundForOrder_shouldRejectWhenAlreadyRefunded() {
        ReservationOrder order = buildOrder(10L, 100L, BigDecimal.TEN, BigDecimal.TEN);
        SettlementRecord settlement = new SettlementRecord();
        settlement.setSettleStatus("REFUNDED");
        when(settlementRecordRepository.findByOrderId(10L)).thenReturn(settlement);

        BizException ex = assertThrows(BizException.class, () ->
            financeService.refundForOrder(order, "测试退款", new SysUser())
        );
        assertEquals(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, ex.getCode());
        verify(accountRepository, never()).increaseBalanceForRefund(any(), any(), any());
    }

    @Test
    void refundForOrder_shouldRejectWhenRefunding() {
        ReservationOrder order = buildOrder(11L, 101L, BigDecimal.TEN, BigDecimal.TEN);
        SettlementRecord settlement = new SettlementRecord();
        settlement.setSettleStatus("REFUNDING");
        when(settlementRecordRepository.findByOrderId(11L)).thenReturn(settlement);

        BizException ex = assertThrows(BizException.class, () ->
            financeService.refundForOrder(order, "测试退款", new SysUser())
        );
        assertEquals(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, ex.getCode());
        verify(accountRepository, never()).increaseBalanceForRefund(any(), any(), any());
    }

    @Test
    void refundForOrder_shouldRejectWhenAmountNonPositive() {
        ReservationOrder order = buildOrder(12L, 102L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(settlementRecordRepository.findByOrderId(12L)).thenReturn(null);
        when(accountRepository.findByUserId(102L)).thenReturn(buildAccount(500L, BigDecimal.valueOf(100)));

        BizException ex = assertThrows(BizException.class, () ->
            financeService.refundForOrder(order, "测试退款", new SysUser())
        );
        assertEquals(ErrorCodes.INVALID_REQUEST, ex.getCode());
        verify(accountRepository, never()).increaseBalanceForRefund(any(), any(), any());
    }

    @Test
    void refundForOrder_shouldSucceedWhenStatusValidAndAmountPositive() {
        ReservationOrder order = buildOrder(13L, 103L, BigDecimal.valueOf(88), BigDecimal.valueOf(66));
        SettlementRecord settlement = new SettlementRecord();
        settlement.setSettleStatus("CONFIRMED");
        when(settlementRecordRepository.findByOrderId(13L)).thenReturn(settlement);
        when(accountRepository.findByUserId(103L)).thenReturn(buildAccount(501L, BigDecimal.valueOf(1000)));
        when(accountRepository.increaseBalanceForRefund(eq(501L), eq(BigDecimal.valueOf(66)), any())).thenReturn(1);

        financeService.refundForOrder(order, "测试退款", new SysUser());

        verify(accountRepository).increaseBalanceForRefund(eq(501L), eq(BigDecimal.valueOf(66)), any());
        verify(settlementRecordRepository).updateStatusByOrderId(eq(13L), eq("REFUNDED"), any(), any(), eq(BigDecimal.valueOf(66)));
        verify(transactionRecordRepository).insert(any(TransactionRecord.class));
        verify(operationLogService).save(any(), eq("FINANCE"), eq("REFUND_ORDER"), any());
    }

    private ReservationOrder buildOrder(Long orderId, Long userId, BigDecimal estimated, BigDecimal finalAmount) {
        ReservationOrder order = new ReservationOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setInstrumentId(1L);
        order.setEstimatedAmount(estimated);
        order.setFinalAmount(finalAmount);
        return order;
    }

    private Account buildAccount(Long accountId, BigDecimal balance) {
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(balance);
        return account;
    }
}

