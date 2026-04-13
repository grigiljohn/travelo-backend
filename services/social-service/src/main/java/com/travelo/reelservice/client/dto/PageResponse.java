package com.travelo.reelservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> data;
    private Integer page;
    private Integer limit;
    
    @com.fasterxml.jackson.annotation.JsonProperty("total_posts")
    private Long totalPosts;
    
    @com.fasterxml.jackson.annotation.JsonProperty("total_pages")
    private Integer totalPages;
    
    @com.fasterxml.jackson.annotation.JsonProperty("has_next")
    private Boolean hasNext;
    
    @com.fasterxml.jackson.annotation.JsonProperty("has_prev")
    private Boolean hasPrev;

    // Getters and Setters
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}

