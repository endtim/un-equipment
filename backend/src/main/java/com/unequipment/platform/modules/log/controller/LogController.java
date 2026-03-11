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

    @GetMapping
    public ApiResponse<PageResponse<OperationLogVO>> logs(
        @RequestParam(required = false) String moduleName,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(operationLogService.page(moduleName, keyword, pageNum, pageSize));
    }
}
