package com.zipbob.zipbob_backend.dto;

import java.time.LocalDateTime;

// 최근 본 레시피 목록의 SQL 조회 결과를 받는 인터페이스
public interface RecentRecipeListProjection {

    Integer getMenuId();
    String getMenuName();
    String getCategory();
    String getCategoryName();
    String getDescription();
    String getImageUrl();
    LocalDateTime getViewedDt();
}
/*
| SQL 별칭 | getter | 반환 내용 |
| `menuId` | `getMenuId()` | 메뉴 ID |
| `menuName` | `getMenuName()` | 메뉴명 |
| `category` | `getCategory()` | 카테고리 코드 |
| `categoryName` | `getCategoryName()` | 카테고리명 |
| `description` | `getDescription()` | 한 줄 설명 |
| `imageUrl` | `getImageUrl()` | 대표 이미지 경로 |
| `viewedDt` | `getViewedDt()` | 마지막 조회 시간 |
*/