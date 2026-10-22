package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;


// 다른 엔티티에 포함할 수 있는 클래스로 지정
@Embeddable
@Getter
@NoArgsConstructor
// 기기 ID와 메뉴 ID가 같으면 같은 키로 판단
@EqualsAndHashCode
// JPA 매핑 규칙: 테이블의 기본 키가 복합 키인 경우, 이를 표현하는 별도의 복합 키 클래스를 만든다.
// implements Serializable : 복합 키 클래스의 직렬화 지원
public class LikeId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "menu_id", nullable = false)
    private Integer menuId;

    public LikeId(String userId, Integer menuId) {
        this.userId = userId;
        this.menuId = menuId;
    }
}