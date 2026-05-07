package com.example.coffeebean.common;

import java.util.List;

public class PageResponse<T> {

    private List<T> items;
    private long page;
    private long pageSize;
    private long total;
    private long totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> items, long page, long pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    public static <T> PageResponse<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageResponse<>(items, page, pageSize, total);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
