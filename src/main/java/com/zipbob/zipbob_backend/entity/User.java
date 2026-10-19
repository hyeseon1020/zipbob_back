package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // 회원이 직접 입력하는 로그인 아이디
    // 자동 증가 번호가 아니므로 @GeneratedValue 사용 안 함
    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    // 회원가입 당시 등록된 기기
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_device_id")
    private UserDevice userDevice;

    // 원문 비밀번호가 아닌 BCrypt 해시 저장
    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw;

    // 회원명 = 닉네임
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    // 이메일 @ 앞부분
    @Column(name = "email", length = 50)
    private String email;

    // 이메일 도메인
    @Column(name = "email_adr", length = 50)
    private String emailAdr;

    // 작성자
    @Column(name = "created_id", length = 50)
    private String createdId;

    // 현재 DB 컬럼명 crated_dt 유지
    // INSERT 시 DB의 DEFAULT CURRENT_TIMESTAMP 사용
    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    // 수정자
    @Column(name = "updated_id", length = 50)
    private String updatedId;

    // 수정일
    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;

    // 이미 해시한 비밀번호를 전달받아 변경
    public void changePassword(String encodedPassword) {
        this.userPw = encodedPassword;
        this.updatedId = this.userId;
        this.updatedDt = LocalDateTime.now();
    }

    // 검증이 완료된 닉네임으로 변경
    public void changeNickname(String userName) {
        this.userName = userName;
        this.updatedId = this.userId;
        this.updatedDt = LocalDateTime.now();
    }
}