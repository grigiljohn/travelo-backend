package com.travelo.adservice.service.impl;

import com.travelo.adservice.dto.CreateAdGroupRequest;
import com.travelo.adservice.dto.AdGroupResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.entity.AdGroup;
import com.travelo.adservice.entity.Campaign;
import com.travelo.adservice.repository.AdGroupRepository;
import com.travelo.adservice.repository.CampaignRepository;
import com.travelo.adservice.service.AdGroupService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdGroupServiceImpl implements AdGroupService {

    private final AdGroupRepository adGroupRepository;
    private final CampaignRepository campaignRepository;

    public AdGroupServiceImpl(AdGroupRepository adGroupRepository, CampaignRepository campaignRepository) {
        this.adGroupRepository = adGroupRepository;
        this.campaignRepository = campaignRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public PageResponse<AdGroupResponse> getAdGroups(UUID campaignId, int page, int size) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + campaignId));
        
        if (campaign.getDeletedAt() != null) {
            throw new EntityNotFoundException("Campaign not found: " + campaignId);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AdGroup> adGroupPage = adGroupRepository.findByCampaignId(campaignId, pageable);
        
        List<AdGroupResponse> content = adGroupPage.getContent().stream()
                .map(AdGroupResponse::fromEntity)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, adGroupPage.getTotalElements());
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public AdGroupResponse getAdGroupById(UUID id, UUID campaignId) {
        AdGroup adGroup = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        
        if (!adGroup.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad group does not belong to this campaign");
        }
        
        if (adGroup.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + id);
        }
        
        return AdGroupResponse.fromEntity(adGroup);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public AdGroupResponse createAdGroup(UUID campaignId, CreateAdGroupRequest request) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + campaignId));
        
        if (campaign.getDeletedAt() != null) {
            throw new EntityNotFoundException("Campaign not found: " + campaignId);
        }
        
        AdGroup adGroup = new AdGroup();
        adGroup.setCampaign(campaign);
        adGroup.setName(request.name());
        adGroup.setBudget(request.budget());
        adGroup.setBudgetType(request.budgetType());
        adGroup.setTargeting(request.targeting() != null ? request.targeting() : new java.util.HashMap<>());
        adGroup.setKeywords(request.keywords() != null ? request.keywords() : new java.util.ArrayList<>());
        adGroup.setNegativeKeywords(request.negativeKeywords() != null ? 
                request.negativeKeywords().toArray(new String[0]) : new String[0]);
        adGroup.setDevices(request.devices() != null ? 
                request.devices().toArray(new String[0]) : new String[0]);
        adGroup.setPlacements(request.placements() != null ? 
                request.placements().toArray(new String[0]) : new String[0]);
        
        AdGroup saved = adGroupRepository.save(adGroup);
        return AdGroupResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public AdGroupResponse updateAdGroup(UUID id, UUID campaignId, CreateAdGroupRequest request) {
        AdGroup adGroup = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        
        if (!adGroup.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad group does not belong to this campaign");
        }
        
        if (adGroup.getDeletedAt() != null) {
            throw new EntityNotFoundException("Ad group not found: " + id);
        }
        
        adGroup.setName(request.name());
        adGroup.setBudget(request.budget());
        adGroup.setBudgetType(request.budgetType());
        adGroup.setTargeting(request.targeting() != null ? request.targeting() : new java.util.HashMap<>());
        adGroup.setKeywords(request.keywords() != null ? request.keywords() : new java.util.ArrayList<>());
        adGroup.setNegativeKeywords(request.negativeKeywords() != null ? 
                request.negativeKeywords().toArray(new String[0]) : new String[0]);
        adGroup.setDevices(request.devices() != null ? 
                request.devices().toArray(new String[0]) : new String[0]);
        adGroup.setPlacements(request.placements() != null ? 
                request.placements().toArray(new String[0]) : new String[0]);
        
        AdGroup saved = adGroupRepository.save(adGroup);
        return AdGroupResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void deleteAdGroup(UUID id, UUID campaignId) {
        AdGroup adGroup = adGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad group not found: " + id));
        
        if (!adGroup.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Ad group does not belong to this campaign");
        }
        
        adGroup.setDeletedAt(OffsetDateTime.now());
        adGroupRepository.save(adGroup);
    }
}

