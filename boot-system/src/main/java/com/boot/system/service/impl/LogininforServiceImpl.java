package com.boot.system.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.repo.LogininforRepository;
import com.boot.system.service.ILogininforService;
import com.boot.system.domain.bo.LogininforBo;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class LogininforServiceImpl implements ILogininforService
{

    private final LogininforRepository logininforRepository;

    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(LogininforBo logininfor)
    {
        logininforRepository.insertLogininfor(logininfor);
    }

    /**
     * 分页查询系统登录日志集合
     *
     * @param page 分页参数
     * @param logininfor 访问日志查询条件
     * @return 登录记录集合
     */
    @Override
    public IPage<LogininforBo> selectLogininforList(IPage<LogininforBo> page, LogininforBo logininfor)
    {
        return logininforRepository.selectLogininforList(page, logininfor);
    }

    /**
     * 查询系统登录日志集合（非分页，用于导出）
     *
     * @param logininfor 访问日志查询条件
     * @return 登录记录集合
     */
    @Override
    public List<LogininforBo> selectLogininforList(LogininforBo logininfor)
    {
        return logininforRepository.selectLogininforList(logininfor);
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return logininforRepository.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor()
    {
        logininforRepository.cleanLogininfor();
    }
}
