package com.zipbob.zipbob_backend.admin.common;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
// 페이지네이션
@Getter
public class AdminPagination {

    // 관리자 목록 공통 설정
    public static final int PAGE_SIZE = 20;
    public static final int PAGE_BLOCK_SIZE = 5;

    private final int currentPage;
    private final int totalPages;
    private final long totalElements;
    private final int pageSize;

    private final int startPage;
    private final int endPage;

    private final String basePath;
    private final Map<String, String> parameters;

    public AdminPagination(
            Page<?> page,
            String basePath,
            Map<String, String> parameters
    ) {
        this.currentPage = page.getNumber() + 1;
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.pageSize = page.getSize();

        this.startPage =
                ((currentPage - 1) / PAGE_BLOCK_SIZE)
                        * PAGE_BLOCK_SIZE + 1;

        this.endPage =
                Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        this.basePath = basePath;
        this.parameters = Map.copyOf(parameters);
    }

    // 화면의 1페이지를 JPA의 0페이지로 변환
    public static PageRequest pageRequest(int page) {
        int currentPage = Math.max(page, 1);

        return PageRequest.of(currentPage - 1, PAGE_SIZE);
    }

    // 페이지 조회 및 마지막 페이지 초과 요청 보정
    public static <T> Page<T> loadPage(
            int requestedPage,
            IntFunction<Page<T>> loader
    ) {
        int currentPage = Math.max(requestedPage, 1);

        Page<T> result = loader.apply(currentPage);

        int lastPage = Math.max(result.getTotalPages(), 1);

        if (currentPage > lastPage) {
            result = loader.apply(lastPage);
        }

        return result;
    }

    // 검색 조건을 유지한 페이지 이동 주소 생성
    public String link(int targetPage) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(basePath);

        Map<String, Object> variables = new HashMap<>();

        parameters.forEach((name, value) -> {
            if (!"page".equals(name)) {
                builder.queryParam(name, "{" + name + "}");
                variables.put(name, value);
            }
        });

        builder.queryParam("page", "{page}");
        variables.put("page", targetPage);

        return builder
                .encode()
                .buildAndExpand(variables)
                .toUriString();
    }
}