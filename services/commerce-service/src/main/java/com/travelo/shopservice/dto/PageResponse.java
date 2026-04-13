package com.travelo.shopservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic paginated response DTO.
 */
public class PageResponse<T> {
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("total")
    private Long total;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("limit")
    private Integer limit;
    
    @JsonProperty("has_more")
    private Boolean hasMore;

    public PageResponse() {
    }

    public PageResponse(List<T> data, Long total, Integer page, Integer limit, Boolean hasMore) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.limit = limit;
        this.hasMore = hasMore;
    }

    // Getters and Setters
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}

