package com.boot.system.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.core.domain.bo.DictTypeBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.DictDataMapper;
import com.boot.system.repo.mapper.DictTypeMapper;
import com.boot.system.repo.po.DictData;
import com.boot.system.repo.po.DictType;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 字典类型 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class DictTypeRepository
{
    private final DictTypeMapper dictTypeMapper;

    private final DictDataMapper dictDataMapper;

    private final Converter converter;

    /**
     * 分页查询字典类型集合
     *
     * @param page 分页参数
     * @param query 字典类型查询条件
     * @return 字典类型集合信息
     */
    public IPage<DictTypeBo> selectDictTypeList(IPage<DictTypeBo> page, DictTypeBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<DictType> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        dictTypeMapper.selectPage(poPage, buildDictTypeListQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, DictTypeBo.class));
    }

    /**
     * 查询字典类型集合（非分页，用于导出）
     *
     * @param query 字典类型查询条件
     * @return 字典类型集合信息
     */
    public List<DictTypeBo> selectDictTypeList(DictTypeBo query)
    {
        return PageUtils.convert(dictTypeMapper.selectList(buildDictTypeListQuery(query)),
                po -> converter.convert(po, DictTypeBo.class));
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    public List<DictTypeBo> selectDictTypeAll()
    {
        return PageUtils.convert(dictTypeMapper.selectList(dictTypeColumnWrapper()),
                po -> converter.convert(po, DictTypeBo.class));
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    public DictTypeBo selectDictTypeById(Long dictId)
    {
        DictType dictType = dictTypeMapper.selectOne(dictTypeColumnWrapper().eq(DictType::getDictId, dictId));
        return converter.convert(dictType, DictTypeBo.class);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    public DictTypeBo selectDictTypeByType(String dictType)
    {
        DictType po = dictTypeMapper.selectOne(dictTypeColumnWrapper().eq(DictType::getDictType, dictType));
        return converter.convert(po, DictTypeBo.class);
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dictType 字典类型
     * @return 结果
     */
    public DictTypeBo checkDictTypeUnique(String dictType)
    {
        DictType po = dictTypeMapper.selectOne(dictTypeColumnWrapper()
                .eq(DictType::getDictType, dictType)
                .last("limit 1"));
        return converter.convert(po, DictTypeBo.class);
    }

    /**
     * 查询字典数据数量
     *
     * @param dictType 字典类型
     * @return 字典数据数量
     */
    public int countDictDataByType(String dictType)
    {
        Long count = dictDataMapper.selectCount(Wrappers.lambdaQuery(DictData.class).eq(DictData::getDictType, dictType));
        return count.intValue();
    }

    /**
     * 新增字典类型信息
     *
     * @param dictType 字典类型信息
     * @return 结果
     */
    public int insertDictType(DictTypeBo dictType)
    {
        DictType po = converter.convert(dictType, DictType.class);
        po.setCreateTime(DateUtils.getNowDate());
        return dictTypeMapper.insert(po);
    }

    /**
     * 修改字典类型信息（含字典数据类型同步）
     *
     * @param dictType 字典类型信息
     * @return 结果
     */
    public int updateDictType(DictTypeBo dictType)
    {
        DictType oldDict = dictTypeMapper.selectOne(dictTypeColumnWrapper().eq(DictType::getDictId, dictType.getDictId()));
        if (oldDict != null)
        {
            // 同步修改字典数据类型
            dictDataMapper.update(null, Wrappers.lambdaUpdate(DictData.class)
                    .set(DictData::getDictType, dictType.getDictType())
                    .eq(DictData::getDictType, oldDict.getDictType()));
        }
        DictType po = converter.convert(dictType, DictType.class);
        // 镜像原 XML updateDictType 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return dictTypeMapper.update(null, Wrappers.lambdaUpdate(DictType.class)
                .set(po.getDictName() != null && !po.getDictName().isEmpty(), DictType::getDictName, po.getDictName())
                .set(po.getDictType() != null && !po.getDictType().isEmpty(), DictType::getDictType, po.getDictType())
                .set(po.getStatus() != null, DictType::getStatus, po.getStatus())
                .set(po.getRemark() != null, DictType::getRemark, po.getRemark())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), DictType::getUpdateBy, po.getUpdateBy())
                .set(DictType::getUpdateTime, DateUtils.getNowDate())
                .eq(DictType::getDictId, po.getDictId()));
    }

    /**
     * 通过字典ID删除字典信息
     *
     * @param dictId 字典ID
     * @return 结果
     */
    public int deleteDictTypeById(Long dictId)
    {
        return dictTypeMapper.deleteById(dictId);
    }

    /**
     * 构建与原 XML selectDictTypeVo 相同列清单的查询壳
     */
    private LambdaQueryWrapper<DictType> dictTypeColumnWrapper()
    {
        return Wrappers.lambdaQuery(DictType.class)
                .select(DictType::getDictId, DictType::getDictName, DictType::getDictType,
                        DictType::getStatus, DictType::getCreateBy, DictType::getCreateTime, DictType::getRemark);
    }

    /**
     * 构建字典类型列表查询条件（与原 XML selectDictTypeList 的动态条件一致）
     *
     * @param query 字典类型查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<DictType> buildDictTypeListQuery(DictTypeBo query)
    {
        LambdaQueryWrapper<DictType> wrapper = dictTypeColumnWrapper();
        if (query.getDictName() != null && !query.getDictName().isEmpty())
        {
            wrapper.like(DictType::getDictName, query.getDictName());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(DictType::getStatus, query.getStatus());
        }
        if (query.getDictType() != null && !query.getDictType().isEmpty())
        {
            wrapper.like(DictType::getDictType, query.getDictType());
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
