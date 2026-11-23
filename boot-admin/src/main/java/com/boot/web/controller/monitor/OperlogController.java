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
import com.boot.system.domain.vo.OperLogVo;
import com.boot.system.service.IOperLogService;
import com.boot.system.domain.bo.OperLogBo;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;

/**
 * 操作日志记录
 *
 * @author boot-plus
 */
@RestController
@RequestMapping("/monitor/operlog")
@RequiredArgsConstructor
public class OperlogController extends BaseController
{
    private final IOperLogService operLogService;

    private final Converter converter;

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(OperLogBo operLog)
    {
        Page<OperLogBo> page = startPage();
        IPage<OperLogVo> result = PageUtils.convert(operLogService.selectOperLogList(page, operLog),
                bo -> converter.convert(bo, OperLogVo.class));
        return getDataTable(result);
    }

    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, OperLogBo operLog)
    {
        List<OperLogBo> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<OperLogBo> util = new ExcelUtil<OperLogBo>(OperLogBo.class);
        util.exportExcel(response, list, "操作日志");
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public AjaxResult remove(@PathVariable Long[] operIds)
    {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return success();
    }
}
