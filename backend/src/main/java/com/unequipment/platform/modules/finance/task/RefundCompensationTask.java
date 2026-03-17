package com.unequipment.platform.modules.finance.task;

import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompensationTask {

    private final SettlementRecordRepository settlementRecordRepository;
    private final ReservationOrderRepository orderRepository;
    private final OperationLogService operationLogService;
    private final FinanceService financeService;

    @Value("${app.finance.refund-compensation-timeout-minutes:30}")
    private int timeoutMinutes;

    @Value("${app.finance.refund-compensation-batch-size:100}")
    private int batchSize;

    /**
     * 退款补偿扫描：
     * 1) 扫描长时间停留在 REFUNDING 的记录
     * 2) 若订单已退款，则自动将结算记录推进到 REFUNDED
     * 3) 若订单仍为已支付，则回退到 REFUND_PENDING 等待人工重试
     */
    @Scheduled(cron = "${app.finance.refund-compensation-cron:0 */10 * * * ?}")
    public void compensateRefundingSettlements() {
        int safeBatch = Math.max(batchSize, 1);
        int safeTimeout = Math.max(timeoutMinutes, 1);
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(safeTimeout);
        List<SettlementRecord> list = settlementRecordRepository.findRefundingTimeout(beforeTime, safeBatch);
        if (list == null || list.isEmpty()) {
            return;
        }
        int repaired = 0;
        int rolledBack = 0;
        for (SettlementRecord settlement : list) {
            try {
                ReservationOrder order = orderRepository.findById(settlement.getOrderId());
                if (order == null) {
                    log.warn("退款补偿发现孤儿结算单，settlementId={}, orderId={}", settlement.getId(), settlement.getOrderId());
                    continue;
                }
                if ("REFUNDED".equalsIgnoreCase(order.getPayStatus())) {
                    settlementRecordRepository.updateStatusById(
                        settlement.getId(),
                        "REFUNDED",
                        null,
                        LocalDateTime.now()
                    );
                    repaired++;
                    operationLogService.save(null, "FINANCE", "REFUND_COMPENSATE_REPAIR",
                        "settlementId:" + settlement.getId() + ",orderId:" + order.getId());
                } else if ("PAID".equalsIgnoreCase(order.getPayStatus())) {
                    settlementRecordRepository.updateStatusById(
                        settlement.getId(),
                        "REFUND_PENDING",
                        null,
                        settlement.getSettledTime()
                    );
                    rolledBack++;
                    operationLogService.save(null, "FINANCE", "REFUND_COMPENSATE_ROLLBACK",
                        "settlementId:" + settlement.getId() + ",orderId:" + order.getId());
                }
            } catch (Exception ex) {
                log.warn("退款补偿处理失败 settlementId={}", settlement.getId(), ex);
            }
        }
        if (repaired > 0 || rolledBack > 0) {
            financeService.notifyFinanceDataChanged();
            log.info("退款补偿完成，repair={}, rollback={}, timeoutMinutes={}, batch={}", repaired, rolledBack, safeTimeout, safeBatch);
        }
    }
}
