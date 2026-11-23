package com.boot.system.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.bo.OperLogBo;

/**
 * 操作日志 服务层
 *
 * @author boot-plus
 */
public interface IOperLogService
{
    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    public void insertOperlog(OperLogBo operLog);

    /**
     * 分页查询系统操作日志集合
     *
     * @param page 分页参数
     * @param operLog 操作日志查询条件
     * @return 操作日志集合
     */
    public IPage<OperLogBo> selectOperLogList(IPage<OperLogBo> page, OperLogBo operLog);

    /**
     * 查询系统操作日志集合（非分页，用于导出）
     *
     * @param operLog 操作日志查询条件
     * @return 操作日志集合
     */
    public List<OperLogBo> selectOperLogList(OperLogBo operLog);

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    public int deleteOperLogByIds(Long[] operIds);

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    public OperLogBo selectOperLogById(Long operId);

    /**
     * 清空操作日志
     */
    public void cleanOperLog();
}
