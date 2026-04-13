package com.travelo.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for search results.
 */
public class SearchResponse {
    
    @JsonProperty("results")
    private List<SearchResultItem> results;
    
    @JsonProperty("total_results")
    private Long totalResults;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("limit")
    private Integer limit;
    
    @JsonProperty("has_more")
    private Boolean hasMore;

    public SearchResponse() {
    }

    public SearchResponse(List<SearchResultItem> results, Long totalResults, Integer page, Integer limit, Boolean hasMore) {
        this.results = results;
        this.totalResults = totalResults;
        this.page = page;
        this.limit = limit;
        this.hasMore = hasMore;
    }

    // Getters and Setters
    public List<SearchResultItem> getResults() { return results; }
    public void setResults(List<SearchResultItem> results) { this.results = results; }
    public Long getTotalResults() { return totalResults; }
    public void setTotalResults(Long totalResults) { this.totalResults = totalResults; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}

