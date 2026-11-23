package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.boot.common.core.domain.bo.DictTypeBo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 字典类型视图对象 sys_dict_type
 *
 * @author boot-plus
 */
@Data
@AutoMapper(target = DictTypeBo.class)
public class DictTypeVo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典主键 */
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
