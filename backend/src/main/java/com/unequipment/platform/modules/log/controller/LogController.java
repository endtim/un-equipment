package com.unequipment.platform.modules.log.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogService operationLogService;

    /**
     * 操作日志分页查询：
     * 支持按模块和关键字检索，关键字匹配动作名与请求地址。
     */
    @GetMapping
    public ApiResponse<PageResponse<OperationLogVO>> logs(
        @RequestParam(required = false) String moduleName,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(operationLogService.page(moduleName, keyword, pageNum, pageSize));
    }
}
