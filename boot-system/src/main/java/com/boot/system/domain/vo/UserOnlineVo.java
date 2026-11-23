package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 当前在线会话视图对象
 *
 * @author boot-plus
 */
@Data
public class UserOnlineVo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话编号 */
    private String tokenId;

    /** 部门名称 */
    private String deptName;

    /** 用户名称 */
    private String userName;

    /** 登录IP地址 */
    private String ipaddr;

    /** 登录地址 */
    private String loginLocation;

    /** 浏览器类型 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录时间 */
    private Long loginTime;
}
