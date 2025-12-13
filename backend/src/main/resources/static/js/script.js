/**
 * Insurance Image Generator - Client Script
 * Handles form submission, loading state, and result display
 */

// ==================== State Management ====================
let isLoading = false;
let currentUserEmail = '';  // *** 전역 사용자 이메일 상태 ***
let attachedImageFile = null; // *** 첨부된 이미지 파일 ***

/**
 * 전역으로 사용자 이메일 설정
 */
function setCurrentUserEmail(email) {
    currentUserEmail = email || '';
}

/**
 * 현재 사용자 이메일 가져오기
 */
function getCurrentUserEmail() {
    return currentUserEmail;
}

/**
 * 사용자가 로그인했는지 확인
 */
function isUserLoggedIn() {
    return currentUserEmail && currentUserEmail.trim() !== '' && currentUserEmail !== 'anonymous';
}

/**
 * 이미지 첨부 처리
 */
function handleImageAttachment(event) {
    const file = event.target.files[0];
    if (!file) return;

    // 파일 크기 확인 (10MB 제한)
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
        showAlert('파일 크기가 너무 큽니다. (최대 10MB)', false);
        document.getElementById('attachImage').value = '';
        return;
    }

    attachedImageFile = file;

    const statusDiv = document.getElementById('attachmentStatus');
    const fileNameSpan = document.getElementById('attachmentFileName');
    const imagePreview = document.getElementById('imagePreview');
    const attachBtn = document.querySelector('.btn-attach-image');

    // 이미지 미리보기 생성
    const reader = new FileReader();
    reader.onload = function(e) {
        if (imagePreview) {
            imagePreview.src = e.target.result;
        }
    };
    reader.readAsDataURL(file);

    fileNameSpan.textContent = `📎 ${file.name}`;
    statusDiv.classList.remove('hidden');

    // 업로드 버튼 텍스트 변경
    const uploadIcon = document.getElementById('uploadIcon');
    const uploadText = document.getElementById('uploadText');
    const uploadHint = document.getElementById('uploadHint');
    const uploadBtn = document.getElementById('uploadBtn');

    if (uploadIcon) {
        uploadIcon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>';
        uploadIcon.classList.remove('text-warm-400', 'group-hover:text-accent');
        uploadIcon.classList.add('text-green-500', 'group-hover:text-green-600');
    }
    if (uploadText) {
        uploadText.textContent = '업로드 성공! (클릭하여 변경)';
        uploadText.classList.remove('text-warm-600', 'group-hover:text-accent');
        uploadText.classList.add('text-green-600', 'group-hover:text-green-700');
    }
    if (uploadHint) {
        uploadHint.textContent = '다른 이미지로 변경할 수 있습니다';
    }
    if (uploadBtn) {
        uploadBtn.classList.remove('border-warm-300', 'hover:border-accent');
        uploadBtn.classList.add('border-green-300', 'bg-green-50', 'hover:border-green-400', 'hover:bg-green-100');
    }

    // 첨부 버튼 색상을 진하게 변경
    if (attachBtn) {
        attachBtn.style.background = 'linear-gradient(135deg, #3B73D4 0%, #5B93FF 100%)';
        attachBtn.style.boxShadow = '0 4px 12px rgba(59, 115, 212, 0.5)';
    }
}

/**
 * 첨부된 이미지 제거
 */
function removeAttachment() {
    attachedImageFile = null;
    document.getElementById('attachImage').value = '';
    document.getElementById('attachmentStatus').classList.add('hidden');

    // 미리보기 이미지 초기화
    const imagePreview = document.getElementById('imagePreview');
    if (imagePreview) {
        imagePreview.src = '';
    }

    // 업로드 버튼 원래 상태로 복원
    const uploadIcon = document.getElementById('uploadIcon');
    const uploadText = document.getElementById('uploadText');
    const uploadHint = document.getElementById('uploadHint');
    const uploadBtn = document.getElementById('uploadBtn');

    if (uploadIcon) {
        uploadIcon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>';
        uploadIcon.classList.remove('text-green-500', 'group-hover:text-green-600');
        uploadIcon.classList.add('text-warm-400', 'group-hover:text-accent');
    }
    if (uploadText) {
        uploadText.textContent = '이미지를 드래그하거나 클릭하여 업로드';
        uploadText.classList.remove('text-green-600', 'group-hover:text-green-700');
        uploadText.classList.add('text-warm-600', 'group-hover:text-accent');
    }
    if (uploadHint) {
        uploadHint.textContent = 'PNG, JPG, WEBP (최대 10MB)';
    }
    if (uploadBtn) {
        uploadBtn.classList.remove('border-green-300', 'bg-green-50', 'hover:border-green-400', 'hover:bg-green-100');
        uploadBtn.classList.add('border-warm-300', 'hover:border-accent');
    }

    // 첨부 버튼 색상을 원래대로 변경
    const attachBtn = document.querySelector('.btn-attach-image');
    if (attachBtn) {
        attachBtn.style.background = 'linear-gradient(135deg, #5B9CFF 0%, #7EAFFF 100%)';
        attachBtn.style.boxShadow = '';
    }
}

/**
 * 이미지를 새 창에서 열기 (브라우저 표시용 URL 사용)
 */
function openImageInNewTab() {
    const resultImage = document.getElementById('resultImage');
    if (resultImage) {
        // displayUrl이 있으면 사용, 없으면 src 사용
        const displayUrl = resultImage.getAttribute('data-display-url');
        const url = displayUrl && displayUrl.trim() !== '' ? displayUrl : resultImage.src;
        if (url) {
            window.open(url, '_blank');
        }
    }
}

/**
 * S3 키를 기반으로 새 이미지 생성 페이지로 이동
 * 이미지를 자동으로 첨부된 상태로 메인 페이지로 이동
 * @param {string} s3Key - 첨부할 이미지의 S3 키
 */
function generateWithImage(s3Key) {
    if (!s3Key) {
        showAlert('이미지 정보를 찾을 수 없습니다.');
        return;
    }

    // S3 키를 세션 스토리지에 저장하고 메인 페이지로 이동
    sessionStorage.setItem('attachImageS3Key', s3Key);
    window.location.href = '/';
}

/**
 * 페이지 로드 시 세션 스토리지에서 S3 키를 확인하고 자동 첨부
 */
function checkAndAttachImageFromSession() {
    const s3Key = sessionStorage.getItem('attachImageS3Key');
    if (!s3Key) return;

    // 세션 스토리지 클리어
    sessionStorage.removeItem('attachImageS3Key');

    // 서버의 /download/ 엔드포인트를 통해 이미지 fetch (CORS 문제 회피)
    const downloadUrl = '/download/' + encodeURIComponent(s3Key);

    fetch(downloadUrl)
        .then(response => {
            if (!response.ok) throw new Error('이미지를 불러올 수 없습니다.');
            return response.blob();
        })
        .then(blob => {
            // Blob을 File 객체로 변환
            const fileName = 'reference_image.' + (blob.type.split('/')[1] || 'jpg');
            const file = new File([blob], fileName, { type: blob.type });

            // 파일 크기 확인 (10MB 제한)
            const maxSize = 10 * 1024 * 1024;
            if (file.size > maxSize) {
                showAlert('파일 크기가 너무 큽니다. (최대 10MB)');
                return;
            }

            // attachedImageFile 전역 변수에 설정
            attachedImageFile = file;

            // UI 업데이트
            const statusDiv = document.getElementById('attachmentStatus');
            const fileNameSpan = document.getElementById('attachmentFileName');
            const imagePreview = document.getElementById('imagePreview');

            if (imagePreview) {
                // 미리보기는 서버 다운로드 URL 사용
                imagePreview.src = downloadUrl;
            }
            if (fileNameSpan) {
                fileNameSpan.textContent = `📎 ${fileName}`;
            }
            if (statusDiv) {
                statusDiv.classList.remove('hidden');
            }

            // 업로드 버튼 상태 변경
            const uploadIcon = document.getElementById('uploadIcon');
            const uploadText = document.getElementById('uploadText');
            const uploadHint = document.getElementById('uploadHint');
            const uploadBtn = document.getElementById('uploadBtn');

            if (uploadIcon) {
                uploadIcon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>';
                uploadIcon.classList.remove('text-warm-400', 'group-hover:text-accent');
                uploadIcon.classList.add('text-green-500', 'group-hover:text-green-600');
            }
            if (uploadText) {
                uploadText.textContent = '이미지 첨부됨 (클릭하여 변경)';
                uploadText.classList.remove('text-warm-600', 'group-hover:text-accent');
                uploadText.classList.add('text-green-600', 'group-hover:text-green-700');
            }
            if (uploadHint) {
                uploadHint.textContent = '다른 이미지로 변경할 수 있습니다';
            }
            if (uploadBtn) {
                uploadBtn.classList.remove('border-warm-300', 'hover:border-accent');
                uploadBtn.classList.add('border-green-300', 'bg-green-50', 'hover:border-green-400', 'hover:bg-green-100');
            }

            showAlert('이미지가 자동으로 첨부되었습니다.');
        })
        .catch(error => {
            console.error('이미지 첨부 실패:', error);
            showAlert('이미지를 자동으로 첨부할 수 없습니다: ' + error.message);
        });
}

// ==================== DOM Manipulation Functions ====================

/**
 * 이미지 생성 버튼 활성/비활성화
 */
function setGenerateButtonState(enabled) {
    const generateBtn = document.querySelector('.btn-generate');
    const viewAllBtn = document.getElementById('viewAllBtn');
    const favoritesBtn = document.getElementById('favoritesBtn');

    if (generateBtn) {
        generateBtn.disabled = !enabled;
        generateBtn.style.opacity = enabled ? '1' : '0.5';
        generateBtn.style.cursor = enabled ? 'pointer' : 'not-allowed';
    }

    if (viewAllBtn) {
        viewAllBtn.disabled = !enabled;
        viewAllBtn.style.opacity = enabled ? '1' : '0.5';
        viewAllBtn.style.cursor = enabled ? 'pointer' : 'not-allowed';
    }

    if(favoritesBtn) {
    favoritesBtn.disabled = !enabled;
    favoritesBtn.style.opacity = enabled ? '1' : '0.5';
    favoritesBtn.style.cursor = enabled ? 'pointer' : 'not-allowed';
    }
}

/**
 * 알림 메시지 표시
 * @param {string} message - 표시할 메시지
 */
function showAlert(message) {
    window.alert(message);
}

/**
 * 초기화 확인
 */
function confirmReset() {
    const confirmed = confirm('정말 이미지 생성 조건을 비우시겠습니까?');
    if (confirmed) {
        document.getElementById('generateForm').reset();
        removeAttachment(); // 첨부 이미지도 제거
        // 초기화 시 알림 메시지 제거
        const allAlerts = document.querySelectorAll('.alert');
        allAlerts.forEach(alert => alert.remove());
    }
}

/**
 * 즐겨찾기 페이지로 이동
 * 로그인 상태 확인 후 이동
 */
function goToFavorites() {

    if (!isUserLoggedIn()) {
        showAlert('로그인 이후 이용할 수 있습니다.', false);
        return;
    }

    window.location.href = `/user/favorites`;
}

// ==================== Form Submission ====================

/**
 * 폼 제출 시 처리 (FormData 기반 통합 요청)
 * 첨부 이미지 있음: POST /generate (multipart/form-data)
 * 첨부 이미지 없음: POST /generate (application/x-www-form-urlencoded)
 * @param {Event} event - Form submit event
 */
function showLoading(event) {
    event.preventDefault();

    const prompt = document.getElementById('prompt').value.trim();

    if (!prompt) {
        showAlert('이미지 생성 조건을 입력해주세요!', false);
        return false;
    }

    if (isLoading) {
        return false;
    }

    // DOM 요소 존재 확인
    const alertDiv = document.querySelector('.alert');
    const resultDiv = document.getElementById('resultDiv');
    const generateForm = document.getElementById('generateForm');
    const loadingDiv = document.getElementById('loadingDiv');

    if (!generateForm || !loadingDiv) {
        console.error('필수 DOM 요소를 찾을 수 없습니다');
        return false;
    }

    // 로딩 상태 시작
    isLoading = true;
    setGenerateButtonState(false);

    // 알림 메시지 숨기기
    if (alertDiv) {
        alertDiv.style.display = 'none';
    }

    // 결과 영역 및 폼 숨기기
    if (resultDiv) resultDiv.style.display = 'none';
    generateForm.style.display = 'none';

    // 로딩 표시
    loadingDiv.style.display = 'block';

    const userEmail = getCurrentUserEmail();

    // *** 통합: 항상 FormData 사용 (첨부 파일이 있으면 multipart, 없으면 urlencoded) ***
    const formData = new FormData();
    formData.append('prompt', prompt);
    formData.append('email', userEmail);
    if (attachedImageFile) {
        formData.append('attachImage', attachedImageFile);
    }

    fetch('/generate', {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
            }
            // 서버에서 렌더링된 전체 HTML을 텍스트로 받음
            return response.text();
        })
        .then(html => {
            // 1. 응답 HTML에서 데이터 추출을 위해 임시 DOM 요소 생성
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const body = doc.body;

            if (!body) {
                throw new Error('응답 HTML이 유효하지 않습니다');
            }

            // 2. 임시 DOM에서 서버가 ModelAttribute로 렌더링한 데이터 추출
            // 이 데이터들은 Mustache가 렌더링 시 body 태그에 data-속성으로 넣어준 값입니다.
            const success = body.getAttribute('data-success');
            const imageUrl = body.getAttribute('data-image-url');
            const displayUrl = body.getAttribute('data-display-url');
            const s3Key = body.getAttribute('data-s3-key');
            const message = body.getAttribute('data-message');
            const isQuotaExceeded = body.getAttribute('data-is-quota-exceeded');
            const promptValue = doc.getElementById('prompt') ? doc.getElementById('prompt').value : '';

            // DOM 요소 존재 검증
            const resultImage = document.getElementById('resultImage');
            const downloadBtn = document.getElementById('downloadBtn');
            const resultDiv = document.getElementById('resultDiv');
            const promptField = document.getElementById('prompt');

            // 3. UI 업데이트
            if (success === 'true' && imageUrl && resultImage && downloadBtn && resultDiv) {
                // 성공
                resultImage.src = imageUrl;
                resultImage.setAttribute('data-s3-key', s3Key);
                resultImage.setAttribute('data-display-url', displayUrl || imageUrl);
                downloadBtn.href = '/download/' + s3Key;
                resultDiv.style.display = 'block';
            } else if (success === 'false') {
                // 실패 (서버에서 success=false로 응답한 경우)
                showAlert(message || '이미지 생성에 실패했습니다.', false);

                if (isQuotaExceeded === 'true') {
                    console.warn("⚠️ API 할당량 초과!");
                }
            } else {
                showAlert('서버에서 예상치 못한 응답을 받았습니다.', false);
            }

            // 입력값 복원
            if (promptField) {
                promptField.value = promptValue || prompt;
            }
        })
        .catch(error => {
            // 네트워크 오류 또는 HTTP 오류 처리
            console.error("❌ Fetch Error:", error);
            showAlert(`서버 요청 중 오류 발생: ${error.message}`, false);
        })
        .finally(() => {
            // 로딩 상태 종료 및 UI 복구
            isLoading = false;
            setGenerateButtonState(true);

            const loadingDiv = document.getElementById('loadingDiv');
            const generateForm = document.getElementById('generateForm');

            if (loadingDiv) loadingDiv.style.display = 'none';
            if (generateForm) generateForm.style.display = 'block';
        });
}

// ==================== Favorite Management ====================

/**
 * 현재 생성된 이미지를 즐겨찾기에 저장
 */
function saveFavorite() {
    // *** 로그인 상태 확인 ***
    if (!isUserLoggedIn()) {
        showAlert('로그인 이후 이용할 수 있습니다.', false);
        return;
    }

    const resultImage = document.getElementById('resultImage');

    if (!resultImage || !resultImage.src) {
        showAlert('저장할 이미지가 없습니다', false);
        return;
    }

    // *** data-s3-key 속성에서 s3Key 직접 읽기 ***
    let s3Key = resultImage.getAttribute('data-s3-key');

    if (!s3Key || s3Key.trim() === '') {
        const imageUrl = resultImage.src;
        if (imageUrl && imageUrl.includes('/download/')) {
            s3Key = imageUrl.split('/download/')[1];
        }
    }

    if (!s3Key || s3Key.trim() === '') {
        showAlert('이미지 정보를 추출할 수 없습니다', false);
        return;
    }

    const userEmail = getCurrentUserEmail();

    fetch('/user/save', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            s3Key: s3Key.trim(),
            email: userEmail
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            showAlert('✅ ' + data.message, true);
        } else {
            showAlert('❌ ' + (data.message || '저장에 실패했습니다'), false);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('요청 중 오류가 발생했습니다: ' + error.message, false);
    });
}


// ==================== Drag and Drop ====================

/**
 * 드래그 앤 드롭 초기화
 */
function initDragAndDrop() {
    const uploadBtn = document.getElementById('uploadBtn');
    if (!uploadBtn) return;

    // 드래그 이벤트 기본 동작 방지
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        uploadBtn.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });

    // 드래그 영역 하이라이트
    ['dragenter', 'dragover'].forEach(eventName => {
        uploadBtn.addEventListener(eventName, () => highlight(uploadBtn), false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        uploadBtn.addEventListener(eventName, () => unhighlight(uploadBtn), false);
    });

    // 파일 드롭 처리
    uploadBtn.addEventListener('drop', handleDrop, false);
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

function highlight(element) {
    element.classList.add('border-accent', 'bg-accent/10');
    element.classList.remove('border-warm-300');
}

function unhighlight(element) {
    element.classList.remove('border-accent', 'bg-accent/10');
    if (!attachedImageFile) {
        element.classList.add('border-warm-300');
    }
}

function handleDrop(e) {
    const dt = e.dataTransfer;
    const files = dt.files;

    if (files.length > 0) {
        const file = files[0];

        // 이미지 파일인지 확인
        if (!file.type.startsWith('image/')) {
            showAlert('이미지 파일만 업로드할 수 있습니다.');
            return;
        }

        // input 요소에 파일 설정 (handleImageAttachment와 동일한 로직 사용)
        const input = document.getElementById('attachImage');

        // DataTransfer를 사용하여 input에 파일 설정
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        input.files = dataTransfer.files;

        // 기존 핸들러 호출
        handleImageAttachment({ target: input });
    }
}

// ==================== Page Initialization ====================

/**
 * 페이지 로드 시 처리 (초기화)
 */
window.addEventListener('load', function() {
    // 드래그 앤 드롭 초기화
    initDragAndDrop();

    // 세션 스토리지에서 이미지 자동 첨부 확인
    checkAndAttachImageFromSession();
    // *** 사용자 이메일 초기화 ***
    const userEmailElement = document.querySelector('.user-email');
    if (userEmailElement) {
        const email = userEmailElement.textContent.trim();
        setCurrentUserEmail(email);
    }

    // 모든 alert 요소 제거
    const allAlerts = document.querySelectorAll('.alert');
    allAlerts.forEach(alert => {
        alert.remove();
    });

    // 로딩/결과 표시 숨기기
    const loadingDiv = document.getElementById('loadingDiv');
    const resultDiv = document.getElementById('resultDiv');
    const generateForm = document.getElementById('generateForm');

    if (loadingDiv) loadingDiv.style.display = 'none';
    if (resultDiv) resultDiv.style.display = 'none';
    if (generateForm) generateForm.style.display = 'block';

    // 이미지 생성 버튼 활성화 (초기 상태)
    isLoading = false;
    setGenerateButtonState(true);

    // body의 data-속성 검증 (새로고침 후 서버 응답이 있는 경우 처리)
    const body = document.body;
    const success = body.getAttribute('data-success');
    const imageUrl = body.getAttribute('data-image-url');
    const displayUrl = body.getAttribute('data-display-url');
    const s3Key = body.getAttribute('data-s3-key');
    const message = body.getAttribute('data-message');

    // 새로고침 후 이전 요청의 결과가 있으면 표시
    if (success === 'true' && imageUrl) {
        try {
            const resultImage = document.getElementById('resultImage');
            const downloadBtn = document.getElementById('downloadBtn');

            if (resultImage && downloadBtn) {
                resultImage.src = imageUrl;
                if (s3Key) {
                    resultImage.setAttribute('data-s3-key', s3Key);
                    downloadBtn.href = '/download/' + s3Key;
                }
                if (displayUrl) {
                    resultImage.setAttribute('data-display-url', displayUrl);
                }
                if (resultDiv) resultDiv.style.display = 'block';
                if (generateForm) generateForm.style.display = 'block';
            }
        } catch (e) {
            console.error('이전 결과 복원 중 오류:', e);
        }
    }
});
