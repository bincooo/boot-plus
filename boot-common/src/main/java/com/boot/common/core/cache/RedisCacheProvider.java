package com.boot.common.core.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 基于 Redis 的统一缓存实现
 *
 * <p>与 {@link CaffeineCacheProvider} 保持一致：ttl <= 0 表示永不过期
 *
 * @author boot-plus
 **/
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@SuppressWarnings(value = { "unchecked", "rawtypes" })
public class RedisCacheProvider implements CacheProvider
{
    /**
     * 缓存 key 的前缀分隔符
     */
    private static final String KEY_SEPARATOR = ":";

    /**
     * 原子自增脚本，计数上限取 Long.MAX_VALUE 使其退化为原子 incr + 首次 expire
     */
    private final DefaultRedisScript<Long> limitScript = buildLimitScript();

    /**
     * spring redis 模板，序列化与反序列化由模板承担
     */
    private final RedisTemplate redisTemplate;

    /**
     * 构建限流脚本：原子自增，首次自增时设置 TTL（上限传 Long.MAX_VALUE 即退化为普通自增）
     *
     * @return 限流脚本
     */
    private static DefaultRedisScript<Long> buildLimitScript()
    {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText("""
                local key = KEYS[1]
                local count = toNumber(ARGV[1])
                local time = toNumber(ARGV[2])
                local current = redis.call('get', key)
                if current and toNumber(current) > count then
                    return toNumber(current);
                end
                current = redis.call('incr', key)
                if toNumber(current) == 1 then
                    redis.call('expire', key, time)
                end
                return toNumber(current)
            """);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 拼接带缓存名称前缀的完整 key
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     * @return 完整 key
     */
    private String fullKey(final String cacheName, final String key)
    {
        return cacheName + KEY_SEPARATOR + key;
    }

    @Override
    public <T> T get(final String cacheName, final String key, final Class<T> type)
    {
        return (T) redisTemplate.opsForValue().get(fullKey(cacheName, key));
    }

    @Override
    public void put(final String cacheName, final String key, final Object value)
    {
        redisTemplate.opsForValue().set(fullKey(cacheName, key), value);
    }

    @Override
    public void put(final String cacheName, final String key, final Object value, final long ttl, final TimeUnit unit)
    {
        // ttl <= 0 表示永不过期
        if (ttl > 0)
        {
            redisTemplate.opsForValue().set(fullKey(cacheName, key), value, Duration.of(ttl, unit.toChronoUnit()));
        }
        else
        {
            put(cacheName, key, value);
        }
    }

    @Override
    public void evict(final String cacheName, final String key)
    {
        redisTemplate.delete(fullKey(cacheName, key));
    }

    @Override
    public boolean hasKey(final String cacheName, final String key)
    {
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey(cacheName, key)));
    }

    @Override
    public Collection<String> keys(final String cacheName)
    {
        Set<String> fullKeys = redisTemplate.keys(cacheName + KEY_SEPARATOR + "*");
        if (fullKeys == null)
        {
            return Collections.emptyList();
        }
        String prefix = cacheName + KEY_SEPARATOR;
        // 剥离前缀后仍以 ":" 开头的为历史双冒号孤儿数据，直接跳过
        return fullKeys.stream()
            .filter(full -> full.startsWith(prefix))
            .map(full -> full.substring(prefix.length()))
            .filter(key -> !key.startsWith(KEY_SEPARATOR))
            .collect(Collectors.toList());
    }

    @Override
    public void clear(final String cacheName)
    {
        Set<String> fullKeys = redisTemplate.keys(cacheName + KEY_SEPARATOR + "*");
        if (fullKeys != null && !fullKeys.isEmpty())
        {
            redisTemplate.delete(fullKeys);
        }
    }

    @Override
    public long increment(final String cacheName, final String key, final long ttl, final TimeUnit unit)
    {
        // 计数上限取 Long.MAX_VALUE：脚本比较恒不成立，退化为原子 incr + 首次 expire
        Long count = (Long) redisTemplate.execute(limitScript, List.of(fullKey(cacheName, key)), Long.MAX_VALUE, unit.toSeconds(ttl));
        return count == null ? 0L : count;
    }
}
