(function() {
    const dialog = document.getElementById("code-edit-dialog");
    const openCategoryBtn = document.getElementById("open-category-create");
    const openTagBtn = document.getElementById("open-tag-create");
    const closeBtn = document.getElementById("close-code-dialog");
    const cancelBtn = document.getElementById("cancel-code-dialog");
    const editButtons = document.querySelectorAll(".code-edit-button");

    // 1. 카테고리 추가 버튼 클릭 시
    if (openCategoryBtn) {
        openCategoryBtn.addEventListener("click", () => {
            document.getElementById("code-dialog-title").innerText = "카테고리 등록";
            document.getElementById("dialog-code-id").value = "";
            document.getElementById("dialog-code-id").readOnly = false;
            document.getElementById("dialog-code-parent").value = "category_code"; // DB parent 값에 맞춤
            document.getElementById("dialog-code-name").value = "";
            document.getElementById("dialog-use-yn").checked = true;
            dialog.showModal();
        });
    }

    // 2. 음식 특징 추가 버튼 클릭 시
    if (openTagBtn) {
        openTagBtn.addEventListener("click", () => {
            document.getElementById("code-dialog-title").innerText = "음식 특징 등록";
            document.getElementById("dialog-code-id").value = "";
            document.getElementById("dialog-code-id").readOnly = false;
            document.getElementById("dialog-code-parent").value = "tag_code"; // DB parent 값에 맞춤
            document.getElementById("dialog-code-name").value = "";
            document.getElementById("dialog-use-yn").checked = true;
            dialog.showModal();
        });
    }

    // 3. 각 행의 '수정' 버튼 클릭 시
    editButtons.forEach(button => {
        button.addEventListener("click", (e) => {
            const codeId = e.target.getAttribute("data-code-id");
            const codeName = e.target.getAttribute("data-code-name");
            const codeParent = e.target.getAttribute("data-code-parent");
            const useYn = e.target.getAttribute("data-use-yn");

            document.getElementById("code-dialog-title").innerText = "공통코드 수정";
            document.getElementById("dialog-code-id").value = codeId;
            document.getElementById("dialog-code-id").readOnly = true;
            document.getElementById("dialog-code-parent").value = codeParent;
            document.getElementById("dialog-code-name").value = codeName;
            document.getElementById("dialog-use-yn").checked = (useYn === "Y");

            dialog.showModal();
        });
    });

    // 4. 팝업 닫기 / 취소 버튼
    if (closeBtn) closeBtn.addEventListener("click", () => dialog.close());
    if (cancelBtn) cancelBtn.addEventListener("click", () => dialog.close());
})();