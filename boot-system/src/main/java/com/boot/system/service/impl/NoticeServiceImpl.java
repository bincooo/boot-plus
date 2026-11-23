package com.boot.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.repo.NoticeRepository;
import com.boot.system.service.INoticeService;
import com.boot.system.domain.bo.NoticeBo;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * 公告 服务层实现
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements INoticeService
{
    private final NoticeRepository noticeRepository;

    /**
     * 查询公告信息
     */
    @Override
    public NoticeBo selectNoticeById(Long noticeId)
    {
        return noticeRepository.selectNoticeById(noticeId);
    }

    /**
     * 分页查询公告列表
     */
    @Override
    public IPage<NoticeBo> selectNoticeList(IPage<NoticeBo> page, NoticeBo notice)
    {
        return noticeRepository.selectNoticeList(page, notice);
    }

    /**
     * 新增公告
     */
    @Override
    public int insertNotice(NoticeBo notice)
    {
        return noticeRepository.insertNotice(notice);
    }

    /**
     * 修改公告
     */
    @Override
    public int updateNotice(NoticeBo notice)
    {
        return noticeRepository.updateNotice(notice);
    }

    /**
     * 删除公告对象
     */
    @Override
    public int deleteNoticeById(Long noticeId)
    {
        return noticeRepository.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeRepository.deleteNoticeByIds(noticeIds);
    }
}
