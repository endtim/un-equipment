package com.unequipment.platform.modules.log.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.log.entity.OperationLog;
import com.unequipment.platform.modules.log.repository.OperationLogRepository;
import com.unequipment.platform.modules.log.vo.OperationLogVO;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    /**
     * 从 detail 文本提取业务主键（orderId/rechargeId/...），用于日志检索与关联追踪。
     */
    private static final Pattern BIZ_ID_PATTERN = Pattern.compile(
        "(?i)(?:bizId|orderId|rechargeId|ruleId|categoryId|instrumentId|attachmentId|helpDocId|noticeId|userId|roleId|departmentId)\\s*[:=]\\s*(\\d+)");
    private static final Map<String, String> MODULE_LABELS = new HashMap<>();
    private static final Map<String, String> ACTION_LABELS = new HashMap<>();

    private final OperationLogRepository operationLogRepository;

    static {
        MODULE_LABELS.put("SYSTEM", "系统管理");
        MODULE_LABELS.put("ORDER", "订单管理");
        MODULE_LABELS.put("FINANCE", "财务管理");
        MODULE_LABELS.put("INSTRUMENT", "仪器管理");
        MODULE_LABELS.put("CONTENT", "内容管理");

        ACTION_LABELS.put("CREATE_USER", "创建用户");
        ACTION_LABELS.put("UPDATE_USER", "更新用户");
        ACTION_LABELS.put("DELETE_USER", "删除用户");
        ACTION_LABELS.put("CREATE_ROLE", "创建角色");
        ACTION_LABELS.put("UPDATE_ROLE", "更新角色");
        ACTION_LABELS.put("DELETE_ROLE", "删除角色");
        ACTION_LABELS.put("CREATE_DEPARTMENT", "创建部门");
        ACTION_LABELS.put("UPDATE_DEPARTMENT", "更新部门");
        ACTION_LABELS.put("DELETE_DEPARTMENT", "删除部门");

        ACTION_LABELS.put("CREATE_MACHINE_ORDER", "创建上机预约");
        ACTION_LABELS.put("CREATE_SAMPLE_ORDER", "创建送样预约");
        ACTION_LABELS.put("AUDIT_ORDER", "审核订单");
        ACTION_LABELS.put("CHECK_IN_ORDER", "上机签到");
        ACTION_LABELS.put("RECEIVE_SAMPLE_ORDER", "接收样品");
        ACTION_LABELS.put("UPLOAD_SAMPLE_RESULT", "上传送样结果");
        ACTION_LABELS.put("FINISH_MACHINE_ORDER", "结束上机");
        ACTION_LABELS.put("SETTLE_ORDER", "订单结算");
        ACTION_LABELS.put("CANCEL_ORDER", "取消订单");
        ACTION_LABELS.put("CLOSE_ORDER", "关闭订单");
        ACTION_LABELS.put("ADJUST_ORDER_AMOUNT", "调整订单金额");

        ACTION_LABELS.put("SUBMIT_RECHARGE", "提交充值申请");
        ACTION_LABELS.put("AUDIT_RECHARGE", "审核充值申请");
        ACTION_LABELS.put("REFUND_ORDER", "订单退款");

        ACTION_LABELS.put("CREATE_CATEGORY", "创建仪器分类");
        ACTION_LABELS.put("UPDATE_CATEGORY", "更新仪器分类");
        ACTION_LABELS.put("DELETE_CATEGORY", "删除仪器分类");
        ACTION_LABELS.put("CREATE_INSTRUMENT", "创建仪器");
        ACTION_LABELS.put("UPDATE_INSTRUMENT", "更新仪器");
        ACTION_LABELS.put("DELETE_INSTRUMENT", "删除仪器");
        ACTION_LABELS.put("CREATE_OPEN_RULE", "创建开放规则");
        ACTION_LABELS.put("UPDATE_OPEN_RULE", "更新开放规则");
        ACTION_LABELS.put("DELETE_OPEN_RULE", "删除开放规则");
        ACTION_LABELS.put("CREATE_ATTACHMENT", "新增附件");
        ACTION_LABELS.put("UPDATE_ATTACHMENT", "更新附件");
        ACTION_LABELS.put("DELETE_ATTACHMENT", "删除附件");

        ACTION_LABELS.put("CREATE_NOTICE", "创建公告");
        ACTION_LABELS.put("UPDATE_NOTICE", "更新公告");
        ACTION_LABELS.put("DELETE_NOTICE", "删除公告");
        ACTION_LABELS.put("CREATE_HELP_DOC", "创建帮助文档");
        ACTION_LABELS.put("UPDATE_HELP_DOC", "更新帮助文档");
        ACTION_LABELS.put("DELETE_HELP_DOC", "删除帮助文档");
    }

    /**
     * 统一日志落库入口：
     * 自动补齐请求方法、URI、来源 IP、结果状态与业务主键。
     */
    public void save(SysUser user, String module, String action, String detail) {
        HttpServletRequest request = currentRequest();

        OperationLog log = new OperationLog();
        log.setUserId(user == null ? null : user.getId());
        log.setModuleName(module);
        log.setActionName(action);
        log.setRequestMethod(resolveRequestMethod(request));
        log.setRequestUri(resolveRequestUri(request, detail));
        log.setRequestIp(resolveRequestIp(request));
        log.setResultCode(200);
        log.setResultMsg("成功");
        log.setBizId(resolveBizId(detail));
        log.setCreateTime(LocalDateTime.now());
        operationLogRepository.insert(log);
    }

    /**
     * 日志分页查询并映射展示文案（模块名/动作名）。
     */
    public PageResponse<OperationLogVO> page(String moduleName, String keyword, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        List<OperationLogVO> list = operationLogRepository.findPage(moduleName, keyword, offset, pageSize)
            .stream()
            .map(this::toView)
            .collect(Collectors.toList());
        long total = operationLogRepository.countPage(moduleName, keyword);
        return new PageResponse<>(list, total, pageNum, pageSize);
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest();
    }

    private String resolveRequestMethod(HttpServletRequest request) {
        return request == null ? "SYSTEM" : request.getMethod();
    }

    private String resolveRequestUri(HttpServletRequest request, String detail) {
        String normalizedDetail = normalizeDetail(detail);
        if (request == null) {
            return normalizedDetail;
        }
        String requestUri = request.getRequestURI();
        String uriWithDetail = requestUri + " | " + normalizedDetail;
        return uriWithDetail.length() > 255 ? uriWithDetail.substring(0, 255) : uriWithDetail;
    }

    private String resolveRequestIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * detail 解析业务 ID：
     * 支持“key:value”与“key=value”两种格式。
     */
    private Long resolveBizId(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = BIZ_ID_PATTERN.matcher(detail);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeDetail(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return "-";
        }
        String normalized = detail.trim();
        return normalized.length() > 250 ? normalized.substring(0, 250) : normalized;
    }

    private OperationLogVO toView(OperationLog item) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setOperatorName(item.getOperatorName());
        vo.setModuleName(item.getModuleName());
        vo.setModuleLabel(MODULE_LABELS.getOrDefault(item.getModuleName(), item.getModuleName()));
        vo.setActionName(item.getActionName());
        vo.setActionLabel(ACTION_LABELS.getOrDefault(item.getActionName(), item.getActionName()));
        vo.setRequestMethod(item.getRequestMethod());
        vo.setRequestUri(item.getRequestUri());
        vo.setRequestIp(item.getRequestIp());
        vo.setResultCode(item.getResultCode());
        vo.setResultMsg(item.getResultMsg());
        vo.setResultLabel(item.getResultCode() != null && item.getResultCode() == 200 ? "成功" : "失败");
        vo.setBizId(item.getBizId());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }
}
