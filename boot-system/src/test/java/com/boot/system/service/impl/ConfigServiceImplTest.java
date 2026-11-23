package com.boot.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.exception.ServiceException;
import com.boot.system.domain.bo.ConfigBo;
import com.boot.system.repo.ConfigRepository;

/**
 * ConfigServiceImpl 缓存迁移与空值防护单元测试（无 Spring 上下文，直接构造 + Mockito）
 *
 * @author boot-plus
 */
class ConfigServiceImplTest
{
    private ConfigRepository configRepository;

    private CacheProvider cacheProvider;

    private ConfigServiceImpl service;

    @BeforeEach
    void setUp()
    {
        configRepository = mock(ConfigRepository.class);
        cacheProvider = mock(CacheProvider.class);
        service = new ConfigServiceImpl(configRepository, cacheProvider);
    }

    /**
     * 待更新的参数不存在时，应抛出 ServiceException 且不触发任何缓存与更新操作
     */
    @Test
    void updateConfigThrowsWhenConfigMissing()
    {
        ConfigBo config = new ConfigBo();
        config.setConfigId(1L);
        when(configRepository.selectConfigById(1L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateConfig(config));

        assertEquals("参数配置不存在", exception.getMessage());
        verify(configRepository, never()).updateConfig(any(ConfigBo.class));
        verify(cacheProvider, never()).evict(anyString(), anyString());
    }

    /**
     * 批量删除时，查不到的 ID 应跳过，其余 ID 正常删除并逐出缓存
     */
    @Test
    void deleteConfigByIdsSkipsMissingIds()
    {
        ConfigBo first = new ConfigBo();
        first.setConfigId(1L);
        first.setConfigKey("boot.name");
        first.setConfigType("N");
        when(configRepository.selectConfigById(1L)).thenReturn(first);

        service.deleteConfigByIds(new Long[] { 1L, 2L });

        verify(configRepository).deleteConfigById(1L);
        verify(cacheProvider).evict(CacheConstants.SYS_CONFIG_CACHE, "boot.name");
        verify(configRepository, never()).deleteConfigById(2L);
    }

    /**
     * 缓存未命中时应回源仓库查询，并回写缓存后返回
     */
    @Test
    void selectConfigByKeyFallsBackToRepository()
    {
        ConfigBo retConfig = new ConfigBo();
        retConfig.setConfigKey("boot.name");
        retConfig.setConfigValue("boot-plus");
        when(cacheProvider.get(CacheConstants.SYS_CONFIG_CACHE, "boot.name", String.class)).thenReturn(null);
        when(configRepository.selectConfigByKey("boot.name")).thenReturn(retConfig);

        String value = service.selectConfigByKey("boot.name");

        assertEquals("boot-plus", value);
        verify(cacheProvider).put(CacheConstants.SYS_CONFIG_CACHE, "boot.name", "boot-plus");
    }
}
