package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.common.core.domain.bo.DictDataBo;
import com.boot.common.core.domain.bo.DictTypeBo;
import com.boot.system.domain.vo.DictDataVo;
import com.boot.system.domain.vo.DictTypeVo;
import com.boot.system.repo.po.DictData;
import com.boot.system.repo.po.DictType;
import io.github.linpeilie.Converter;

/**
 * 字典 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class DictModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void dictTypePoToBoToVoKeepsFields()
    {
        DictType po = new DictType();
        po.setDictId(1L);
        po.setDictName("用户性别");
        po.setDictType("sys_user_sex");
        po.setStatus("0");
        po.setCreateBy("admin");
        po.setCreateTime(new Date());
        po.setRemark("性别列表");

        DictTypeBo bo = converter.convert(po, DictTypeBo.class);
        assertNotNull(bo);
        assertEquals("用户性别", bo.getDictName());
        assertEquals("sys_user_sex", bo.getDictType());
        assertEquals("性别列表", bo.getRemark());

        DictTypeVo vo = converter.convert(bo, DictTypeVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getDictId());
        assertEquals("用户性别", vo.getDictName());
        assertEquals("sys_user_sex", vo.getDictType());
        assertEquals("0", vo.getStatus());
        assertEquals("admin", vo.getCreateBy());
    }

    @Test
    void dictDataPoToBoToVoKeepsFieldsAndDefaultFlag()
    {
        DictData po = new DictData();
        po.setDictCode(1L);
        po.setDictSort(1L);
        po.setDictLabel("男");
        po.setDictValue("0");
        po.setDictType("sys_user_sex");
        po.setIsDefault("Y");
        po.setStatus("0");
        po.setCreateTime(new Date());

        DictDataBo bo = converter.convert(po, DictDataBo.class);
        assertNotNull(bo);
        assertEquals("男", bo.getDictLabel());
        assertEquals("0", bo.getDictValue());
        assertEquals("Y", bo.getIsDefault());
        assertTrue(bo.getDefault());

        DictDataVo vo = converter.convert(bo, DictDataVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getDictCode());
        assertEquals(1L, vo.getDictSort());
        assertEquals("男", vo.getDictLabel());
        assertEquals("0", vo.getDictValue());
        assertEquals("Y", vo.getIsDefault());
        assertTrue(vo.getDefault());
    }

    @Test
    void onlyDictTypeBoExtendsQueryParam()
    {
        assertTrue(QueryParam.class.isAssignableFrom(DictTypeBo.class));
        assertFalse(QueryParam.class.isAssignableFrom(DictDataBo.class));
        assertNotNull(new DictTypeBo().getParams());
    }

    @Test
    void dictVosHaveNoQueryParamsOrDelFlag()
    {
        assertThrows(NoSuchFieldException.class, () -> DictTypeVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> DictDataVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> DictTypeVo.class.getDeclaredField("delFlag"));
        assertThrows(NoSuchFieldException.class, () -> DictDataVo.class.getDeclaredField("delFlag"));
    }

    @Test
    void dictPosDeclareOnlyTableColumns()
    {
        assertThrows(NoSuchFieldException.class, () -> DictType.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> DictType.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> DictData.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> DictData.class.getDeclaredField("searchValue"));
    }
}
