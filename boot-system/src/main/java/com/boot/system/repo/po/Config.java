package com.boot.system.repo.po;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.system.domain.bo.ConfigBo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 参数配置表 sys_config
 *
 * @author boot-plus
 */
@Data
@TableName("sys_config")
@AutoMapper(target = ConfigBo.class)
public class Config implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /** 参数名称 */
    private String configName;

    /** 参数键名 */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置（Y是 N否） */
    private String configType;

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
