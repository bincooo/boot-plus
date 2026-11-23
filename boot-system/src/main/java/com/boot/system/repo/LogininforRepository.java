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
import com.boot.system.repo.mapper.LogininforMapper;
import com.boot.system.repo.po.Logininfor;
import com.boot.system.domain.bo.LogininforBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 系统访问日志 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class LogininforRepository
{
    private final LogininforMapper logininforMapper;

    private final Converter converter;

    /**
     * 分页查询系统访问日志集合
     *
     * @param page 分页参数
     * @param query 访问日志查询条件
     * @return 登录记录集合
     */
    public IPage<LogininforBo> selectLogininforList(IPage<LogininforBo> page, LogininforBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<Logininfor> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        logininforMapper.selectPage(poPage, buildLogininforQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, LogininforBo.class));
    }

    /**
     * 查询系统访问日志集合（非分页，用于导出）
     *
     * @param query 访问日志查询条件
     * @return 登录记录集合
     */
    public List<LogininforBo> selectLogininforList(LogininforBo query)
    {
        return PageUtils.convert(logininforMapper.selectList(buildLogininforQuery(query)),
                po -> converter.convert(po, LogininforBo.class));
    }

    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(LogininforBo logininfor)
    {
        Logininfor po = converter.convert(logininfor, Logininfor.class);
        po.setLoginTime(DateUtils.getNowDate());
        logininforMapper.insert(po);
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return logininforMapper.deleteByIds(Arrays.asList(infoIds));
    }

    /**
     * 清空系统登录日志
     *
     * @return 结果
     */
    public int cleanLogininfor()
    {
        // 原 XML 为 truncate table sys_logininfor；MP 无 TRUNCATE 语句，改用无条件的全表删除。
        // 行为差异：DELETE 不重置 AUTO_INCREMENT（原 TRUNCATE 会重置），其余删除语义一致。
        return logininforMapper.delete(Wrappers.<Logininfor>query());
    }

    /**
     * 构建登录日志列表查询条件（与原 XML selectLogininforList 的动态条件一致）
     *
     * @param query 访问日志查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<Logininfor> buildLogininforQuery(LogininforBo query)
    {
        LambdaQueryWrapper<Logininfor> wrapper = Wrappers.lambdaQuery(Logininfor.class);
        if (query.getIpaddr() != null && !query.getIpaddr().isEmpty())
        {
            wrapper.like(Logininfor::getIpaddr, query.getIpaddr());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Logininfor::getStatus, query.getStatus());
        }
        if (query.getUserName() != null && !query.getUserName().isEmpty())
        {
            wrapper.like(Logininfor::getUserName, query.getUserName());
        }
        Object beginTime = query.getParams().get("beginTime");
        if (beginTime != null && !"".equals(beginTime))
        {
            wrapper.ge(Logininfor::getLoginTime, beginTime);
        }
        Object endTime = query.getParams().get("endTime");
        if (endTime != null && !"".equals(endTime))
        {
            wrapper.le(Logininfor::getLoginTime, endTime);
        }
        wrapper.orderByDesc(Logininfor::getInfoId);
        return wrapper;
    }
}
