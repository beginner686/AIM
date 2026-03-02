package com.ailink.module.dict.service.impl;

import com.ailink.module.dict.entity.DictItem;
import com.ailink.module.dict.mapper.DictItemMapper;
import com.ailink.module.dict.service.DictService;
import com.ailink.module.dict.vo.DictBundleVO;
import com.ailink.module.dict.vo.DictEntryVO;
import com.ailink.module.dict.vo.DictTypeItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictServiceImpl implements DictService {

    private final DictItemMapper dictItemMapper;
    private final ObjectMapper objectMapper;

    public DictServiceImpl(DictItemMapper dictItemMapper, ObjectMapper objectMapper) {
        this.dictItemMapper = dictItemMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DictBundleVO getPublicDicts(List<String> types, Long version) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getEnabled, 1);
        if (!CollectionUtils.isEmpty(types)) {
            wrapper.in(DictItem::getDictType, types);
        }
        wrapper.orderByAsc(DictItem::getDictType)
                .orderByAsc(DictItem::getSortNo)
                .orderByAsc(DictItem::getId);
        List<DictItem> rows = dictItemMapper.selectList(wrapper);

        long latestVersion = rows.stream()
                .map(DictItem::getUpdatedTime)
                .filter(java.util.Objects::nonNull)
                .mapToLong(time -> time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .max()
                .orElse(0L);

        DictBundleVO bundle = new DictBundleVO();
        bundle.setVersion(latestVersion);
        if (version != null && version.equals(latestVersion)) {
            bundle.setUnchanged(true);
            bundle.setItems(Map.of());
            return bundle;
        }

        Map<String, List<DictEntryVO>> grouped = new LinkedHashMap<>();
        for (DictItem row : rows) {
            DictEntryVO entry = new DictEntryVO();
            entry.setCode(row.getDictCode());
            entry.setLabel(row.getDictLabel());
            entry.setTag(row.getTagType());
            entry.setGroup(row.getGroupKey());
            entry.setSort(row.getSortNo());
            entry.setExtra(row.getExtraJson());
            grouped.computeIfAbsent(row.getDictType(), key -> new ArrayList<>()).add(entry);
        }
        bundle.setUnchanged(false);
        bundle.setItems(grouped);
        return bundle;
    }

    @Override
    public List<DictEntryVO> searchByType(String type, String keyword) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getEnabled, 1);
        if (org.springframework.util.StringUtils.hasText(type)) {
            wrapper.eq(DictItem::getDictType, type.trim().toUpperCase());
        }
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            wrapper.like(DictItem::getDictLabel, keyword.trim());
        }
        wrapper.orderByAsc(DictItem::getSortNo)
                .orderByAsc(DictItem::getId);
        List<DictItem> rows = dictItemMapper.selectList(wrapper);

        List<DictEntryVO> result = new ArrayList<>();
        for (DictItem row : rows) {
            DictEntryVO entry = new DictEntryVO();
            entry.setCode(row.getDictCode());
            entry.setLabel(row.getDictLabel());
            entry.setTag(row.getTagType());
            entry.setGroup(row.getGroupKey());
            entry.setSort(row.getSortNo());
            entry.setExtra(row.getExtraJson());
            result.add(entry);
        }
        return result;
    }

    @Override
    public List<DictTypeItemVO> listByType(String type) {
        if (!StringUtils.hasText(type)) {
            return List.of();
        }
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getEnabled, 1)
                .eq(DictItem::getDictType, type.trim().toUpperCase())
                .orderByAsc(DictItem::getSortNo)
                .orderByAsc(DictItem::getId);
        List<DictItem> rows = dictItemMapper.selectList(wrapper);

        return rows.stream()
                .map(this::toTypeItemVO)
                .sorted(Comparator
                        .comparing((DictTypeItemVO item) -> isHot(item.getExtraJson()))
                        .reversed()
                        .thenComparing(item -> item.getSortNo() == null ? Integer.MAX_VALUE : item.getSortNo()))
                .toList();
    }

    private DictTypeItemVO toTypeItemVO(DictItem row) {
        DictTypeItemVO vo = new DictTypeItemVO();
        vo.setDictCode(row.getDictCode());
        vo.setDictLabel(row.getDictLabel());
        vo.setSortNo(row.getSortNo());
        vo.setExtraJson(row.getExtraJson());
        return vo;
    }

    private boolean isHot(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return false;
        }
        try {
            return objectMapper.readTree(extraJson).path("hot").asBoolean(false);
        } catch (Exception ignored) {
            return false;
        }
    }
}
