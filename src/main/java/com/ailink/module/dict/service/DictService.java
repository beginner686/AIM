package com.ailink.module.dict.service;

import com.ailink.module.dict.vo.DictBundleVO;
import com.ailink.module.dict.vo.DictEntryVO;
import com.ailink.module.dict.vo.DictTypeItemVO;

import java.util.List;

public interface DictService {

    DictBundleVO getPublicDicts(List<String> types, Long version);

    List<DictEntryVO> searchByType(String type, String keyword);

    List<DictTypeItemVO> listByType(String type);
}
