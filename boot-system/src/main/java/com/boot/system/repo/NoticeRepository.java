package com.boot.system.repo;

import java.util.Arrays;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.NoticeMapper;
import com.boot.system.repo.po.Notice;
import com.boot.system.domain.bo.NoticeBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 通知公告 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class NoticeRepository
{
    private final NoticeMapper noticeMapper;

    private final Converter converter;

    /**
     * 分页查询公告集合
     *
     * @param page 分页参数
     * @param query 公告查询条件
     * @return 公告集合
     */
    public IPage<NoticeBo> selectNoticeList(IPage<NoticeBo> page, NoticeBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        // 原 selectNoticeVo 列清单，含 cast(notice_content as char) 投影
        QueryWrapper<Notice> wrapper = noticeVoQuery();
        if (query.getNoticeTitle() != null && !query.getNoticeTitle().isEmpty())
        {
            wrapper.like("notice_title", query.getNoticeTitle());
        }
        if (query.getNoticeType() != null && !query.getNoticeType().isEmpty())
        {
            wrapper.eq("notice_type", query.getNoticeType());
        }
        if (query.getCreateBy() != null && !query.getCreateBy().isEmpty())
        {
            wrapper.like("create_by", query.getCreateBy());
        }
        wrapper.orderByDesc("notice_id");
        Page<Notice> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        noticeMapper.selectPage(poPage, wrapper);
        return PageUtils.convert(poPage, po -> converter.convert(po, NoticeBo.class));
    }

    /**
     * 构建与原 selectNoticeVo 完全相同的列清单查询壳（含 cast 投影）
     */
    private QueryWrapper<Notice> noticeVoQuery()
    {
        return new QueryWrapper<Notice>().select(
                "notice_id",
                "notice_title",
                "notice_type",
                "cast(notice_content as char) as notice_content",
                "status",
                "create_by",
                "create_time",
                "update_by",
                "update_time",
                "remark");
    }

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public NoticeBo selectNoticeById(Long noticeId)
    {
        Notice notice = noticeMapper.selectOne(noticeVoQuery().eq("notice_id", noticeId));
        return converter.convert(notice, NoticeBo.class);
    }

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int insertNotice(NoticeBo notice)
    {
        Notice po = converter.convert(notice, Notice.class);
        po.setCreateTime(DateUtils.getNowDate());
        return noticeMapper.insert(po);
    }

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int updateNotice(NoticeBo notice)
    {
        Notice po = converter.convert(notice, Notice.class);
        // 镜像原 XML updateNotice 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return noticeMapper.update(null, Wrappers.lambdaUpdate(Notice.class)
                .set(po.getNoticeTitle() != null && !po.getNoticeTitle().isEmpty(), Notice::getNoticeTitle, po.getNoticeTitle())
                .set(po.getNoticeType() != null && !po.getNoticeType().isEmpty(), Notice::getNoticeType, po.getNoticeType())
                .set(po.getNoticeContent() != null, Notice::getNoticeContent, po.getNoticeContent())
                .set(po.getStatus() != null && !po.getStatus().isEmpty(), Notice::getStatus, po.getStatus())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Notice::getUpdateBy, po.getUpdateBy())
                .set(Notice::getUpdateTime, DateUtils.getNowDate())
                .eq(Notice::getNoticeId, po.getNoticeId()));
    }

    /**
     * 删除公告
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    public int deleteNoticeById(Long noticeId)
    {
        return noticeMapper.deleteById(noticeId);
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeMapper.deleteByIds(Arrays.asList(noticeIds));
    }
}
