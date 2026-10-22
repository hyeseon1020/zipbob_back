package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 관리할 엔티티: User, PK 타입: String
public interface UserRepository extends JpaRepository<User, String> {

    // 이메일로 회원 조회
    Optional<User> findByEmailAndEmailAdr(
            String email,
            String emailAdr
    );

    // 닉네임 중복 검사
    boolean existsByUserName(String userName);

    // 이메일 전체 주소 중복 검사
    boolean existsByEmailAndEmailAdr(
            String email,
            String emailAdr
    );

    // 같은 회원의 최근 조회 저장 요청을 순서대로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.userId = :userId
        """)
    Optional<User> findByIdForUpdate(
            @Param("userId") String userId
    );

    /*
     회원가입 전용 INSERT
     중복 없음: 1 반환
     PK 또는 UNIQUE 충돌: 저장하지 않고 0 반환
     회원가입에서 기존 회원을 수정하지 않도록 명시적으로 INSERT만 실행
     */
    @Modifying
    @Query(value = """
        INSERT INTO zipbob_app.user_mng (
            user_id,
            user_device_id,
            user_pw,
            user_name,
            email,
            email_adr,
            created_id
        )
        VALUES (
            :userId,
            :userDeviceId,
            :userPw,
            :userName,
            :email,
            :emailAdr,
            :userId
        )
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertUser(
            @Param("userId") String userId,
            @Param("userDeviceId") Integer userDeviceId,
            @Param("userPw") String userPw,
            @Param("userName") String userName,
            @Param("email") String email,
            @Param("emailAdr") String emailAdr
    );
}