package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.system.domain.bo.PostBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 岗位表 sys_post
 *
 * @author boot-plus
 */
@Data
@TableName("sys_post")
@AutoMapper(target = PostBo.class)
public class Post implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 岗位序号 */
    @TableId(value = "post_id", type = IdType.AUTO)
    private Long postId;

    /** 岗位编码 */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 岗位排序 */
    private Integer postSort;

    /** 状态（0正常 1停用） */
    private String status;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
