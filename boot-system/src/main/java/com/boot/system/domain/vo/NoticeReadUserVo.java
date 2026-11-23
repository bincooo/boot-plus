package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.boot.system.repo.po.NoticeReadUserRow;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 公告已读用户视图对象
 *
 * @author boot-plus
 */
@Data
@AutoMapper(target = NoticeReadUserRow.class)
public class NoticeReadUserVo implements Serializable
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;
}
