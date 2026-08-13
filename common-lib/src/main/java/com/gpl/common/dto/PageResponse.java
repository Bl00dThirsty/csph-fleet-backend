package com.gpl.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Réponse paginée pour les listes de données.
 * Supporte à la fois la nomenclature Maximo OSLC (member, totalCount)
 * et la nomenclature Spring Data (content, totalElements, Page<T>).
 *
 * @param <T> Type des éléments
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> member;
    private long totalCount;
    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private String href;

    /** Info de réponse (pattern Maximo responseInfo). */
    private ResponseInfo responseInfo;

    // Aliases to support Spring Data naming conventions (content, totalElements)
    public List<T> getContent() {
        return member;
    }

    public void setContent(List<T> content) {
        this.member = content;
    }

    public long getTotalElements() {
        return totalCount;
    }

    public void setTotalElements(long totalElements) {
        this.totalCount = totalElements;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseInfo {
        private String href;
        private int pagenum;
        private int pageSize;
        private long totalCount;
    }

    public static <T> PageResponse<T> of(List<T> content, long totalCount, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalCount / Math.max(1, size));
        return PageResponse.<T>builder()
                .member(content)
                .totalCount(totalCount)
                .pageNumber(page)
                .pageSize(size)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .responseInfo(ResponseInfo.builder()
                        .pagenum(page)
                        .pageSize(size)
                        .totalCount(totalCount)
                        .build())
                .build();
    }

    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return of(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
