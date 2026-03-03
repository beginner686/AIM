package com.ailink.module.public_api.controller;

import com.ailink.common.Result;
import com.ailink.module.demand.dto.DemandQueryRequest;
import com.ailink.module.demand.service.DemandService;
import com.ailink.module.demand.vo.DemandVO;
import com.ailink.module.worker.dto.WorkerQueryRequest;
import com.ailink.module.worker.service.WorkerProfileService;
import com.ailink.module.worker.vo.WorkerProfileVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final DemandService demandService;
    private final WorkerProfileService workerProfileService;

    public PublicController(DemandService demandService, WorkerProfileService workerProfileService) {
        this.demandService = demandService;
        this.workerProfileService = workerProfileService;
    }

    @GetMapping("/demands")
    public Result<List<PublicDemandVO>> publicDemands(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String keyword) {
        DemandQueryRequest query = new DemandQueryRequest();
        query.setStatus("OPEN");
        query.setCategory(category);
        query.setTargetCountry(country);

        List<DemandVO> list = demandService.listDemand(query);
        String key = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";

        List<PublicDemandVO> rows = list.stream()
                .filter(d -> !StringUtils.hasText(key)
                        || containsIgnoreCase(d.getCategory(), key)
                        || containsIgnoreCase(d.getDescription(), key)
                        || containsIgnoreCase(d.getTargetCountry(), key))
                .map(this::toPublicDemand)
                .toList();
        return Result.success(rows);
    }

    @GetMapping("/workers")
    public Result<List<PublicWorkerVO>> publicWorkers(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String skillKeyword) {
        WorkerQueryRequest query = new WorkerQueryRequest();
        query.setCountry(country);
        query.setSkillKeyword(skillKeyword);
        query.setVerified(1);

        List<WorkerProfileVO> list = workerProfileService.list(query);
        List<PublicWorkerVO> rows = list.stream()
                .map(this::toPublicWorker)
                .toList();
        return Result.success(rows);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> platformStats() {
        DemandQueryRequest demandQuery = new DemandQueryRequest();
        demandQuery.setStatus("OPEN");
        List<DemandVO> openDemands = demandService.listDemand(demandQuery);

        WorkerQueryRequest workerQuery = new WorkerQueryRequest();
        workerQuery.setVerified(1);
        List<WorkerProfileVO> verifiedWorkers = workerProfileService.list(workerQuery);

        Set<String> countries = new HashSet<>();
        openDemands.forEach(d -> {
            if (StringUtils.hasText(d.getTargetCountry())) {
                countries.add(d.getTargetCountry().trim().toUpperCase(Locale.ROOT));
            }
        });
        verifiedWorkers.forEach(w -> {
            if (StringUtils.hasText(w.getCountry())) {
                countries.add(w.getCountry().trim().toUpperCase(Locale.ROOT));
            }
        });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("demandCount", openDemands.size());
        stats.put("workerCount", verifiedWorkers.size());
        stats.put("countryCount", countries.size());
        return Result.success(stats);
    }

    private PublicDemandVO toPublicDemand(DemandVO row) {
        return new PublicDemandVO(
                row.getId(),
                row.getTargetCountry(),
                row.getCategory(),
                row.getBudget(),
                row.getDescription(),
                row.getStatus(),
                row.getCreatedTime());
    }

    private PublicWorkerVO toPublicWorker(WorkerProfileVO row) {
        String displayName = maskName(row.getRealName());
        return new PublicWorkerVO(
                row.getId(),
                displayName,
                row.getCountry(),
                row.getSkillTags(),
                row.getRating(),
                row.getPriceMin(),
                row.getPriceMax(),
                row.getVerified(),
                row.getCreatedTime());
    }

    private boolean containsIgnoreCase(String text, String key) {
        return StringUtils.hasText(text) && text.toLowerCase(Locale.ROOT).contains(key);
    }

    private String maskName(String realName) {
        if (!StringUtils.hasText(realName)) {
            return "执行者";
        }
        String name = realName.trim();
        if (name.length() <= 1) {
            return name + "*";
        }
        return name.charAt(0) + "**";
    }

    public record PublicDemandVO(Long id,
                                 String targetCountry,
                                 String category,
                                 BigDecimal budget,
                                 String description,
                                 String status,
                                 LocalDateTime createdTime) {
    }

    public record PublicWorkerVO(Long workerProfileId,
                                 String displayName,
                                 String country,
                                 String skillTags,
                                 Float rating,
                                 BigDecimal priceMin,
                                 BigDecimal priceMax,
                                 Integer verified,
                                 LocalDateTime createdTime) {
    }
}
