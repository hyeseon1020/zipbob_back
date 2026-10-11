package com.zipbob.zipbob_backend.dto;
// SQL 조회 결과를 getter로 읽을 수 있게 정의
public interface MenuListProjection {

    Integer getMenuId();
    String getMenuName();
    String getCategory();
    String getCategoryName();
    String getDescription();
    String getImageUrl();
}
/*
| SQL 결과 별칭 | 읽는 메서드 | 예시 |
| `menuId` | `getMenuId()` | `1` |
| `menuName` | `getMenuName()` | `간장계란밥` |
| `category` | `getCategory()` | `rice_bowl` |
| `categoryName` | `getCategoryName()` | `덮밥·비빔밥` |
| `description` | `getDescription()` | 메뉴 설명 |
| `imageUrl` | `getImageUrl()` | 이미지 경로 |
*/