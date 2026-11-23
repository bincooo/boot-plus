package com.boot.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.bo.NoticeBo;

/**
 * 公告 服务层
 *
 * @author boot-plus
 */
public interface INoticeService
{
    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public NoticeBo selectNoticeById(Long noticeId);

    /**
     * 分页查询公告列表
     *
     * @param page 分页参数
     * @param notice 公告查询条件
     * @return 公告集合
     */
    public IPage<NoticeBo> selectNoticeList(IPage<NoticeBo> page, NoticeBo notice);

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int insertNotice(NoticeBo notice);

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int updateNotice(NoticeBo notice);

    /**
     * 删除公告信息
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    public int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    public int deleteNoticeByIds(Long[] noticeIds);
}
