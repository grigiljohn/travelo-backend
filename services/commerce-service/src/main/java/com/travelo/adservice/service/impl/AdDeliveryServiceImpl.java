package com.travelo.adservice.service.impl;

import com.travelo.adservice.dto.AdDeliveryRequest;
import com.travelo.adservice.dto.AdDeliveryResponse;
import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.AdGroup;
import com.travelo.adservice.entity.AdGroupStatus;
import com.travelo.adservice.entity.AdStatus;
import com.travelo.adservice.entity.Campaign;
import com.travelo.adservice.entity.CampaignStatus;
import com.travelo.adservice.repository.AdRepository;
import com.travelo.adservice.repository.AdGroupRepository;
import com.travelo.adservice.repository.CampaignRepository;
import com.travelo.adservice.repository.AssetRepository;
import com.travelo.adservice.service.AdDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for delivering ads to feed-service and reel-service.
 * Handles ad selection based on placement, targeting, and status.
 */
@Service
public class AdDeliveryServiceImpl implements AdDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AdDeliveryServiceImpl.class);

    private final AdRepository adRepository;
    private final AdGroupRepository adGroupRepository;
    private final CampaignRepository campaignRepository;
    private final AssetRepository assetRepository;

    public AdDeliveryServiceImpl(
            AdRepository adRepository,
            AdGroupRepository adGroupRepository,
            CampaignRepository campaignRepository,
            AssetRepository assetRepository) {
        this.adRepository = adRepository;
        this.adGroupRepository = adGroupRepository;
        this.campaignRepository = campaignRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public List<AdDeliveryResponse> fetchAdsForPlacement(AdDeliveryRequest request, int count) {
        log.info("Fetching {} ads for placement: {}, userId: {}", count, request.placement(), request.userId());

        // Find active campaigns
        OffsetDateTime now = OffsetDateTime.now();
        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaigns(CampaignStatus.ACTIVE, now);

        if (activeCampaigns.isEmpty()) {
            log.debug("No active campaigns found");
            return Collections.emptyList();
        }

        // Find ads from active campaigns with matching placement
        List<Ad> candidateAds = new ArrayList<>();
        for (Campaign campaign : activeCampaigns) {
            List<AdGroup> adGroups = adGroupRepository.findAllByCampaignId(campaign.getId()).stream()
                    .filter(ag -> ag.getStatus() == AdGroupStatus.ACTIVE)
                    .filter(ag -> ag.getDeletedAt() == null)
                    .filter(ag -> hasPlacement(ag, request.placement()))
                    .collect(Collectors.toList());

            for (AdGroup adGroup : adGroups) {
                List<Ad> ads = adRepository.findByAdGroupIdAndStatus(adGroup.getId(), AdStatus.ACTIVE).stream()
                        .filter(ad -> ad.getDeletedAt() == null)
                        .collect(Collectors.toList());
                candidateAds.addAll(ads);
            }
        }

        if (candidateAds.isEmpty()) {
            log.debug("No ads found for placement: {}", request.placement());
            return Collections.emptyList();
        }

        // Basic targeting check (can be enhanced later)
        candidateAds = filterByTargeting(candidateAds, request);

        // Shuffle and limit
        Collections.shuffle(candidateAds);
        List<Ad> selectedAds = candidateAds.stream()
                .limit(count)
                .collect(Collectors.toList());

        // Convert to AdDeliveryResponse
        return selectedAds.stream()
                .map(ad -> convertToDeliveryResponse(ad, request.placement()))
                .collect(Collectors.toList());
    }


    private boolean hasPlacement(AdGroup adGroup, String placement) {
        if (adGroup.getPlacements() == null || adGroup.getPlacements().length == 0) {
            return true; // No placement restriction means all placements
        }
        return Arrays.asList(adGroup.getPlacements()).contains(placement);
    }

    private List<Ad> filterByTargeting(List<Ad> ads, AdDeliveryRequest request) {
        // Basic targeting - can be enhanced with location, demographics, interests, etc.
        // For now, just return all ads (targeting logic can be added later)
        return ads;
    }

    /**
     * Convert Ad entity to AdDeliveryResponse.
     * Includes shop metadata if CTA is SHOP_NOW.
     */
    private AdDeliveryResponse convertToDeliveryResponse(Ad ad, String format) {
        // Build creative metadata
        Map<String, Object> creative = new HashMap<>();
        if (ad.getCreative() != null) {
            creative.putAll(ad.getCreative());
        }
        
        // Fetch asset if creativeId exists
        if (ad.getCreativeId() != null) {
            assetRepository.findById(ad.getCreativeId())
                    .filter(asset -> asset.getDeletedAt() == null)
                    .ifPresent(asset -> {
                        creative.put("imageUrl", asset.getUrl());
                        creative.put("thumbnailUrl", asset.getThumbnailUrl());
                        creative.put("videoUrl", asset.getType().name().equals("VIDEO") ? asset.getUrl() : null);
                        creative.put("width", asset.getWidth());
                        creative.put("height", asset.getHeight());
                    });
        }

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("adGroupId", ad.getAdGroupId());
        if (ad.getAdGroup() != null && ad.getAdGroup().getCampaign() != null) {
            metadata.put("campaignId", ad.getAdGroup().getCampaign().getId());
            metadata.put("brandName", ad.getBrandName());
        }

        // Add shop routing metadata if CTA is SHOP_NOW
        UUID shopId = ad.getShopId();
        UUID productId = ad.getProductId();
        String shopUrl = null;
        
        if ("SHOP_NOW".equalsIgnoreCase(ad.getCallToAction()) && shopId != null) {
            shopUrl = "travelo://shop/" + shopId;
            if (productId != null) {
                shopUrl += "/product/" + productId;
            }
            metadata.put("shopId", shopId.toString());
            if (productId != null) {
                metadata.put("productId", productId.toString());
            }
            metadata.put("shopUrl", shopUrl);
        }

        return new AdDeliveryResponse(
                ad.getId(),
                ad.getAdGroupId(),
                (ad.getAdGroup() != null && ad.getAdGroup().getCampaign() != null) 
                        ? ad.getAdGroup().getCampaign().getId() : null,
                ad.getAdType(),
                format,
                creative,
                ad.getHeadlines() != null ? List.of(ad.getHeadlines()) : List.of(),
                ad.getDescriptions() != null ? List.of(ad.getDescriptions()) : List.of(),
                ad.getCallToAction(),
                ad.getCtaText(),
                ad.getFinalUrl(),
                ad.getDisplayUrl(),
                ad.getBrandName(),
                ad.getBrandWebsite(),
                shopId,  // shopId
                productId,  // productId
                shopUrl,  // shopUrl
                metadata
        );
    }
}

