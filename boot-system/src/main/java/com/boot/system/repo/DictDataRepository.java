package com.boot.system.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.core.domain.bo.DictDataBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.DictDataMapper;
import com.boot.system.repo.po.DictData;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 字典数据 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class DictDataRepository
{
    private final DictDataMapper dictDataMapper;

    private final Converter converter;

    /**
     * 分页查询字典数据集合
     *
     * @param page 分页参数
     * @param query 字典数据查询条件
     * @return 字典数据集合信息
     */
    public IPage<DictDataBo> selectDictDataList(IPage<DictDataBo> page, DictDataBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<DictData> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        dictDataMapper.selectPage(poPage, buildDictDataListQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, DictDataBo.class));
    }

    /**
     * 查询字典数据集合（非分页，用于导出和缓存加载）
     *
     * @param query 字典数据查询条件
     * @return 字典数据集合信息
     */
    public List<DictDataBo> selectDictDataList(DictDataBo query)
    {
        return PageUtils.convert(dictDataMapper.selectList(buildDictDataListQuery(query)),
                po -> converter.convert(po, DictDataBo.class));
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    public List<DictDataBo> selectDictDataByType(String dictType)
    {
        List<DictData> records = dictDataMapper.selectList(dictDataColumnWrapper()
                .eq(DictData::getStatus, "0")
                .eq(DictData::getDictType, dictType)
                .orderByAsc(DictData::getDictSort));
        return PageUtils.convert(records, po -> converter.convert(po, DictDataBo.class));
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public String selectDictLabel(String dictType, String dictValue)
    {
        DictData data = dictDataMapper.selectOne(Wrappers.lambdaQuery(DictData.class)
                .select(DictData::getDictLabel)
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getDictValue, dictValue));
        return data != null ? data.getDictLabel() : null;
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    public DictDataBo selectDictDataById(Long dictCode)
    {
        DictData data = dictDataMapper.selectOne(dictDataColumnWrapper().eq(DictData::getDictCode, dictCode));
        return converter.convert(data, DictDataBo.class);
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
     * 新增字典数据信息
     *
     * @param dictData 字典数据信息
     * @return 结果
     */
    public int insertDictData(DictDataBo dictData)
    {
        DictData po = converter.convert(dictData, DictData.class);
        po.setCreateTime(DateUtils.getNowDate());
        return dictDataMapper.insert(po);
    }

    /**
     * 修改字典数据信息
     *
     * @param dictData 字典数据信息
     * @return 结果
     */
    public int updateDictData(DictDataBo dictData)
    {
        DictData po = converter.convert(dictData, DictData.class);
        // 镜像原 XML updateDictData 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return dictDataMapper.update(null, Wrappers.lambdaUpdate(DictData.class)
                .set(po.getDictSort() != null, DictData::getDictSort, po.getDictSort())
                .set(po.getDictLabel() != null && !po.getDictLabel().isEmpty(), DictData::getDictLabel, po.getDictLabel())
                .set(po.getDictValue() != null && !po.getDictValue().isEmpty(), DictData::getDictValue, po.getDictValue())
                .set(po.getDictType() != null && !po.getDictType().isEmpty(), DictData::getDictType, po.getDictType())
                .set(po.getCssClass() != null, DictData::getCssClass, po.getCssClass())
                .set(po.getListClass() != null, DictData::getListClass, po.getListClass())
                .set(po.getIsDefault() != null && !po.getIsDefault().isEmpty(), DictData::getIsDefault, po.getIsDefault())
                .set(po.getStatus() != null, DictData::getStatus, po.getStatus())
                .set(po.getRemark() != null, DictData::getRemark, po.getRemark())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), DictData::getUpdateBy, po.getUpdateBy())
                .set(DictData::getUpdateTime, DateUtils.getNowDate())
                .eq(DictData::getDictCode, po.getDictCode()));
    }

    /**
     * 通过字典ID删除字典数据信息
     *
     * @param dictCode 字典数据ID
     * @return 结果
     */
    public int deleteDictDataById(Long dictCode)
    {
        return dictDataMapper.deleteById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     * @return 结果
     */
    public int deleteDictDataByIds(Long[] dictCodes)
    {
        return dictDataMapper.deleteByIds(Arrays.asList(dictCodes));
    }

    /**
     * 构建与原 XML selectDictDataVo 相同列清单的查询壳
     */
    private LambdaQueryWrapper<DictData> dictDataColumnWrapper()
    {
        return Wrappers.lambdaQuery(DictData.class)
                .select(DictData::getDictCode, DictData::getDictSort, DictData::getDictLabel,
                        DictData::getDictValue, DictData::getDictType, DictData::getCssClass,
                        DictData::getListClass, DictData::getIsDefault, DictData::getStatus,
                        DictData::getCreateBy, DictData::getCreateTime, DictData::getRemark);
    }

    /**
     * 构建字典数据列表查询条件（与原 XML selectDictDataList 的动态条件一致）
     *
     * @param query 字典数据查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<DictData> buildDictDataListQuery(DictDataBo query)
    {
        LambdaQueryWrapper<DictData> wrapper = dictDataColumnWrapper();
        if (query.getDictType() != null && !query.getDictType().isEmpty())
        {
            wrapper.eq(DictData::getDictType, query.getDictType());
        }
        if (query.getDictLabel() != null && !query.getDictLabel().isEmpty())
        {
            wrapper.like(DictData::getDictLabel, query.getDictLabel());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(DictData::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(DictData::getDictSort);
        return wrapper;
    }
}
