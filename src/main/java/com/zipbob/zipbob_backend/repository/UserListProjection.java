package com.zipbob.zipbob_backend.repository;

public interface UserListProjection {
    String userId();       //아이디
    Integer userDevice();    // 기기아이디
    String userName();   // 회원명
    String email();      // 이메일
    String emailAdr();    // 도메인
    String userRole();    //규정
}
