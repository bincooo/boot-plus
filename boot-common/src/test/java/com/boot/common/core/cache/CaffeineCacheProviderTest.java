package com.boot.common.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * CaffeineCacheProvider 单元测试
 *
 * @author boot-plus
 */
class CaffeineCacheProviderTest
{
    @Test
    void putGetWithoutTtlSurvives()
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1");
        assertEquals("v1", provider.get("sys_config", "k1", String.class));
    }

    @Test
    void putWithTtlExpires() throws InterruptedException
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1", 50, TimeUnit.MILLISECONDS);
        assertEquals("v1", provider.get("sys_config", "k1", String.class));
        Thread.sleep(150);
        assertNull(provider.get("sys_config", "k1", String.class));
    }

    @Test
    void rePutWithTtlReplacesNoTtl() throws InterruptedException
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1");
        // 重新 put 带较短 TTL：新条目的过期时间应覆盖旧的“永不过期”
        provider.put("sys_config", "k1", "v2", 50, TimeUnit.MILLISECONDS);
        Thread.sleep(150);
        assertNull(provider.get("sys_config", "k1", String.class));
    }

    @Test
    void evictAndHasKey()
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1");
        assertTrue(provider.hasKey("sys_config", "k1"));
        provider.evict("sys_config", "k1");
        assertFalse(provider.hasKey("sys_config", "k1"));
    }

    @Test
    void keysReturnsLogicalKeys()
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1");
        provider.put("sys_config", "k2", "v2");
        Collection<String> keys = provider.keys("sys_config");
        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
    }

    @Test
    void clearRemovesAll()
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        provider.put("sys_config", "k1", "v1");
        provider.put("sys_config", "k2", "v2");
        provider.clear("sys_config");
        assertTrue(provider.keys("sys_config").isEmpty());
        assertFalse(provider.hasKey("sys_config", "k1"));
    }

    @Test
    void incrementCountsAndExpires() throws InterruptedException
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("");
        assertEquals(1, provider.increment("pwd_err_cnt", "user1", 100, TimeUnit.MILLISECONDS));
        assertEquals(2, provider.increment("pwd_err_cnt", "user1", 100, TimeUnit.MILLISECONDS));
        // TTL 过期后重新自增，从 1 开始计数
        Thread.sleep(150);
        assertEquals(1, provider.increment("pwd_err_cnt", "user1", 100, TimeUnit.MILLISECONDS));
    }

    @Test
    void specParsingAcceptsMaximumSize()
    {
        CaffeineCacheProvider provider = new CaffeineCacheProvider("maximumSize=10000");
        provider.put("sys_config", "k1", "v1");
        assertEquals("v1", provider.get("sys_config", "k1", String.class));
    }
}
