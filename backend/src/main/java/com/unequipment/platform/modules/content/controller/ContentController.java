package com.unequipment.platform.modules.content.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.content.entity.HelpDoc;
import com.unequipment.platform.modules.content.entity.Notice;
import com.unequipment.platform.modules.content.service.ContentService;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final MessageService messageService;

    /**
     * 前台公告分页：
     * 固定只返回已发布内容，避免草稿/下线内容外泄。
     */
    @GetMapping("/notices")
    public ApiResponse<PageResponse<Notice>> notices(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(contentService.noticePage(keyword, "PUBLISHED", pageNum, pageSize));
    }

    /**
     * 前台公告详情：
     * 仅允许查看已发布公告。
     */
    @GetMapping("/notices/{id}")
    public ApiResponse<?> notice(@PathVariable Long id) {
        return ApiResponse.success(contentService.publishedNotice(id));
    }

    /**
     * 前台帮助文档分页（仅已发布）。
     */
    @GetMapping("/help-docs")
    public ApiResponse<PageResponse<HelpDoc>> helpDocs(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(contentService.helpDocPage(keyword, "PUBLISHED", pageNum, pageSize));
    }

    /**
     * 前台帮助文档详情（仅已发布）。
     */
    @GetMapping("/help-docs/{id}")
    public ApiResponse<?> helpDoc(@PathVariable Long id) {
        return ApiResponse.success(contentService.publishedHelpDoc(id));
    }

    /**
     * 站内消息分页。
     */
    @GetMapping("/messages")
    public ApiResponse<?> messages(@CurrentUser SysUser user,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(messageService.findByUser(user, pageNum, pageSize));
    }

    /**
     * 单条消息已读。
     */
    @PostMapping("/messages/{id}/read")
    public ApiResponse<?> read(@PathVariable Long id, @CurrentUser SysUser user) {
        messageService.markRead(user, id);
        return ApiResponse.success("ok");
    }

    /**
     * 全部消息一键已读。
     */
    @PostMapping("/messages/read-all")
    public ApiResponse<?> readAll(@CurrentUser SysUser user) {
        messageService.markAllRead(user);
        return ApiResponse.success("ok");
    }
}
