package com.boot.framework.aspectj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.constant.Constants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.bo.DeptBo;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;

/**
 * DataScopeAspect 数据权限回归测试
 *
 * <p>分页方法签名为 {@code (IPage, BO)}，数据权限必须写入 BO 而不是分页对象。</p>
 *
 * @author boot-plus
 */
class DataScopeAspectTest
{
    private static final String DATA_SCOPE = DataScopeAspect.DATA_SCOPE;

    @Test
    void clearDataScopeClearsPreSetValueOnQueryParamAfterPageArg()
    {
        Page<UserBo> page = new Page<>(1, 10);
        UserBo query = new UserBo();
        query.getParams().put(DATA_SCOPE, " AND 1=1");
        JoinPoint joinPoint = joinPoint(page, query);

        ReflectionTestUtils.invokeMethod(new DataScopeAspect(), "clearDataScope", joinPoint);

        assertEquals("", query.getParams().get(DATA_SCOPE));
    }

    @Test
    void dataScopeFilterWritesDataScopeToQueryParamAfterPageArg()
    {
        Page<UserBo> page = new Page<>(1, 10);
        UserBo query = new UserBo();
        query.getParams().put(DATA_SCOPE, " AND 1=1");
        JoinPoint joinPoint = joinPoint(page, query);

        DataScopeAspect.dataScopeFilter(joinPoint, nonAdminUser(), "u", "d", "user_id", "dept_id", "");

        Object value = query.getParams().get(DATA_SCOPE);
        assertNotNull(value);
        assertNotEquals(" AND 1=1", value);
        assertTrue(value.toString().contains("d.dept_id"));
    }

    @Test
    void dataScopeFilterWritesDataScopeToDeptBoAfterPageArg()
    {
        Page<DeptBo> page = new Page<>(1, 10);
        DeptBo query = new DeptBo();
        query.getParams().put(DATA_SCOPE, " AND 1=1");
        JoinPoint joinPoint = joinPoint(page, query);

        DataScopeAspect.dataScopeFilter(joinPoint, nonAdminUser(), "u", "d", "user_id", "dept_id", "");

        Object value = query.getParams().get(DATA_SCOPE);
        assertNotNull(value);
        assertNotEquals(" AND 1=1", value);
        assertTrue(value.toString().contains("d.dept_id"));
    }

    private static JoinPoint joinPoint(Object... args)
    {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private static UserBo nonAdminUser()
    {
        RoleBo role = new RoleBo();
        role.setDataScope(Constants.Dept.DATA_SCOPE_DEPT);
        role.setStatus(UserConstants.ROLE_NORMAL);

        UserBo user = new UserBo();
        user.setUserId(2L);
        user.setDeptId(100L);
        user.setRoles(List.of(role));
        return user;
    }
}
