package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.reelservice.client.dto.AdDeliveryResponse;
import com.travelo.reelservice.client.dto.PostDto;

/**
 * Unified reel item that can be either a reel post or an ad.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReelItem {
    
    @JsonProperty("type")
    private String type;  // "reel" or "ad"
    
    // Reel fields (when type = "reel")
    @JsonProperty("reel_id")
    private String reelId;
    
    @JsonProperty("reel")
    private PostDto reel;
    
    // Ad fields (when type = "ad")
    @JsonProperty("ad_id")
    private String adId;
    
    @JsonProperty("ad")
    private AdDeliveryResponse ad;
    
    @JsonProperty("format")
    private String format;  // For ads: "reel-ad"

    public ReelItem() {
    }

    public static ReelItem fromReel(PostDto reel) {
        ReelItem item = new ReelItem();
        item.setType("reel");
        item.setReelId(reel.getId());
        item.setReel(reel);
        return item;
    }

    public static ReelItem fromAd(AdDeliveryResponse ad) {
        ReelItem item = new ReelItem();
        item.setType("ad");
        item.setAdId(ad.adId() != null ? ad.adId().toString() : null);
        item.setAd(ad);
        item.setFormat("reel-ad");
        return item;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReelId() { return reelId; }
    public void setReelId(String reelId) { this.reelId = reelId; }
    public PostDto getReel() { return reel; }
    public void setReel(PostDto reel) { this.reel = reel; }
    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }
    public AdDeliveryResponse getAd() { return ad; }
    public void setAd(AdDeliveryResponse ad) { this.ad = ad; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}

