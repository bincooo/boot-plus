package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.system.domain.bo.UserRoleBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author boot-plus
 */
@Data
@TableName("sys_user_role")
@AutoMapper(target = UserRoleBo.class)
public class UserRole implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}
