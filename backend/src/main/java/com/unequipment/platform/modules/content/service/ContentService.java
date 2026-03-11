package com.unequipment.platform.modules.content.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.content.entity.HelpDoc;
import com.unequipment.platform.modules.content.entity.Notice;
import com.unequipment.platform.modules.content.repository.HelpDocRepository;
import com.unequipment.platform.modules.content.repository.NoticeRepository;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final NoticeRepository noticeRepository;
    private final HelpDocRepository helpDocRepository;
    private final OperationLogService operationLogService;

    public List<Notice> notices() {
        return noticeRepository.findAll();
    }

    public PageResponse<Notice> noticePage(String keyword, String publishStatus, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        return new PageResponse<>(
            noticeRepository.findPage(keyword, publishStatus, offset, pageSize),
            noticeRepository.countPage(keyword, publishStatus),
            pageNum,
            pageSize
        );
    }

    public Notice notice(Long id) {
        return noticeRepository.findById(id);
    }

    public List<HelpDoc> helpDocs() {
        return helpDocRepository.findAll();
    }

    public PageResponse<HelpDoc> helpDocPage(String keyword, String publishStatus, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        return new PageResponse<>(
            helpDocRepository.findPage(keyword, publishStatus, offset, pageSize),
            helpDocRepository.countPage(keyword, publishStatus),
            pageNum,
            pageSize
        );
    }

    public HelpDoc helpDoc(Long id) {
        return helpDocRepository.findById(id);
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (noticeRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "notice not found");
        }
        noticeRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "CONTENT", "DELETE_NOTICE", "noticeId:" + id);
    }

    @Transactional
    public void deleteHelpDoc(Long id) {
        if (helpDocRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "help doc not found");
        }
        helpDocRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "CONTENT", "DELETE_HELP_DOC", "helpDocId:" + id);
    }

    @Transactional
    public Notice saveNotice(Long id, Notice request, SysUser user) {
        Notice notice = id == null ? new Notice() : noticeRepository.findById(id);
        if (notice == null) {
            notice = new Notice();
        }
        notice.setTitle(request.getTitle());
        notice.setCategory(request.getCategory() == null ? "NOTICE" : request.getCategory());
        notice.setSummary(request.getSummary());
        notice.setContent(request.getContent());
        notice.setCoverUrl(request.getCoverUrl());
        notice.setInstrumentId(request.getInstrumentId());
        notice.setTopFlag(request.getTopFlag() == null ? 0 : request.getTopFlag());
        notice.setPublishStatus(request.getPublishStatus() == null ? "PUBLISHED" : request.getPublishStatus());
        notice.setPublishTime(request.getPublishTime() == null ? LocalDateTime.now() : request.getPublishTime());
        notice.setViewCount(request.getViewCount() == null ? 0 : request.getViewCount());
        notice.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            notice.setCreateTime(LocalDateTime.now());
            notice.setDeleted(0);
            notice.setCreateBy(user == null ? null : user.getId());
            noticeRepository.insert(notice);
        } else {
            noticeRepository.update(notice);
        }
        operationLogService.save(user, "CONTENT", id == null ? "CREATE_NOTICE" : "UPDATE_NOTICE", "notice:" + notice.getTitle());
        return notice;
    }

    @Transactional
    public HelpDoc saveHelpDoc(Long id, HelpDoc request, SysUser user) {
        HelpDoc helpDoc = id == null ? new HelpDoc() : helpDocRepository.findById(id);
        if (helpDoc == null) {
            helpDoc = new HelpDoc();
        }
        helpDoc.setTitle(request.getTitle());
        helpDoc.setDocType(request.getDocType() == null ? "HELP" : request.getDocType());
        helpDoc.setSummary(request.getSummary());
        helpDoc.setContent(request.getContent());
        helpDoc.setFileUrl(request.getFileUrl());
        helpDoc.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        helpDoc.setPublishStatus(request.getPublishStatus() == null ? "PUBLISHED" : request.getPublishStatus());
        helpDoc.setPublishTime(request.getPublishTime() == null ? LocalDateTime.now() : request.getPublishTime());
        helpDoc.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            helpDoc.setCreateTime(LocalDateTime.now());
            helpDoc.setDeleted(0);
            helpDoc.setCreateBy(user == null ? null : user.getId());
            helpDocRepository.insert(helpDoc);
        } else {
            helpDocRepository.update(helpDoc);
        }
        operationLogService.save(user, "CONTENT", id == null ? "CREATE_HELP_DOC" : "UPDATE_HELP_DOC", "helpDoc:" + helpDoc.getTitle());
        return helpDoc;
    }
}
