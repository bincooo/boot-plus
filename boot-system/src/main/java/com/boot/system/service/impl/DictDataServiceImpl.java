package com.boot.system.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.common.core.domain.bo.DictDataBo;
import com.boot.common.utils.DictUtils;
import com.boot.system.repo.DictDataRepository;
import com.boot.system.service.IDictDataService;

/**
 * 字典 业务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class DictDataServiceImpl implements IDictDataService
{
    private final DictDataRepository dictDataRepository;

    /**
     * 根据条件分页查询字典数据
     *
     * @param page 分页参数
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public IPage<DictDataBo> selectDictDataList(IPage<DictDataBo> page, DictDataBo dictData)
    {
        return dictDataRepository.selectDictDataList(page, dictData);
    }

    /**
     * 根据条件查询字典数据（非分页，用于导出）
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<DictDataBo> selectDictDataList(DictDataBo dictData)
    {
        return dictDataRepository.selectDictDataList(dictData);
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue)
    {
        return dictDataRepository.selectDictLabel(dictType, dictValue);
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public DictDataBo selectDictDataById(Long dictCode)
    {
        return dictDataRepository.selectDictDataById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes)
    {
        for (Long dictCode : dictCodes)
        {
            DictDataBo data = selectDictDataById(dictCode);
            dictDataRepository.deleteDictDataById(dictCode);
            List<DictDataBo> dictDatas = dictDataRepository.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int insertDictData(DictDataBo data)
    {
        int row = dictDataRepository.insertDictData(data);
        if (row > 0)
        {
            List<DictDataBo> dictDatas = dictDataRepository.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int updateDictData(DictDataBo data)
    {
        int row = dictDataRepository.updateDictData(data);
        if (row > 0)
        {
            List<DictDataBo> dictDatas = dictDataRepository.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }
}
