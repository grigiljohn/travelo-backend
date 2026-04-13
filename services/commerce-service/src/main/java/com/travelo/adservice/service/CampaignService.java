package com.travelo.adservice.service;

import com.travelo.adservice.dto.CreateCampaignRequest;
import com.travelo.adservice.dto.CampaignResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.entity.CampaignStatus;

import java.util.UUID;

public interface CampaignService {
    PageResponse<CampaignResponse> getCampaigns(UUID businessAccountId, CampaignStatus status, String search, int page, int size, String sort, String direction);
    
    CampaignResponse getCampaignById(UUID id, UUID businessAccountId);
    
    CampaignResponse createCampaign(UUID businessAccountId, UUID userId, CreateCampaignRequest request);
    
    CampaignResponse updateCampaign(UUID id, UUID businessAccountId, CreateCampaignRequest request);
    
    void deleteCampaign(UUID id, UUID businessAccountId);
    
    CampaignResponse updateCampaignStatus(UUID id, UUID businessAccountId, CampaignStatus status);
    
    void bulkUpdateCampaigns(UUID businessAccountId, java.util.List<UUID> campaignIds, java.util.Map<String, Object> updates);
}

