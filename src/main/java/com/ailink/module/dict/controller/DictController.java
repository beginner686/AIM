package com.ailink.module.dict.controller;

import com.ailink.common.Result;
import com.ailink.module.dict.service.DictService;
import com.ailink.module.dict.vo.DictBundleVO;
import com.ailink.module.dict.vo.DictEntryVO;
import com.ailink.module.dict.vo.DictTypeItemVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/public/items")
    public Result<DictBundleVO> publicItems(
            @RequestParam(required = false) String types,
            @RequestParam(required = false) Long version) {
        List<String> typeList = StringUtils.hasText(types)
                ? Arrays.stream(types.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList()
                : List.of();
        return Result.success(dictService.getPublicDicts(typeList, version));
    }

    @GetMapping("/public/search")
    public Result<List<DictEntryVO>> searchDict(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return Result.success(dictService.searchByType(type, keyword));
    }

    @GetMapping("/{type}")
    public Result<List<DictTypeItemVO>> listByType(@PathVariable String type) {
        return Result.success(dictService.listByType(type));
    }
}
