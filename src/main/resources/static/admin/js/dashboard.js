/*
인기 메뉴 이미지 로딩 실패 처리
 - 이미지 로딩에 실패하면 img 요소를 숨김
 - 이미지 뒤에 배치한 기본 밥 아이콘 표시
 - 스크립트 실행 전에 로딩에 실패한 이미지도 확인
 */
document.querySelectorAll(".stats-food-image").forEach((image) => {
    const showPlaceholder = () => {
        image.hidden = true;
    };

    image.addEventListener("error", showPlaceholder);

    if (image.complete && image.naturalWidth === 0) {
        showPlaceholder();
    }
});