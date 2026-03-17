package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.finance.dto.SettlementRefundRequest;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.repository.SettlementAdminRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.vo.SettlementAdminVO;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.entity.AuditRecord;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.AuditRecordRepository;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementAdminService {
    private static final int MAX_PAGE_SIZE = 200;

    private final SettlementAdminRepository settlementAdminRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final ReservationOrderRepository orderRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final OrderService orderService;
    private final FinanceService financeService;
    private final MessageService messageService;
    private final OperationLogService operationLogService;

    /**
     * 结算管理分页：
     * - ADMIN 查看全量
     * - DEPT_MANAGER 仅查看本部门范围
     * 过滤条件与权限范围统一下推到 SQL，避免前端侧二次过滤。
     */
    public PageResponse<SettlementAdminVO> page(SysUser user, String keyword, String status,
                                                Long departmentId, Long orderId,
                                                LocalDateTime createStart, LocalDateTime createEnd,
                                                LocalDateTime settledStart, LocalDateTime settledEnd,
                                                int pageNum, int pageSize) {
        validateTimeRange(createStart, createEnd, "创建时间范围不合法，开始时间不能晚于结束时间");
        validateTimeRange(settledStart, settledEnd, "结算时间范围不合法，开始时间不能晚于结束时间");
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String roleCode = normalizeRole(user);
        int offset = Math.max(safePageNum - 1, 0) * safePageSize;
        Long scopeDepartmentId = user == null ? null : user.getDepartmentId();
        // 结算分页列表按角色下钻：ADMIN 全量，DEPT_MANAGER 仅本部门。
        return new PageResponse<>(
            settlementAdminRepository.findPage(keyword, status, departmentId, orderId, createStart, createEnd,
                settledStart, settledEnd, roleCode, scopeDepartmentId, offset, safePageSize),
            settlementAdminRepository.countPage(keyword, status, departmentId, orderId, createStart, createEnd,
                settledStart, settledEnd, roleCode, scopeDepartmentId),
            safePageNum,
            safePageSize
        );
    }

    /**
     * 结算详情：
     * 详情查询同样受角色范围约束，避免通过 ID 越权读取。
     */
    public SettlementAdminVO detail(SysUser user, Long id) {
        SettlementAdminVO detail = settlementAdminRepository.findDetail(id, normalizeRole(user),
            user == null ? null : user.getDepartmentId());
        if (detail == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        return detail;
    }

    /**
     * 结算执行入口：
     * - 仅允许 PENDING 的结算单执行
     * - 实际扣费与状态流转复用 OrderService.settle，确保单一结算口径
     */
    @Transactional
    public ReservationOrder settle(SysUser user, Long settlementId) {
        SettlementRecord settlement = settlementRecordRepository.findById(settlementId);
        if (settlement == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        // 仅 PENDING 可执行结算，避免已确认/已退款记录被重复处理。
        if (!"PENDING".equalsIgnoreCase(settlement.getSettleStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "当前结算状态不允许执行结算");
        }
        ReservationOrder order = orderRepository.findById(settlement.getOrderId());
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        assertManageable(user, order);
        return orderService.settle(order.getId(), user);
    }

    /**
     * 管理端退款执行：
     * - 允许 CONFIRMED / REFUND_PENDING 进入退款
     * - 先将结算单锁定为 REFUNDING（乐观锁）再执行资金退款
     * - 退款成功后同步更新订单支付与结算状态
     */
    @Transactional
    public ReservationOrder refund(SysUser user, Long settlementId, SettlementRefundRequest request) {
        SettlementRecord settlement = settlementRecordRepository.findById(settlementId);
        if (settlement == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        String currentStatus = settlement.getSettleStatus() == null ? "" : settlement.getSettleStatus().toUpperCase();
        // 退款状态机入口：仅允许 CONFIRMED 或 REFUND_PENDING 进入退款执行。
        if (!"CONFIRMED".equals(currentStatus) && !"REFUND_PENDING".equals(currentStatus)) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "仅已确认或退款中的结算单可退款");
        }
        ReservationOrder order = orderRepository.findById(settlement.getOrderId());
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        assertManageable(user, order);
        if (!"PAID".equalsIgnoreCase(order.getPayStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "仅已支付订单可退款");
        }

        String reason = request == null || request.getComment() == null || request.getComment().trim().isEmpty()
            ? "管理员发起退款"
            : request.getComment().trim();
        // 通过“按当前状态更新”做乐观锁，防止并发重复退款。
        if ("CONFIRMED".equals(currentStatus)) {
            int locked = settlementRecordRepository.updateStatusByIdWhenCurrent(
                settlementId, "CONFIRMED", "REFUNDING", user == null ? null : user.getId(), LocalDateTime.now()
            );
            if (locked <= 0) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
            }
        } else {
            int locked = settlementRecordRepository.updateStatusByIdWhenCurrent(
                settlementId, "REFUND_PENDING", "REFUNDING", user == null ? null : user.getId(), LocalDateTime.now()
            );
            if (locked <= 0) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
            }
        }
        // 资金侧退款成功后再更新订单状态，保持账务与订单一致。
        financeService.refundForOrder(order, reason, user);
        order.setPayStatus("REFUNDED");
        order.setSettlementStatus("REFUNDED");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, user, "REFUND", reason);
        messageService.send(simpleUser(order.getUserId()), "订单退款通知", "您的订单已完成退款，金额已原路退回或回充余额。");
        operationLogService.save(user, "FINANCE", "REFUND_ORDER", "orderId:" + order.getId() + ",settlementId:" + settlementId);
        return order;
    }

    /**
     * 管理端发起退款申请：
     * - 仅做状态推进（CONFIRMED -> REFUND_PENDING）与审计记录
     * - 不直接动账，便于与“执行退款”流程分离
     */
    @Transactional
    public ReservationOrder requestRefund(SysUser user, Long settlementId, SettlementRefundRequest request) {
        SettlementRecord settlement = settlementRecordRepository.findById(settlementId);
        if (settlement == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        if (!"CONFIRMED".equalsIgnoreCase(settlement.getSettleStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "仅已确认结算单可发起退款");
        }
        ReservationOrder order = orderRepository.findById(settlement.getOrderId());
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        assertManageable(user, order);
        // 申请退款仅做“状态推进 + 审计记录”，不直接动账。
        int changed = settlementRecordRepository.updateStatusByIdWhenCurrent(
            settlementId, "CONFIRMED", "REFUND_PENDING", user == null ? null : user.getId(), LocalDateTime.now()
        );
        if (changed <= 0) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
        }
        String reason = request == null || request.getComment() == null || request.getComment().trim().isEmpty()
            ? "管理员发起退款申请"
            : request.getComment().trim();
        appendAudit(order, user, "REFUND_REQUEST", reason);
        operationLogService.save(user, "FINANCE", "REQUEST_REFUND", "orderId:" + order.getId() + ",settlementId:" + settlementId);
        return order;
    }

    private String normalizeRole(SysUser user) {
        if (hasRole(user, "ADMIN")) {
            return "ADMIN";
        }
        if (hasRole(user, "DEPT_MANAGER")) {
            return "DEPT_MANAGER";
        }
        if (user == null || user.getPrimaryRoleCode() == null || user.getPrimaryRoleCode().trim().isEmpty()) {
            return "INTERNAL_USER";
        }
        return user.getPrimaryRoleCode().trim().toUpperCase();
    }

    /**
     * 结算管理权限：
     * - ADMIN：全量
     * - DEPT_MANAGER：仅本部门订单
     */
    private void assertManageable(SysUser user, ReservationOrder order) {
        if (user == null) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
        }
        String role = normalizeRole(user);
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("DEPT_MANAGER".equals(role)
            && order.getDepartmentId() != null
            && order.getDepartmentId().equals(user.getDepartmentId())) {
            return;
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权操作该结算单");
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return RoleAuthUtils.hasRole(user, roleCode);
    }

    /**
     * 追加订单审计节点，保证退款申请/退款执行可追溯。
     */
    private void appendAudit(ReservationOrder order, SysUser operator, String action, String reason) {
        AuditRecord record = new AuditRecord();
        Integer maxNode = auditRecordRepository.findMaxNodeNo(order.getId());
        record.setOrderId(order.getId());
        record.setNodeNo(maxNode == null ? 1 : maxNode + 1);
        record.setAuditorId(operator == null ? null : operator.getId());
        record.setAuditorRole(operator == null ? null : operator.getPrimaryRoleCode());
        record.setAuditResult("PASS");
        record.setAuditOpinion(action + (reason == null || reason.trim().isEmpty() ? "" : ": " + reason.trim()));
        record.setAuditTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        auditRecordRepository.insert(record);
    }

    private SysUser simpleUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end, String message) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, message);
        }
    }
}
