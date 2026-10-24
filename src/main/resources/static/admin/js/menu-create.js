(() => {
    const dialog = document.getElementById('menu-create-dialog');
    const form = document.getElementById('menu-create-form');

    const openButton = document.getElementById('open-menu-create');
    const closeButton = document.getElementById('close-menu-create');
    const saveButton = document.getElementById('save-menu-create');

    const title = document.getElementById('menu-create-title');
    const message = document.getElementById('menu-create-message');

    const imageInput = document.getElementById('create-image');
    const preview = document.getElementById('create-image-preview');

    const category = document.getElementById('create-category');
    const difficulty = document.getElementById('create-difficulty');
    const tagOptions = document.getElementById('create-tag-options');

    if (!dialog || !form || !openButton || !closeButton
        || !saveButton || !title || !message
        || !imageInput || !preview
        || !category || !difficulty || !tagOptions) {
        return;
    }

    // null이면 신규 등록, 메뉴 ID가 있으면 수정
    let editingMenuId = null;

    // DB에 저장된 기존 이미지 주소
    let existingImageUrl = null;

    // 새로 선택한 파일의 임시 미리보기 주소
    let previewUrl = null;

    let saving = false;
    let loading = false;
    let optionsReady = false;

    // 조회·저장 상태와 등록·수정 모드에 따라 버튼 변경
    function updateButtons() {
        saveButton.disabled =
            saving || loading || !optionsReady;

        closeButton.disabled = saving || loading;

        saveButton.textContent = saving
            ? (editingMenuId ? '저장 중...' : '등록 중...')
            : (editingMenuId ? '저장' : '등록');
    }

    // 미리보기 초기화 및 임시 URL 해제
    function clearPreview() {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }

        preview.removeAttribute('src');
        preview.hidden = true;
    }

    // 기존 이미지 표시
    function showExistingPreview() {
        if (existingImageUrl) {
            preview.src = existingImageUrl;
            preview.hidden = false;
        }
    }

    // 카테고리·난이도 선택 항목 생성
    function setOptions(select, options) {
        select.replaceChildren(
            new Option('선택해주세요', '')
        );

        for (const item of options) {
            select.add(
                new Option(item.codeName, item.codeId)
            );
        }
    }

    // 음식 특징 체크박스 생성
    function setTagOptions(options) {
        tagOptions.replaceChildren();

        if (options.length === 0) {
            const empty = document.createElement('p');

            empty.className = 'menu-tag-empty';
            empty.textContent =
                '등록된 음식 특징이 없습니다.';

            tagOptions.append(empty);
            return;
        }

        for (const item of options) {
            const label = document.createElement('label');
            label.className = 'menu-tag-item';

            const checkbox = document.createElement('input');

            checkbox.type = 'checkbox';
            checkbox.name = 'tagIds';
            checkbox.value = item.codeId;
            checkbox.dataset.tagName = item.codeName;

            const text = document.createElement('span');
            text.textContent = item.codeName;

            label.append(checkbox, text);
            tagOptions.append(label);
        }
    }

    // JSON 응답 확인 및 오류 메시지 처리
    async function readResponse(response) {
        const contentType =
            response.headers.get('content-type') || '';

        if (!contentType.includes('application/json')) {
            throw new Error(
                '요청을 처리하지 못했습니다. '
                + '로그인 상태와 첨부 용량을 확인해주세요.'
            );
        }

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message
                || '요청 처리 중 오류가 발생했습니다.'
            );
        }

        return result;
    }

    // 등록·수정 공용 팝업 열기
    async function openMenu(menuId = null) {
        if (loading || saving || dialog.open) {
            return;
        }

        editingMenuId = menuId;
        existingImageUrl = null;

        title.textContent = menuId
            ? '메뉴 수정'
            : '신규 메뉴 등록';

        // 등록에서는 이미지 필수, 수정에서는 선택
        imageInput.required = !menuId;

        form.reset();
        clearPreview();

        setOptions(category, []);
        setOptions(difficulty, []);
        tagOptions.replaceChildren();

        optionsReady = false;
        loading = true;

        message.textContent =
            '입력 항목을 불러오는 중입니다.';

        updateButtons();
        dialog.showModal();

        try {
            // 선택 항목을 먼저 조회
            const response = await fetch(
                form.dataset.optionsUrl,
                {
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: {
                        Accept: 'application/json'
                    }
                }
            );

            const options = await readResponse(response);

            const categories = options.categories || [];
            const difficulties = options.difficulties || [];
            const tags = options.tags || [];

            setOptions(category, categories);
            setOptions(difficulty, difficulties);
            setTagOptions(tags);

            if (categories.length === 0) {
                throw new Error(
                    '등록 가능한 카테고리가 없습니다.'
                );
            }

            if (difficulties.length === 0) {
                throw new Error(
                    '등록 가능한 난이도가 없습니다.'
                );
            }

            if (tags.length === 0) {
                throw new Error(
                    '등록 가능한 음식 특징이 없습니다. '
                    + '공통코드를 확인해주세요.'
                );
            }

            message.textContent = '';

            // 수정 모드일 때 기존 메뉴 상세 조회
            if (menuId) {
                const detailResponse = await fetch(
                    `${form.dataset.saveUrl}/${encodeURIComponent(menuId)}`,
                    {
                        credentials: 'same-origin',
                        cache: 'no-store',
                        headers: {
                            Accept: 'application/json'
                        }
                    }
                );

                const detail =
                    await readResponse(detailResponse);

                // 일반 입력값과 선택값 복원
                const fieldNames = [
                    'menuName',
                    'category',
                    'difficultyLvl',
                    'servings',
                    'cookTime',
                    'cookMinute',
                    'ingredients',
                    'steps'
                ];

                for (const name of fieldNames) {
                    const field =
                        form.elements.namedItem(name);

                    const value = detail[name] ?? '';

                    // 현재 선택 목록에 없는 기존 코드도 표시
                    if (
                        field.tagName === 'SELECT'
                        && value
                        && !Array.from(field.options).some(
                            option => option.value === value
                        )
                    ) {
                        field.add(
                            new Option(
                                `${value} (기존 값 · 변경 필요)`,
                                value
                            )
                        );

                        message.textContent =
                            '현재 사용하지 않는 코드가 있습니다. '
                            + '사용 가능한 항목으로 변경해주세요.';
                    }

                    field.value = value;
                }

                // description에 저장된 음식 특징 이름 분리
                const storedNames = (
                    detail.description || ''
                )
                    .split(',')
                    .map(value => value.trim())
                    .filter(Boolean);

                const availableNames = new Set();

                // 저장된 이름과 일치하는 음식 특징 체크
                for (
                    const checkbox of tagOptions.querySelectorAll(
                        'input[name="tagIds"]'
                    )
                ) {
                    availableNames.add(
                        checkbox.dataset.tagName
                    );

                    checkbox.checked = storedNames.includes(
                        checkbox.dataset.tagName
                    );
                }

                // 현재 코드와 일치하지 않는 기존 특징 표시
                const unmatchedNames = storedNames.filter(
                    name => !availableNames.has(name)
                );

                for (const name of unmatchedNames) {
                    const label =
                        document.createElement('label');

                    label.className = 'menu-tag-item';

                    const checkbox =
                        document.createElement('input');

                    checkbox.type = 'checkbox';
                    checkbox.name = 'legacyTagNames';
                    checkbox.checked = true;

                    const text =
                        document.createElement('span');

                    text.textContent =
                        `${name} (기존 값 · 해제 후 다시 선택)`;

                    label.append(checkbox, text);
                    tagOptions.append(label);

                    message.textContent =
                        '현재 코드와 일치하지 않는 음식 특징이 있습니다. '
                        + '기존 항목을 해제하고 다시 선택해주세요.';
                }

                existingImageUrl = detail.imageUrl;
                showExistingPreview();
            }

            optionsReady = true;

        } catch (error) {
            message.textContent = error.message;

        } finally {
            loading = false;
            updateButtons();
        }
    }

    // 신규 등록 버튼
    openButton.addEventListener('click', () => {
        openMenu();
    });

    // 메뉴별 수정 버튼
    document.querySelectorAll('.menu-edit-button')
        .forEach(button => {
            button.addEventListener('click', () => {
                openMenu(button.dataset.menuId);
            });
        });

    // 팝업 닫기
    closeButton.addEventListener('click', () => {
        if (!saving && !loading) {
            dialog.close();
        }
    });

    // 조회·저장 중 ESC로 닫지 않도록 처리
    dialog.addEventListener('cancel', event => {
        if (saving || loading) {
            event.preventDefault();
        }
    });

    dialog.addEventListener('close', clearPreview);

    // 새 이미지 선택 및 미리보기
    imageInput.addEventListener('change', () => {
        clearPreview();
        message.textContent = '';

        const file = imageInput.files[0];

        if (!file) {
            showExistingPreview();
            return;
        }

        if (
            !['image/png', 'image/jpeg'].includes(file.type)
        ) {
            message.textContent =
                'JPG 또는 PNG 이미지를 선택해주세요.';

            imageInput.value = '';
            showExistingPreview();
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            message.textContent =
                '이미지는 10MB 이하로 선택해주세요.';

            imageInput.value = '';
            showExistingPreview();
            return;
        }

        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.hidden = false;
    });

    // 신규 등록·기존 메뉴 수정
    form.addEventListener('submit', async event => {
        event.preventDefault();

        if (saving || loading || !optionsReady) {
            return;
        }

        message.textContent = '';

        // required, min, max 등 HTML 입력 조건 검사
        if (!form.reportValidity()) {
            return;
        }

        // 현재 코드와 일치하지 않는 기존 특징은 재선택 필요
        if (
            tagOptions.querySelector(
                'input[name="legacyTagNames"]:checked'
            )
        ) {
            message.textContent =
                '현재 코드와 일치하지 않는 음식 특징을 '
                + '해제하고 다시 선택해주세요.';

            return;
        }

        const selectedTags = Array.from(
            tagOptions.querySelectorAll(
                'input[name="tagIds"]:checked'
            )
        );

        if (selectedTags.length === 0) {
            message.textContent =
                '음식 특징을 최소 1개 선택해주세요.';

            tagOptions.querySelector(
                'input[name="tagIds"]'
            )?.focus();

            return;
        }

        // DB description 길이에 맞춰 확인
        const description = selectedTags
            .map(checkbox => checkbox.dataset.tagName)
            .join(', ');

        if (Array.from(description).length > 100) {
            message.textContent =
                '선택한 음식 특징의 전체 이름은 '
                + '쉼표와 공백을 포함해 100자 이하여야 합니다.';

            selectedTags[0].focus();
            return;
        }

        const cookTime = Number(
            form.elements.namedItem('cookTime').value
        );

        const cookMinute = Number(
            form.elements.namedItem('cookMinute').value
        );

        if (cookTime === 0 && cookMinute === 0) {
            message.textContent =
                '조리 시간은 1분 이상 입력해주세요.';

            form.elements.namedItem('cookMinute').focus();
            return;
        }

        saving = true;
        updateButtons();

        try {
            // 입력값·선택한 음식 특징·이미지 포함
            const formData = new FormData(form);

            // 이미지 미선택 시 파일 항목 제외
            if (!imageInput.files.length) {
                formData.delete('image');
            }

            const saveUrl = editingMenuId
                ? `${form.dataset.saveUrl}/${encodeURIComponent(editingMenuId)}`
                : form.dataset.saveUrl;

            const response = await fetch(saveUrl, {
                method: editingMenuId ? 'PUT' : 'POST',
                credentials: 'same-origin',
                headers: {
                    Accept: 'application/json',
                    [form.dataset.csrfHeader]:
                        form.dataset.csrfToken
                },
                body: formData
            });

            const result = await readResponse(response);

            alert(result.message);
            dialog.close();

            // 수정은 현재 목록 조건을 유지하여 새로고침
            if (editingMenuId) {
                window.location.reload();
                return;
            }

            // 신규 등록은 등록한 메뉴명으로 검색
            const listUrl = new URL(
                form.dataset.listUrl,
                window.location.origin
            );

            listUrl.searchParams.set(
                'keyword',
                form.elements.namedItem('menuName')
                    .value.trim()
            );

            listUrl.searchParams.set('page', '1');

            window.location.assign(listUrl.toString());

        } catch (error) {
            // 실패 시 팝업과 입력 내용을 유지
            message.textContent = error.message;

        } finally {
            saving = false;
            updateButtons();
        }
    });
})();