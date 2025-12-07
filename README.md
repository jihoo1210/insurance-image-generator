# 🎨 KB 손해보험 이미지 생성 서비스

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green)
![Gradle](https://img.shields.io/badge/Gradle-8.x-blue)

구글 Gemini API를 활용하여 KB 손해보험 맞춤형 이미지를 자동으로 생성하는 웹 애플리케이션입니다.

---

## ✨ 주요 기능

### 📸 이미지 생성
- **텍스트 기반**: 프롬프트만으로 이미지 생성
- **이미지 기반**: 첨부된 이미지를 기반으로 새로운 이미지 생성
- **Presigned URL**: S3 퍼블릭 접근 비활성화 상태에서도 1시간 유효한 임시 URL 제공

### 👤 사용자 관리
- **Google OAuth2 로그인**: 별도 가입 없이 구글 계정으로 로그인
- **세션 관리**: 로그인 상태 유지 및 사용자 이메일 추적
- **비회원 지원**: 비로그인 사용자도 이미지 생성 가능

### ❤️ 즐겨찾기
- 생성된 이미지를 즐겨찾기 저장
- 사용자별 즐겨찾기 목록 관리
- 저장 수 기준 인기도 정렬

### 🖼️ 이미지 관리
- 전체 생성 이미지 목록 조회
- 프롬프트 재사용 가능
- 이미지 새 창에서 보기
- 다운로드 지원

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────┐
│       Web Browser (Frontend)        │
│  ┌─────────────────────────────────┐│
│  │  index.mustache / imagelist.html ││
│  │  script.js / style.css           ││
│  └─────────────────────────────────┘│
└──────────────┬──────────────────────┘
               │ HTTP POST/GET
               ↓
┌──────────────────────────────────────┐
│    Spring Boot Application (8080)    │
│  ┌────────────────────────────────┐ │
│  │    ImageController             │ │
│  │  - POST /generate              │ │
│  │  - GET /download/{s3Key}       │ │
│  │  - GET /list                   │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │    ImgService                  │ │
│  │  - generateImage()             │ │
│  │  - generateImageWithAttachment()│ │
│  │  - generateS3Url()             │ │
│  │  - getAllImages()              │ │
│  └────────────────────────────────┘ │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        ↓             ↓
    ┌────────┐   ┌─────────────┐
    │ Gemini │   │ AWS S3      │
    │  API   │   │ + S3        │
    │        │   │ Presigner   │
    └────────┘   └─────────────┘
```

---

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.2.14
- **Language**: Java 17
- **Build Tool**: Gradle
- **ORM**: JPA / Hibernate
- **Database**: H2 (Development)
- **Authentication**: Spring Security + OAuth2

### Frontend
- **Template**: Mustache
- **Styling**: CSS3 (Dark Mode)
- **HTTP Client**: Fetch API
- **State Management**: JavaScript (Vanilla)

### Cloud Services
- **API**: Google Gemini API (Image Generation)
- **Storage**: AWS S3 (Image Storage)
- **Auth**: AWS S3 Presigner (Temporary URLs)

### Dependencies
```gradle
// Google Gemini
com.google:genai:1.25.0

// AWS SDK
software.amazon.awssdk:s3:2.x.x
software.amazon.awssdk:s3-presigner:2.x.x

// Spring
org.springframework.boot:spring-boot-starter-web
org.springframework.boot:spring-boot-starter-security
org.springframework.boot:spring-boot-starter-oauth2-client
org.springframework.boot:spring-boot-starter-data-jpa
org.springframework.boot:spring-boot-starter-thymeleaf

// Database
com.h2database:h2

// Utilities
org.projectlombok:lombok
org.apache.tika:tika-core
com.google.code.gson:gson
```

---

## 📋 API 명세

### 1. 이미지 생성
**URL**: `POST /generate`

**Request**:
```javascript
{
  "prompt": "string (required) - 이미지 생성 조건",
  "email": "string (optional) - 사용자 이메일",
  "attachImage": "file (optional) - 첨부 이미지 (최대 10MB)"
}
```

**Response** (HTML with data attributes):
```html
<body 
  data-success="true|false"
  data-image-url="https://s3.presigned.url"
  data-s3-key="uuid_filename.png"
  data-message="성공/실패 메시지"
  data-is-quota-exceeded="true|false">
</body>
```

**상태 코드**:
- `200 OK`: 요청 처리 완료 (성공/실패 여부는 data-success 확인)
- `400 Bad Request`: 필수 파라미터 없음
- `500 Internal Server Error`: 서버 오류

---

### 2. 이미지 다운로드
**URL**: `GET /download/{s3Key}`

**Response**:
- `200 OK`: 이미지 바이너리 (Content-Type: image/png)
- `404 Not Found`: S3에서 파일을 찾을 수 없음

**Header**:
```
Content-Type: image/png
Content-Disposition: inline; filename="uuid_filename.png"
```

---

### 3. 이미지 목록 조회
**URL**: `GET /list`

**Response**: `imagelist.mustache` (HTML)
- 모든 생성 이미지 목록
- 저장 수 기준 정렬
- 사용자별 즐겨찾기 여부 표시

---

### 4. 즐겨찾기 저장 (별도 컨트롤러)
**URL**: `POST /user/save`

**Request**:
```javascript
{
  "s3Key": "uuid_filename.png",
  "email": "user@example.com"
}
```

**Response** (JSON):
```javascript
{
  "success": true|false,
  "message": "저장 완료/실패 메시지"
}
```

---

## 🚀 설치 및 실행

### 필수 요구사항
- Java 17+
- Gradle 8.x
- 구글 Gemini API 키
- AWS S3 버킷 및 IAM 자격증명

### 환경 설정

#### 1. 구글 Gemini API 키
```bash
# 환경 변수 설정 (Windows)
set GEMINI_API_KEY=your-api-key

# 또는 application.yaml에 직접 설정
google:
  api:
    key: your-api-key
```

#### 2. AWS S3 설정
```yaml
# src/main/resources/application.yaml
aws:
  s3:
    bucket-name: your-bucket-name
    region: ap-northeast-2
  credentials:
    access-key: your-access-key
    secret-key: your-secret-key
```

#### 3. OAuth2 설정
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: email, profile
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://www.googleapis.com/oauth2/v4/token
            user-info-uri: https://www.googleapis.com/oauth2/v1/userinfo
            user-name-attribute: email
```

### 빌드 및 실행

```bash
# 프로젝트 클론
git clone https://github.com/your-repo/insurance-image-generator.git
cd insurance-image-generator/backend

# 빌드
./gradlew clean build

# 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

### 접속
```
http://localhost:8080
```

---

## 📁 프로젝트 구조

```
insurance-image-generator/
├── backend/
│   ├── src/main/java/com/example/backend/
│   │   ├── controller/
│   │   │   ├── ImageController.java      # 이미지 생성/다운로드
│   │   │   ├── MainController.java       # 로그인 상태 확인
│   │   │   └── UserController.java       # 사용자 관리
│   │   ├── service/
│   │   │   ├── ImgService.java           # Gemini API 호출
│   │   │   └── UserService.java          # 사용자 서비스
│   │   ├── entity/
│   │   │   ├── Image.java                # 이미지 엔티티
│   │   │   ├── User.java                 # 사용자 엔티티
│   │   │   └── UserSaveImages.java       # 즐겨찾기 엔티티
│   │   ├── dto/
│   │   │   ├── ImageListResponse.java    # 이미지 리스트 DTO
│   │   │   └── UserResponseDto.java      # 사용자 DTO
│   │   ├── repository/
│   │   │   ├── ImageRepository.java
│   │   │   └── UserRepository.java
│   │   └── config/
│   │       ├── SecurityConfig.java       # Spring Security 설정
│   │       └── AwsConfig.java            # AWS S3 설정
│   ├── src/main/resources/
│   │   ├── templates/
│   │   │   ├── index.mustache            # 메인 페이지
│   │   │   ├── imagelist.mustache        # 이미지 목록
│   │   │   └── favorites.mustache        # 즐겨찾기 페이지
│   │   ├── static/
│   │   │   ├── js/
│   │   │   │   └── script.js             # 메인 로직
│   │   │   └── css/
│   │   │       └── style.css             # 스타일 (다크모드)
│   │   └── application.yaml              # 설정 파일
│   └── build.gradle
└── README.md
```

---

## 🔄 주요 플로우

### 이미지 생성 플로우

```
1. 사용자 입력
   └─ 프롬프트 입력 (필수)
   └─ 이미지 첨부 (선택)

2. 폼 제출
   └─ showLoading(event)
   └─ FormData 생성 { prompt, email, attachImage }
   └─ fetch POST /generate

3. 서버 처리
   └─ ImageController.generateImage()
   └─ attachImage 여부 확인
   │  ├─ 있음: ImgService.generateImageWithAttachment()
   │  └─ 없음: ImgService.generateImage()
   └─ ImgService.generateS3Url() - Presigned URL 생성
   └─ Model에 데이터 추가
   └─ index.mustache 렌더링

4. 클라이언트 처리
   └─ HTML 파싱 (DOMParser)
   └─ data 속성 추출
   └─ UI 업데이트
   └─ 이미지 표시
```

### 상태 관리

```javascript
// 전역 상태
let isLoading = false;           // 로딩 여부
let currentUserEmail = '';       // 사용자 이메일
let attachedImageFile = null;    // 첨부 파일

// 상태 변화
초기화 → 파일 선택 → 로딩 → 완료 → 표시
```

---

## 🎨 UI/UX 특징

### 다크 모드 (기본)
- 눈 건강 보호
- 배터리 절약
- KB 손해보험 색상 기반 (#FFCA00, #000000)

### 반응형 디자인
```css
/* Desktop: 1200px+ */
/* Tablet: 768px - 1199px */
/* Mobile: 480px - 767px */
/* Extra Small: <480px */
```

### 인터랙티브 요소
- 파일 첨부 시 버튼 색상 변경
- 로딩 애니메이션
- 이미지 클릭 시 새 창에서 열기
- 에러 메시지 실시간 표시

---

## ⚙️ 설정 옵션

### application.yaml

```yaml
# 서버
server:
  port: 8080

# 데이터베이스
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

# 템플릿
  thymeleaf:
    mode: HTML
    encoding: UTF-8

# 보안
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

# 파일 업로드
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# Google Gemini
google:
  api:
    key: ${GEMINI_API_KEY}

# AWS S3
aws:
  s3:
    bucket-name: ${AWS_BUCKET_NAME}
  region: ap-northeast-2
```

---

## 🔐 보안 검사 항목

### ✅ 구현 완료
- [x] HTTPS 지원 (프로덕션)
- [x] CSRF 방지 (Spring Security)
- [x] SQL Injection 방지 (매개변수화 쿼리)
- [x] 파일 업로드 제한 (10MB, MIME 타입)
- [x] OAuth2 인증
- [x] 세션 관리
- [x] XSS 방지 (템플릿 엔진)
- [x] API 키 환경 변수 관리 (반드시)
- [x] AWS IAM 권한 최소화
- [x] S3 버킷 정책 재검토
- [x] 프로덕션 데이터베이스 전환 (RDS)
- [x] SSL/TLS 인증서 설치
- [x] Rate Limiting 추가
- [x] 로그 모니터링

---

## 🐛 트러블슈팅

### 문제: "API 할당량 초과" 에러
**원인**: Gemini API 무료 할당량 초과  
**해결**:
1. Google Cloud Console에서 API 설정 확인
2. 결제 정보 추가 또는 사용량 제한 설정
3. 서버 로그에서 `Please retry in` 메시지 확인

### 문제: S3 접근 거부
**원인**: AWS 자격증명 또는 IAM 권한 부족  
**해결**:
1. 환경 변수 설정 확인
2. IAM 사용자에 S3 전체 권한 부여
3. S3 버킷 정책 확인

### 문제: 이미지가 다운로드됨
**원인**: Content-Disposition 헤더 설정  
**해결**:
- `inline`: 브라우저에서 표시
- `attachment`: 다운로드

현재 설정: `inline` (권장)

### 문제: 로그인 안 됨
**원인**: OAuth2 설정 오류  
**해결**:
1. Google Cloud Console에서 클라이언트 ID/비밀 확인
2. 리다이렉트 URI 일치 확인 (`http://localhost:8080/login/oauth2/code/google`)
3. 브라우저 쿠키 삭제 후 재시도

---

## 📊 성능 최적화

### 이미지 생성 시간
- 텍스트 기반: ~10-15초
- 이미지 기반: ~15-20초
- (Gemini API 응답 시간 기준)

### 캐싱 전략
```javascript
// Presigned URL: 1시간 유효
// 브라우저 캐시: HTTP 기본 설정
// 데이터베이스: H2 인메모리
```

### 동시 요청 처리
```yaml
# 톰캣 스레드 풀
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

---

## 📚 API 문서

### Gemini API 모델
```
gemini-3-pro-image-preview
- 이미지 생성 전용 모델
- 고해상도 출력
- 1시간 응답 시간 정보 제공
```

### 시스템 인스트럭션
```
KB 손해보험 홍보 이미지 전문가 역할
- 색상: #FFCA00, #000000, #FFFFFF
- 스타일: 신뢰감, 강렬함, 직관성
- 요구사항: 한국인 대상, 오탈자 없음
```

---

## 🤝 기여 가이드

### 버그 신고
1. GitHub Issues에서 버그 신고
2. 스크린샷 및 에러 로그 포함
3. 재현 단계 상세 설명

### 기능 제안
1. Discussions에서 아이디어 공유
2. 구현 방식 논의
3. Pull Request 제출

### 코드 기여
```bash
# 1. Fork
git clone https://github.com/your-fork/insurance-image-generator.git

# 2. Feature 브랜치 생성
git checkout -b feature/amazing-feature

# 3. 변경 사항 커밋
git commit -m 'Add amazing feature'

# 4. 브랜치에 푸시
git push origin feature/amazing-feature

# 5. Pull Request 생성
```

---

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

---

## 📞 연락처

**프로젝트 관리자**
- 이메일: jihoostudy1@gmail.com
- GitHub: [@jihoo1210](https://github.com/jihoo1210)

**문의**
- 버그 리포트: GitHub Issues
- 기술 지원: Discussions
- 보안 취약점: 이메일로 직접 신고

---

## 🔄 업데이트 이력

### v1.0.0 (2025-12-07)
- ✨ 초기 출시
- 📸 이미지 생성 기능
- 👤 Google OAuth2 로그인
- ❤️ 즐겨찾기 기능
- 🎨 다크 모드 UI
- 📱 반응형 디자인

### 계획 중
- [ ] 이미지 필터링
- [ ] 사용자 통계 대시보드
- [ ] API 속도 제한
- [ ] 다국어 지원
- [ ] 모바일 앱

---

## 🙏 감사의 말

- Google Gemini API
- AWS S3
- Spring Boot 팀
- 모든 기여자들

---

**마지막 업데이트**: 2025년 12월 7일  
**현재 버전**: v1.0.0  
**상태**: ✅ Production Ready

