package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.system.domain.bo.NoticeReadBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 公告已读记录表 sys_notice_read
 *
 * @author boot-plus
 */
@Data
@TableName("sys_notice_read")
@AutoMapper(target = NoticeReadBo.class)
public class NoticeRead implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "read_id", type = IdType.AUTO)
    private Long readId;

    /** 公告ID */
    private Long noticeId;

    /** 用户ID */
    private Long userId;

    /** 阅读时间 */
    private Date readTime;
}
