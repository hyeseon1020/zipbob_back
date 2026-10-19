# 집밥PICK Backend

> 고민은 짧게, 집밥은 맛있게.

**집밥 메뉴 추천 앱의 메뉴·레시피·기기별 기록·회원 인증을 제공하는 Spring Boot REST API 서버입니다.**

비회원도 메뉴를 탐색하고 즐겨찾기를 이용할 수 있도록 기기 기반 저장 기능을 구현했습니다. 회원 기능은 JWT 기반 인증을 사용하며, <br>
이메일 인증을 회원가입·아이디 찾기·비밀번호 재설정에 공통으로 적용했습니다.

[Flutter 프론트엔드](https://github.com/WONDongin/zipbob_app) · [백엔드 저장소](https://github.com/hyeseon1020/zipbob_back) · [ERD](https://www.erdcloud.com/d/3aFinoeCdppLGQ8Na)

<br>

## 1. 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 서비스 | 집밥 메뉴 추천 및 레시피 앱 |
| 백엔드 역할 | 메뉴·추천 조회, 기기별 기록 관리, 회원·인증 처리 |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.1.1 |
| 데이터 접근 | Spring Data JPA · JPQL · Native SQL · JdbcTemplate |
| 데이터베이스 | PostgreSQL |
| 인증 | Spring Security · JWT · BCrypt |
| 빌드 | Gradle Wrapper |
| 현재 상태 | 주요 API 구현 및 Flutter 연동, 로컬 개발 환경 기준 |

버전은 현재 저장소의 `build.gradle`을 기준으로 작성했습니다.

<br>

## 2. 주요 기능

| 영역 | 구현 내용 |
| --- | --- |
| 메뉴 목록 | 메뉴명 검색, 카테고리 필터, 대표 이미지 조회 |
| 레시피 상세 | 메뉴·이미지·인분·조리 시간·난이도·재료·조리 순서 조회 |
| 추천 | 카테고리와 음식 특징에 맞는 후보 목록 조회 |
| 공통코드 | 카테고리·음식 특징 코드 조회 |
| 기기 등록 | UUID·플랫폼 검증 및 서버 기기 ID 반환 |
| 즐겨찾기 | 기기별 등록·해제·목록 조회 |
| 최근 본 레시피 | 마지막 조회 시간 갱신, 최신 5개 유지 |
| 회원가입 | 입력 검증, 중복 확인, 이메일 인증 검증, 비밀번호 해시 저장 |
| 로그인 | 비밀번호 확인 후 액세스·리프레시 토큰 발급 |
| 토큰 재발급 | 리프레시 토큰 검증·교체 |
| 로그아웃 | 해당 리프레시 토큰 삭제 |
| 내 정보 | 인증된 회원 정보 조회 |
| 이메일 인증 | 인증번호 발송·확인, 목적별 인증 증명 발급·사용 |
| 아이디 찾기 | 이메일 인증 후 가입 아이디 조회 |
| 비밀번호 재설정 | 이메일 인증 후 비밀번호 변경 및 기존 리프레시 토큰 삭제 |

추천 API는 **조건에 맞는 후보 목록**을 반환합니다. 후보 중 하나를 무작위로 선택하고 다시 추천하는 동작은 Flutter 앱에서 처리합니다.

<br>

## 3. 시스템 구성

| 구성 요소 | 역할 |
| --- | --- |
| Flutter 앱 | 사용자 입력, API 요청, 화면 표시, 클라이언트 토큰 관리 |
| Spring Boot | REST API, 비즈니스 규칙, 인증·인가, 트랜잭션 처리 |
| PostgreSQL | 메뉴·회원·기기별 기록·인증 데이터 저장 |
| SMTP 서버 | 이메일 인증번호 전송 |

### 요청 처리 구조

| 계층 | 책임 |
| --- | --- |
| Controller | HTTP 요청·응답 처리 |
| Service | 입력 검증, 비즈니스 로직, 트랜잭션 경계 |
| Repository | JPA·JPQL·Native SQL을 통한 데이터 접근 |
| Entity | 테이블 매핑과 상태 변경 |
| DTO·Projection | 요청·응답 구조 및 조회 결과 정의 |
| Security | JWT 검증, 인증 정보 등록, 접근 정책 |
| Config | 비밀번호 인코더·CORS·Security 설정 |

<br>

## 4. 핵심 설계와 구현 이유

### 4-1. 목록 조회와 상세 조회의 데이터 접근 분리

**문제**

목록 화면은 메뉴명·설명·카테고리·대표 이미지가 필요하지만, 상세 화면은 재료·조리 순서와 이미지 정보까지 필요합니다.

**구현**

- 목록은 필요한 컬럼만 조회하고 인터페이스 Projection으로 받습니다.
- 조회 결과를 응답 DTO로 변환합니다.
- 상세는 `LEFT JOIN FETCH`로 메뉴와 연결된 파일을 함께 조회합니다.
- 목록의 대표 이미지는 사용 중인 파일 중 ID가 가장 작은 파일 한 개를 선택합니다.

**선택 이유**

화면별로 필요한 데이터 범위를 구분하고, 목록 조회에서 불필요한 엔티티 전체 로딩을 줄였습니다. 상세 조회는 파일 컬렉션을 함께 가져와 DTO 변환 시 별도 지연 조회를 피하도록 구성했습니다.

### 4-2. 카테고리·음식 특징 기반 추천

**구현**

- 카테고리 조건과 음식 특징 조건을 함께 적용합니다.
- 음식 특징을 여러 개 선택하면 선택한 특징 중 하나 이상에 해당하는 메뉴를 조회합니다.
- 조건이 없으면 해당 조건의 필터링을 생략합니다.
- 태그가 포함된 전체 목록은 메뉴 ID를 기준으로 그룹화해 응답합니다.

예를 들어 카테고리가 `rice_bowl`, 특징이 `EASY`, `QUICK`이면 다음 조건으로 후보를 조회합니다.

```text
category = rice_bowl
AND
(tag = EASY OR tag = QUICK)
```

백엔드는 후보 조회를 담당하고, 앱은 후보 중 무작위 선택과 재추천 표시를 담당합니다.

### 4-3. 비회원 사용을 위한 기기별 즐겨찾기

**문제**

메뉴를 저장하기 위해 반드시 회원가입해야 한다면 서비스 진입 과정이 길어집니다.

**구현**

- 앱이 생성한 UUID를 기기 등록 API로 받습니다.
- 서버의 `userDeviceId`를 기준으로 즐겨찾기와 최근 조회 기록을 관리합니다.
- 즐겨찾기는 기기 ID와 메뉴 ID의 복합키로 구분합니다.
- 등록 쿼리에 `ON CONFLICT DO NOTHING`을 사용합니다.

**결과**

같은 메뉴의 등록 요청이 반복되어도 즐겨찾기가 중복 생성되지 않습니다.

현재 기기 ID는 기록 구분을 위한 식별자입니다. 계정 인증이나 기기 소유 증명을 대신하지 않으며, 여러 기기 간 계정 기반 동기화는 후속 작업입니다.

### 4-4. 최근 본 레시피를 최신 5개로 유지

**문제**

같은 메뉴를 다시 조회하면 기존 기록의 순서를 갱신해야 하고, 동시 저장 요청에서도 기록 수를 일관되게 관리해야 합니다.

**구현**

1. 기기 ID와 메뉴 ID를 검증합니다.
2. 기기 행에 쓰기 잠금을 획득합니다.
3. 기존 기록이 있으면 조회 시간을 갱신하고, 없으면 새로 저장합니다.
4. 변경 내용을 `flush()`한 후 최신순으로 조회합니다.
5. 앞의 5개를 유지하고 초과 기록을 삭제합니다.

**선택 이유**

기기별 저장 요청을 트랜잭션 안에서 순서대로 처리합니다. 저장·시간 갱신·초과 기록 삭제가 같은 트랜잭션에 포함되므로 이후 작업이 실패하면 함께 롤백됩니다.

조회 시간이 같으면 기록 ID를 추가 정렬 기준으로 사용합니다.

### 4-5. 액세스 토큰과 리프레시 토큰 분리

| 구분 | 액세스 토큰 | 리프레시 토큰 |
| --- | --- | --- |
| 형태 | HS256 서명 JWT | 32바이트 난수 기반 문자열 |
| 용도 | 회원 API 인증 | 새 토큰 발급 |
| 서버 저장 | 토큰 원문을 저장하지 않음 | SHA-256 해시 저장 |
| 검증 | 서명·만료·발급자·토큰 종류·회원 존재 | 형식·해시 조회·만료 |
| 재발급 | 새 JWT 발급 | 기존 토큰 삭제 후 새 토큰 발급 |

**재발급 처리**

- 리프레시 토큰 해시로 대상 행을 조회하면서 쓰기 잠금을 획득합니다.
- 만료 여부를 확인합니다.
- 기존 리프레시 토큰을 삭제하고 새 토큰을 발급합니다.
- 새 액세스·리프레시 토큰을 반환합니다.

같은 리프레시 토큰의 동시 사용을 직렬화하고, 사용이 끝난 토큰을 교체하도록 구성했습니다.

로그아웃은 해당 리프레시 토큰을 삭제합니다. 이미 발급된 액세스 토큰을 즉시 폐기하는 차단 목록은 현재 구현 범위에 포함되지 않습니다.

### 4-6. 이메일 인증의 목적과 사용 상태 검증

**문제**

인증번호가 일치하는지만 확인하면 다른 이메일이나 다른 작업에 인증 결과를 재사용할 수 있습니다.

**구현**

인증을 번호 확인 단계와 후속 작업 승인 단계로 구분합니다.

1. 6자리 인증번호를 생성해 이메일로 전송합니다.
2. 인증번호는 BCrypt 해시로 저장합니다.
3. 번호 확인에 성공하면 별도의 인증 증명을 발급합니다.
4. 인증 증명은 SHA-256 해시로 저장합니다.
5. 후속 요청에서 이메일·목적·만료·사용 여부를 검증합니다.
6. 실제 작업의 트랜잭션 안에서 인증 증명을 사용 처리합니다.

| 인증 목적 | 코드 |
| --- | --- |
| 회원가입 | `SIGNUP` |
| 아이디 찾기 | `FIND_ID` |
| 비밀번호 재설정 | `RESET_PASSWORD` |

### 인증 정책

| 항목 | 현재 설정 |
| --- | --- |
| 인증번호 | 숫자 6자리 |
| 인증번호 유효시간 | 5분 |
| 인증 증명 유효시간 | 10분 |
| 같은 이메일·목적의 재발송 간격 | 60초 |
| 인증번호 불일치 허용 횟수 | 최대 5회 |
| 재발송 | 기존 미사용 인증번호·인증 증명 만료 처리 |

회원가입용 인증으로 비밀번호를 재설정하거나, 인증한 이메일을 바꾸어 가입하거나, 이미 사용한 인증 증명을 다시 사용하는 요청을 거절합니다.

### 동시 요청 처리

같은 이메일·목적의 발송·확인·사용 요청에는 PostgreSQL 트랜잭션 단위 advisory lock을 적용합니다. 인증 기록을 변경하는 단계에서는 행 잠금도 사용합니다.

인증번호가 틀린 경우에도 실패 횟수는 저장되도록 확인 메서드의 롤백 정책을 구분했습니다.

### 4-7. 비밀번호 재설정과 인증 증명 소비를 하나의 작업으로 처리

- 회원가입과 같은 비밀번호 검증 규칙을 사용합니다.
- `RESET_PASSWORD` 목적의 인증 증명을 검증합니다.
- 아이디에 등록된 이메일과 요청 이메일이 일치하는지 확인합니다.
- 새 비밀번호를 BCrypt 해시로 변경합니다.
- 해당 회원의 기존 리프레시 토큰을 삭제합니다.

이 과정은 하나의 트랜잭션으로 처리합니다. 회원 정보가 일치하지 않는 등 후속 검증에 실패하면 인증 증명의 사용 처리도 롤백됩니다.

<br>

## 5. 데이터베이스 구성

현재 엔티티와 주요 SQL은 `zipbob_app` 스키마를 사용합니다.

| 테이블 | 역할 |
| --- | --- |
| `menu_mng` | 메뉴 기본 정보와 레시피 |
| `file_mng` | 메뉴 이미지와 파일 정보 |
| `code_mng` | 카테고리·음식 특징 등 공통코드 |
| `tag_mng` | 메뉴별 음식 특징 연결 |
| `user_device_mng` | UUID·플랫폼 등 기기 정보 |
| `like_mng` | 기기별 즐겨찾기 |
| `recent_recipe_mng` | 기기별 최근 조회 기록 |
| `user_mng` | 회원 정보와 비밀번호 해시 |
| `user_refresh_token_mng` | 리프레시 토큰 해시와 만료 정보 |
| `email_verification_mng` | 인증번호·인증 증명·시도 횟수·사용 상태 |

### 주요 관계와 정책

- 메뉴 한 개에 여러 이미지와 태그를 연결합니다.
- 즐겨찾기는 `(user_device_id, menu_id)` 조합으로 구분합니다.
- 최근 조회 기록은 같은 기기·메뉴 조합을 중복 생성하지 않고 조회 시간을 갱신합니다.
- 회원가입 시 아이디·닉네임·이메일·가입 기기의 중복을 검사합니다.
- 카테고리·태그 이름 조회에는 `zipbob_app.codename_f()` 함수를 사용합니다.

상세 설계는 [ERD](https://www.erdcloud.com/d/3aFinoeCdppLGQ8Na)를 참고합니다. 실행 환경은 현재 엔티티·쿼리와 일치하는 테이블, 제약조건, 시퀀스, 함수가 필요합니다.

<br>

## 6. API 명세 요약

### 메뉴·추천·공통코드

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/api/menus` | 메뉴 목록·검색·카테고리 필터 |
| GET | `/api/menus/{menuId}` | 레시피 상세 |
| GET | `/api/codes/category` | 카테고리 코드 |
| GET | `/api/codes/tag` | 음식 특징 코드 |
| GET | `/api/menufilter` | 태그를 포함한 메뉴 목록 |
| POST | `/api/menufilter/recommend` | 조건에 맞는 추천 후보 |

메뉴 검색 예시:

```http
GET /api/menus?keyword=계란&category=rice_bowl
```

추천 요청 예시:

```json
{
  "category": "rice_bowl",
  "tags": ["EASY", "QUICK"]
}
```

조건 없는 추천 후보 조회:

```json
{
  "category": null,
  "tags": []
}
```

코드 값은 DB에 등록된 실제 공통코드를 사용해야 합니다.

### 기기·즐겨찾기·최근 기록

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| POST | `/api/user-devices` | 기기 등록 |
| GET | `/api/favorites?userDeviceId={id}` | 즐겨찾기 목록 |
| PUT | `/api/favorites/{menuId}?userDeviceId={id}` | 즐겨찾기 등록 |
| DELETE | `/api/favorites/{menuId}?userDeviceId={id}` | 즐겨찾기 해제 |
| GET | `/api/recent-recipes?userDeviceId={id}` | 최근 본 레시피 |
| PUT | `/api/recent-recipes/{menuId}?userDeviceId={id}` | 조회 기록 저장·갱신 |

즐겨찾기 등록·해제와 최근 조회 기록 저장은 성공 시 `204 No Content`를 반환합니다.

### 회원·인증

| 메서드 | 경로 | 기능 | 인증 방식 |
| --- | --- | --- | --- |
| POST | `/api/users/signup` | 회원가입 | 이메일 인증 증명 |
| POST | `/api/users/login` | 로그인 | 아이디·비밀번호 |
| POST | `/api/users/refresh` | 토큰 재발급 | 리프레시 토큰 |
| POST | `/api/users/logout` | 로그아웃 | 리프레시 토큰 |
| GET | `/api/users/me` | 내 정보 조회 | 액세스 토큰 |
| POST | `/api/email-verifications/send` | 인증번호 발송 | 이메일·목적 |
| POST | `/api/email-verifications/confirm` | 인증번호 확인 | 인증 요청 ID·번호 |
| POST | `/api/users/find-id` | 아이디 찾기 | 이메일 인증 증명 |
| POST | `/api/users/reset-password` | 비밀번호 재설정 | 이메일 인증 증명·회원 정보 |

회원가입 성공 시 `201 Created`, 로그아웃 성공 시 `204 No Content`를 반환합니다.

인증된 회원 조회:

```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

### 접근 정책

- 세션을 생성하지 않는 `STATELESS` 인증을 사용합니다.
- 메뉴·추천·기기별 기록 API는 비회원에게 허용합니다.
- 내 정보 조회는 인증된 회원에게 허용합니다.
- 명시적으로 허용하지 않은 요청은 기본적으로 거절합니다.

<br>

## 7. 프로젝트 구조

기준 패키지: `com.zipbob.zipbob_backend`

| 경로 | 주요 역할 |
| --- | --- |
| `config/` | Spring Security, CORS, BCrypt 설정 |
| `controller/` | 메뉴·추천·기기·회원·이메일 인증 API |
| `service/` | 비즈니스 로직과 트랜잭션 처리 |
| `repository/` | 조회·저장 쿼리와 잠금 설정 |
| `entity/` | 테이블 매핑과 상태 변경 |
| `dto/` | 요청·응답 DTO와 조회 Projection |
| `security/` | JWT 생성·검증과 인증 필터 |
| `src/main/resources/static/images/menus/` | 메뉴 이미지 |
| `src/main/resources/application.properties.example` | DB 연결 설정 예시 |
| `src/test/` | 애플리케이션 컨텍스트 로딩 테스트 |

<br>

## 8. 로컬 실행 방법

### 준비 사항

- JDK 21
- PostgreSQL
- 현재 코드와 일치하는 `zipbob_app` 스키마와 테이블
- `zipbob_app.codename_f()` 함수
- 메뉴·태그·공통코드 등 초기 데이터
- SMTP 발신 계정과 접속 정보

현재 저장소는 DB 자동 생성 대신 스키마 검증을 사용합니다. 빈 데이터베이스에서 실행하려면 테이블과 관련 DB 객체를 먼저 준비해야 합니다.

### 저장소 받기

```bash
git clone https://github.com/hyeseon1020/zipbob_back.git
cd zipbob_back
```

### 로컬 설정 파일 만들기

`src/main/resources/application.properties.example`을 복사해 같은 위치에 `application.properties`를 만듭니다.

현재 예시 파일에는 DB 설정만 포함되어 있으므로 JWT·메일 설정을 추가해야 합니다.

아래는 로컬 구성 예시입니다. 토큰 유효기간과 SMTP 값은 실행 환경에 맞게 설정합니다.

```properties
spring.application.name=zipbob_backend
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/${DB_NAME}?currentSchema=zipbob_app
spring.datasource.username=${DB_USER_NAME}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.default_schema=zipbob_app

# 로컬 SQL 확인
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=${JWT_SECRET}
jwt.issuer=zipbob
jwt.access-token-expiration-seconds=900
jwt.refresh-token-expiration-days=14

# SMTP: 아래는 STARTTLS를 사용하는 구성 예시
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

- `JWT_SECRET`은 **32바이트 난수의 Base64 인코딩 값**을 사용합니다.
- JWT·DB·SMTP 값은 실행 환경변수에 등록합니다.
- SMTP 암호화 방식과 포트는 사용하는 메일 서비스 설정에 맞춥니다.
- `application.properties`는 `.gitignore`에 포함되어 있습니다.
- 일부 Native SQL은 스키마 이름을 생략하므로 연결의 기본 스키마도 맞춰야 합니다.

### 실행

macOS·Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

### Flutter Web 연동

현재 `WebConfig`는 다음 출처를 허용합니다.

```text
http://localhost:3000
```

프론트엔드를 같은 포트로 실행할 수 있습니다.

```bash
flutter run -d chrome --web-port=3000
```

다른 포트를 사용하면 `WebConfig`의 허용 출처를 실제 프론트엔드 주소에 맞게 수정합니다.

Android 에뮬레이터에서는 개발 PC의 서버에 다음 주소로 접근합니다.

```text
http://10.0.2.2:8080
```

<br>

## 9. 빌드와 테스트

### 빌드

macOS·Linux:

```bash
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

### 테스트

macOS·Linux:

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

### 현재 테스트 범위

현재 자동 테스트는 `@SpringBootTest` 기반 컨텍스트 로딩 테스트입니다.

실행 시 DB·JWT·메일 관련 설정이 필요하며, 비즈니스 규칙과 동시 요청을 검증하는 별도 테스트는 보완 예정입니다.

| 보완 대상 | 검증할 내용 |
| --- | --- |
| 회원가입 | 중복 가입, 인증 없는 가입, 인증 대상 불일치 |
| 이메일 인증 | 만료·오입력 횟수·목적 불일치·재사용 |
| 토큰 재발급 | 만료 토큰, 교체된 토큰 재사용, 동시 요청 |
| 최근 조회 기록 | 재조회 순서 갱신, 최신 5개 유지, 동시 저장 |
| 비밀번호 재설정 | 회원 정보 일치, 인증 소비, 기존 리프레시 토큰 삭제 |

<br>

## 10. 현재 범위와 개선 계획

| 항목 | 현재 상태 | 개선 계획 |
| --- | --- | --- |
| 내 정보 | 인증 후 조회 | 회원 정보 수정 |
| 기기별 기록 | 기기 ID 기준 저장 | 기기 소유 검증·계정 동기화 검토 |
| DB 초기화 | 사전 생성된 DB에 의존 | 마이그레이션·초기 데이터 관리 |
| 실행 설정 | DB 예시 파일 제공 | JWT·메일 설정 예시 통합 |
| 자동 테스트 | 컨텍스트 로딩 테스트 | 서비스·통합·동시성 테스트 |
| 인증 데이터 | 만료 검증·명시적 삭제 | 만료 데이터 정리 작업 |
| 배포 | 로컬 개발 기준 | 배포 환경 구성 및 앱 출시 연동 |

<br>

## 관련 문서

- [Flutter 프론트엔드](https://github.com/WONDongin/zipbob_app)
- [Spring Boot 백엔드](https://github.com/hyeseon1020/zipbob_back)
- [ERD](https://www.erdcloud.com/d/3aFinoeCdppLGQ8Na)
- [화면 설계서](https://docs.google.com/presentation/d/1v0SMmEkxajPqGHR3RwibuGB7U1ARyDGVYvMwpgL6tSM/edit)