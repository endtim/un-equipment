package com.unequipment.platform.modules.order.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderSummaryVO {

    private List<AuditRecordVO> auditRecords;
    private UsageRecordVO usageRecord;
    private SampleOrderVO sampleDetail;
}
