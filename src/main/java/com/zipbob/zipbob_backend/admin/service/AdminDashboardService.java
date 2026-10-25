package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.dto.DashboardResponseDto;
import com.zipbob.zipbob_backend.admin.dto.DashboardResponseDto.DailySignup;
import com.zipbob.zipbob_backend.admin.dto.DashboardResponseDto.Distribution;
import com.zipbob.zipbob_backend.admin.dto.DashboardResponseDto.PopularMenu;
import com.zipbob.zipbob_backend.admin.repository.AdminDashboardRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

// 날짜 보완·그래프 계산·DTO 변환
@Service
public class AdminDashboardService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final AdminDashboardRepository repository;
    private final Clock clock;

    // 실제 애플리케이션에서 사용하는 생성자 (한국 시간 기준의 Clock 설정)
    @Autowired
    public AdminDashboardService(
            AdminDashboardRepository repository
    ) {
        this(repository, Clock.system(SEOUL));
    }

    /*
    테스트에서 사용하는 생성자
     - Clock을 직접 전달하여 기준 시각을 고정
     - 자정 경계와 최근 7일 계산을 반복 검증할 때 사용
     */
    AdminDashboardService(
            AdminDashboardRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    /*
    대시보드 전체 데이터 조회
     - 한국 시간 기준 최근 7일 계산
     - 일별 신규 회원 데이터 생성
     - 인기 메뉴 및 카테고리·태그 집계 결과 변환
     - 전체 결과를 DashboardResponseDto로 반환

     트랜잭션:
     readOnly : 조회 전용
     REPEATABLE_READ : 여러 집계 쿼리를 같은 DB 스냅샷으로 조회
     */
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public DashboardResponseDto getDashboard() {

        /*
        조회 기간 계산
         - 한국 시간 기준 현재 시각 사용
         - 오늘을 포함한 최근 7일이므로 시작 날짜는 6일 전
         */
        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL));
        LocalDate firstDate = now.toLocalDate().minusDays(6);

        /*
        날짜별 신규 회원 수 조회
         - 시작 날짜의 00:00 이상부터 현재 시각 미만까지 조회
         - DB의 crated_dt가 한국 현지 시각으로 저장된다는 전제

         반환 순서:
         [0] 가입 날짜
         [1] 신규 회원 수
         */
        List<Object[]> signupRows =
                repository.findDailySignups(
                        firstDate.atStartOfDay(),
                        now
                );

        /*
        날짜별 신규 회원 수를 Map에 저장
         - 날짜를 키로 사용하여 회원 수를 조회
         - JDBC에서 반환한 날짜가 java.sql.Date이면 LocalDate로 변환

         저장 예시:
         2026-10-19 → 2
         2026-10-21 → 1
         */
        Map<LocalDate, Long> signupCounts = new HashMap<>();

        for (Object[] row : signupRows) {

            LocalDate date =
                    row[0] instanceof java.sql.Date sqlDate
                            ? sqlDate.toLocalDate()
                            : (LocalDate) row[0];

            long count = ((Number) row[1]).longValue();
            signupCounts.put(date, count);
        }

        /*
        신규 회원 그래프의 Y축 최댓값 계산
         - 일별 신규 회원 수 중 가장 큰 값 사용
         - 가입자가 적거나 없어도 눈금을 유지하도록 최소 4로 설정
         */
        long maxSignupCount =
                signupCounts.values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0);

        long signupAxisMax =
                Math.max(4, maxSignupCount);

        /*
        최근 7일의 그래프 데이터 생성
         - 시작 날짜부터 하루씩 증가하여 총 7일 생성
         - 가입자가 없는 날짜는 0명으로 처리
         - 날짜 라벨은 MM.dd 형식으로 표시
         - 막대 높이는 Y축 최댓값 기준으로 계산

         계산 예시:
         신규 회원 3명 / Y축 최댓값 4명 × 100 = 75%
         */
        DateTimeFormatter labelFormatter =
                DateTimeFormatter.ofPattern("MM.dd");

        List<DailySignup> dailySignups =
                IntStream.range(0, 7)
                        .mapToObj(index -> {

                            LocalDate date = firstDate.plusDays(index);
                            long count = signupCounts.getOrDefault(date, 0L);
                            double percent = count * 100.0 / signupAxisMax;

                            return new DailySignup(
                                    date,
                                    date.format(labelFormatter),
                                    count,
                                    percent
                            );
                        })
                        .toList();

        /*
        최근 7일 신규 회원 수 합산
         - 그래프에 사용하는 회원 수를 그대로 합산
         - 상단 신규 회원 카드와 그래프 합계를 일치시킴
         */
        long newUsers =
                dailySignups.stream()
                        .mapToLong(DailySignup::getCount)
                        .sum();

        /*
        즐겨찾기 인기 메뉴 조회 결과를 DTO로 변환

         반환 순서:
         [0] 메뉴 ID
         [1] 메뉴명
         [2] 대표 이미지 경로
         [3] 즐겨찾기 수
         */
        List<PopularMenu> popularMenus =
                repository.findPopularMenus()
                        .stream()
                        .map(row -> new PopularMenu(
                                ((Number) row[0]).intValue(),
                                (String) row[1],
                                (String) row[2],
                                ((Number) row[3]).longValue()
                        ))
                        .toList();

        /*
        상단 현황 카드 데이터 조회
         - 전체 메뉴 수
         - 관리자 계정을 제외한 전체 일반 회원 수
         */
        long totalMenus = repository.countMenus();
        long totalUsers = repository.countUsers();

        /*
        카테고리·음식 태그별 그래프 데이터 생성
         - 각 조회 결과를 Distribution DTO로 변환
         - 각 그래프의 최댓값을 기준으로 막대 길이를 개별 계산
         */
        List<Distribution> categories = toDistributions(repository.findCategories());
        List<Distribution> tags = toDistributions(repository.findTags());

        /*
        대시보드 전체 결과 반환
         - 상단 현황 카드
         - 카테고리·음식 태그별 메뉴 수
         - 즐겨찾기 인기 메뉴
         - 최근 7일 신규 회원 그래프
         */
        return new DashboardResponseDto(
                totalMenus,
                totalUsers,
                newUsers,
                categories,
                tags,
                popularMenus,
                dailySignups,
                signupAxisMax
        );
    }

    /*
    카테고리·음식 태그 집계 결과를 그래프용 DTO로 변환
     - 해당 그래프의 가장 큰 메뉴 수를 기준으로 막대 길이 계산
     - 전체 항목이 0개라면 막대 길이도 0으로 처리
     - percent는 전체 메뉴 대비 구성비가 아닌 상대적인 막대 길이

     입력 순서:
     [0] 코드 ID
     [1] 카테고리명 또는 태그명
     [2] 메뉴 수
     List<Object[]> : 집계 조회 결과의 여러 행
     */
    private List<Distribution> toDistributions(
            List<Object[]> rows
    ) {
        /*
        집계 항목 중 가장 큰 메뉴 수 조회
         - 결과가 없으면 0 사용
         */
        long maxCount =
                rows.stream()
                        .mapToLong(row ->
                                ((Number) row[2]).longValue()
                        )
                        .max()
                        .orElse(0);

        /*
        각 항목을 Distribution DTO로 변환
         - 코드 ID, 이름, 메뉴 수 매핑
         - 최댓값이 0이면 나눗셈을 수행하지 않음
         */
        return rows.stream()
                .map(row -> {

                    String code = (String) row[0];
                    String name = (String) row[1];

                    long count = ((Number) row[2]).longValue();
                    double percent =
                            maxCount == 0
                                    ? 0
                                    : count * 100.0 / maxCount;
                    return new Distribution(
                            code,
                            name,
                            count,
                            percent
                    );
                })
                .toList();
    }
}