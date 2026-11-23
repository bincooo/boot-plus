package com.boot.web.controller.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.AjaxResult;
import com.boot.system.domain.vo.CacheVo;

/**
 * 缓存监控 CacheController 单元测试（无 Spring 上下文，直接构造 + Mockito）
 *
 * @author boot-plus
 */
class CacheControllerTest
{
    private CacheProvider cacheProvider;

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, String> redisTemplate;

    private CacheController controller;

    @BeforeEach
    void setUp()
    {
        cacheProvider = mock(CacheProvider.class);
        redisTemplate = mock(RedisTemplate.class);
        controller = new CacheController(cacheProvider, redisTemplate);
    }

    /**
     * Redis 不可用时 getInfo 应降级返回：dbSize 为 0，commandStats 为空列表，整体仍为成功响应
     */
    @Test
    void getInfoReturnsEmptyWhenRedisDown()
    {
        doThrow(new RuntimeException("redis down")).when(redisTemplate).execute(any(RedisCallback.class));

        AjaxResult ajax = controller.getInfo();

        assertTrue(ajax.isSuccess());
        Map<?, ?> result = (Map<?, ?>) ajax.get(AjaxResult.DATA_TAG);
        assertEquals(0L, result.get("dbSize"));
        assertTrue(((List<?>) result.get("commandStats")).isEmpty());
    }

    /**
     * 缓存键名不含 cacheName 前缀时应返回错误，且不触发任何逐出操作
     */
    @Test
    void clearCacheKeyRejectsKeyWithoutPrefix()
    {
        AjaxResult ajax = controller.clearCacheKey("badkey");

        assertEquals(500, ajax.get(AjaxResult.CODE_TAG));
        verify(cacheProvider, never()).evict(anyString(), anyString());
    }

    /**
     * 缓存键名带 cacheName 前缀时应拆分出缓存名与逻辑 key 并逐出
     */
    @Test
    void clearCacheKeyParsesPrefixAndEvicts()
    {
        AjaxResult ajax = controller.clearCacheKey("login_tokens:u1");

        assertTrue(ajax.isSuccess());
        verify(cacheProvider).evict("login_tokens", "u1");
    }

    /**
     * getKeys 应将逻辑 key 重新拼上 cacheName 前缀，保证前端展示兼容
     */
    @Test
    void getKeysPrefixesLogicalKeys()
    {
        when(cacheProvider.keys("login_tokens")).thenReturn(Collections.singletonList("u1"));

        AjaxResult ajax = controller.getCacheKeys("login_tokens");

        assertEquals(Collections.singleton("login_tokens:u1"), ajax.get(AjaxResult.DATA_TAG));
    }

    /**
     * getValue 遇到无法反序列化的缓存条目时应降级返回空串，不抛异常，整体仍为成功响应
     */
    @Test
    void getValueToleratesDeserializationFailure()
    {
        when(cacheProvider.get("sys_config", "x", Object.class)).thenThrow(new RuntimeException("bad entry"));

        AjaxResult ajax = controller.getCacheValue("sys_config", "sys_config:x");

        assertTrue(ajax.isSuccess());
        CacheVo cacheVo = (CacheVo) ajax.get(AjaxResult.DATA_TAG);
        assertEquals("", cacheVo.getCacheValue());
    }

    /**
     * getValue 应剥离 cacheName 前缀取逻辑 key 读缓存，String 值原样返回
     */
    @Test
    void getValueStripsPrefixAndReturnsValue()
    {
        when(cacheProvider.get("login_tokens", "u1", Object.class)).thenReturn("abc");

        AjaxResult ajax = controller.getCacheValue("login_tokens", "login_tokens:u1");

        CacheVo cacheVo = (CacheVo) ajax.get(AjaxResult.DATA_TAG);
        assertEquals("u1", cacheVo.getCacheKey());
        assertEquals("abc", cacheVo.getCacheValue());
    }
}
