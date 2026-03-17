package com.unequipment.platform.modules.order.task;

import com.unequipment.platform.modules.order.service.OrderService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Value("${app.order.pending-audit-timeout-minutes:1440}")
    private int pendingAuditTimeoutMinutes;

    @Value("${app.order.pending-audit-timeout-batch-size:200}")
    private int pendingAuditTimeoutBatchSize;

    @Scheduled(cron = "${app.order.pending-audit-timeout-cron:0 */5 * * * ?}")
    public void closeExpiredPendingAuditOrders() {
        if (pendingAuditTimeoutMinutes <= 0) {
            return;
        }
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(pendingAuditTimeoutMinutes);
            int closed = orderService.autoCloseExpiredPendingAudit(cutoffTime, pendingAuditTimeoutBatchSize);
            if (closed > 0) {
                log.info("自动关闭超时待审核订单完成: closed={}, cutoff={}", closed, cutoffTime);
            }
        } catch (Exception ex) {
            // 定时任务异常隔离，避免单次失败影响后续周期执行。
            log.error("自动关闭超时待审核订单失败", ex);
        }
    }
}
