package com.zipbob.zipbob_backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
// 신규 메뉴 등록 요청
public class MenuCreateRequestDto {

    @NotBlank(message = "메뉴명을 입력해주세요.")
    @Size(max = 50, message = "메뉴명은 50자 이하로 입력해주세요.")
    private String menuName;

    @NotBlank(message = "카테고리를 선택해주세요.")
    @Size(max = 50)
    private String category;

    // 음식 특징: 최소 1개 필수, 선택 개수 상한 없음
    @NotEmpty(message = "음식 특징을 최소 1개 선택해주세요.")
    private List<
            @NotBlank(message = "음식 특징 코드가 비어 있습니다.")
            @Size(max = 50, message = "음식 특징 코드는 50자 이하여야 합니다.")
                    String
            > tagIds = new ArrayList<>();

    @NotNull(message = "기준 인분을 입력해주세요.")
    @Min(value = 1, message = "기준 인분은 1 이상이어야 합니다.")
    @Max(value = 100, message = "기준 인분은 100 이하여야 합니다.")
    private Integer servings;

    @NotNull(message = "조리 시간을 입력해주세요.")
    @Min(value = 0, message = "조리 시간은 0 이상이어야 합니다.")
    @Max(value = 99, message = "조리 시간은 99 이하여야 합니다.")
    private Integer cookTime;

    @NotNull(message = "조리 분을 입력해주세요.")
    @Min(value = 0, message = "조리 분은 0 이상이어야 합니다.")
    @Max(value = 59, message = "조리 분은 0~59로 입력해주세요.")
    private Integer cookMinute;

    @NotBlank(message = "재료를 입력해주세요.")
    @Size(max = 20000, message = "재료는 20,000자 이하로 입력해주세요.")
    private String ingredients;

    @NotBlank(message = "조리 순서를 입력해주세요.")
    @Size(max = 50000, message = "조리 순서는 50,000자 이하로 입력해주세요.")
    private String steps;

    @NotBlank(message = "난이도를 선택해주세요.")
    @Size(max = 50)
    private String difficultyLvl;

    // 이미지 필수 여부·용량·형식은 서비스에서 검증
    private MultipartFile image;
}