package com.ailink.module.dict.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_item")
public class DictItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dictType;
    private String dictCode;
    private String dictLabel;
    private String tagType;
    private String groupKey;
    private Integer sortNo;
    private Integer enabled;
    private String extraJson;
}
