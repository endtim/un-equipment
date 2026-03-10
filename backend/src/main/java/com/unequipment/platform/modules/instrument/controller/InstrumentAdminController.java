package com.unequipment.platform.modules.instrument.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.instrument.dto.InstrumentSaveRequest;
import com.unequipment.platform.modules.instrument.dto.OpenRuleSaveRequest;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instruments")
@RequiredArgsConstructor
public class InstrumentAdminController {

    private final InstrumentService instrumentService;
    private final InstrumentRepository instrumentRepository;

    @GetMapping
    public ApiResponse<PageResponse<Instrument>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        return ApiResponse.success(new PageResponse<>(
            instrumentRepository.findPageByCondition(keyword, status, categoryId, offset, pageSize),
            instrumentRepository.countPageByCondition(keyword, status, categoryId),
            pageNum,
            pageSize
        ));
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.success(instrumentService.categories());
    }

    @GetMapping("/open-rules")
    public ApiResponse<?> openRules() {
        return ApiResponse.success(instrumentService.allOpenRules());
    }

    @GetMapping("/attachments")
    public ApiResponse<?> attachments() {
        return ApiResponse.success(instrumentService.allAttachments());
    }

    @PostMapping
    public ApiResponse<Instrument> create(@Valid @RequestBody InstrumentSaveRequest request, @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.save(null, request, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<Instrument> update(@PathVariable Long id, @Valid @RequestBody InstrumentSaveRequest request,
                                          @CurrentUser SysUser operator) {
        return ApiResponse.success(instrumentService.save(id, request, operator));
    }

    @PostMapping("/categories")
    public ApiResponse<InstrumentCategory> createCategory(@RequestBody InstrumentCategory request) {
        return ApiResponse.success(instrumentService.saveCategory(null, request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<InstrumentCategory> updateCategory(@PathVariable Long id, @RequestBody InstrumentCategory request) {
        return ApiResponse.success(instrumentService.saveCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        instrumentService.deleteInstrument(id);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<?> deleteCategory(@PathVariable Long id) {
        instrumentService.deleteCategory(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/open-rules")
    public ApiResponse<InstrumentOpenRule> saveRule(@Valid @RequestBody OpenRuleSaveRequest request) {
        return ApiResponse.success(instrumentService.saveRule(request));
    }

    @PutMapping("/open-rules/{id}")
    public ApiResponse<InstrumentOpenRule> updateRule(@PathVariable Long id, @Valid @RequestBody OpenRuleSaveRequest request) {
        return ApiResponse.success(instrumentService.saveRule(id, request));
    }

    @DeleteMapping("/open-rules/{id}")
    public ApiResponse<?> deleteRule(@PathVariable Long id) {
        instrumentService.deleteOpenRule(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/attachments")
    public ApiResponse<InstrumentAttachment> createAttachment(@RequestBody InstrumentAttachment request) {
        return ApiResponse.success(instrumentService.saveAttachment(null, request));
    }

    @PutMapping("/attachments/{id}")
    public ApiResponse<InstrumentAttachment> updateAttachment(@PathVariable Long id, @RequestBody InstrumentAttachment request) {
        return ApiResponse.success(instrumentService.saveAttachment(id, request));
    }

    @DeleteMapping("/attachments/{id}")
    public ApiResponse<?> deleteAttachment(@PathVariable Long id) {
        instrumentService.deleteAttachment(id);
        return ApiResponse.success("ok");
    }
}
