package com.boot.system.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.repo.OperLogRepository;
import com.boot.system.service.IOperLogService;
import com.boot.system.domain.bo.OperLogBo;

/**
 * 操作日志 服务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements IOperLogService
{
    private final OperLogRepository operLogRepository;

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(OperLogBo operLog)
    {
        operLogRepository.insertOperlog(operLog);
    }

    /**
     * 分页查询系统操作日志集合
     *
     * @param page 分页参数
     * @param operLog 操作日志查询条件
     * @return 操作日志集合
     */
    @Override
    public IPage<OperLogBo> selectOperLogList(IPage<OperLogBo> page, OperLogBo operLog)
    {
        return operLogRepository.selectOperLogList(page, operLog);
    }

    /**
     * 查询系统操作日志集合（非分页，用于导出）
     *
     * @param operLog 操作日志查询条件
     * @return 操作日志集合
     */
    @Override
    public List<OperLogBo> selectOperLogList(OperLogBo operLog)
    {
        return operLogRepository.selectOperLogList(operLog);
    }

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds)
    {
        return operLogRepository.deleteOperLogByIds(operIds);
    }

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public OperLogBo selectOperLogById(Long operId)
    {
        return operLogRepository.selectOperLogById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog()
    {
        operLogRepository.cleanOperLog();
    }
}
