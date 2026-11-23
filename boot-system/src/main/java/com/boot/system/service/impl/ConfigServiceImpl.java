package com.boot.system.service.impl;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.common.constant.CacheConstants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.text.Convert;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.StringUtils;
import com.boot.system.repo.ConfigRepository;
import com.boot.system.service.IConfigService;
import com.boot.system.domain.bo.ConfigBo;

/**
 * 参数配置 服务层实现
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements IConfigService
{
    private final ConfigRepository configRepository;

    private final CacheProvider cacheProvider;

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public ConfigBo selectConfigById(Long configId)
    {
        return configRepository.selectConfigById(configId);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        String configValue = cacheProvider.get(CacheConstants.SYS_CONFIG_CACHE, configKey, String.class);
        if (StringUtils.isNotEmpty(configValue))
        {
            return configValue;
        }
        ConfigBo retConfig = configRepository.selectConfigByKey(configKey);
        if (StringUtils.isNotNull(retConfig))
        {
            configValue = retConfig.getConfigValue();
            cacheProvider.put(CacheConstants.SYS_CONFIG_CACHE, configKey, configValue);
            return configValue;
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取验证码开关
     *
     * @return true开启，false关闭
     */
    @Override
    public boolean selectCaptchaEnabled()
    {
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        if (StringUtils.isEmpty(captchaEnabled))
        {
            return true;
        }
        return Convert.toBool(captchaEnabled);
    }

    /**
     * 分页查询参数配置列表
     *
     * @param page 分页参数
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public IPage<ConfigBo> selectConfigList(IPage<ConfigBo> page, ConfigBo config)
    {
        return configRepository.selectConfigList(page, config);
    }

    /**
     * 查询参数配置列表（非分页，用于导出）
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<ConfigBo> selectConfigList(ConfigBo config)
    {
        return configRepository.selectConfigList(config);
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(ConfigBo config)
    {
        int row = configRepository.insertConfig(config);
        if (row > 0)
        {
            cacheProvider.put(CacheConstants.SYS_CONFIG_CACHE, config.getConfigKey(), config.getConfigValue());
        }
        return row;
    }

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(ConfigBo config)
    {
        ConfigBo temp = configRepository.selectConfigById(config.getConfigId());
        if (StringUtils.isNull(temp))
        {
            throw new ServiceException("参数配置不存在");
        }
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey()))
        {
            cacheProvider.evict(CacheConstants.SYS_CONFIG_CACHE, temp.getConfigKey());
        }

        int row = configRepository.updateConfig(config);
        if (row > 0)
        {
            cacheProvider.put(CacheConstants.SYS_CONFIG_CACHE, config.getConfigKey(), config.getConfigValue());
        }
        return row;
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            ConfigBo config = selectConfigById(configId);
            if (StringUtils.isNull(config))
            {
                continue;
            }
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configRepository.deleteConfigById(configId);
            cacheProvider.evict(CacheConstants.SYS_CONFIG_CACHE, config.getConfigKey());
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<ConfigBo> configsList = configRepository.selectConfigList(new ConfigBo());
        for (ConfigBo config : configsList)
        {
            cacheProvider.put(CacheConstants.SYS_CONFIG_CACHE, config.getConfigKey(), config.getConfigValue());
        }
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache()
    {
        cacheProvider.clear(CacheConstants.SYS_CONFIG_CACHE);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(ConfigBo config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        ConfigBo info = configRepository.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
