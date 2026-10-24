package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /*
     관리자 초기 생성 전용 INSERT
     비밀번호는 BCrypt 해시를 전달받음
     기기와 이메일은 연결하지 않음
     중복 시 기존 계정을 수정하지 않음
     */
    @Modifying
    @Query(value = """
        INSERT INTO zipbob_app.user_mng (
            user_id,
            user_pw,
            user_name,
            user_role,
            created_id
        )
        VALUES (
            :userId,
            :encodedPassword,
            :userName,
            'ADMIN',
            :userId
        )
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertAdmin(
            @Param("userId") String userId,
            @Param("encodedPassword") String encodedPassword,
            @Param("userName") String userName
    );


    // 관리자 페이지 회원 목록 조회용 Native Query
    @Query(value = """
        SELECT 
            u.user_id AS userId,
            u.login_id AS loginId,
            u.nickname AS nickname,
            u.email AS email
        FROM zipbob_app.user_mng u
        ORDER BY u.user_id DESC
    """, nativeQuery = true)
    List<UserListProjection> findAllUsersForAdmin();

    // 관리자 페이지 회원 목록 검색·페이지 조회
    @Query(
            value = """
            SELECT u
            FROM User u
            WHERE (
                :keyword = ''
                OR (
                    :searchType = 'userId'
                    AND LOWER(u.userId)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                OR (
                    :searchType = 'userName'
                    AND LOWER(u.userName)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                OR (
                    :searchType = 'email'
                    AND LOWER(
                        CONCAT(
                            CONCAT(COALESCE(u.email, ''), '@'),
                            COALESCE(u.emailAdr, '')
                        )
                    ) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            """,
            countQuery = """
            SELECT COUNT(u)
            FROM User u
            WHERE (
                :keyword = ''
                OR (
                    :searchType = 'userId'
                    AND LOWER(u.userId)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                OR (
                    :searchType = 'userName'
                    AND LOWER(u.userName)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                OR (
                    :searchType = 'email'
                    AND LOWER(
                        CONCAT(
                            CONCAT(COALESCE(u.email, ''), '@'),
                            COALESCE(u.emailAdr, '')
                        )
                    ) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            """
    )
    Page<User> searchUsersForAdmin(
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}