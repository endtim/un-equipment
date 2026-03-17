package com.unequipment.platform.modules.content.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.content.entity.UserMessage;
import com.unequipment.platform.modules.content.repository.UserMessageRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final UserMessageRepository userMessageRepository;

    /**
     * 发送系统消息：统一写入未读状态。
     */
    public void send(SysUser user, String title, String content) {
        UserMessage message = new UserMessage();
        message.setUserId(user.getId());
        message.setMsgType("SYSTEM");
        message.setTitle(title);
        message.setContent(content);
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        userMessageRepository.insert(message);
    }

    /**
     * 用户消息分页。
     */
    public PageResponse<UserMessage> findByUser(SysUser user, int pageNum, int pageSize) {
        int validPageNum = Math.max(1, pageNum);
        int validPageSize = Math.max(1, pageSize);
        int offset = (validPageNum - 1) * validPageSize;
        return new PageResponse<>(
            userMessageRepository.findPageByUserId(user.getId(), offset, validPageSize),
            userMessageRepository.countByUserId(user.getId()),
            validPageNum,
            validPageSize
        );
    }

    /**
     * 单条已读：
     * 先校验“消息属于当前用户”，再执行更新。
     */
    public void markRead(SysUser user, Long id) {
        if (userMessageRepository.findByIdAndUserId(id, user.getId()) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "消息不存在");
        }
        userMessageRepository.markRead(id, user.getId(), LocalDateTime.now());
    }

    /**
     * 全部已读：仅更新当前用户未读消息。
     */
    public void markAllRead(SysUser user) {
        userMessageRepository.markAllRead(user.getId(), LocalDateTime.now());
    }
}
