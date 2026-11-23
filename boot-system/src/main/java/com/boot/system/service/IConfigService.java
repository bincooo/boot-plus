package com.boot.system.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.bo.ConfigBo;

/**
 * 参数配置 服务层
 *
 * @author boot-plus
 */
public interface IConfigService
{
    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    public ConfigBo selectConfigById(Long configId);

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数键值
     */
    public String selectConfigByKey(String configKey);

    /**
     * 获取验证码开关
     *
     * @return true开启，false关闭
     */
    public boolean selectCaptchaEnabled();

    /**
     * 分页查询参数配置列表
     *
     * @param page 分页参数
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    public IPage<ConfigBo> selectConfigList(IPage<ConfigBo> page, ConfigBo config);

    /**
     * 查询参数配置列表（非分页，用于导出）
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    public List<ConfigBo> selectConfigList(ConfigBo config);

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    public int insertConfig(ConfigBo config);

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    public int updateConfig(ConfigBo config);

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    public void deleteConfigByIds(Long[] configIds);

    /**
     * 加载参数缓存数据
     */
    public void loadingConfigCache();

    /**
     * 清空参数缓存数据
     */
    public void clearConfigCache();

    /**
     * 重置参数缓存数据
     */
    public void resetConfigCache();

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数信息
     * @return 结果
     */
    public boolean checkConfigKeyUnique(ConfigBo config);
}
