package com.zipbob.zipbob_backend.dto;
// SQL 조회 결과를 getter로 읽을 수 있게 정의
public interface MenuTagListProjection {
    Integer getMenuId();
    String getMenuName();
    String getCategory();
    String getCategoryName();
    String getDescription();
    String getTagSeq();
    String getCodeId();
    String getTagName();
}
/*
| SQL 결과 별칭 | 읽는 메서드 | 예시 |
| `menuId` | `getMenuId()` | `1` |
| `menuName` | `getMenuName()` | `간장계란밥` |
| `category` | `getCategory()` | `rice_bowl` |
| `categoryName` | `getCategoryName()` | `덮밥·비빔밥` |
| `description` | `getDescription()` | 메뉴 설명 |
| `tagSeq` | `getTagSeq()` | `1` |
| `tagSeq` | `getCodeId()` | `qick_10min` |
| `tagSeq` | `getTagName()` | `10분 완성` |
*/