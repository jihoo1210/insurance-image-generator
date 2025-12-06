# 보험 이미지 생성 서비스 - 실행 가이드

## 🚀 빠른 시작

### 1. 프로젝트 구조
```
insurance-image-generator/
├── backend/                          # Spring Boot 백엔드
│   ├── src/main/java/com/example/backend/
│   │   ├── BackendApplication.java   # 메인 클래스
│   │   ├── config/
│   │   │   └── AwsConfig.java        # AWS S3 설정
│   │   ├── controller/
│   │   │   └── ImageController.java  # 이미지 생성 컨트롤러
│   │   └── service/
│   │       └── ImgService.java       # Gemini API 및 S3 저장 로직
│   ├── src/main/resources/
│   │   ├── application.yaml          # 애플리케이션 설정
│   │   └── templates/
│   │       └── index.html            # 웹 인터페이스
│   └── build.gradle                  # Gradle 설정
└── README.md
```

### 2. 필수 설정

**application.yaml에서 테스트 모드 설정:**
```yaml
app:
  mock-mode: true  # true: API 호출 없이 테스트, false: 실제 API 호출
```

### 3. 실행 방법

#### 방법 1: IDE에서 실행 (권장)
1. JetBrains IntelliJ IDEA 또는 Eclipse에서 프로젝트 열기
2. `BackendApplication.java` 우클릭
3. **Run 'BackendApplication.main()'** 선택 또는 `Shift+F10` 단축키

#### 방법 2: Gradle 명령어
```bash
cd backend
./gradlew bootRun
```

#### 방법 3: JAR 파일 빌드 후 실행
```bash
cd backend
./gradlew clean build
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

### 4. 애플리케이션 접속

브라우저에서 다음 주소로 접속:
```
http://localhost:8080
```

## 📝 사용 방법

### 메인 페이지 (`/`)
- 프롬프트 입력: 생성하고 싶은 이미지 설명 작성
- AI 역할 설정 (선택사항): 생성 스타일 지정
- "이미지 생성" 버튼 클릭

### 예시 입력값
**프롬프트:**
```
KB 손해보험 자동차보험을 나타내는 현대적이고 전문적인 이미지. 
파란색, 은색, 흰색 톤으로 신뢰감과 안정감을 전달
```

**AI 역할 설정:**
```
당신은 보험사 마케팅 전문가입니다. 
전문적이고 신뢰감 있는 고품질 이미지를 생성하세요.
```

## 🧪 테스트 모드

### 테스트 모드 활성화
`application.yaml`에서:
```yaml
app:
  mock-mode: true
```

테스트 모드에서는:
- ✅ 실제 Gemini API 호출 없음
- ✅ 더미 S3 경로 반환
- ✅ 즉시 응답 (대기 시간 없음)
- ✅ API 할당량 소비 없음

### 실제 API 모드
`application.yaml`에서:
```yaml
app:
  mock-mode: false
```

실제 API 모드에서는:
- Gemini API로 실제 이미지 생성
- 생성된 이미지를 AWS S3에 저장
- API 할당량 소비

## ⚠️ 주의사항

### API 키 보안
**`application.yaml`의 API 키는 공개되지 않도록 주의하세요!**

실제 배포 전에:
1. 환경변수로 변경
2. `.env` 파일 사용
3. CI/CD 환경변수 활용

### AWS S3 설정
실제 S3에 저장하려면:
1. AWS 계정 필요
2. S3 버킷 생성
3. IAM 사용자 생성 및 액세스 키 발급
4. `application.yaml`에서 설정:
```yaml
aws:
  s3:
    bucket-name: your-bucket-name
  region: ap-northeast-2
  access-key-id: YOUR_ACCESS_KEY
  secret-key-id: YOUR_SECRET_KEY
```

### Gemini API 할당량
- Free Tier: 제한된 요청 수
- 할당량 초과 시: 429 에러 + 재시도 카운트다운

## 🔧 API 엔드포인트

### 1. 메인 페이지
- **URL:** `GET /`
- **응답:** HTML (index.html)

### 2. 이미지 생성 (HTML 폼)
- **URL:** `POST /generate`
- **파라미터:**
  - `prompt` (필수): 이미지 생성 프롬프트
  - `systemInstruction` (선택): AI 역할 지정
- **응답:** HTML (결과 포함)

### 3. 이미지 생성 (JSON API)
- **URL:** `POST /api/generate`
- **파라미터:**
  - `prompt` (필수): 이미지 생성 프롬프트
  - `systemInstruction` (선택): AI 역할 지정
- **응답:**
```json
{
  "success": true,
  "message": "이미지 생성 성공!",
  "s3Key": "uuid_generated_image.png",
  "imageUrl": "/images/uuid_generated_image.png"
}
```

## 🚨 문제 해결

### 포트 8080 이미 사용 중
```bash
# 다른 포트로 실행
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar --server.port=8081
```

### Thymeleaf 템플릿을 찾을 수 없음
- `src/main/resources/templates/index.html` 파일 확인
- IDE 캐시 무효화 및 재빌드

### API 할당량 초과
- `app.mock-mode: true` 설정으로 테스트 모드 활용
- Gemini API 유료 플랜 업그레이드

## 📊 로그 확인

콘솔에서 다음과 같은 로그를 볼 수 있습니다:
```
2025-01-01 12:00:00.000  INFO  메인 페이지 요청
2025-01-01 12:00:01.000  INFO  이미지 생성 요청 - Prompt: ...
2025-01-01 12:00:02.000  INFO  테스트 모드로 작동 중. 더미 이미지 경로 반환
```

## 📚 참고 자료

- [Gemini API 문서](https://ai.google.dev/gemini-api)
- [AWS S3 문서](https://docs.aws.amazon.com/s3/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Thymeleaf 문서](https://www.thymeleaf.org/)

---

**개발자:** 보험 이미지 생성 팀
**버전:** 1.0.0
**마지막 업데이트:** 2025-01-01

