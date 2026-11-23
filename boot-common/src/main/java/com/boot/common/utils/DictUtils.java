package com.boot.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.bo.DictDataBo;
import com.boot.common.utils.spring.SpringUtils;

/**
 * 字典工具类
 * 
 * @author boot-plus
 */
public class DictUtils
{
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    /**
     * 设置字典缓存
     * 
     * @param key 参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<DictDataBo> dictDatas)
    {
        dictProvider().put(CacheConstants.SYS_DICT_CACHE, key, dictDatas);
    }

    /**
     * 获取字典缓存
     *
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    @SuppressWarnings("unchecked")
    public static List<DictDataBo> getDictCache(String key)
    {
        return dictProvider().get(CacheConstants.SYS_DICT_CACHE, key, List.class);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue)
    {
        if (StringUtils.isEmpty(dictValue))
        {
            return StringUtils.EMPTY;
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel)
    {
        if (StringUtils.isEmpty(dictLabel))
        {
            return StringUtils.EMPTY;
        }
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        List<DictDataBo> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas) || StringUtils.isEmpty(dictValue))
        {
            return StringUtils.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictValue(), dict.getDictLabel()), Map::putAll);
        if (!StringUtils.contains(dictValue, separator))
        {
            return dictMap.getOrDefault(dictValue, StringUtils.EMPTY);
        }
        StringBuilder labelBuilder = new StringBuilder();
        for (String seperatedValue : dictValue.split(separator))
        {
            if (dictMap.containsKey(seperatedValue))
            {
                labelBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return StringUtils.removeEnd(labelBuilder.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        List<DictDataBo> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas) || StringUtils.isEmpty(dictLabel))
        {
            return StringUtils.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictLabel(), dict.getDictValue()), Map::putAll);
        if (!StringUtils.contains(dictLabel, separator))
        {
            return dictMap.getOrDefault(dictLabel, StringUtils.EMPTY);
        }
        StringBuilder valueBuilder = new StringBuilder();
        for (String seperatedValue : dictLabel.split(separator))
        {
            if (dictMap.containsKey(seperatedValue))
            {
                valueBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return StringUtils.removeEnd(valueBuilder.toString(), separator);
    }

    /**
     * 根据字典类型获取字典所有值
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictValues(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<DictDataBo> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas))
        {
            return StringUtils.EMPTY;
        }
        for (DictDataBo dict : datas)
        {
            propertyString.append(dict.getDictValue()).append(SEPARATOR);
        }
        return StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 根据字典类型获取字典所有标签
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictLabels(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<DictDataBo> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas))
        {
            return StringUtils.EMPTY;
        }
        for (DictDataBo dict : datas)
        {
            propertyString.append(dict.getDictLabel()).append(SEPARATOR);
        }
        return StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 删除指定字典缓存
     * 
     * @param key 字典键
     */
    public static void removeDictCache(String key)
    {
        dictProvider().evict(CacheConstants.SYS_DICT_CACHE, key);
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        dictProvider().clear(CacheConstants.SYS_DICT_CACHE);
    }

    /**
     * 字典缓存（统一走 CacheProvider，caffeine 或 redis 由配置决定）
     */
    private static CacheProvider dictProvider()
    {
        return SpringUtils.getBean(CacheProvider.class);
    }
}
