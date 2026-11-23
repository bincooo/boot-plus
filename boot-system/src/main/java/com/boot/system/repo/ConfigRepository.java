package com.boot.system.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.ConfigMapper;
import com.boot.system.repo.po.Config;
import com.boot.system.domain.bo.ConfigBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 参数配置 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class ConfigRepository
{
    private final ConfigMapper configMapper;

    private final Converter converter;

    /**
     * 分页查询参数配置集合
     *
     * @param page 分页参数
     * @param query 参数配置查询条件
     * @return 参数配置集合
     */
    public IPage<ConfigBo> selectConfigList(IPage<ConfigBo> page, ConfigBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<Config> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        configMapper.selectPage(poPage, buildConfigListQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, ConfigBo.class));
    }

    /**
     * 查询参数配置集合（非分页，用于导出）
     *
     * @param query 参数配置查询条件
     * @return 参数配置集合
     */
    public List<ConfigBo> selectConfigList(ConfigBo query)
    {
        return PageUtils.convert(configMapper.selectList(buildConfigListQuery(query)),
                po -> converter.convert(po, ConfigBo.class));
    }

    /**
     * 查询参数配置信息
     *
     * @param query 参数配置查询条件
     * @return 参数配置信息
     */
    public ConfigBo selectConfig(ConfigBo query)
    {
        Config config = configMapper.selectOne(Wrappers.lambdaQuery(Config.class)
                .eq(query.getConfigId() != null, Config::getConfigId, query.getConfigId())
                .eq(query.getConfigKey() != null && !query.getConfigKey().isEmpty(),
                        Config::getConfigKey, query.getConfigKey()));
        return converter.convert(config, ConfigBo.class);
    }

    /**
     * 通过ID查询配置
     *
     * @param configId 参数ID
     * @return 参数配置信息
     */
    public ConfigBo selectConfigById(Long configId)
    {
        return converter.convert(configMapper.selectById(configId), ConfigBo.class);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数配置信息
     */
    public ConfigBo selectConfigByKey(String configKey)
    {
        return checkConfigKeyUnique(configKey);
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param configKey 参数键名
     * @return 参数配置信息
     */
    public ConfigBo checkConfigKeyUnique(String configKey)
    {
        Config config = configMapper.selectOne(Wrappers.lambdaQuery(Config.class)
                .eq(Config::getConfigKey, configKey)
                .last("limit 1"));
        return converter.convert(config, ConfigBo.class);
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    public int insertConfig(ConfigBo config)
    {
        Config po = converter.convert(config, Config.class);
        po.setCreateTime(DateUtils.getNowDate());
        return configMapper.insert(po);
    }

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    public int updateConfig(ConfigBo config)
    {
        Config po = converter.convert(config, Config.class);
        // 镜像原 XML updateConfig 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return configMapper.update(null, Wrappers.lambdaUpdate(Config.class)
                .set(po.getConfigName() != null && !po.getConfigName().isEmpty(), Config::getConfigName, po.getConfigName())
                .set(po.getConfigKey() != null && !po.getConfigKey().isEmpty(), Config::getConfigKey, po.getConfigKey())
                .set(po.getConfigValue() != null && !po.getConfigValue().isEmpty(), Config::getConfigValue, po.getConfigValue())
                .set(po.getConfigType() != null && !po.getConfigType().isEmpty(), Config::getConfigType, po.getConfigType())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Config::getUpdateBy, po.getUpdateBy())
                .set(po.getRemark() != null, Config::getRemark, po.getRemark())
                .set(Config::getUpdateTime, DateUtils.getNowDate())
                .eq(Config::getConfigId, po.getConfigId()));
    }

    /**
     * 删除参数配置
     *
     * @param configId 参数ID
     * @return 结果
     */
    public int deleteConfigById(Long configId)
    {
        return configMapper.deleteById(configId);
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     * @return 结果
     */
    public int deleteConfigByIds(Long[] configIds)
    {
        return configMapper.deleteByIds(Arrays.asList(configIds));
    }

    /**
     * 构建参数配置列表查询条件（与原 XML selectConfigList 的动态条件一致）
     *
     * @param query 参数配置查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<Config> buildConfigListQuery(ConfigBo query)
    {
        LambdaQueryWrapper<Config> wrapper = Wrappers.lambdaQuery(Config.class);
        if (query.getConfigName() != null && !query.getConfigName().isEmpty())
        {
            wrapper.like(Config::getConfigName, query.getConfigName());
        }
        if (query.getConfigType() != null && !query.getConfigType().isEmpty())
        {
            wrapper.eq(Config::getConfigType, query.getConfigType());
        }
        if (query.getConfigKey() != null && !query.getConfigKey().isEmpty())
        {
            wrapper.like(Config::getConfigKey, query.getConfigKey());
        }
        Object beginTime = query.getParams().get("beginTime");
        if (beginTime != null && !"".equals(beginTime))
        {
            wrapper.apply("cast(create_time as date) >= cast({0} as date)", beginTime);
        }
        Object endTime = query.getParams().get("endTime");
        if (endTime != null && !"".equals(endTime))
        {
            wrapper.apply("cast(create_time as date) <= cast({0} as date)", endTime);
        }
        return wrapper;
    }
}
