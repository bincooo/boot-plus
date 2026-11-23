package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 公告已读用户查询行
 *
 * @author boot-plus
 */
@Data
public class NoticeReadUserRow implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 登录名 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 部门名称 */
    private String deptName;

    /** 手机号码 */
    private String phonenumber;

    /** 阅读时间 */
    private Date readTime;
}
