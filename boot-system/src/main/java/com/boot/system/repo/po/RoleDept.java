package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色和部门关联 sys_role_dept
 *
 * @author boot-plus
 */
@Data
@TableName("sys_role_dept")
public class RoleDept implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;
}
