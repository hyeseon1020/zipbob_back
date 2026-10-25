package com.zipbob.zipbob_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

/*
 관리자 대시보드에 전달할 통계 데이터
 - 전체 메뉴 수
 - 전체 회원 수
 - 최근 7일 신규 회원 수
 - 카테고리별 메뉴 수
 - 음식 태그별 메뉴 수
 - 즐겨찾기 인기 메뉴 TOP 5
 - 최근 7일 일별 신규 회원 수
 */
@Getter
@AllArgsConstructor
public class DashboardResponseDto {

    // 전체 메뉴 수
    private final long totalMenus;
    // 전체 일반 회원 수: 관리자 계정 제외
    private final long totalUsers;
    // 오늘을 포함한 최근 7일 신규 회원 수
    private final long newUsers;
    // 카테고리별 메뉴 수
    private final List<Distribution> categories;
    // 음식 태그별 적용 메뉴 수
    private final List<Distribution> tags;
    // 즐겨찾기 인기 메뉴 TOP 5
    private final List<PopularMenu> popularMenus;
    // 최근 7일의 날짜별 신규 회원 수
    private final List<DailySignup> dailySignups;
    // 신규 회원 그래프의 Y축 최댓값
    private final long signupAxisMax;

    // 카테고리·음식 태그별 메뉴 수 : 두 그래프는 필요한 데이터 구조가 같으므로, 공통 클래스를 사용
    @Getter
    @AllArgsConstructor
    public static class Distribution {

        // 공통코드 ID
        private final String code;
        // 화면에 표시할 카테고리명 또는 태그명
        private final String name;
        // 해당 카테고리 또는 태그가 적용된 메뉴 수
        private final long count;
        // 해당 그래프의 최댓값을 기준으로 계산한 막대 길이(%)
        private final double percent;
    }

    // 즐겨찾기 인기 메뉴 한 개의 정보
    @Getter
    @AllArgsConstructor
    public static class PopularMenu {

        // 메뉴 ID
        private final int menuId;
        // 메뉴 이름
        private final String menuName;
        // 대표 이미지 경로
        private final String imageUrl;
        // 현재 해당 메뉴를 즐겨찾기에 저장한 일반 회원 수
        private final long favoriteCount;
    }

    // 특정 날짜의 신규 회원 수
    @Getter
    @AllArgsConstructor
    public static class DailySignup {

        // 실제 날짜: 예) 2026-10-25
        private final LocalDate date;
        // 그래프에 표시할 날짜: 예) 10.25
        private final String label;
        // 해당 날짜의 신규 회원 수
        private final long count;
        // 그래프 Y축 최댓값 기준 막대 높이(%)
        private final double percent;
    }
}