package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.system.domain.bo.LogininforBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 系统访问记录表 sys_logininfor
 *
 * @author boot-plus
 */
@Data
@TableName("sys_logininfor")
@AutoMapper(target = LogininforBo.class)
public class Logininfor implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(value = "info_id", type = IdType.AUTO)
    private Long infoId;

    /** 用户账号 */
    private String userName;

    /** 登录状态 0成功 1失败 */
    private String status;

    /** 登录IP地址 */
    private String ipaddr;

    /** 登录地点 */
    private String loginLocation;

    /** 浏览器类型 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 提示消息 */
    private String msg;

    /** 访问时间 */
    private Date loginTime;
}
