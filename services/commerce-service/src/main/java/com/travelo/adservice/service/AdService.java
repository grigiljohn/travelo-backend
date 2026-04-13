package com.travelo.adservice.service;

import com.travelo.adservice.dto.CreateAdRequest;
import com.travelo.adservice.dto.AdResponse;
import com.travelo.adservice.dto.PageResponse;

import java.util.UUID;

public interface AdService {
    PageResponse<AdResponse> getAds(UUID campaignId, UUID adGroupId, int page, int size);
    
    AdResponse getAdById(UUID id, UUID campaignId, UUID adGroupId);
    
    AdResponse createAd(UUID campaignId, UUID adGroupId, CreateAdRequest request);
    
    AdResponse updateAd(UUID id, UUID campaignId, UUID adGroupId, CreateAdRequest request);
    
    void deleteAd(UUID id, UUID campaignId, UUID adGroupId);
    
    Object getAdPreview(UUID id, String placement, String device);
}

