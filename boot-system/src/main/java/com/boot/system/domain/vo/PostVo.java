package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.boot.system.domain.bo.PostBo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 岗位视图对象 sys_post
 *
 * @author boot-plus
 */
@Data
@AutoMapper(target = PostBo.class)
public class PostVo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 岗位序号 */
    private Long postId;

    /** 岗位编码 */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 岗位排序 */
    private Integer postSort;

    /** 状态（0正常 1停用） */
    private String status;

    /** 用户是否存在此岗位标识 默认不存在 */
    private boolean flag = false;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;
}
