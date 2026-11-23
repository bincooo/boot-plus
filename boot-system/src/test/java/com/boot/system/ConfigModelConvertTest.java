package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.system.domain.vo.ConfigVo;
import com.boot.system.repo.po.Config;
import com.boot.system.domain.bo.ConfigBo;
import io.github.linpeilie.Converter;

/**
 * 参数配置 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class ConfigModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void configPoToBoToVoKeepsFields()
    {
        Config po = new Config();
        po.setConfigId(1L);
        po.setConfigName("主框架页-默认皮肤样式名称");
        po.setConfigKey("sys.index.skinName");
        po.setConfigValue("skin-blue");
        po.setConfigType("Y");
        po.setCreateBy("admin");
        po.setCreateTime(new Date());
        po.setRemark("蓝色 skin-blue");

        ConfigBo bo = converter.convert(po, ConfigBo.class);
        assertNotNull(bo);
        assertEquals("主框架页-默认皮肤样式名称", bo.getConfigName());
        assertEquals("sys.index.skinName", bo.getConfigKey());
        assertEquals("skin-blue", bo.getConfigValue());
        assertEquals("Y", bo.getConfigType());
        assertEquals("蓝色 skin-blue", bo.getRemark());

        ConfigVo vo = converter.convert(bo, ConfigVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getConfigId());
        assertEquals("主框架页-默认皮肤样式名称", vo.getConfigName());
        assertEquals("sys.index.skinName", vo.getConfigKey());
        assertEquals("skin-blue", vo.getConfigValue());
        assertEquals("Y", vo.getConfigType());
        assertEquals("admin", vo.getCreateBy());
        assertEquals("蓝色 skin-blue", vo.getRemark());
    }

    @Test
    void configBoExtendsQueryParam()
    {
        assertTrue(QueryParam.class.isAssignableFrom(ConfigBo.class));
        assertNotNull(new ConfigBo().getParams());
    }

    @Test
    void configVoHasNoQueryParamsOrDelFlag()
    {
        assertThrows(NoSuchFieldException.class, () -> ConfigVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> ConfigVo.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> ConfigVo.class.getDeclaredField("delFlag"));
    }

    @Test
    void configPoDeclaresOnlyTableColumns()
    {
        assertThrows(NoSuchFieldException.class, () -> Config.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> Config.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> Config.class.getDeclaredField("delFlag"));
    }
}
