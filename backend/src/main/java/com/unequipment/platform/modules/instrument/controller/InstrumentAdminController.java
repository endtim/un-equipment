package com.unequipment.platform.modules.instrument.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.instrument.dto.InstrumentSaveRequest;
import com.unequipment.platform.modules.instrument.dto.OpenRuleSaveRequest;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instruments")
@RequiredArgsConstructor
public class InstrumentAdminController {

    private final InstrumentService instrumentService;

    @GetMapping
    public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.page(keyword, categoryId, status, pageNum, pageSize, operator));
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories(@CurrentUser SysUser operator) {
        // Full list endpoint: used by instrument/open-rule dropdowns.
        instrumentService.assertAdminOrInstrumentManager(operator);
        return ApiResponse.success(instrumentService.categories());
    }

    @GetMapping("/categories/page")
    public ApiResponse<PageResponse<InstrumentCategory>> categoryPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.pageCategories(pageNum, pageSize, operator));
    }

    /**
     * 开放规则分页查询入口。
     * weekDay 参数支持“单天命中 week_day 或多天命中 week_days”两种存量数据口径。
     */
    @GetMapping("/open-rules")
    public ApiResponse<PageResponse<InstrumentOpenRule>> openRules(
        @RequestParam(required = false) Long instrumentId,
        @RequestParam(required = false) Integer weekDay,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.pageOpenRules(
            instrumentId, weekDay, status, pageNum, pageSize, operator
        ));
    }

    @GetMapping("/attachments")
    public ApiResponse<?> attachments(@CurrentUser SysUser operator) {
        // Full list endpoint: kept for internal selection/legacy integration.
        return ApiResponse.success(instrumentService.allAttachments(operator));
    }

    @GetMapping("/attachments/page")
    public ApiResponse<PageResponse<InstrumentAttachment>> attachmentPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.pageAttachments(operator, pageNum, pageSize));
    }

    @PostMapping
    public ApiResponse<Instrument> create(@Valid @RequestBody InstrumentSaveRequest request,
                                          @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.save(null, request, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<Instrument> update(@PathVariable Long id, @Valid @RequestBody InstrumentSaveRequest request,
                                          @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.save(id, request, operator));
    }

    @PostMapping("/categories")
    public ApiResponse<InstrumentCategory> createCategory(@RequestBody InstrumentCategory request,
                                                          @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveCategory(null, request, operator));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<InstrumentCategory> updateCategory(@PathVariable Long id,
                                                          @RequestBody InstrumentCategory request,
                                                          @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveCategory(id, request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id, @CurrentUser SysUser operator) {
        instrumentService.deleteInstrument(id, operator);
        return ApiResponse.success("成功");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<?> deleteCategory(@PathVariable Long id, @CurrentUser SysUser operator) {
        instrumentService.deleteCategory(id, operator);
        return ApiResponse.success("成功");
    }

    /**
     * 新增开放规则（支持多天）。
     */
    @PostMapping("/open-rules")
    public ApiResponse<InstrumentOpenRule> saveRule(@Valid @RequestBody OpenRuleSaveRequest request,
                                                    @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveRule(request, operator));
    }

    /**
     * 编辑开放规则（支持多天）。
     */
    @PutMapping("/open-rules/{id}")
    public ApiResponse<InstrumentOpenRule> updateRule(@PathVariable Long id,
                                                      @Valid @RequestBody OpenRuleSaveRequest request,
                                                      @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveRule(id, request, operator));
    }

    @DeleteMapping("/open-rules/{id}")
    public ApiResponse<?> deleteRule(@PathVariable Long id, @CurrentUser SysUser operator) {
        instrumentService.deleteOpenRule(id, operator);
        return ApiResponse.success("成功");
    }

    @PostMapping("/attachments")
    public ApiResponse<InstrumentAttachment> createAttachment(@RequestBody InstrumentAttachment request,
                                                              @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveAttachment(null, request, operator));
    }

    @PutMapping("/attachments/{id}")
    public ApiResponse<InstrumentAttachment> updateAttachment(@PathVariable Long id,
                                                              @RequestBody InstrumentAttachment request,
                                                              @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.saveAttachment(id, request, operator));
    }

    @DeleteMapping("/attachments/{id}")
    public ApiResponse<?> deleteAttachment(@PathVariable Long id, @CurrentUser SysUser operator) {
        instrumentService.deleteAttachment(id, operator);
        return ApiResponse.success("成功");
    }
}
