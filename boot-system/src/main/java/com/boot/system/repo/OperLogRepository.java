package com.boot.system.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.OperLogMapper;
import com.boot.system.repo.po.OperLog;
import com.boot.system.domain.bo.OperLogBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 操作日志 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class OperLogRepository
{
    private final OperLogMapper operLogMapper;

    private final Converter converter;

    /**
     * 分页查询操作日志集合
     *
     * @param page 分页参数
     * @param query 操作日志查询条件
     * @return 操作日志集合
     */
    public IPage<OperLogBo> selectOperLogList(IPage<OperLogBo> page, OperLogBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<OperLog> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        operLogMapper.selectPage(poPage, buildOperLogQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, OperLogBo.class));
    }

    /**
     * 查询操作日志集合（非分页，用于导出）
     *
     * @param query 操作日志查询条件
     * @return 操作日志集合
     */
    public List<OperLogBo> selectOperLogList(OperLogBo query)
    {
        return PageUtils.convert(operLogMapper.selectList(buildOperLogQuery(query)),
                po -> converter.convert(po, OperLogBo.class));
    }

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    public void insertOperlog(OperLogBo operLog)
    {
        OperLog po = converter.convert(operLog, OperLog.class);
        po.setOperTime(DateUtils.getNowDate());
        operLogMapper.insert(po);
    }

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    public OperLogBo selectOperLogById(Long operId)
    {
        return converter.convert(operLogMapper.selectById(operId), OperLogBo.class);
    }

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    public int deleteOperLogByIds(Long[] operIds)
    {
        return operLogMapper.deleteByIds(Arrays.asList(operIds));
    }

    /**
     * 清空操作日志
     */
    public void cleanOperLog()
    {
        // 原 XML 为 truncate table sys_oper_log；MP 无 TRUNCATE 语句，改用无条件的全表删除。
        // 行为差异：DELETE 不重置 AUTO_INCREMENT（原 TRUNCATE 会重置），其余删除语义一致。
        operLogMapper.delete(Wrappers.<OperLog>query());
    }

    /**
     * 构建操作日志列表查询条件（与原 XML selectOperLogList 的动态条件一致）
     *
     * @param query 操作日志查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<OperLog> buildOperLogQuery(OperLogBo query)
    {
        LambdaQueryWrapper<OperLog> wrapper = Wrappers.lambdaQuery(OperLog.class);
        if (query.getOperIp() != null && !query.getOperIp().isEmpty())
        {
            wrapper.like(OperLog::getOperIp, query.getOperIp());
        }
        if (query.getTitle() != null && !query.getTitle().isEmpty())
        {
            wrapper.like(OperLog::getTitle, query.getTitle());
        }
        if (query.getBusinessType() != null)
        {
            wrapper.eq(OperLog::getBusinessType, query.getBusinessType());
        }
        if (query.getBusinessTypes() != null && query.getBusinessTypes().length > 0)
        {
            wrapper.in(OperLog::getBusinessType, Arrays.asList(query.getBusinessTypes()));
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(OperLog::getStatus, query.getStatus());
        }
        if (query.getOperName() != null && !query.getOperName().isEmpty())
        {
            wrapper.like(OperLog::getOperName, query.getOperName());
        }
        Object beginTime = query.getParams().get("beginTime");
        if (beginTime != null && !"".equals(beginTime))
        {
            wrapper.ge(OperLog::getOperTime, beginTime);
        }
        Object endTime = query.getParams().get("endTime");
        if (endTime != null && !"".equals(endTime))
        {
            wrapper.le(OperLog::getOperTime, endTime);
        }
        wrapper.orderByDesc(OperLog::getOperId);
        return wrapper;
    }
}
