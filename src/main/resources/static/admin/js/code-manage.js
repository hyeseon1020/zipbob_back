(function () {
    const dialog = document.getElementById("code-edit-dialog");
    const form = document.getElementById("code-edit-form");

    if (!dialog || !form) return;

    const openCategoryBtn = document.getElementById("open-category-create");
    const openTagBtn = document.getElementById("open-tag-create");
    const closeBtn = document.getElementById("close-code-dialog");
    const cancelBtn = document.getElementById("cancel-code-dialog");
    const editButtons = document.querySelectorAll(".code-edit-button");

    const title = document.getElementById("code-dialog-title");
    const codeIdInput = document.getElementById("dialog-code-id");
    const codeParentInput = document.getElementById("dialog-code-parent");
    const codeNameInput = document.getElementById("dialog-code-name");
    const useYnInput = document.getElementById("dialog-use-yn");
    const saveBtn = form.querySelector('button[type="submit"]');

    let saving = false;
    let currentMode = ""; //현재 모드를 저장할 변수 ("create" 또는 "update")

    // 1. 등록 팝업 열기
    function openCreateDialog(dialogTitle, codeParent) {
        form.reset();

        currentMode = "create"; //등록 모드로 설정
        title.textContent = dialogTitle;
        codeIdInput.value = "";
        codeIdInput.readOnly = false;
        codeIdInput.required = true;
        codeParentInput.value = codeParent;
        codeNameInput.value = "";
        useYnInput.checked = true;

        dialog.showModal();
    }

    // 2. 카테고리 추가
    openCategoryBtn?.addEventListener("click", () => {
        openCreateDialog("카테고리 등록", "category_code");
    });

    // 3. 음식 특징 추가
    openTagBtn?.addEventListener("click", () => {
        openCreateDialog("음식 특징 등록", "tag_code");
    });

    // 4. 수정 팝업 열기
    editButtons.forEach(button => {
        button.addEventListener("click", () => {
            form.reset();

            currentMode = "update"; //수정 모드로 설정
            title.textContent = "공통코드 수정";
            codeIdInput.value = button.dataset.codeId;
            codeIdInput.readOnly = true;
            codeParentInput.value = button.dataset.codeParent;
            codeNameInput.value = button.dataset.codeName;
            useYnInput.checked = button.dataset.useYn === "Y";

            dialog.showModal();
        });
    });

    // 5. 팝업 닫기
    function closeDialog() {
        if (saving) return;

        dialog.close();
    }

    closeBtn?.addEventListener("click", closeDialog);
    cancelBtn?.addEventListener("click", closeDialog);

    // 저장 중에는 ESC로 팝업을 닫지 않도록 처리
    dialog.addEventListener("cancel", event => {
        if (saving) {
            event.preventDefault();
        }
    });

    // 6. 공통코드 등록·수정 저장
    form.addEventListener("submit", async event => {
        // 브라우저의 기본 폼 제출 방지
        event.preventDefault();

        if (saving) return;

        const requestData = {
            codeId: codeIdInput.value.trim(),
            codeParent: codeParentInput.value.trim(),
            codeName: codeNameInput.value.trim(),
            // 체크하면 Y, 체크를 해제하면 N
            useYn: useYnInput.checked ? "Y" : "N",
            mode: currentMode // ★ 백엔드로 "create" 또는 "update" 전송
        };

        if (!requestData.codeId) {
            alert("코드 ID를 입력해주세요.");
            codeIdInput.focus();
            return;
        }

        if (!requestData.codeName) {
            alert("코드 이름을 입력해주세요.");
            codeNameInput.focus();
            return;
        }

        // layout.html에 추가한 CSRF 메타 태그 읽기
        const csrfToken = document.querySelector(
            'meta[name="_csrf"]'
        )?.content;

        const csrfHeader = document.querySelector(
            'meta[name="_csrf_header"]'
        )?.content;

        if (!csrfToken || !csrfHeader) {
            alert(
                "보안 토큰을 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해주세요."
            );
            return;
        }

        const originalButtonText = saveBtn.textContent;

        saving = true;
        saveBtn.disabled = true;
        saveBtn.textContent = "저장 중...";

        let saved = false;

        try {
            // 현재 /admin/codes 주소를 기준으로 저장 API 주소 생성
            // 서버에 context path가 있어도 유지
            const pageUrl = new URL(window.location.href);
            pageUrl.pathname = pageUrl.pathname.replace(/\/+$/, "");

            const saveUrl = new URL("api/codes/save", pageUrl);

            const response = await fetch(saveUrl, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(requestData)
            });

            // 로그인 화면으로 이동된 응답을 저장 성공으로 처리하지 않음
            if (response.redirected || response.status === 401) {
                throw new Error(
                    "로그인 상태를 확인해주세요. 페이지를 새로고침한 뒤 다시 시도해주세요."
                );
            }

            if (response.status === 403) {
                throw new Error(
                    "저장 권한 또는 보안 토큰을 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해주세요."
                );
            }

            if (!response.ok) {
                // 서버에서 보낸 에러 메시지(중복 등)를 읽어서 에러로 던짐
                const errorMessage = await response.text();
                throw new Error(
                    errorMessage || `저장에 실패했습니다. (HTTP ${response.status})`
                );
            }

            // 현재 API는 JSON이 아닌 문자열 "success"를 반환
            const result = await response.text();

            if (result.trim() !== "success") {
                throw new Error("저장 결과를 확인할 수 없습니다.");
            }

            saved = true;
        } catch (error) {
            console.error("공통코드 저장 실패:", error);

            alert(
                error instanceof Error
                    ? error.message
                    : "저장 중 오류가 발생했습니다."
            );
        } finally {
            saving = false;
            saveBtn.disabled = false;
            saveBtn.textContent = originalButtonText;
        }

        if (saved) {
            alert("저장되었습니다.");
            dialog.close();

            // DB에 반영된 상태로 목록 다시 조회
            window.location.reload();
        }
    });
})();