package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final FinanceService financeService;

    /**
     * 当前用户资金账户概览。
     */
    @GetMapping("/account/me")
    public ApiResponse<?> me(@CurrentUser SysUser user) {
        return ApiResponse.success(financeService.accountInfo(user));
    }

    /**
     * 当前用户资金流水分页。
     */
    @GetMapping("/account/transactions")
    public ApiResponse<PageResponse<TransactionRecord>> transactions(
        @CurrentUser SysUser user,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageMyTransactions(user, pageNum, pageSize));
    }

    /**
     * 当前用户充值记录分页。
     */
    @GetMapping("/account/recharges")
    public ApiResponse<PageResponse<RechargeOrder>> recharges(
        @CurrentUser SysUser user,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageMyRecharges(user, pageNum, pageSize));
    }

    /**
     * 提交充值申请（待财务审核）。
     */
    @PostMapping("/recharges")
    public ApiResponse<?> submitRecharge(@CurrentUser SysUser user, @Valid @RequestBody RechargeRequest request) {
        return ApiResponse.success(financeService.submitRecharge(user, request));
    }
}
