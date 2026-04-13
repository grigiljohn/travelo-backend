package com.travelo.adservice.service.impl;

import com.travelo.adservice.dto.CreateAdRequest;
import com.travelo.adservice.dto.AdResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.AdGroup;
import com.travelo.adservice.repository.AdGroupRepository;
import com.travelo.adservice.repository.AdRepository;
import com.travelo.adservice.service.AdService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdServiceImpl implements AdService {

    private final AdRepository adRepository;
    private final AdGroupRepository adGroupRepository;

    public AdServiceImpl(AdRepository adRepository, AdGroupRepository adGroupRepository) {
        this.adRepository = adRepository;
        this.adGroupRepository = adGroupRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public PageResponse<AdResponse> getAds(UUID campaignId, UUID adGroupId, int page, int size) {
        AdGroup adGroup = adGroupRepository.findById(adGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + adGroupId));
        
        if (!adGroup.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad group does not belong to this campaign");
        }
        
        if (adGroup.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + adGroupId);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Ad> adPage = adRepository.findByAdGroupId(adGroupId, pageable);
        
        List<AdResponse> content = adPage.getContent().stream()
                .map(AdResponse::fromEntity)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, adPage.getTotalElements());
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public AdResponse getAdById(UUID id, UUID campaignId, UUID adGroupId) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        
        if (!ad.getAdGroupId().equals(adGroupId)) {
            throw new IllegalArgumentException("Ad does not belong to this ad group");
        }
        
        if (!ad.getAdGroup().getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad does not belong to this campaign");
        }
        
        if (ad.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad not found: " + id);
        }
        
        return AdResponse.fromEntity(ad);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public AdResponse createAd(UUID campaignId, UUID adGroupId, CreateAdRequest request) {
        AdGroup adGroup = adGroupRepository.findById(adGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + adGroupId));
        
        if (!adGroup.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad group does not belong to this campaign");
        }
        
        if (adGroup.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + adGroupId);
        }
        
        Ad ad = new Ad();
        ad.setAdGroup(adGroup);
        ad.setName(request.name());
        ad.setAdType(request.adType());
        ad.setCreativeId(request.creativeId());
        ad.setHeadlines(request.headlines() != null ? 
                request.headlines().toArray(new String[0]) : new String[0]);
        ad.setDescriptions(request.descriptions() != null ? 
                request.descriptions().toArray(new String[0]) : new String[0]);
        ad.setCallToAction(request.callToAction());
        ad.setFinalUrl(request.finalUrl());
        ad.setDisplayUrl(request.displayUrl());
        
        // Calculate ad strength based on completeness
        ad.setAdStrength(calculateAdStrength(ad));
        
        Ad saved = adRepository.save(ad);
        return AdResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public AdResponse updateAd(UUID id, UUID campaignId, UUID adGroupId, CreateAdRequest request) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        
        if (!ad.getAdGroupId().equals(adGroupId)) {
            throw new IllegalArgumentException("Ad does not belong to this ad group");
        }
        
        if (!ad.getAdGroup().getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad does not belong to this campaign");
        }
        
        if (ad.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad not found: " + id);
        }
        
        ad.setName(request.name());
        ad.setAdType(request.adType());
        ad.setCreativeId(request.creativeId());
        ad.setHeadlines(request.headlines() != null ? 
                request.headlines().toArray(new String[0]) : new String[0]);
        ad.setDescriptions(request.descriptions() != null ? 
                request.descriptions().toArray(new String[0]) : new String[0]);
        ad.setCallToAction(request.callToAction());
        ad.setFinalUrl(request.finalUrl());
        ad.setDisplayUrl(request.displayUrl());
        
        ad.setAdStrength(calculateAdStrength(ad));
        
        Ad saved = adRepository.save(ad);
        return AdResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void deleteAd(UUID id, UUID campaignId, UUID adGroupId) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        
        if (!ad.getAdGroupId().equals(adGroupId)) {
            throw new IllegalArgumentException("Ad does not belong to this ad group");
        }
        
        if (!ad.getAdGroup().getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad does not belong to this campaign");
        }
        
        ad.setDeletedAt(OffsetDateTime.now());
        adRepository.save(ad);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public Object getAdPreview(UUID id, String placement, String device) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found: " + id));
        
        if (ad.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad not found: " + id);
        }
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("id", ad.getId());
        preview.put("name", ad.getName());
        preview.put("adType", ad.getAdType());
        preview.put("headlines", ad.getHeadlines());
        preview.put("descriptions", ad.getDescriptions());
        preview.put("callToAction", ad.getCallToAction());
        preview.put("finalUrl", ad.getFinalUrl());
        preview.put("displayUrl", ad.getDisplayUrl());
        preview.put("placement", placement);
        preview.put("device", device);
        
        return preview;
    }

    private Integer calculateAdStrength(Ad ad) {
        int score = 0;
        
        if (ad.getHeadlines() != null && ad.getHeadlines().length >= 3) score++;
        if (ad.getDescriptions() != null && ad.getDescriptions().length >= 2) score++;
        if (ad.getFinalUrl() != null && !ad.getFinalUrl().isEmpty()) score++;
        if (ad.getCallToAction() != null && !ad.getCallToAction().isEmpty()) score++;
        if (ad.getCreativeId() != null) score++;
        
        return score;
    }
}

