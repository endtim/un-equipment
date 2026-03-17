package com.unequipment.platform.modules.order.assembler;

import com.unequipment.platform.modules.order.entity.AuditRecord;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.entity.SampleOrder;
import com.unequipment.platform.modules.order.entity.UsageRecord;
import com.unequipment.platform.modules.order.vo.AuditRecordVO;
import com.unequipment.platform.modules.order.vo.OrderDetailVO;
import com.unequipment.platform.modules.order.vo.OrderSummaryVO;
import com.unequipment.platform.modules.order.vo.ReservedSlotVO;
import com.unequipment.platform.modules.order.vo.SampleOrderVO;
import com.unequipment.platform.modules.order.vo.UsageRecordVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
/**
 * 订单对象装配器：
 * 负责实体到前端 VO 的转换，避免在 Service 中散落字段映射逻辑。
 */
public class OrderAssembler {

    private static final DateTimeFormatter SLOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 订单摘要转换。
     */
    public OrderSummaryVO toSummary(ReservationOrder order) {
        OrderSummaryVO vo = new OrderSummaryVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getOrderStatus());
        vo.setInstrumentName(order.getInstrumentName());
        vo.setUserName(order.getUserName());
        vo.setReservedStart(order.getReserveStart());
        vo.setReservedEnd(order.getReserveEnd());
        vo.setAmount(order.getFinalAmount());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreateTime());
        return vo;
    }

    /**
     * 订单摘要批量转换。
     */
    public List<OrderSummaryVO> toSummaryList(List<ReservationOrder> list) {
        return list.stream().map(this::toSummary).collect(Collectors.toList());
    }

    /**
     * 订单详情转换：聚合摘要、审批轨迹、上机记录、送样记录。
     */
    public OrderDetailVO toDetail(ReservationOrder order, List<AuditRecord> auditRecords, UsageRecord usage, SampleOrder sample) {
        OrderDetailVO vo = new OrderDetailVO();
        OrderSummaryVO summary = toSummary(order);
        vo.setId(summary.getId());
        vo.setOrderNo(summary.getOrderNo());
        vo.setOrderType(summary.getOrderType());
        vo.setStatus(summary.getStatus());
        vo.setInstrumentName(summary.getInstrumentName());
        vo.setUserName(summary.getUserName());
        vo.setReservedStart(summary.getReservedStart());
        vo.setReservedEnd(summary.getReservedEnd());
        vo.setAmount(summary.getAmount());
        vo.setRemark(summary.getRemark());
        vo.setCreatedAt(summary.getCreatedAt());
        vo.setAuditRecords(auditRecords == null ? null : auditRecords.stream().map(this::toAuditRecord).collect(Collectors.toList()));
        vo.setUsageRecord(usage == null ? null : toUsageRecord(usage));
        vo.setSampleDetail(sample == null ? null : toSampleOrder(sample));
        return vo;
    }

    /**
     * 已占用时段转换：
     * 将跨天区间裁剪到目标自然日窗口，供前端日视图展示。
     */
    public ReservedSlotVO toReservedSlot(ReservationOrder order, LocalDateTime dayStart, LocalDateTime dayEnd) {
        LocalDateTime start = order.getReserveStart();
        LocalDateTime end = order.getReserveEnd();
        if (start == null) {
            start = dayStart;
        }
        if (end == null) {
            end = dayEnd;
        }
        if (start.isBefore(dayStart)) {
            start = dayStart;
        }
        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        String startText = start.format(SLOT_TIME_FORMATTER);
        String endText = end.format(SLOT_TIME_FORMATTER);

        ReservedSlotVO slot = new ReservedSlotVO();
        slot.setOrderId(order.getId());
        slot.setStartTime(start.toLocalTime());
        slot.setEndTime(end.toLocalTime());
        slot.setStatus(order.getOrderStatus());
        slot.setStatusLabel(orderStatusLabel(order.getOrderStatus()));
        slot.setText(startText + " - " + endText + " 已占用");
        slot.setReservedStart(start);
        slot.setReservedEnd(end);
        return slot;
    }

    private AuditRecordVO toAuditRecord(AuditRecord item) {
        AuditRecordVO vo = new AuditRecordVO();
        vo.setId(item.getId());
        vo.setNodeNo(item.getNodeNo());
        vo.setAuditorId(item.getAuditorId());
        vo.setAuditorRole(item.getAuditorRole());
        vo.setAuditResult(item.getAuditResult());
        vo.setAuditOpinion(item.getAuditOpinion());
        vo.setAuditTime(item.getAuditTime());
        return vo;
    }

    private UsageRecordVO toUsageRecord(UsageRecord item) {
        UsageRecordVO vo = new UsageRecordVO();
        vo.setId(item.getId());
        vo.setOrderId(item.getOrderId());
        vo.setInstrumentId(item.getInstrumentId());
        vo.setOperatorUserId(item.getOperatorUserId());
        vo.setCheckinTime(item.getCheckinTime());
        vo.setStartTime(item.getStartTime());
        vo.setEndTime(item.getEndTime());
        vo.setActualMinutes(item.getActualMinutes());
        vo.setAbnormalFlag(item.getAbnormalFlag());
        vo.setAbnormalDesc(item.getAbnormalDesc());
        vo.setOwnerConfirmUserId(item.getOwnerConfirmUserId());
        vo.setOwnerConfirmTime(item.getOwnerConfirmTime());
        return vo;
    }

    private SampleOrderVO toSampleOrder(SampleOrder item) {
        SampleOrderVO vo = new SampleOrderVO();
        vo.setId(item.getId());
        vo.setOrderId(item.getOrderId());
        vo.setSampleName(item.getSampleName());
        vo.setSampleCount(item.getSampleCount());
        vo.setSampleType(item.getSampleType());
        vo.setSampleSpec(item.getSampleSpec());
        vo.setTestRequirement(item.getTestRequirement());
        vo.setDangerFlag(item.getDangerFlag());
        vo.setDangerDesc(item.getDangerDesc());
        vo.setReceiveStatus(item.getReceiveStatus());
        vo.setReceivedTime(item.getReceivedTime());
        vo.setReceiverUserId(item.getReceiverUserId());
        vo.setTestingStatus(item.getTestingStatus());
        vo.setResultSummary(item.getResultSummary());
        return vo;
    }

    /**
     * 订单状态展示文案映射。
     */
    private String orderStatusLabel(String status) {
        if ("PENDING_AUDIT".equals(status)) {
            return "待审核";
        }
        if ("WAITING_USE".equals(status)) {
            return "待使用";
        }
        if ("WAITING_RECEIVE".equals(status)) {
            return "待接样";
        }
        if ("IN_USE".equals(status)) {
            return "使用中";
        }
        if ("TESTING".equals(status)) {
            return "检测中";
        }
        if ("WAITING_SETTLEMENT".equals(status)) {
            return "待结算";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("CANCELED".equals(status)) {
            return "已取消";
        }
        return "已占用";
    }
}
