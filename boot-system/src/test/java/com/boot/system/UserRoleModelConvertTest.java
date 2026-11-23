package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.system.domain.vo.UserVo;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.User;
import io.github.linpeilie.Converter;

/**
 * 用户/角色 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class UserRoleModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void poToBoToVoKeepsUserNameDeptNameAndRoleKey()
    {
        Dept dept = new Dept();
        dept.setDeptId(103L);
        dept.setDeptName("研发部门");

        Role role = new Role();
        role.setRoleId(2L);
        role.setRoleKey("common");
        role.setRoleName("普通角色");

        User po = new User();
        po.setUserId(2L);
        po.setUserName("ry");
        po.setDept(dept);
        po.setRoles(List.of(role));

        UserBo bo = converter.convert(po, UserBo.class);
        assertNotNull(bo);
        assertEquals("ry", bo.getUserName());
        assertNotNull(bo.getDept());
        assertEquals("研发部门", bo.getDept().getDeptName());
        assertNotNull(bo.getRoles());
        assertEquals("common", bo.getRoles().get(0).getRoleKey());

        UserVo vo = converter.convert(bo, UserVo.class);
        assertNotNull(vo);
        assertEquals("ry", vo.getUserName());
        assertNotNull(vo.getDept());
        assertEquals("研发部门", vo.getDept().getDeptName());
        assertNotNull(vo.getRoles());
        assertEquals("common", vo.getRoles().get(0).getRoleKey());
    }

    @Test
    void userAndRoleBosExtendQueryParamWithNonNullParams()
    {
        assertTrue(QueryParam.class.isAssignableFrom(UserBo.class));
        assertTrue(QueryParam.class.isAssignableFrom(RoleBo.class));
        assertNotNull(new UserBo().getParams());
        assertNotNull(new RoleBo().getParams());
    }

    @Test
    void userVoHasNoDelFlagField()
    {
        assertThrows(NoSuchFieldException.class, () -> UserVo.class.getDeclaredField("delFlag"));
    }
}
