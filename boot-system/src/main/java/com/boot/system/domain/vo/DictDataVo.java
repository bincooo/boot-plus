package com.boot.system.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.bo.DictDataBo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 字典数据视图对象 sys_dict_data
 *
 * @author boot-plus
 */
@Data
@AutoMapper(target = DictDataBo.class)
public class DictDataVo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典编码 */
    private Long dictCode;

    /** 字典排序 */
    private Long dictSort;

    /** 字典标签 */
    private String dictLabel;

    /** 字典键值 */
    private String dictValue;

    /** 字典类型 */
    private String dictType;

    /** 样式属性（其他样式扩展） */
    private String cssClass;

    /** 表格字典样式 */
    private String listClass;

    /** 是否默认（Y是 N否） */
    private String isDefault;

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

    /**
     * 是否为默认字典数据
     *
     * @return 结果
     */
    public boolean getDefault()
    {
        return UserConstants.YES.equals(this.isDefault);
    }
}
