package com.boot.common.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * RedisCacheProvider 单元测试
 *
 * @author boot-plus
 */
@SuppressWarnings(value = { "unchecked", "rawtypes" })
class RedisCacheProviderTest
{
    private RedisTemplate redisTemplate;

    private ValueOperations valueOperations;

    private RedisCacheProvider provider;

    @BeforeEach
    void setUp()
    {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        provider = new RedisCacheProvider(redisTemplate);
    }

    @Test
    void getUsesPrefixedKey()
    {
        when(valueOperations.get("sys_config:k")).thenReturn("v");
        assertEquals("v", provider.get("sys_config", "k", String.class));
    }

    @Test
    void putWithMillisTtlUsesDuration()
    {
        provider.put("repeat_submit", "k", "v", 500, TimeUnit.MILLISECONDS);
        verify(valueOperations).set(eq("repeat_submit:k"), any(), eq(Duration.ofMillis(500)));
    }

    @Test
    void keysStripsPrefixAndSkipsLegacyDoubleColon()
    {
        when(redisTemplate.keys("sys_config:*")).thenReturn(Set.of("sys_config:a", "sys_config::legacy"));
        Collection<String> keys = provider.keys("sys_config");
        assertEquals(List.of("a"), List.copyOf(keys));
    }

    @Test
    void clearDeletesAllPrefixedKeys()
    {
        when(redisTemplate.keys("sys_config:*")).thenReturn(Set.of("sys_config:a", "sys_config:b"));
        provider.clear("sys_config");
        // 一次 delete(Collection) 删除全部前缀 key，而非逐个删除
        verify(redisTemplate).keys("sys_config:*");
        verify(redisTemplate, times(1)).delete((Collection) argThat(argument -> {
            Collection collection = (Collection) argument;
            return collection.contains("sys_config:a") && collection.contains("sys_config:b");
        }));
        Mockito.verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void incrementCallsScriptAndReturnsCount()
    {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(3L);
        long count = provider.increment("rate_limit", "k", 60, TimeUnit.SECONDS);
        assertEquals(3L, count);
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("rate_limit:k")), eq(Long.MAX_VALUE), eq(60L));
    }

    @Test
    void hasKeyDelegates()
    {
        when(redisTemplate.hasKey("captcha_codes:k")).thenReturn(true);
        assertTrue(provider.hasKey("captcha_codes", "k"));
    }
}
