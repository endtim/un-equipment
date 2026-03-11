package com.unequipment.platform.modules.content.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.content.entity.UserMessage;
import com.unequipment.platform.modules.content.repository.UserMessageRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final UserMessageRepository userMessageRepository;

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

    public List<UserMessage> findByUser(SysUser user) {
        return userMessageRepository.findByUserId(user.getId());
    }

    public void markRead(SysUser user, Long id) {
        if (userMessageRepository.findByIdAndUserId(id, user.getId()) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "message not found");
        }
        userMessageRepository.markRead(id, user.getId(), LocalDateTime.now());
    }

    public void markAllRead(SysUser user) {
        userMessageRepository.markAllRead(user.getId(), LocalDateTime.now());
    }
}
