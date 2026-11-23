package com.boot.system.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户和角色关联业务对象 sys_user_role
 *
 * @author boot-plus
 */
@Data
public class UserRoleBo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}
