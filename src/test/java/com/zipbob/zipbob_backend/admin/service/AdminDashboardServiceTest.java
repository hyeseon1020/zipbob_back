package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.dto.DashboardResponseDto;
import com.zipbob.zipbob_backend.admin.repository.AdminDashboardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
대시보드 서비스 단위 테스트
 - 실제 DB 대신 가짜 Repository 사용
 - 현재 시각을 고정하여 한국 날짜 기준 계산 검증
 - 날짜 보완·합계·그래프 비율 검증
 */
class AdminDashboardServiceTest {

    private AdminDashboardRepository repository;
    private AdminDashboardService service;

    /*
    각 테스트 실행 전 공통 준비
     - UTC 10월 24일 15:30은 한국 시간 10월 25일 00:30
     - 기본 조회 결과는 모두 빈 데이터로 설정
     */
    @BeforeEach
    void setUp() {
        repository = mock(AdminDashboardRepository.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-10-24T15:30:00Z"),
                ZoneOffset.UTC
        );

        service = new AdminDashboardService(repository, clock);

        when(repository.findDailySignups(any(), any()))
                .thenReturn(List.of());

        when(repository.findCategories())
                .thenReturn(List.of());

        when(repository.findTags())
                .thenReturn(List.of());

        when(repository.findPopularMenus())
                .thenReturn(List.of());

        when(repository.countMenus())
                .thenReturn(0L);

        when(repository.countUsers())
                .thenReturn(0L);
    }

    /*
    한국 시간 기준 최근 7일 계산
     - 서버 Clock의 날짜가 UTC 기준 24일이어도 한국은 25일
     - 조회 범위는 19일 00:00부터 25일 00:30까지
     - 날짜 목록은 19일부터 25일까지 총 7일
     */
    @Test
    void usesSeoulTimeAndBuildsSevenCalendarDays() {
        DashboardResponseDto result = service.getDashboard();

        List<LocalDate> expectedDates = List.of(
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 25)
        );

        List<LocalDate> actualDates =
                result.getDailySignups().stream()
                        .map(day -> day.getDate())
                        .toList();

        assertEquals(expectedDates, actualDates);

        assertEquals(
                "10.19",
                result.getDailySignups().get(0).getLabel()
        );

        assertEquals(
                "10.25",
                result.getDailySignups().get(6).getLabel()
        );

        verify(repository).findDailySignups(
                LocalDateTime.of(2026, 10, 19, 0, 0),
                LocalDateTime.of(2026, 10, 25, 0, 30)
        );
    }

    /*
    가입자가 없는 날짜 보완 및 합계 계산
     - 조회 결과는 19일 2명, 25일 3명만 반환
     - 중간 날짜는 0명으로 채움
     - 합계는 5명
     - Y축 최대 4명 기준 3명의 막대 높이는 75%
     */
    @Test
    void fillsMissingDaysAndCalculatesSignupTotalAndBarHeight() {
        when(repository.findDailySignups(any(), any()))
                .thenReturn(List.of(
                        new Object[]{
                                Date.valueOf("2026-10-19"),
                                2L
                        },
                        new Object[]{
                                LocalDate.of(2026, 10, 25),
                                3L
                        }
                ));

        DashboardResponseDto result = service.getDashboard();

        List<Long> counts =
                result.getDailySignups().stream()
                        .map(day -> day.getCount())
                        .toList();

        assertEquals(
                List.of(2L, 0L, 0L, 0L, 0L, 0L, 3L),
                counts
        );

        assertEquals(5L, result.getNewUsers());
        assertEquals(4L, result.getSignupAxisMax());

        assertEquals(
                50.0,
                result.getDailySignups().get(0).getPercent(),
                0.001
        );

        assertEquals(
                0.0,
                result.getDailySignups().get(1).getPercent(),
                0.001
        );

        assertEquals(
                75.0,
                result.getDailySignups().get(6).getPercent(),
                0.001
        );
    }

    /*
    모든 집계 값이 0인 경우
     - 신규 회원 합계는 0명
     - 7일의 날짜와 최소 Y축 눈금 유지
     - 카테고리·태그 막대 계산에서 0으로 나누지 않음
     */
    @Test
    void handlesZeroCountsWithoutInvalidPercentages() {
        when(repository.findCategories())
                .thenReturn(List.<Object[]>of(
                        new Object[]{
                                "rice",
                                "밥류",
                                0L
                        }
                ));

        when(repository.findTags())
                .thenReturn(List.<Object[]>of(
                        new Object[]{
                                "easy",
                                "간편식",
                                0L
                        }
                ));

        DashboardResponseDto result = service.getDashboard();

        assertEquals(0L, result.getTotalMenus());
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getNewUsers());

        assertEquals(7, result.getDailySignups().size());
        assertEquals(4L, result.getSignupAxisMax());

        assertTrue(
                result.getDailySignups().stream()
                        .allMatch(day ->
                                day.getCount() == 0
                                        && day.getPercent() == 0.0
                        )
        );

        assertEquals(
                0.0,
                result.getCategories().get(0).getPercent(),
                0.001
        );

        assertEquals(
                0.0,
                result.getTags().get(0).getPercent(),
                0.001
        );

        assertTrue(result.getPopularMenus().isEmpty());
    }

    /*
    그래프별 최댓값 기준 비율 계산
     - 카테고리 최대 12개: 12개는 100%, 6개는 50%
     - 태그 최대 24개: 24개는 100%, 6개는 25%
     - 일별 신규 회원 최대 8명: 8명은 100%, 2명은 25%
     */
    @Test
    void scalesEachChartUsingItsOwnMaximum() {
        when(repository.findCategories())
                .thenReturn(List.of(
                        new Object[]{
                                "rice",
                                "밥류",
                                12L
                        },
                        new Object[]{
                                "noodle",
                                "면류",
                                6L
                        }
                ));

        when(repository.findTags())
                .thenReturn(List.of(
                        new Object[]{
                                "easy",
                                "간편식",
                                24L
                        },
                        new Object[]{
                                "spicy",
                                "매운맛",
                                6L
                        }
                ));

        when(repository.findDailySignups(any(), any()))
                .thenReturn(List.of(
                        new Object[]{
                                LocalDate.of(2026, 10, 19),
                                8L
                        },
                        new Object[]{
                                LocalDate.of(2026, 10, 25),
                                2L
                        }
                ));

        DashboardResponseDto result = service.getDashboard();

        assertEquals(
                100.0,
                result.getCategories().get(0).getPercent(),
                0.001
        );

        assertEquals(
                50.0,
                result.getCategories().get(1).getPercent(),
                0.001
        );

        assertEquals(
                100.0,
                result.getTags().get(0).getPercent(),
                0.001
        );

        assertEquals(
                25.0,
                result.getTags().get(1).getPercent(),
                0.001
        );

        assertEquals(8L, result.getSignupAxisMax());

        assertEquals(
                100.0,
                result.getDailySignups().get(0).getPercent(),
                0.001
        );

        assertEquals(
                25.0,
                result.getDailySignups().get(6).getPercent(),
                0.001
        );

        assertEquals(10L, result.getNewUsers());
    }
}