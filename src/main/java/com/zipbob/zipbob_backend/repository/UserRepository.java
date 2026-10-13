package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 관리할 엔티티: User, PK 타입: String
public interface UserRepository extends JpaRepository<User, String> {

    // 닉네임 중복 검사
    boolean existsByUserName(String userName);

    // 이메일 전체 주소 중복 검사
    boolean existsByEmailAndEmailAdr(
            String email,
            String emailAdr
    );

    // 같은 기기로 가입한 회원이 있는지 검사
    boolean existsByUserDevice_UserDeviceId(Integer userDeviceId);

    /*
     회원가입 전용 INSERT
     중복 없음: 1 반환
     PK 또는 UNIQUE 충돌: 저장하지 않고 0 반환
     회원가입에서 기존 회원을 수정하지 않도록 명시적으로 INSERT만 실행
     */
    @Modifying
    @Query(value = """
        INSERT INTO zipbob_app.user_mng
            (
                user_id,
                user_device_id,
                user_pw,
                user_name,
                email,
                email_adr,
                created_id
            )
        VALUES
            (
                :userId,
                :userDeviceId,
                :userPw,
                :userName,
                :email,
                :emailAdr,
                :userId
            )
        -- PostgreSQL의 중복 처리 문법          
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