package com.unequipment.platform.modules.log.service;

import com.unequipment.platform.modules.log.entity.OperationLog;
import com.unequipment.platform.modules.log.repository.OperationLogRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public void save(SysUser user, String module, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(user == null ? null : user.getId());
        log.setModuleName(module);
        log.setActionName(action);
        log.setRequestMethod("SYSTEM");
        log.setRequestUri(detail);
        log.setResultCode(200);
        log.setResultMsg("success");
        log.setCreateTime(LocalDateTime.now());
        operationLogRepository.insert(log);
    }
}
