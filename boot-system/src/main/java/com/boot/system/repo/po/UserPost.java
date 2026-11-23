package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户和岗位关联 sys_user_post
 *
 * @author boot-plus
 */
@Data
@TableName("sys_user_post")
public class UserPost implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 岗位ID */
    private Long postId;
}
