package com.unequipment.platform.modules.content.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.content.entity.HelpDoc;
import com.unequipment.platform.modules.content.entity.Notice;
import com.unequipment.platform.modules.content.service.ContentService;
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
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class ContentAdminController {

    private final ContentService contentService;

    @GetMapping("/notices")
    public ApiResponse<PageResponse<Notice>> noticePage(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String publishStatus,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(contentService.noticePage(keyword, publishStatus, pageNum, pageSize));
    }

    @GetMapping("/help-docs")
    public ApiResponse<PageResponse<HelpDoc>> helpDocPage(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String publishStatus,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(contentService.helpDocPage(keyword, publishStatus, pageNum, pageSize));
    }

    @PostMapping("/notices")
    public ApiResponse<Notice> createNotice(@Valid @RequestBody Notice notice, @CurrentUser SysUser user) {
        return ApiResponse.success(contentService.saveNotice(null, notice, user));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<Notice> updateNotice(@PathVariable Long id, @Valid @RequestBody Notice notice, @CurrentUser SysUser user) {
        return ApiResponse.success(contentService.saveNotice(id, notice, user));
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<?> deleteNotice(@PathVariable Long id) {
        contentService.deleteNotice(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/help-docs")
    public ApiResponse<HelpDoc> createHelpDoc(@Valid @RequestBody HelpDoc helpDoc, @CurrentUser SysUser user) {
        return ApiResponse.success(contentService.saveHelpDoc(null, helpDoc, user));
    }

    @PutMapping("/help-docs/{id}")
    public ApiResponse<HelpDoc> updateHelpDoc(@PathVariable Long id, @Valid @RequestBody HelpDoc helpDoc, @CurrentUser SysUser user) {
        return ApiResponse.success(contentService.saveHelpDoc(id, helpDoc, user));
    }

    @DeleteMapping("/help-docs/{id}")
    public ApiResponse<?> deleteHelpDoc(@PathVariable Long id) {
        contentService.deleteHelpDoc(id);
        return ApiResponse.success("ok");
    }
}
