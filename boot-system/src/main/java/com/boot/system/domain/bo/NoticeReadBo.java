package com.boot.system.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 公告已读记录业务对象 sys_notice_read
 *
 * @author boot-plus
 */
@Data
public class NoticeReadBo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long readId;

    /** 公告ID */
    private Long noticeId;

    /** 用户ID */
    private Long userId;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;
}
