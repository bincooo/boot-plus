package com.boot.system.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.bo.DictDataBo;
import com.boot.common.core.domain.bo.DictTypeBo;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.DictUtils;
import com.boot.common.utils.StringUtils;
import com.boot.system.repo.DictDataRepository;
import com.boot.system.repo.DictTypeRepository;
import com.boot.system.service.IDictTypeService;

/**
 * 字典 业务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class DictTypeServiceImpl implements IDictTypeService
{
    private final DictTypeRepository dictTypeRepository;

    private final DictDataRepository dictDataRepository;

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingDictCache();
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param page 分页参数
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public IPage<DictTypeBo> selectDictTypeList(IPage<DictTypeBo> page, DictTypeBo dictType)
    {
        return dictTypeRepository.selectDictTypeList(page, dictType);
    }

    /**
     * 根据条件查询字典类型（非分页，用于导出）
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<DictTypeBo> selectDictTypeList(DictTypeBo dictType)
    {
        return dictTypeRepository.selectDictTypeList(dictType);
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<DictTypeBo> selectDictTypeAll()
    {
        return dictTypeRepository.selectDictTypeAll();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<DictDataBo> selectDictDataByType(String dictType)
    {
        List<DictDataBo> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            return dictDatas;
        }
        dictDatas = dictDataRepository.selectDictDataByType(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return null;
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public DictTypeBo selectDictTypeById(Long dictId)
    {
        return dictTypeRepository.selectDictTypeById(dictId);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public DictTypeBo selectDictTypeByType(String dictType)
    {
        return dictTypeRepository.selectDictTypeByType(dictType);
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds)
    {
        for (Long dictId : dictIds)
        {
            DictTypeBo dictType = selectDictTypeById(dictId);
            if (dictDataRepository.countDictDataByType(dictType.getDictType()) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            dictTypeRepository.deleteDictTypeById(dictId);
            DictUtils.removeDictCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache()
    {
        DictDataBo dictData = new DictDataBo();
        dictData.setStatus("0");
        Map<String, List<DictDataBo>> dictDataMap = dictDataRepository.selectDictDataList(dictData).stream().collect(Collectors.groupingBy(DictDataBo::getDictType));
        for (Map.Entry<String, List<DictDataBo>> entry : dictDataMap.entrySet())
        {
            DictUtils.setDictCache(entry.getKey(), entry.getValue().stream().sorted(Comparator.comparing(DictDataBo::getDictSort)).collect(Collectors.toList()));
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache()
    {
        DictUtils.clearDictCache();
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache()
    {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(DictTypeBo dict)
    {
        int row = dictTypeRepository.insertDictType(dict);
        if (row > 0)
        {
            DictUtils.setDictCache(dict.getDictType(), null);
        }
        return row;
    }

    /**
     * 修改保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDictType(DictTypeBo dict)
    {
        int row = dictTypeRepository.updateDictType(dict);
        if (row > 0)
        {
            List<DictDataBo> dictDatas = dictDataRepository.selectDictDataByType(dict.getDictType());
            DictUtils.setDictCache(dict.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(DictTypeBo dict)
    {
        Long dictId = StringUtils.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        DictTypeBo dictType = dictTypeRepository.checkDictTypeUnique(dict.getDictType());
        if (StringUtils.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
