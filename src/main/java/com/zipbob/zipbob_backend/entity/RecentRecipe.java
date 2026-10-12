package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "recent_recipe_mng", schema = "zipbob_app",
        // 기기 ID + 메뉴 ID 중복 방지 규칙을 Entity에도 명시
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_recent_recipe_device_menu",
                        columnNames = {"user_device_id", "menu_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentRecipe {

    // 기록 한 행을 구분하는 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recent_recipe_id")
    private Integer recentRecipeId;

    // 사용자 기기 테이블의 숫자 PK
    @Column(name = "user_device_id", nullable = false, updatable = false)
    private Integer userDeviceId;

    // 메뉴 테이블의 숫자 PK
    @Column(name = "menu_id", nullable = false, updatable = false)
    private Integer menuId;

    // 마지막으로 레시피를 본 시간
    @Column(name = "viewed_dt", nullable = false)
    private LocalDateTime viewedDt;

    /*
    최근 본 레시피
    - 개별 기록의 생성과 시간 변경이 단순해서 Entity + JPA로 구현
    - Service에서 트랜잭션과 기기별 최대 5개 유지 규칙 처리

    신규 저장 - Service에서 Entity 생성 → Repository save() → JPA가 INSERT 실행
    기존 기록 수정 - Service의 트랜잭션 안에서 Repository로 Entity 조회 → updateViewedDt() 호출 → JPA 변경 감지로 UPDATE 실행
    초과 기록 삭제 - 기기별 기록이 5개를 초과하면 → Repository로 가장 오래된 기록을 찾아 삭제
    */
    // 처음 본 레시피의 기록 생성
    public RecentRecipe(
            Integer userDeviceId,
            Integer menuId,
            LocalDateTime viewedDt
    ) {
        this.userDeviceId =
                Objects.requireNonNull(userDeviceId, "기기 ID는 필수입니다.");
        this.menuId =
                Objects.requireNonNull(menuId, "메뉴 ID는 필수입니다.");
        this.viewedDt =
                Objects.requireNonNull(viewedDt, "조회 시간은 필수입니다.");
    }

    // 이미 본 레시피를 다시 보면 조회 시간만 변경
    public void updateViewedDt(LocalDateTime viewedDt) {
        this.viewedDt =
                Objects.requireNonNull(viewedDt, "조회 시간은 필수입니다.");
    }
}