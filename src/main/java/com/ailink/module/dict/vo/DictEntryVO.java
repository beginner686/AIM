package com.ailink.module.dict.vo;

import lombok.Data;

@Data
public class DictEntryVO {

    private String code;
    private String label;
    private String tag;
    private String group;
    private Integer sort;
    private String extra;
}
