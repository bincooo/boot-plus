package com.boot.framework.web.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import com.boot.common.constant.Constants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.utils.StringUtils;
import com.boot.system.service.IMenuService;
import com.boot.system.service.IRoleService;

/**
 * 用户权限处理
 * 
 * @author boot-plus
 */
@Component
@RequiredArgsConstructor
public class SysPermissionService
{
    private final IRoleService roleService;

    private final IMenuService menuService;

    /**
     * 获取角色数据权限
     * 
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(UserBo user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            roles.add(Constants.SUPER_ADMIN);
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     * 
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(UserBo user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            perms.add(Constants.ALL_PERMISSION);
        }
        else
        {
            List<RoleBo> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (RoleBo role : roles)
                {
                    if (StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && !role.isAdmin())
                    {
                        Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                        role.setPermissions(rolePerms);
                        perms.addAll(rolePerms);
                    }
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
