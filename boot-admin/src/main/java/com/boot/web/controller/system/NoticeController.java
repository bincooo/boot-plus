package com.boot.web.controller.system;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.annotation.Log;
import com.boot.common.core.controller.BaseController;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.core.page.TableDataInfo;
import com.boot.common.core.text.Convert;
import com.boot.common.enums.BusinessType;
import com.boot.common.utils.PageUtils;
import com.boot.system.domain.vo.NoticeReadUserVo;
import com.boot.system.domain.vo.NoticeVo;
import com.boot.system.service.INoticeReadService;
import com.boot.system.service.INoticeService;
import com.boot.system.domain.bo.NoticeBo;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;

/**
 * 公告 信息操作处理
 *
 * @author boot-plus
 */
@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
public class NoticeController extends BaseController
{
    private final INoticeService noticeService;

    private final INoticeReadService noticeReadService;

    private final Converter converter;

    /**
     * 获取通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(NoticeBo notice)
    {
        Page<NoticeBo> page = startPage();
        IPage<NoticeVo> result = PageUtils.convert(noticeService.selectNoticeList(page, notice),
                bo -> converter.convert(bo, NoticeVo.class));
        return getDataTable(result);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable Long noticeId)
    {
        return success(converter.convert(noticeService.selectNoticeById(noticeId), NoticeVo.class));
    }

    /**
     * 新增通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody NoticeBo notice)
    {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody NoticeBo notice)
    {
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 首页顶部公告列表（返回全部正常公告，带当前用户已读标记，最多5条）
     */
    @GetMapping("/listTop")
    @ResponseBody
    public AjaxResult listTop()
    {
        Long userId = getUserId();
        List<NoticeVo> list = PageUtils.convert(noticeReadService.selectNoticeListWithReadStatus(userId, 5),
                bo -> converter.convert(bo, NoticeVo.class));
        long unreadCount = list.stream().filter(n -> !n.getIsRead()).count();
        AjaxResult result = AjaxResult.success(list);
        result.put("unreadCount", unreadCount);
        return result;
    }

    /**
     * 标记公告已读
     */
    @PostMapping("/markRead")
    @ResponseBody
    public AjaxResult markRead(Long noticeId)
    {
        Long userId = getUserId();
        noticeReadService.markRead(noticeId, userId);
        return success();
    }

    /**
     * 批量标记已读
     */
    @PostMapping("/markReadAll")
    @ResponseBody
    public AjaxResult markReadAll(String ids)
    {
        Long userId = getUserId();
        Long[] noticeIds = Convert.toLongArray(ids);
        noticeReadService.markReadBatch(userId, noticeIds);
        return success();
    }

    /**
     * 已读用户列表数据
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/readUsers/list")
    @ResponseBody
    public TableDataInfo readUsersList(Long noticeId, String searchValue)
    {
        Page<NoticeReadUserVo> page = startPage();
        IPage<NoticeReadUserVo> list = noticeReadService.selectReadUsersByNoticeId(page, noticeId, searchValue);
        return getDataTable(list);
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        noticeReadService.deleteByNoticeIds(noticeIds);
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
