package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.annotation.TableField;
import com.boot.common.core.domain.QueryParam;
import com.boot.common.core.domain.bo.DeptBo;
import com.boot.common.core.domain.bo.MenuBo;
import com.boot.system.domain.vo.DeptVo;
import com.boot.system.domain.vo.MenuVo;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.Menu;
import io.github.linpeilie.Converter;

/**
 * 菜单/部门 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class MenuDeptModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void menuPoToBoToVoKeepsParentNameAndChildren()
    {
        Menu po = new Menu();
        po.setMenuId(100L);
        po.setMenuName("用户管理");
        po.setParentId(1L);
        po.setParentName("系统管理");
        po.setMenuType("C");
        po.setPath("user");
        po.setOrderNum(1);
        po.setCreateTime(new Date());

        MenuBo bo = converter.convert(po, MenuBo.class);
        assertNotNull(bo);
        assertEquals("系统管理", bo.getParentName());
        assertNotNull(bo.getChildren());
        assertTrue(bo.getChildren().isEmpty());

        MenuBo child = new MenuBo();
        child.setMenuId(101L);
        child.setMenuName("新增用户");
        bo.getChildren().add(child);

        MenuVo vo = converter.convert(bo, MenuVo.class);
        assertNotNull(vo);
        assertEquals("系统管理", vo.getParentName());
        assertEquals(1, vo.getChildren().size());
        assertEquals("新增用户", vo.getChildren().get(0).getMenuName());
    }

    @Test
    void deptPoToBoToVoKeepsParentNameAndChildren()
    {
        Dept po = new Dept();
        po.setDeptId(101L);
        po.setDeptName("深圳总公司");
        po.setParentId(100L);
        po.setParentName("若依科技");
        po.setOrderNum(1);
        po.setCreateTime(new Date());

        DeptBo bo = converter.convert(po, DeptBo.class);
        assertNotNull(bo);
        assertEquals("若依科技", bo.getParentName());
        assertNotNull(bo.getChildren());
        assertTrue(bo.getChildren().isEmpty());

        DeptBo child = new DeptBo();
        child.setDeptId(103L);
        child.setDeptName("研发部门");
        bo.getChildren().add(child);

        DeptVo vo = converter.convert(bo, DeptVo.class);
        assertNotNull(vo);
        assertEquals("若依科技", vo.getParentName());
        assertEquals(1, vo.getChildren().size());
        assertEquals("研发部门", vo.getChildren().get(0).getDeptName());
    }

    @Test
    void menuBoToPoWritePathKeepsFields()
    {
        MenuBo bo = new MenuBo();
        bo.setMenuId(100L);
        bo.setMenuName("用户管理");
        bo.setParentId(1L);
        bo.setMenuType("C");
        bo.setPath("user");
        bo.setOrderNum(1);

        Menu po = converter.convert(bo, Menu.class);
        assertNotNull(po);
        assertEquals(100L, po.getMenuId());
        assertEquals("用户管理", po.getMenuName());
        assertEquals(1L, po.getParentId());
        assertEquals("C", po.getMenuType());
        assertEquals("user", po.getPath());
        assertEquals(1, po.getOrderNum());
    }

    @Test
    void deptBoToPoWritePathKeepsFields()
    {
        DeptBo bo = new DeptBo();
        bo.setDeptId(101L);
        bo.setDeptName("深圳总公司");
        bo.setParentId(100L);
        bo.setOrderNum(1);

        Dept po = converter.convert(bo, Dept.class);
        assertNotNull(po);
        assertEquals(101L, po.getDeptId());
        assertEquals("深圳总公司", po.getDeptName());
        assertEquals(100L, po.getParentId());
        assertEquals(1, po.getOrderNum());
    }

    @Test
    void menuAndDeptBosExtendQueryParamWithNonNullParams()
    {
        assertTrue(QueryParam.class.isAssignableFrom(MenuBo.class));
        assertTrue(QueryParam.class.isAssignableFrom(DeptBo.class));
        assertNotNull(new MenuBo().getParams());
        assertNotNull(new DeptBo().getParams());
    }

    @Test
    void joinOnlyFieldsLiveOnModelsNotTables() throws Exception
    {
        assertFalse(Menu.class.getDeclaredField("parentName").getAnnotation(TableField.class).exist());
        assertFalse(Dept.class.getDeclaredField("parentName").getAnnotation(TableField.class).exist());
        assertThrows(NoSuchFieldException.class, () -> Menu.class.getDeclaredField("children"));
        assertThrows(NoSuchFieldException.class, () -> Dept.class.getDeclaredField("children"));
        assertNotNull(MenuBo.class.getDeclaredField("children"));
        assertNotNull(MenuVo.class.getDeclaredField("children"));
        assertNotNull(DeptBo.class.getDeclaredField("children"));
        assertNotNull(DeptVo.class.getDeclaredField("children"));
    }

    @Test
    void menuAndDeptVosHaveNoQueryParamsOrDelFlag()
    {
        assertThrows(NoSuchFieldException.class, () -> MenuVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> DeptVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> MenuVo.class.getDeclaredField("delFlag"));
        assertThrows(NoSuchFieldException.class, () -> DeptVo.class.getDeclaredField("delFlag"));
    }
}
