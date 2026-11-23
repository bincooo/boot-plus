package com.boot.system.service.impl;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.vo.NoticeReadUserVo;
import com.boot.system.repo.NoticeReadRepository;
import com.boot.system.service.INoticeReadService;
import com.boot.system.domain.bo.NoticeBo;
import com.boot.system.domain.bo.NoticeReadBo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 公告已读记录 服务层实现
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class NoticeReadServiceImpl implements INoticeReadService
{
    private final NoticeReadRepository noticeReadRepository;

    /**
     * 标记已读
     */
    @Override
    public void markRead(Long noticeId, Long userId)
    {
        NoticeReadBo record = new NoticeReadBo();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        noticeReadRepository.insertNoticeRead(record);
    }

    /**
     * 查询某用户未读公告数量
     */
    @Override
    public int selectUnreadCount(Long userId)
    {
        return noticeReadRepository.selectUnreadCount(userId);
    }

    /**
     * 查询公告列表并标记当前用户已读状态
     */
    @Override
    public List<NoticeBo> selectNoticeListWithReadStatus(Long userId, int limit)
    {
        return noticeReadRepository.selectNoticeListWithReadStatus(userId, limit);
    }

    /**
     * 批量标记已读
     */
    @Override
    @Transactional
    public void markReadBatch(Long userId, Long[] noticeIds)
    {
        if (noticeIds == null || noticeIds.length == 0)
        {
            return;
        }
        noticeReadRepository.insertNoticeReadBatch(userId, noticeIds);
    }

    /**
     * 分页查询已阅读某公告的用户列表
     */
    @Override
    public IPage<NoticeReadUserVo> selectReadUsersByNoticeId(IPage<NoticeReadUserVo> page, Long noticeId,
            String searchValue)
    {
        return noticeReadRepository.selectReadUsersByNoticeId(page, noticeId, searchValue);
    }

    /**
     * 删除公告时清理对应已读记录
     */
    @Override
    public void deleteByNoticeIds(Long[] noticeIds)
    {
        noticeReadRepository.deleteByNoticeIds(noticeIds);
    }
}
