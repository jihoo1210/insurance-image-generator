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
        uploadIcon.classList.remove('text-warm-400');
        uploadIcon.classList.add('text-green-500');
    }
    if (uploadText) {
        uploadText.textContent = '업로드 성공! (클릭하여 변경)';
        uploadText.classList.remove('text-warm-600');
        uploadText.classList.add('text-green-600');
    }
    if (uploadHint) {
        uploadHint.textContent = '다른 이미지로 변경할 수 있습니다';
    }
    if (uploadBtn) {
        uploadBtn.classList.remove('border-warm-300');
        uploadBtn.classList.add('border-green-300', 'bg-green-50');
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
        uploadIcon.classList.remove('text-green-500');
        uploadIcon.classList.add('text-warm-400');
    }
    if (uploadText) {
        uploadText.textContent = '이미지를 드래그하거나 클릭하여 업로드';
        uploadText.classList.remove('text-green-600');
        uploadText.classList.add('text-warm-600');
    }
    if (uploadHint) {
        uploadHint.textContent = 'PNG, JPG, WEBP (최대 10MB)';
    }
    if (uploadBtn) {
        uploadBtn.classList.remove('border-green-300', 'bg-green-50');
        uploadBtn.classList.add('border-warm-300');
    }

    // 첨부 버튼 색상을 원래대로 변경
    const attachBtn = document.querySelector('.btn-attach-image');
    if (attachBtn) {
        attachBtn.style.background = 'linear-gradient(135deg, #5B9CFF 0%, #7EAFFF 100%)';
        attachBtn.style.boxShadow = '';
    }
}

/**
 * 이미지를 새 창에서 열기
 */
function openImageInNewTab() {
    const resultImage = document.getElementById('resultImage');
    if (resultImage && resultImage.src) {
        window.open(resultImage.src, '_blank');
    }
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
 * 토스트 알림 메시지 표시
 * @param {string} message - 표시할 메시지
 * @param {boolean} success - 성공 여부
 */
function showAlert(message, success) {
    // 토스트 컨테이너 찾기 또는 생성
    let container = document.getElementById('toastContainer') || document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.style.cssText = 'position: fixed; top: 24px; left: 50%; transform: translateX(-50%); z-index: 1000; display: flex; flex-direction: column; gap: 8px;';
        document.body.appendChild(container);
    }

    // 토스트 요소 생성
    const toast = document.createElement('div');
    toast.style.cssText = `
        padding: 16px 24px;
        border-radius: 12px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
        display: flex;
        align-items: center;
        gap: 12px;
        animation: slideDown 0.3s ease-out;
        ${success
            ? 'background: linear-gradient(135deg, #10b981, #059669); color: white;'
            : 'background: linear-gradient(135deg, #ef4444, #dc2626); color: white;'
        }
    `;

    const icon = success
        ? '<svg style="width: 20px; height: 20px; flex-shrink: 0;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>'
        : '<svg style="width: 20px; height: 20px; flex-shrink: 0;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>';

    toast.innerHTML = `${icon}<span style="font-size: 14px; font-weight: 500;">${message}</span>`;
    container.appendChild(toast);

    // 3초 후 제거
    setTimeout(() => {
        toast.style.animation = 'slideUp 0.3s ease-out forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
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


// ==================== Page Initialization ====================

/**
 * 페이지 로드 시 처리 (초기화)
 */
window.addEventListener('load', function() {
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
                if (resultDiv) resultDiv.style.display = 'block';
                if (generateForm) generateForm.style.display = 'block';
            }
        } catch (e) {
            console.error('이전 결과 복원 중 오류:', e);
        }
    }
});
