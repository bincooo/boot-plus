package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author boot-plus
 */
@Data
@TableName("sys_role_menu")
public class RoleMenu implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
