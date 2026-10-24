(() => {
    const dialog = document.getElementById('menu-create-dialog');
    const form = document.getElementById('menu-create-form');
    const openButton = document.getElementById('open-menu-create');
    const closeButton = document.getElementById('close-menu-create');
    const saveButton = document.getElementById('save-menu-create');
    const message = document.getElementById('menu-create-message');

    const imageInput = document.getElementById('create-image');
    const preview = document.getElementById('create-image-preview');

    const category = document.getElementById('create-category');
    const difficulty = document.getElementById('create-difficulty');
    const tagOptions = document.getElementById('create-tag-options');

    if (!dialog || !form || !openButton || !closeButton
        || !saveButton || !message || !imageInput || !preview
        || !category || !difficulty || !tagOptions) {
        return;
    }

    let previewUrl = null;
    let saving = false;
    let loading = false;
    let optionsReady = false;

    function updateButtons() {
        saveButton.disabled = saving || loading || !optionsReady;
        closeButton.disabled = saving || loading;
        saveButton.textContent = saving ? '등록 중...' : '등록';
    }

    function clearPreview() {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }

        preview.removeAttribute('src');
        preview.hidden = true;
    }

    function setOptions(select, options) {
        select.replaceChildren(new Option('선택해주세요', ''));

        for (const item of options) {
            select.add(new Option(item.codeName, item.codeId));
        }
    }

    function setTagOptions(options) {
        tagOptions.replaceChildren();

        if (options.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'menu-tag-empty';
            empty.textContent = '등록된 음식 특징이 없습니다.';

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

    async function readResponse(response) {
        const contentType = response.headers.get('content-type') || '';

        if (!contentType.includes('application/json')) {
            throw new Error(
                '요청을 처리하지 못했습니다. 로그인 상태와 첨부 용량을 확인해주세요.'
            );
        }

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || '요청 처리 중 오류가 발생했습니다.'
            );
        }

        return result;
    }

    // 팝업 열기 및 선택 항목 조회
    openButton.addEventListener('click', async () => {
        if (loading || saving || dialog.open) {
            return;
        }

        form.reset();
        clearPreview();

        setOptions(category, []);
        setOptions(difficulty, []);
        tagOptions.replaceChildren();

        optionsReady = false;
        loading = true;

        message.textContent = '입력 항목을 불러오는 중입니다.';
        updateButtons();

        dialog.showModal();

        try {
            const response = await fetch(form.dataset.optionsUrl, {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: {
                    Accept: 'application/json'
                }
            });

            const options = await readResponse(response);

            const categories = options.categories || [];
            const difficulties = options.difficulties || [];
            const tags = options.tags || [];

            setOptions(category, categories);
            setOptions(difficulty, difficulties);
            setTagOptions(tags);

            if (categories.length === 0) {
                throw new Error('등록 가능한 카테고리가 없습니다.');
            }

            if (difficulties.length === 0) {
                throw new Error('등록 가능한 난이도가 없습니다.');
            }

            if (tags.length === 0) {
                throw new Error(
                    '등록 가능한 음식 특징이 없습니다. 공통코드를 확인해주세요.'
                );
            }

            optionsReady = true;
            message.textContent = '';

        } catch (error) {
            message.textContent = error.message;
        } finally {
            loading = false;
            updateButtons();
        }
    });

    // 팝업 닫기
    closeButton.addEventListener('click', () => {
        if (!saving && !loading) {
            dialog.close();
        }
    });

    // 저장·조회 중에는 ESC로 닫지 않음
    dialog.addEventListener('cancel', (event) => {
        if (saving || loading) {
            event.preventDefault();
        }
    });

    dialog.addEventListener('close', clearPreview);

    // 이미지 미리보기
    imageInput.addEventListener('change', () => {
        clearPreview();
        message.textContent = '';

        const file = imageInput.files[0];

        if (!file) {
            return;
        }

        if (!['image/png', 'image/jpeg'].includes(file.type)) {
            message.textContent = 'JPG 또는 PNG 이미지를 선택해주세요.';
            imageInput.value = '';
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            message.textContent = '이미지는 10MB 이하로 선택해주세요.';
            imageInput.value = '';
            return;
        }

        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.hidden = false;
    });

    // 신규 메뉴 등록
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (saving || loading || !optionsReady) {
            return;
        }

        message.textContent = '';

        if (!form.reportValidity()) {
            return;
        }

        // 음식 특징 최소 1개 선택
        const selectedTags = Array.from(
            tagOptions.querySelectorAll(
                'input[name="tagIds"]:checked'
            )
        );

        if (selectedTags.length === 0) {
            message.textContent = '음식 특징을 최소 1개 선택해주세요.';

            tagOptions.querySelector(
                'input[name="tagIds"]'
            )?.focus();

            return;
        }

        // 선택 개수 상한은 없고, 저장 문자열은 최대 100자
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
            message.textContent = '조리 시간은 1분 이상 입력해주세요.';

            form.elements.namedItem('cookMinute').focus();
            return;
        }

        saving = true;
        updateButtons();

        try {
            // 체크된 tagIds와 이미지가 함께 포함됨
            const formData = new FormData(form);

            const response = await fetch(form.dataset.saveUrl, {
                method: 'POST',
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

            // 등록한 메뉴명으로 목록 조회
            const listUrl = new URL(
                form.dataset.listUrl,
                window.location.origin
            );

            listUrl.searchParams.set(
                'keyword',
                form.elements.namedItem('menuName').value.trim()
            );

            listUrl.searchParams.set('page', '1');

            window.location.assign(listUrl.toString());

        } catch (error) {
            message.textContent = error.message;
        } finally {
            saving = false;
            updateButtons();
        }
    });
})();