package com.travelo.adservice.service;

import com.travelo.adservice.dto.CreateAdGroupRequest;
import com.travelo.adservice.dto.AdGroupResponse;
import com.travelo.adservice.dto.PageResponse;

import java.util.UUID;

public interface AdGroupService {
    PageResponse<AdGroupResponse> getAdGroups(UUID campaignId, int page, int size);
    
    AdGroupResponse getAdGroupById(UUID id, UUID campaignId);
    
    AdGroupResponse createAdGroup(UUID campaignId, CreateAdGroupRequest request);
    
    AdGroupResponse updateAdGroup(UUID id, UUID campaignId, CreateAdGroupRequest request);
    
    void deleteAdGroup(UUID id, UUID campaignId);
}

