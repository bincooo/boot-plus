package com.boot.web.controller.monitor;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.annotation.Log;
import com.boot.common.core.controller.BaseController;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.core.page.TableDataInfo;
import com.boot.common.enums.BusinessType;
import com.boot.common.utils.PageUtils;
import com.boot.common.utils.poi.ExcelUtil;
import com.boot.framework.web.service.PasswordService;
import com.boot.system.domain.vo.LogininforVo;
import com.boot.system.service.ILogininforService;
import com.boot.system.domain.bo.LogininforBo;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;

/**
 * 系统访问记录
 *
 * @author boot-plus
 */
@RestController
@RequestMapping("/monitor/logininfor")
@RequiredArgsConstructor
public class LogininforController extends BaseController
{
    private final ILogininforService logininforService;

    private final Converter converter;

    private final PasswordService passwordService;

    @PreAuthorize("@ss.hasPermi('monitor:logininfor:list')")
    @GetMapping("/list")
    public TableDataInfo list(LogininforBo logininfor)
    {
        Page<LogininforBo> page = startPage();
        IPage<LogininforVo> result = PageUtils.convert(logininforService.selectLogininforList(page, logininfor),
                bo -> converter.convert(bo, LogininforVo.class));
        return getDataTable(result);
    }

    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LogininforBo logininfor)
    {
        List<LogininforBo> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<LogininforBo> util = new ExcelUtil<LogininforBo>(LogininforBo.class);
        util.exportExcel(response, list, "登录日志");
    }

    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@PathVariable Long[] infoIds)
    {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        logininforService.cleanLogininfor();
        return success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:logininfor:unlock')")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @GetMapping("/unlock/{userName}")
    public AjaxResult unlock(@PathVariable("userName") String userName)
    {
        passwordService.clearLoginRecordCache(userName);
        return success();
    }
}
