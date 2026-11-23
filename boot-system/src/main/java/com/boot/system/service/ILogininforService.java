package com.boot.system.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.bo.LogininforBo;

/**
 * 系统访问日志情况信息 服务层
 *
 * @author boot-plus
 */
public interface ILogininforService
{
    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(LogininforBo logininfor);

    /**
     * 分页查询系统登录日志集合
     *
     * @param page 分页参数
     * @param logininfor 访问日志查询条件
     * @return 登录记录集合
     */
    public IPage<LogininforBo> selectLogininforList(IPage<LogininforBo> page, LogininforBo logininfor);

    /**
     * 查询系统登录日志集合（非分页，用于导出）
     *
     * @param logininfor 访问日志查询条件
     * @return 登录记录集合
     */
    public List<LogininforBo> selectLogininforList(LogininforBo logininfor);

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     */
    public void cleanLogininfor();
}
