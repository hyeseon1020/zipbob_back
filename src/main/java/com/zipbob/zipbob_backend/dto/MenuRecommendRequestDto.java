package com.zipbob.zipbob_backend.dto;

import java.util.List;

/*
 * Flutter가 보내는 JSON을 받을 DTO
 * 카테고리, 음식특징 선택된 값 받아오기
 */
public class MenuRecommendRequestDto {
    private String category;
    private List<String> tags;

    public String getCategory(){
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags(){
        return tags;
    }

    public void SetTags(List<String> tags){
        this.tags = tags;
    }
}
