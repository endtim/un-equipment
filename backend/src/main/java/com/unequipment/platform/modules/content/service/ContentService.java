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
    private static final String PUBLISHED = "PUBLISHED";

    private final NoticeRepository noticeRepository;
    private final HelpDocRepository helpDocRepository;
    private final OperationLogService operationLogService;

    public List<Notice> notices() {
        return noticeRepository.findAll();
    }

    public PageResponse<Notice> noticePage(String keyword, String publishStatus, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        return new PageResponse<>(
            noticeRepository.findPage(keyword, publishStatus, offset, safePageSize),
            noticeRepository.countPage(keyword, publishStatus),
            safePageNum,
            safePageSize
        );
    }

    public Notice notice(Long id) {
        return noticeRepository.findById(id);
    }

    public Notice publishedNotice(Long id) {
        Notice notice = noticeRepository.findPublishedById(id);
        if (notice == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "公告不存在或未发布");
        }
        return notice;
    }

    public List<HelpDoc> helpDocs() {
        return helpDocRepository.findAll();
    }

    public PageResponse<HelpDoc> helpDocPage(String keyword, String publishStatus, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        return new PageResponse<>(
            helpDocRepository.findPage(keyword, publishStatus, offset, safePageSize),
            helpDocRepository.countPage(keyword, publishStatus),
            safePageNum,
            safePageSize
        );
    }

    private int normalizePageNum(int pageNum) {
        return Math.max(pageNum, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public HelpDoc helpDoc(Long id) {
        return helpDocRepository.findById(id);
    }

    public HelpDoc publishedHelpDoc(Long id) {
        HelpDoc helpDoc = helpDocRepository.findPublishedById(id);
        if (helpDoc == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "帮助文档不存在或未发布");
        }
        return helpDoc;
    }

    @Transactional
    public void deleteNotice(Long id, SysUser operator) {
        if (noticeRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "公告不存在");
        }
        noticeRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "CONTENT", "DELETE_NOTICE", "noticeId:" + id);
    }

    @Transactional
    public void deleteHelpDoc(Long id, SysUser operator) {
        if (helpDocRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "帮助文档不存在");
        }
        helpDocRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "CONTENT", "DELETE_HELP_DOC", "helpDocId:" + id);
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
