package com.boot.framework.aspectj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import com.boot.common.annotation.DataScope;
import com.boot.common.constant.Constants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.QueryParam;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.core.domain.model.LoginUser;
import com.boot.common.core.text.Convert;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.framework.security.context.PermissionContextHolder;

/**
 * 数据过滤处理
 *
 * @author boot-plus
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 数据权限过滤关键字
     */
    public static final String DATA_SCOPE = "dataScope";

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        clearDataScope(point);
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            UserBo currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.userAlias(), controllerDataScope.deptAlias(), controllerDataScope.userField(), controllerDataScope.deptField(), permission);
            }
        }
    }

    /**
     * 数据范围过滤
     *
     * @param joinPoint 切点
     * @param user 用户
     * @param deptAlias 部门别名
     * @param userAlias 用户别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, UserBo user, String userAlias, String deptAlias, String userField, String deptField, String permission)
    {
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<String>();
        List<String> scopeCustomIds = new ArrayList<String>();
        user.getRoles().forEach(role -> {
            if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && (StringUtils.isEmpty(permission) || StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))))
            {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        for (RoleBo role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(permission) && !StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope))
            {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
            else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                if (scopeCustomIds.size() > 1)
                {
                    // 多个自定数据权限使用in查询，避免多次拼接。
                    sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", deptAlias, deptField, String.join(",", scopeCustomIds)));
                }
                else
                {
                    sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias, deptField, role.getRoleId()));
                }
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope))
            {
                sqlString.append(StringUtils.format(" OR {}.{} = {} ", deptAlias, deptField, user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )", deptAlias, deptField, user.getDeptId(), user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope))
            {
                if (StringUtils.isNotBlank(userAlias))
                {
                    sqlString.append(StringUtils.format(" OR {}.{} = {} ", userAlias, userField, user.getUserId()));
                }
                else
                {
                    // 数据权限为仅本人且没有userAlias别名不查询任何数据
                    sqlString.append(StringUtils.format(" OR {}.{} = 0 ", deptAlias, deptField));
                }
            }
            conditions.add(dataScope);
        }

        // 角色都不包含传递过来的权限字符，这个时候sqlString也会为空，所以要限制一下,不查询任何数据
        if (StringUtils.isEmpty(conditions))
        {
            sqlString.append(StringUtils.format(" OR {}.{} = 0 ", deptAlias, deptField));
        }

        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            Map<String, Object> params = findDataScopeParams(joinPoint);
            if (params != null)
            {
                params.put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 拼接权限sql前先清空params.dataScope参数防止注入
     */
    private void clearDataScope(final JoinPoint joinPoint)
    {
        Map<String, Object> params = findDataScopeParams(joinPoint);
        if (params != null)
        {
            params.put(DATA_SCOPE, "");
        }
    }

    /**
     * 从切点参数中定位查询参数对象。
     *
     * <p>返回方法参数中第一个（且应是唯一一个）{@link QueryParam} 实例；
     * 因此 {@code @DataScope} 方法必须且只能声明一个查询 BO 参数。</p>
     *
     * <p>分页方法签名为 {@code (IPage, BO)}，因此不能只取第一个参数；
     * 需要遍历参数找到 {@link QueryParam}。</p>
     */
    private static Map<String, Object> findDataScopeParams(final JoinPoint joinPoint)
    {
        Object[] args = joinPoint.getArgs();
        if (args == null)
        {
            return null;
        }
        for (Object arg : args)
        {
            if (arg instanceof QueryParam)
            {
                return ((QueryParam) arg).getParams();
            }
        }
        return null;
    }
}
