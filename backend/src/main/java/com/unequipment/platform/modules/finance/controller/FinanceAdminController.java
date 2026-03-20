package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.finance.dto.FinanceAnomalyHandleRequest;
import com.unequipment.platform.modules.finance.dto.FinanceBudgetSaveRequest;
import com.unequipment.platform.modules.finance.dto.FinanceExpenseCreateRequest;
import com.unequipment.platform.modules.finance.entity.FinanceBudget;
import com.unequipment.platform.modules.finance.entity.FinanceExpense;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import com.unequipment.platform.modules.finance.vo.FinanceBudgetWarningVO;
import com.unequipment.platform.modules.finance.vo.FinanceDetailVO;
import com.unequipment.platform.modules.finance.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class FinanceAdminController {
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final FinanceService financeService;

    /**
     * 充值审核单分页查询。
     * 角色范围与过滤条件由后端统一处理，前端不再做权限筛选。
     */
    @GetMapping("/recharges/page")
    public ApiResponse<PageResponse<RechargeOrder>> rechargePage(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long auditUserId,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageRecharges(
            user, keyword, status, userId, auditUserId, minAmount, maxAmount, startTime, endTime, pageNum, pageSize
        ));
    }

    /**
     * 充值审核单导出（CSV）。
     * 导出口径与分页查询保持一致，避免“看得到导不出/导出范围不一致”。
     */
    @GetMapping(value = "/recharges/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportRechargeCsv(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long auditUserId,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        String csv = financeService.exportRechargesCsv(
            user, keyword, status, userId, auditUserId, minAmount, maxAmount, startTime, endTime
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recharges.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }

    /**
     * 对账总览：返回充值、结算、退款与异常类汇总指标。
     */
    @GetMapping("/reconciliation/overview")
    public ApiResponse<Map<String, Object>> reconciliationOverview(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
        LocalDateTime startDateTime = parseDateTimeParam(startTime, true, "开始时间格式不正确");
        LocalDateTime endDateTime = parseDateTimeParam(endTime, false, "结束时间格式不正确");
        return ApiResponse.success(financeService.reconciliationOverview(user, startDateTime, endDateTime));
    }

    private LocalDateTime parseDateTimeParam(String value, boolean startOfDay, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate parsedDate = LocalDate.parse(trimmed, ISO_DATE);
                return startOfDay ? parsedDate.atStartOfDay() : parsedDate.atTime(23, 59, 59);
            } catch (DateTimeParseException ex) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, errorMessage);
            }
        }
    }

    /**
     * 对账异常分页：按异常类型与时间窗口查询。
     */
    @GetMapping("/reconciliation/anomalies")
    public ApiResponse<PageResponse<FinanceAnomalyVO>> reconciliationAnomalies(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.reconciliationAnomalies(
            user, type, startTime, endTime, pageNum, pageSize
        ));
    }

    @PostMapping("/recharges/{id}/audit")
    public ApiResponse<?> audit(@PathVariable Long id, @Valid @RequestBody RechargeAuditRequest request,
                                @CurrentUser SysUser user) {
        return ApiResponse.success(financeService.auditRecharge(id, request, user));
    }

    @PostMapping("/reconciliation/anomalies/handle")
    public ApiResponse<?> handleAnomaly(@CurrentUser SysUser user,
                                        @Valid @RequestBody FinanceAnomalyHandleRequest request) {
        financeService.handleReconciliationAnomaly(user, request);
        return ApiResponse.success(null);
    }

    /**
     * 经费明细分页：汇总充值、收费、退款、维护支出。
     */
    @GetMapping("/details")
    public ApiResponse<PageResponse<FinanceDetailVO>> financeDetails(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String inoutType,
        @RequestParam(required = false) Long instrumentId,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageFinanceDetails(
            user, keyword, bizType, inoutType, instrumentId, departmentId, startTime, endTime, pageNum, pageSize
        ));
    }

    /**
     * 经费明细导出（CSV）。
     */
    @GetMapping(value = "/details/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportFinanceDetails(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String inoutType,
        @RequestParam(required = false) Long instrumentId,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        String csv = financeService.exportFinanceDetailsCsv(
            user, keyword, bizType, inoutType, instrumentId, departmentId, startTime, endTime
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance-details.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }

    /**
     * 登记维护支出。
     */
    @PostMapping("/expenses")
    public ApiResponse<FinanceExpense> createExpense(@CurrentUser SysUser user,
                                                     @Valid @RequestBody FinanceExpenseCreateRequest request) {
        return ApiResponse.success(financeService.createFinanceExpense(user, request));
    }

    @GetMapping("/budgets")
    public ApiResponse<PageResponse<FinanceBudget>> budgetPage(
        @CurrentUser SysUser user,
        @RequestParam(required = false) Integer budgetYear,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Long instrumentId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageBudgets(
            user, budgetYear, departmentId, instrumentId, status, pageNum, pageSize
        ));
    }

    @PostMapping("/budgets")
    public ApiResponse<FinanceBudget> saveBudget(@CurrentUser SysUser user,
                                                 @Valid @RequestBody FinanceBudgetSaveRequest request) {
        return ApiResponse.success(financeService.saveBudget(user, request));
    }

    @GetMapping("/budget-warnings")
    public ApiResponse<java.util.List<FinanceBudgetWarningVO>> budgetWarnings(
        @CurrentUser SysUser user,
        @RequestParam(required = false) Integer budgetYear) {
        return ApiResponse.success(financeService.budgetWarnings(user, budgetYear));
    }
}
