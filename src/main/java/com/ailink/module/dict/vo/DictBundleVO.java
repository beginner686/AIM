package com.ailink.module.dict.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DictBundleVO {

    private Long version;
    private boolean unchanged;
    private Map<String, List<DictEntryVO>> items;
}
