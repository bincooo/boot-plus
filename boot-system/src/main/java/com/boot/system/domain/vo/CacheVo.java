package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import com.boot.common.utils.StringUtils;
import lombok.Data;

/**
 * 缓存信息视图对象
 *
 * @author boot-plus
 */
@Data
public class CacheVo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 缓存名称 */
    private String cacheName = "";

    /** 缓存键名 */
    private String cacheKey = "";

    /** 缓存内容 */
    private String cacheValue = "";

    /** 备注 */
    private String remark = "";

    public CacheVo()
    {

    }

    public CacheVo(String cacheName, String remark)
    {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    public CacheVo(String cacheName, String cacheKey, String cacheValue)
    {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }
}
