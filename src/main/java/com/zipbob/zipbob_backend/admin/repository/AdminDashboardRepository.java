package com.zipbob.zipbob_backend.admin.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 대시보드 통계 조회 : 엔티티 전체를 가져오지 않고, DB에서 집계한 결과만 조회한다
@Repository
@RequiredArgsConstructor
public class AdminDashboardRepository {

    // 대시보드 집계 쿼리를 한곳에 모으기 위해 EntityManager를 사용 (JPA를 통해 SQL을 실행)
    private final EntityManager entityManager;

    // 전체 메뉴 수 (DB에서 개수만 계산해서 가져오는 방식)
    public long countMenus() {

        Number result = (Number) entityManager
                .createNativeQuery("""
                    SELECT COUNT(*)
                    FROM zipbob_app.menu_mng
                    """)
                .getSingleResult();

        return result.longValue();
    }

    // 전체 일반 회원 수 (관리자 계정은 제외)
    // getSingleResult() : 결과가 하나일 때 사용
    public long countUsers() {

        Number result = (Number) entityManager
                .createNativeQuery("""
                    SELECT COUNT(*)
                    FROM zipbob_app.user_mng
                    WHERE user_role = 'USER'
                    """)
                .getSingleResult();

        return result.longValue();
    }

    /*
    카테고리별 메뉴 수
     - 사용 중인 카테고리는 메뉴가 없어도 0개로 표시
     - 미사용 카테고리에 연결된 기존 메뉴도 집계
     - 정상적인 카테고리 코드가 없는 메뉴는 미분류로 집계

     반환 순서:
     [0] 코드 ID
     [1] 카테고리명
     [2] 메뉴 수
     List<Object[]> : 집계 조회 결과의 여러 행
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findCategories() {

        return entityManager
                .createNativeQuery("""
                    SELECT
                        c.code_id,
                        c.code_name,
                        COUNT(m.menu_id)
                    FROM zipbob_app.code_mng c
                    -- 메뉴가 없는 카테고리
                    LEFT JOIN zipbob_app.menu_mng m
                        ON m.category = c.code_id

                    WHERE c.code_parent = 'category_code'

                    GROUP BY
                        c.code_id,
                        c.code_name,
                        c.use_yn
                    -- 미사용 카테고리 처리
                    HAVING
                        c.use_yn = 'Y'
                        OR COUNT(m.menu_id) > 0

                    UNION ALL
                    -- __uncategorized__ : 이 조회 결과에서만 사용하는 식별자
                    SELECT
                        '__uncategorized__',
                        '미분류',
                        COUNT(*)
                    FROM zipbob_app.menu_mng m

                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM zipbob_app.code_mng c
                        WHERE c.code_id = m.category
                          AND c.code_parent = 'category_code'
                    )

                    HAVING COUNT(*) > 0

                    ORDER BY
                        3 DESC,
                        1 ASC
                    """)
                .getResultList();
    }


    /*
     음식 태그별 적용 메뉴 수
     - 사용 중인 음식 태그와 태그 연결만 집계
     - 같은 메뉴에 같은 태그가 중복 등록되어도 한 번만 집계
     - 적용된 메뉴가 없는 사용 태그는 0개로 표시

     반환 순서:
     [0] 코드 ID
     [1] 태그명
     [2] 메뉴 수
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findTags() {
        return entityManager
                .createNativeQuery("""
                SELECT
                    c.code_id,
                    c.code_name,
                    COUNT(DISTINCT m.menu_id)
                FROM zipbob_app.code_mng c

                LEFT JOIN zipbob_app.tag_mng t
                    ON t.code_id = c.code_id

                LEFT JOIN zipbob_app.menu_mng m
                    ON m.menu_id = t.menu_id

                WHERE c.code_parent = 'tag_code'

                GROUP BY
                    c.code_id,
                    c.code_name

                ORDER BY
                    3 DESC,
                    c.code_id ASC
                """)
                .getResultList();
    }

    /*
    즐겨찾기 인기 메뉴 TOP 5
     - 일반 회원의 현재 즐겨찾기만 집계
     - 저장 수가 많은 메뉴부터 조회
     - 동점이면 메뉴 ID가 작은 순서로 조회
     - 즐겨찾기가 없는 메뉴는 제외

     반환 순서:
     [0] 메뉴 ID
     [1] 메뉴명
     [2] 대표 이미지 경로
     [3] 즐겨찾기 수
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findPopularMenus() {

        return entityManager
                .createNativeQuery("""
                    SELECT
                        m.menu_id,
                        m.menu_name,

                        (
                            -- 대표 이미지 조회
                            SELECT f.file_path
                            FROM zipbob_app.file_mng f
                            WHERE f.menu_id = m.menu_id
                              AND f.use_yn = 'Y'
                            ORDER BY f.file_id ASC
                            LIMIT 1
                        ),

                        COUNT(*) AS favorite_count

                    FROM zipbob_app.like_mng l

                    INNER JOIN zipbob_app.menu_mng m
                        ON m.menu_id = l.menu_id

                    INNER JOIN zipbob_app.user_mng u
                        ON u.user_id = l.user_id
                       AND u.user_role = 'USER'

                    GROUP BY
                        m.menu_id,
                        m.menu_name
                    -- 동점이면 메뉴 ID가 작은 메뉴 우선
                    ORDER BY
                        -- 많이 저장한 메뉴 우선
                        favorite_count DESC,
                        m.menu_id ASC
                    -- 최대 5개 조회
                    LIMIT 5
                    """)
                .getResultList();
    }


    /*
    지정한 기간의 날짜별 신규 일반 회원 수 (조회 범위: start 이상, end 미만)
    - 기존 User 엔티티의 LocalDateTime 매핑에 맞춘 조회
    - crated_dt가 한국 현지 시각으로 저장된다는 전제

     반환 순서:
     [0] 가입 날짜
     [1] 신규 회원 수
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findDailySignups(
            LocalDateTime start,
            LocalDateTime end
    ) {

        return entityManager
                .createNativeQuery("""
                    SELECT
                        CAST(crated_dt AS date),
                        COUNT(*)

                    FROM zipbob_app.user_mng

                    WHERE user_role = 'USER'
                      AND crated_dt >= :start
                      AND crated_dt < :end

                    GROUP BY CAST(crated_dt AS date)

                    ORDER BY 1 ASC
                    """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }
}