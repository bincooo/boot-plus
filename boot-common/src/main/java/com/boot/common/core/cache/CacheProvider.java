package com.boot.common.core.cache;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存抽象接口
 *
 * @author boot-plus
 */
public interface CacheProvider
{
    /**
     * 获取缓存的基本对象
     *
     * @param cacheName 缓存名称
     * @param key 缓存键值
     * @param type 值类型
     * @return 缓存键值对应的数据，不存在或已过期返回 null
     */
    <T> T get(String cacheName, String key, Class<T> type);

    /**
     * 缓存基本的对象，永不过期
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    void put(String cacheName, String key, Object value);

    /**
     * 缓存基本的对象，并设置有效时间
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param ttl 有效时间
     * @param unit 时间单位
     */
    void put(String cacheName, String key, Object value, long ttl, TimeUnit unit);

    /**
     * 删除单个缓存对象
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     */
    void evict(String cacheName, String key);

    /**
     * 判断 key 是否存在
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     * @return true 存在 false 不存在
     */
    boolean hasKey(String cacheName, String key);

    /**
     * 获得指定缓存下的所有逻辑 key
     *
     * @param cacheName 缓存名称
     * @return key 集合
     */
    Collection<String> keys(String cacheName);

    /**
     * 清空指定缓存下的所有对象
     *
     * @param cacheName 缓存名称
     */
    void clear(String cacheName);

    /**
     * 原子自增并返回新值，首次创建时设置有效时间
     *
     * @param cacheName 缓存名称
     * @param key 缓存的键值
     * @param ttl 有效时间
     * @param unit 时间单位
     * @return 自增后的新值
     */
    long increment(String cacheName, String key, long ttl, TimeUnit unit);
}
