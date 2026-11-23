package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.common.core.domain.bo.DictTypeBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 字典类型表 sys_dict_type
 *
 * @author boot-plus
 */
@Data
@TableName("sys_dict_type")
@AutoMapper(target = DictTypeBo.class)
public class DictType implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典主键 */
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Long dictId;

    /** 字典名称 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

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
