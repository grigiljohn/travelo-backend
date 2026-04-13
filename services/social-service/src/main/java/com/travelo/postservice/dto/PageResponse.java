package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> data;
    private Integer page;
    private Integer limit;
    private Long totalPosts;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrev;

    public PageResponse() {
    }

    public PageResponse(List<T> data, Integer page, Integer limit, Long totalPosts, Integer totalPages, Boolean hasNext, Boolean hasPrev) {
        this.data = data;
        this.page = page;
        this.limit = limit;
        this.totalPosts = totalPosts;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrev = hasPrev;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // Getters and Setters
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrev() {
        return hasPrev;
    }

    public void setHasPrev(Boolean hasPrev) {
        this.hasPrev = hasPrev;
    }

    /**
     * Create PageResponse from Spring Data Page.
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber() + 1, // Convert 0-based to 1-based
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    public static class Builder<T> {
        private List<T> data;
        private Integer page;
        private Integer limit;
        private Long totalPosts;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrev;

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Builder<T> page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder<T> limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder<T> totalPosts(Long totalPosts) {
            this.totalPosts = totalPosts;
            return this;
        }

        public Builder<T> totalPages(Integer totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder<T> hasNext(Boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder<T> hasPrev(Boolean hasPrev) {
            this.hasPrev = hasPrev;
            return this;
        }

        public PageResponse<T> build() {
            return new PageResponse<>(data, page, limit, totalPosts, totalPages, hasNext, hasPrev);
        }
    }

    @Override
    public String toString() {
        return "PageResponse{" +
                "data=" + data +
                ", page=" + page +
                ", limit=" + limit +
                ", totalPosts=" + totalPosts +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrev=" + hasPrev +
                '}';
    }
}

