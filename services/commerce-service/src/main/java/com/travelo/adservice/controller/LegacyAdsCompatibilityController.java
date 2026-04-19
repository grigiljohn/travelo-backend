package com.travelo.adservice.controller;

import com.travelo.adservice.dto.CreateAdGroupRequest;
import com.travelo.adservice.dto.CreateAdRequest;
import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.AdGroup;
import com.travelo.adservice.entity.AdGroupStatus;
import com.travelo.adservice.entity.AdStatus;
import com.travelo.adservice.entity.enums.AdType;
import com.travelo.adservice.entity.enums.BudgetType;
import com.travelo.adservice.repository.AdGroupRepository;
import com.travelo.adservice.repository.AdRepository;
import com.travelo.adservice.service.AdGroupService;
import com.travelo.adservice.service.AdService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class LegacyAdsCompatibilityController {
    private final AdGroupService adGroupService;
    private final AdService adService;
    private final AdGroupRepository adGroupRepository;
    private final AdRepository adRepository;

    public LegacyAdsCompatibilityController(
            AdGroupService adGroupService,
            AdService adService,
            AdGroupRepository adGroupRepository,
            AdRepository adRepository
    ) {
        this.adGroupService = adGroupService;
        this.adService = adService;
        this.adGroupRepository = adGroupRepository;
        this.adRepository = adRepository;
    }

    @GetMapping("/ad-sets")
    public ResponseEntity<List<Map<String, Object>>> getAdSets(
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        if (campaignId != null) {
            var response = adGroupService.getAdGroups(campaignId, page, size);
            List<Map<String, Object>> out = response.getContent().stream().map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId());
                map.put("campaignId", item.getCampaignId());
                map.put("name", item.getName());
                map.put("status", item.getStatus() == null ? null : item.getStatus().name().toLowerCase(Locale.ROOT));
                map.put("budget", item.getBudget());
                map.put("budgetType", item.getBudgetType() == null ? null : item.getBudgetType().name().toLowerCase(Locale.ROOT));
                map.put("audience", toLegacyAudience(item.getTargeting()));
                map.put("placements", item.getPlacements());
                map.put("createdAt", item.getCreatedAt());
                map.put("updatedAt", item.getUpdatedAt());
                return map;
            }).toList();
            return ResponseEntity.ok(out);
        }

        Pageable pageable = PageRequest.of(page, size);
        List<Map<String, Object>> out = adGroupRepository.findAllActive(pageable).getContent().stream()
                .map(this::toLegacyAdSetMap)
                .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/ad-sets/{id}")
    public ResponseEntity<Map<String, Object>> getAdSetById(@PathVariable UUID id) {
        AdGroup adGroup = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        if (adGroup.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + id);
        }
        return ResponseEntity.ok(toLegacyAdSetMap(adGroup));
    }

    @PostMapping("/ad-sets")
    public ResponseEntity<Map<String, Object>> createAdSet(@RequestBody Map<String, Object> payload) {
        UUID campaignId = requiredUuid(payload.get("campaignId"), "campaignId");
        String name = String.valueOf(payload.getOrDefault("name", "Ad Set"));
        Double budget = toDouble(payload.get("budget"), 100d);
        BudgetType budgetType = parseBudgetType(payload.get("budgetType"), BudgetType.DAILY);

        @SuppressWarnings("unchecked")
        Map<String, Object> audience = (Map<String, Object>) payload.getOrDefault("audience", Map.of());
        @SuppressWarnings("unchecked")
        List<String> placements = (List<String>) payload.getOrDefault("placements", List.of());

        CreateAdGroupRequest req = new CreateAdGroupRequest(
                name,
                budget,
                budgetType,
                audience,
                listOfStrings(audience.get("keywords")),
                List.of(),
                List.of("mobile", "desktop"),
                placements
        );
        var created = adGroupService.createAdGroup(campaignId, req);
        return ResponseEntity.ok(toLegacyAdSetMap(Objects.requireNonNull(adGroupRepository.findById(created.getId()).orElse(null))));
    }

    @PutMapping("/ad-sets/{id}")
    public ResponseEntity<Map<String, Object>> updateAdSet(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        AdGroup existing = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        if (existing.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + id);
        }

        String name = String.valueOf(payload.getOrDefault("name", existing.getName()));
        Double budget = toDouble(payload.get("budget"), existing.getBudget());
        BudgetType budgetType = parseBudgetType(payload.get("budgetType"), existing.getBudgetType());
        @SuppressWarnings("unchecked")
        Map<String, Object> audience = (Map<String, Object>) payload.getOrDefault("audience", existing.getTargeting());
        @SuppressWarnings("unchecked")
        List<String> placements = (List<String>) payload.getOrDefault("placements", Arrays.asList(existing.getPlacements()));

        CreateAdGroupRequest req = new CreateAdGroupRequest(
                name,
                budget,
                budgetType,
                audience,
                listOfStrings(audience.get("keywords")),
                existing.getNegativeKeywords() == null ? List.of() : Arrays.asList(existing.getNegativeKeywords()),
                existing.getDevices() == null ? List.of() : Arrays.asList(existing.getDevices()),
                placements
        );
        adGroupService.updateAdGroup(id, existing.getCampaignId(), req);
        AdGroup updated = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        return ResponseEntity.ok(toLegacyAdSetMap(updated));
    }

    @DeleteMapping("/ad-sets/{id}")
    public ResponseEntity<Void> deleteAdSet(@PathVariable UUID id) {
        AdGroup existing = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        adGroupService.deleteAdGroup(id, existing.getCampaignId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ad-sets/bulk-update")
    public ResponseEntity<Void> bulkUpdateAdSets(@RequestBody Map<String, Object> payload) {
        List<UUID> ids = uuidList(payload.get("ids"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
        String statusRaw = data.get("status") == null ? null : data.get("status").toString();
        AdGroupStatus targetStatus = parseEnum(statusRaw, AdGroupStatus.class, null);

        for (UUID id : ids) {
            adGroupRepository.findById(id).ifPresent(group -> {
                if (targetStatus != null && group.getDeletedAt() == null) {
                    group.setStatus(targetStatus);
                    adGroupRepository.save(group);
                }
            });
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ads")
    public ResponseEntity<List<Map<String, Object>>> getAds(
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam(value = "adSetId", required = false) UUID adSetId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        List<Ad> rows;
        Pageable pageable = PageRequest.of(page, size);
        if (adSetId != null) {
            rows = adRepository.findByAdGroupId(adSetId, pageable).getContent();
        } else if (campaignId != null) {
            rows = adRepository.findByCampaignId(campaignId, pageable).getContent();
        } else {
            rows = adRepository.findAllActive(pageable).getContent();
        }
        return ResponseEntity.ok(rows.stream().map(this::toLegacyAdMap).toList());
    }

    @GetMapping("/ads/{id}")
    public ResponseEntity<Map<String, Object>> getAdById(@PathVariable UUID id) {
        Ad ad = adRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        if (ad.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad not found: " + id);
        }
        return ResponseEntity.ok(toLegacyAdMap(ad));
    }

    @PostMapping("/ads")
    public ResponseEntity<Map<String, Object>> createAd(@RequestBody Map<String, Object> payload) {
        UUID adGroupId = optionalUuid(payload.get("adSetId"));
        UUID campaignId = optionalUuid(payload.get("campaignId"));
        if (adGroupId == null && campaignId != null) {
            List<AdGroup> groups = adGroupRepository.findAllByCampaignId(campaignId);
            if (!groups.isEmpty()) {
                adGroupId = groups.get(0).getId();
            }
        }
        if (adGroupId == null) {
            throw new IllegalArgumentException("adSetId is required");
        }
        final UUID resolvedAdGroupId = adGroupId;
        AdGroup group = adGroupRepository.findById(resolvedAdGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + resolvedAdGroupId));
        UUID resolvedCampaignId = campaignId != null ? campaignId : group.getCampaignId();

        CreateAdRequest request = new CreateAdRequest(
                String.valueOf(payload.getOrDefault("name", payload.getOrDefault("title", "Untitled Ad"))),
                parseEnum(payload.get("type"), AdType.class, AdType.IMAGE),
                optionalUuid(payload.get("creativeId")),
                listOfStrings(payload.get("headlines")),
                listOfStrings(payload.get("descriptions")),
                payload.get("callToAction") == null ? null : payload.get("callToAction").toString(),
                stringOr(payload.get("finalUrl"), stringOr(payload.get("linkUrl"), null)),
                payload.get("displayUrl") == null ? null : payload.get("displayUrl").toString()
        );
        var created = adService.createAd(resolvedCampaignId, resolvedAdGroupId, request);
        Ad ad = adRepository.findById(created.getId()).orElseThrow(() -> new EntityNotFoundException("Ad not found: " + created.getId()));
        return ResponseEntity.ok(toLegacyAdMap(ad));
    }

    @PutMapping("/ads/{id}")
    public ResponseEntity<Map<String, Object>> updateAd(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        Ad existing = adRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        if (existing.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad not found: " + id);
        }
        String name = String.valueOf(payload.getOrDefault("name", payload.getOrDefault("title", existing.getName())));
        CreateAdRequest request = new CreateAdRequest(
                name,
                parseEnum(payload.get("type"), AdType.class, existing.getAdType()),
                optionalUuid(payload.get("creativeId")) != null ? optionalUuid(payload.get("creativeId")) : existing.getCreativeId(),
                listOfStringsOrDefault(payload.get("headlines"), existing.getHeadlines()),
                listOfStringsOrDefault(payload.get("descriptions"), existing.getDescriptions()),
                stringOr(payload.get("callToAction"), existing.getCallToAction()),
                stringOr(payload.get("finalUrl"), stringOr(payload.get("linkUrl"), existing.getFinalUrl())),
                stringOr(payload.get("displayUrl"), existing.getDisplayUrl())
        );
        adService.updateAd(id, existing.getAdGroup().getCampaignId(), existing.getAdGroupId(), request);

        Ad updated = adRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        if (payload.get("status") != null) {
            AdStatus status = parseEnum(payload.get("status"), AdStatus.class, null);
            if (status != null) {
                updated.setStatus(status);
                adRepository.save(updated);
            }
        }
        return ResponseEntity.ok(toLegacyAdMap(updated));
    }

    @DeleteMapping("/ads/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable UUID id) {
        Ad existing = adRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        adService.deleteAd(id, existing.getAdGroup().getCampaignId(), existing.getAdGroupId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ads/bulk-update")
    public ResponseEntity<Void> bulkUpdateAds(@RequestBody Map<String, Object> payload) {
        List<UUID> ids = uuidList(payload.get("ids"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
        AdStatus targetStatus = parseEnum(data.get("status"), AdStatus.class, null);

        for (UUID id : ids) {
            adRepository.findById(id).ifPresent(ad -> {
                if (targetStatus != null && ad.getDeletedAt() == null) {
                    ad.setStatus(targetStatus);
                    adRepository.save(ad);
                }
            });
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ads/{id}/preview")
    public ResponseEntity<Object> previewAd(
            @PathVariable UUID id,
            @RequestParam(value = "placement", required = false) String placement
    ) {
        return ResponseEntity.ok(adService.getAdPreview(id, placement, null));
    }

    private Map<String, Object> toLegacyAdSetMap(AdGroup ag) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ag.getId());
        map.put("campaignId", ag.getCampaignId());
        map.put("name", ag.getName());
        map.put("status", ag.getStatus() == null ? null : ag.getStatus().name().toLowerCase(Locale.ROOT));
        map.put("budget", ag.getBudget());
        map.put("budgetType", ag.getBudgetType() == null ? null : ag.getBudgetType().name().toLowerCase(Locale.ROOT));
        map.put("audience", toLegacyAudience(ag.getTargeting()));
        map.put("placements", ag.getPlacements() == null ? List.of() : Arrays.asList(ag.getPlacements()));
        map.put("createdAt", ag.getCreatedAt());
        map.put("updatedAt", ag.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toLegacyAdMap(Ad ad) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ad.getId());
        map.put("campaignId", ad.getAdGroup().getCampaignId());
        map.put("adSetId", ad.getAdGroupId());
        map.put("adGroupId", ad.getAdGroupId());
        map.put("name", ad.getName());
        map.put("title", ad.getName());
        map.put("description", joinOne(ad.getDescriptions()));
        map.put("type", ad.getAdType() == null ? null : ad.getAdType().name().toLowerCase(Locale.ROOT));
        map.put("status", ad.getStatus() == null ? null : ad.getStatus().name().toLowerCase(Locale.ROOT));
        map.put("callToAction", ad.getCallToAction());
        map.put("finalUrl", ad.getFinalUrl());
        map.put("linkUrl", ad.getFinalUrl());
        map.put("displayUrl", ad.getDisplayUrl());
        map.put("headlines", ad.getHeadlines() == null ? List.of() : Arrays.asList(ad.getHeadlines()));
        map.put("descriptions", ad.getDescriptions() == null ? List.of() : Arrays.asList(ad.getDescriptions()));
        map.put("creativeId", ad.getCreativeId());
        map.put("imageUrl", null);
        map.put("createdAt", ad.getCreatedAt());
        map.put("updatedAt", ad.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toLegacyAudience(Map<String, Object> targeting) {
        if (targeting == null) {
            return Map.of("locations", List.of(), "ageMin", 18, "ageMax", 65, "genders", List.of("all"), "interests", List.of());
        }
        Object demographicsObj = targeting.get("demographics");
        int ageMin = 18;
        int ageMax = 65;
        List<String> genders = List.of("all");
        if (demographicsObj instanceof Map<?, ?> d) {
            Object minRaw = d.get("ageMin");
            Object maxRaw = d.get("ageMax");
            ageMin = minRaw instanceof Number n ? n.intValue() : 18;
            ageMax = maxRaw instanceof Number n ? n.intValue() : 65;
            genders = listOfStrings(d.get("genders"));
        }
        return Map.of(
                "locations", listOfStrings(targeting.get("locations")),
                "ageMin", ageMin,
                "ageMax", ageMax,
                "genders", genders.isEmpty() ? List.of("all") : genders,
                "interests", listOfStrings(targeting.get("interests"))
        );
    }

    private UUID requiredUuid(Object value, String field) {
        UUID id = optionalUuid(value);
        if (id == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return id;
    }

    private UUID optionalUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString().trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double toDouble(Object value, Double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private <E extends Enum<E>> E parseEnum(Object raw, Class<E> enumType, E fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.toString().trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BudgetType parseBudgetType(Object raw, BudgetType fallback) {
        return parseEnum(raw, BudgetType.class, fallback);
    }

    private List<UUID> uuidList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (Object value : list) {
            UUID id = optionalUuid(value);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    private List<String> listOfStrings(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !item.toString().isBlank()) {
                out.add(item.toString());
            }
        }
        return out;
    }

    private List<String> listOfStringsOrDefault(Object raw, String[] fallback) {
        List<String> parsed = listOfStrings(raw);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        if (fallback == null) {
            return List.of();
        }
        return Arrays.asList(fallback);
    }

    private String stringOr(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private String joinOne(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return values[0] == null ? "" : values[0];
    }
}

