package com.travelo.searchservice.client.dto;

import java.util.List;

/**
 * Paginated response wrapper.
 */
public class PageResponse<T> {
    private List<T> data;
    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPages;

    // Getters and Setters
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
}

