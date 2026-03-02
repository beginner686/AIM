package com.ailink.module.dict.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DictTypeItemVO {

    @JsonProperty("dict_code")
    private String dictCode;

    @JsonProperty("dict_label")
    private String dictLabel;

    @JsonProperty("sort_no")
    private Integer sortNo;

    @JsonProperty("extra_json")
    private String extraJson;
}
