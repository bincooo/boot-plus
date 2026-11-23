package com.boot.common.core.cache;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于 Caffeine 的统一缓存实现
 *
 * @author boot-plus
 */
@Component
@ConditionalOnClass(Cache.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
public class CaffeineCacheProvider implements CacheProvider
{
    /**
     * spec 字符串中 maximumSize 配置的解析规则
     */
    private static final Pattern MAXIMUM_SIZE_PATTERN = Pattern.compile("maximumSize\\s*=\\s*(\\d+)");

    /**
     * 每个 cacheName 惰性持有的本地缓存
     */
    private final ConcurrentHashMap<String, Cache<String, ExpirableValue>> caches = new ConcurrentHashMap<>();

    /**
     * Caffeine 过期策略：按条目自身的过期时间戳判定
     */
    private final Expiry<String, ExpirableValue> expiry = new Expiry<>()
    {
        @Override
        public long expireAfterCreate(final String key, final ExpirableValue value, final long currentTime)
        {
            return value.remainingNanos(currentTime);
        }

        @Override
        public long expireAfterUpdate(final String key, final ExpirableValue value, final long currentTime, final long currentDuration)
        {
            return value.remainingNanos(currentTime);
        }

        @Override
        public long expireAfterRead(final String key, final ExpirableValue value, final long currentTime, final long currentDuration)
        {
            return value.remainingNanos(currentTime);
        }
    };

    /**
     * spec 字符串解析出的 maximumSize 上限，null 表示不设上限
     */
    private final Long maximumSize;

    /**
     * 构造本地缓存工厂，spec 为空则不设 maximumSize 上限
     *
     * @param spec spring.cache.caffeine.spec 配置字符串
     */
    public CaffeineCacheProvider(@Value("${spring.cache.caffeine.spec:}") final String spec)
    {
        Matcher matcher = MAXIMUM_SIZE_PATTERN.matcher(spec);
        this.maximumSize = matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    /**
     * 构建一个本地缓存实例
     *
     * @return 本地缓存实例
     */
    private Cache<String, ExpirableValue> buildCache()
    {
        Caffeine<String, ExpirableValue> builder = Caffeine.newBuilder().expireAfter(expiry);
        if (maximumSize != null)
        {
            builder.maximumSize(maximumSize);
        }
        return builder.build();
    }

    /**
     * 获取指定 cacheName 对应的本地缓存，不存在则创建
     *
     * @param cacheName 缓存名称
     * @return 本地缓存实例
     */
    private Cache<String, ExpirableValue> cacheOf(final String cacheName)
    {
        return caches.computeIfAbsent(cacheName, name -> buildCache());
    }

    @Override
    public <T> T get(final String cacheName, final String key, final Class<T> type)
    {
        ExpirableValue expirable = cacheOf(cacheName).getIfPresent(key);
        return expirable == null ? null : type.cast(expirable.value);
    }

    @Override
    public void put(final String cacheName, final String key, final Object value)
    {
        cacheOf(cacheName).put(key, new ExpirableValue(value, 0L));
    }

    @Override
    public void put(final String cacheName, final String key, final Object value, final long ttl, final TimeUnit unit)
    {
        cacheOf(cacheName).put(key, new ExpirableValue(value, expireAtNanos(ttl, unit)));
    }

    @Override
    public void evict(final String cacheName, final String key)
    {
        cacheOf(cacheName).invalidate(key);
    }

    @Override
    public boolean hasKey(final String cacheName, final String key)
    {
        return cacheOf(cacheName).getIfPresent(key) != null;
    }

    @Override
    public Collection<String> keys(final String cacheName)
    {
        // getIfPresent 触发过期判定，过滤掉已过期但未被回收的条目
        return cacheOf(cacheName).asMap().keySet().stream()
            .filter(key -> hasKey(cacheName, key))
            .collect(Collectors.toList());
    }

    @Override
    public void clear(final String cacheName)
    {
        cacheOf(cacheName).invalidateAll();
    }

    @Override
    public long increment(final String cacheName, final String key, final long ttl, final TimeUnit unit)
    {
        // asMap().compute 保证自增的原子性：首建时设置 TTL，命中则保留原过期时间
        ExpirableValue result = cacheOf(cacheName).asMap().compute(key, (k, old) ->
        {
            long now = System.nanoTime();
            if (old != null && old.isExpired(now))
            {
                old = null;
            }
            if (old == null)
            {
                return new ExpirableValue(new AtomicLong(1), expireAtNanos(ttl, unit));
            }
            ((AtomicLong) old.value).incrementAndGet();
            return old;
        });
        return ((AtomicLong) result.value).get();
    }

    /**
     * 计算条目的过期时间戳
     *
     * @param ttl 有效时间
     * @param unit 时间单位
     * @return 过期时间戳（nanoTime），0 表示永不过期
     */
    private long expireAtNanos(final long ttl, final TimeUnit unit)
    {
        return ttl > 0 ? System.nanoTime() + unit.toNanos(ttl) : 0L;
    }

    /**
     * 带过期时间的缓存值
     */
    private static class ExpirableValue
    {
        /**
         * 缓存的值（计数器场景为 AtomicLong）
         */
        private final Object value;

        /**
         * 过期时间戳（nanoTime），0 表示永不过期
         */
        private final long expireAtNanos;

        ExpirableValue(final Object value, final long expireAtNanos)
        {
            this.value = value;
            this.expireAtNanos = expireAtNanos;
        }

        /**
         * 计算距过期的剩余时长
         *
         * @param currentTime 当前 nanoTime
         * @return 剩余时长，永不过期返回 Long.MAX_VALUE
         */
        long remainingNanos(final long currentTime)
        {
            return expireAtNanos == 0 ? Long.MAX_VALUE : expireAtNanos - currentTime;
        }

        /**
         * 判断条目是否已过期
         *
         * @param currentTime 当前 nanoTime
         * @return true 已过期
         */
        boolean isExpired(final long currentTime)
        {
            return expireAtNanos != 0 && currentTime >= expireAtNanos;
        }
    }
}
