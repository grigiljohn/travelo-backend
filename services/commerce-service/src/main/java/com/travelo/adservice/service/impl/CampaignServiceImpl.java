package com.travelo.adservice.service.impl;

import com.travelo.adservice.dto.CreateCampaignRequest;
import com.travelo.adservice.dto.CampaignResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.entity.Campaign;
import com.travelo.adservice.entity.CampaignStatus;
import com.travelo.adservice.repository.CampaignRepository;
import com.travelo.adservice.service.CampaignService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignServiceImpl(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public PageResponse<CampaignResponse> getCampaigns(UUID businessAccountId, CampaignStatus status, String search, 
                                                        int page, int size, String sort, String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(direction), sort);
        
        Page<Campaign> campaignPage;
        boolean hasSearch = search != null && !search.isBlank();
        if (status != null && hasSearch) {
            campaignPage = campaignRepository.findByBusinessAccountIdAndStatusAndSearch(
                    businessAccountId, status, search.trim(), pageable
            );
        } else if (status != null) {
            campaignPage = campaignRepository.findByBusinessAccountIdAndStatus(businessAccountId, status, pageable);
        } else if (hasSearch) {
            campaignPage = campaignRepository.findByBusinessAccountIdAndSearch(
                    businessAccountId, search.trim(), pageable
            );
        } else {
            campaignPage = campaignRepository.findByBusinessAccountId(businessAccountId, pageable);
        }
        
        List<CampaignResponse> content = campaignPage.getContent().stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, campaignPage.getTotalElements());
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public CampaignResponse getCampaignById(UUID id, UUID businessAccountId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
        
        if (!campaign.getBusinessAccountId().equals(businessAccountId)) {
            throw new IllegalArgumentException("Campaign does not belong to this business account");
        }
        
        if (campaign.getDeletedAt() != null) {
            throw new EntityNotFoundException("Campaign not found: " + id);
        }
        
        return CampaignResponse.fromEntity(campaign);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public CampaignResponse createCampaign(UUID businessAccountId, UUID userId, CreateCampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setBusinessAccountId(businessAccountId);
        campaign.setName(request.name());
        campaign.setObjective(request.objective());
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setBudget(request.budget());
        campaign.setBudgetType(request.budgetType());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setBidStrategy(request.bidStrategy());
        campaign.setBidAmount(request.bidAmount());
        campaign.setTargetRoas(request.targetRoas());
        campaign.setTargetCpa(request.targetCpa());
        campaign.setCreatedBy(userId);
        
        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public CampaignResponse updateCampaign(UUID id, UUID businessAccountId, CreateCampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
        
        if (!campaign.getBusinessAccountId().equals(businessAccountId)) {
            throw new IllegalArgumentException("Campaign does not belong to this business account");
        }
        
        if (campaign.getDeletedAt() != null) {
            throw new EntityNotFoundException("Campaign not found: " + id);
        }
        
        campaign.setName(request.name());
        campaign.setObjective(request.objective());
        campaign.setBudget(request.budget());
        campaign.setBudgetType(request.budgetType());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setBidStrategy(request.bidStrategy());
        campaign.setBidAmount(request.bidAmount());
        campaign.setTargetRoas(request.targetRoas());
        campaign.setTargetCpa(request.targetCpa());
        
        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void deleteCampaign(UUID id, UUID businessAccountId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
        
        if (!campaign.getBusinessAccountId().equals(businessAccountId)) {
            throw new IllegalArgumentException("Campaign does not belong to this business account");
        }
        
        campaign.setDeletedAt(OffsetDateTime.now());
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public CampaignResponse updateCampaignStatus(UUID id, UUID businessAccountId, CampaignStatus status) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
        
        if (!campaign.getBusinessAccountId().equals(businessAccountId)) {
            throw new IllegalArgumentException("Campaign does not belong to this business account");
        }
        
        if (campaign.getDeletedAt() != null) {
            throw new EntityNotFoundException("Campaign not found: " + id);
        }
        
        campaign.setStatus(status);
        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void bulkUpdateCampaigns(UUID businessAccountId, List<UUID> campaignIds, java.util.Map<String, Object> updates) {
        List<Campaign> campaigns = campaignRepository.findAllById(campaignIds);
        
        for (Campaign campaign : campaigns) {
            if (!campaign.getBusinessAccountId().equals(businessAccountId)) {
                continue; // Skip campaigns that don't belong to this business account
            }
            
            if (campaign.getDeletedAt() != null) {
                continue; // Skip deleted campaigns
            }
            
            if (updates.containsKey("status")) {
                campaign.setStatus(CampaignStatus.valueOf(updates.get("status").toString()));
            }
            if (updates.containsKey("budget")) {
                campaign.setBudget(Double.parseDouble(updates.get("budget").toString()));
            }
            // Add more fields as needed
        }
        
        campaignRepository.saveAll(campaigns);
    }
}

