package com.travelo.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for grouped search results.
 * Groups results by type: Top, Users, Hashtags, Places, Posts.
 */
public class GroupedSearchResponse {
    
    @JsonProperty("top")
    private List<SearchResultItem> top;        // Top 5 results across all types
    
    @JsonProperty("users")
    private List<SearchResultItem> users;      // User results
    
    @JsonProperty("hashtags")
    private List<SearchResultItem> hashtags;    // Hashtag results
    
    @JsonProperty("places")
    private List<SearchResultItem> places;      // Location results
    
    @JsonProperty("posts")
    private List<SearchResultItem> posts;       // Post results
    
    @JsonProperty("shops")
    private List<SearchResultItem> shops;       // Shop results
    
    @JsonProperty("products")
    private List<SearchResultItem> products;    // Product results
    
    @JsonProperty("total_results")
    private Long totalResults;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("limit")
    private Integer limit;
    
    @JsonProperty("has_more")
    private Boolean hasMore;

    public GroupedSearchResponse() {
    }

    public GroupedSearchResponse(List<SearchResultItem> top, 
                                 List<SearchResultItem> users,
                                 List<SearchResultItem> hashtags,
                                 List<SearchResultItem> places,
                                 List<SearchResultItem> posts,
                                 List<SearchResultItem> shops,
                                 List<SearchResultItem> products,
                                 Long totalResults,
                                 Integer page,
                                 Integer limit,
                                 Boolean hasMore) {
        this.top = top;
        this.users = users;
        this.hashtags = hashtags;
        this.places = places;
        this.posts = posts;
        this.shops = shops;
        this.products = products;
        this.totalResults = totalResults;
        this.page = page;
        this.limit = limit;
        this.hasMore = hasMore;
    }

    // Getters and Setters
    public List<SearchResultItem> getTop() { return top; }
    public void setTop(List<SearchResultItem> top) { this.top = top; }
    public List<SearchResultItem> getUsers() { return users; }
    public void setUsers(List<SearchResultItem> users) { this.users = users; }
    public List<SearchResultItem> getHashtags() { return hashtags; }
    public void setHashtags(List<SearchResultItem> hashtags) { this.hashtags = hashtags; }
    public List<SearchResultItem> getPlaces() { return places; }
    public void setPlaces(List<SearchResultItem> places) { this.places = places; }
    public List<SearchResultItem> getPosts() { return posts; }
    public void setPosts(List<SearchResultItem> posts) { this.posts = posts; }
    public List<SearchResultItem> getShops() { return shops; }
    public void setShops(List<SearchResultItem> shops) { this.shops = shops; }
    public List<SearchResultItem> getProducts() { return products; }
    public void setProducts(List<SearchResultItem> products) { this.products = products; }
    public Long getTotalResults() { return totalResults; }
    public void setTotalResults(Long totalResults) { this.totalResults = totalResults; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}

