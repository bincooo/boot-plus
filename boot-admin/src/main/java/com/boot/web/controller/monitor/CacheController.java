package com.boot.web.controller.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.alibaba.fastjson2.JSON;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.utils.StringUtils;
import com.boot.system.domain.vo.CacheVo;

/**
 * 缓存监控
 *
 * @author boot-plus
 */
@RestController
@RequestMapping("/monitor/cache")
@RequiredArgsConstructor
public class CacheController
{
    private final CacheProvider cacheProvider;

    /** 仅用于 getInfo 读取 Redis 服务器信息 */
    private final RedisTemplate<String, String> redisTemplate;

    private final static List<CacheVo> caches = new ArrayList<CacheVo>();

    private final static List<String> cacheNames = Arrays.asList(
            CacheConstants.LOGIN_TOKEN_CACHE,
            CacheConstants.SYS_CONFIG_CACHE,
            CacheConstants.SYS_DICT_CACHE,
            CacheConstants.CAPTCHA_CODE_CACHE,
            CacheConstants.REPEAT_SUBMIT_CACHE,
            CacheConstants.RATE_LIMIT_CACHE,
            CacheConstants.PWD_ERR_CNT_CACHE);

    static
    {
        caches.add(new CacheVo(CacheConstants.LOGIN_TOKEN_CACHE, "用户信息"));
        caches.add(new CacheVo(CacheConstants.SYS_CONFIG_CACHE, "配置信息"));
        caches.add(new CacheVo(CacheConstants.SYS_DICT_CACHE, "数据字典"));
        caches.add(new CacheVo(CacheConstants.CAPTCHA_CODE_CACHE, "验证码"));
        caches.add(new CacheVo(CacheConstants.REPEAT_SUBMIT_CACHE, "防重提交"));
        caches.add(new CacheVo(CacheConstants.RATE_LIMIT_CACHE, "限流处理"));
        caches.add(new CacheVo(CacheConstants.PWD_ERR_CNT_CACHE, "密码错误次数"));
    }

    @SuppressWarnings("deprecation")
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping()
    public AjaxResult getInfo()
    {
        Properties info;
        Properties commandStats;
        Object dbSize;
        try
        {
            info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());
            commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));
            dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());
        }
        catch (Exception e)
        {
            // Redis 不可用时降级：信息为空、键数量为 0、命令统计为空列表
            info = new Properties();
            commandStats = new Properties();
            dbSize = 0L;
        }

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);
        result.put("commandStats", buildCommandStats(commandStats));
        return AjaxResult.success(result);
    }

    /**
     * 将 Redis 命令统计信息转换为前端饼图数据列表
     */
    private static List<Map<String, String>> buildCommandStats(Properties commandStats)
    {
        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_"));
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
            pieList.add(data);
        });
        return pieList;
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getNames")
    public AjaxResult cache()
    {
        return AjaxResult.success(caches);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName)
    {
        Set<String> cacheKeys = new TreeSet<>();
        for (String key : cacheProvider.keys(cacheName))
        {
            cacheKeys.add(cacheName + ":" + key);
        }
        return AjaxResult.success(cacheKeys);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        String prefix = cacheName + ":";
        String logicalKey = cacheKey.startsWith(prefix) ? cacheKey.substring(prefix.length()) : cacheKey;
        String cacheValue;
        try
        {
            Object value = cacheProvider.get(cacheName, logicalKey, Object.class);
            if (value == null)
            {
                cacheValue = null;
            }
            else if (value instanceof String)
            {
                cacheValue = (String) value;
            }
            else
            {
                cacheValue = JSON.toJSONString(value);
            }
        }
        catch (Exception e)
        {
            // 反序列化失败（如遗留 JDK 序列化条目）时降级返回空串，不抛 500
            cacheValue = "";
        }
        CacheVo sysCache = new CacheVo(cacheName, logicalKey, cacheValue);
        return AjaxResult.success(sysCache);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName)
    {
        cacheProvider.clear(cacheName);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey)
    {
        int idx = cacheKey.indexOf(':');
        if (idx <= 0)
        {
            return AjaxResult.error("缓存键名格式错误");
        }
        cacheProvider.evict(cacheKey.substring(0, idx), cacheKey.substring(idx + 1));
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public AjaxResult clearCacheAll()
    {
        cacheNames.forEach(cacheProvider::clear);
        return AjaxResult.success();
    }
}
