# WAI Admin Backend

스프링 부트 기반의 관리자 백엔드 API 프로젝트입니다. 외부 인증 서버와 연동하여 JWT 토큰 기반 인증을 제공합니다.

## 주요 기능

- **외부 인증 서버 연동**: 외부 인증 서버(`http://192.168.233.128:8000`)와 연동
- **JWT 토큰 관리**: HttpOnly 쿠키를 통한 보안 토큰 관리
- **헤더 기반 상태 관리**: `system_code`, `user_id`, `wai_lang` 등을 HTTP 헤더로 관리
- **Stateless 서버**: 세션에 의존하지 않는 무상태 서버 아키텍처
- **토큰 갱신**: 자동 토큰 갱신 기능
- **한글 지원**: UTF-8 인코딩으로 한글 로그 및 메시지 지원

## 기술 스택

- **Spring Boot 3.2.0**
- **Java 21**
- **Spring Web**
- **Maven**
- **HttpURLConnection** (외부 API 호출)
- **JWT** (JSON Web Token)

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/wai/admin/
│   │       ├── WaiAdminBackendApplication.java
│   │       ├── config/
│   │       │   └── WebConfig.java
│   │       ├── controller/
│   │       │   ├── CodeController.java
│   │       │   ├── CostController.java
│   │       │   ├── DashboardController.java
│   │       │   ├── MachineController.java
│   │       │   ├── MainController.java          # 인증 관련 컨트롤러
│   │       │   ├── ModelController.java
│   │       │   ├── ProcessController.java
│   │       │   ├── ProjectController.java
│   │       │   ├── TestController.java
│   │       │   └── UserController.java
│   │       ├── service/
│   │       │   ├── CodeService.java
│   │       │   ├── CostService.java
│   │       │   ├── DashboardService.java
│   │       │   ├── MachineService.java
│   │       │   ├── MainService.java             # 인증 관련 서비스
│   │       │   ├── ModelService.java
│   │       │   ├── ProcessService.java
│   │       │   ├── ProjectService.java
│   │       │   ├── TestService.java
│   │       │   └── UserService.java
│   │       └── util/
│   │           ├── CommonCodeUtil.java          # 공통 코드 유틸리티
│   │           ├── CommonUtil.java              # 공통 유틸리티
│   │           ├── HttpUtil.java                # HTTP 요청 유틸리티 (헤더 기반)
│   │           └── JsonUtil.java                # JSON 처리 유틸리티
│   └── resources/
│       ├── application.yml                      # 메인 설정
│       ├── application-dev.yml                  # 개발 환경 설정
│       ├── api.properties                       # 외부 API 설정
│       └── logback-spring.xml                   # 로깅 설정
└── test/
    └── java/
        └── com/wai/admin/
```

## 시작하기

### 필수 요구사항

- Java 21 이상
- Maven 3.6 이상
- 외부 인증 서버 접근 가능

### 환경 설정

#### 1. 외부 인증 서버 설정

`src/main/resources/api.properties` 파일에서 외부 인증 서버 정보를 설정합니다:

```properties
auth.server.ip=192.168.233.128
auth.server.port=8000
auth.server.base-url=http://192.168.233.128:8000
```

#### 2. 한글 인코딩 설정

프로젝트는 UTF-8 인코딩으로 설정되어 있습니다. 터미널에서 한글이 깨지는 경우:

**Windows PowerShell:**

```powershell
chcp 65001
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
```

### 실행 방법

#### 1. Docker 실행 (권장)

**Windows:**

```cmd
# Docker로 자동 빌드 및 실행
build-and-run-docker.bat

# Docker 관리 도구
docker-commands.bat
```

**Linux:**

```bash
# 스크립트에 실행 권한 부여
chmod +x *.sh

# Docker로 자동 빌드 및 실행
./build-and-run-docker.sh

# Docker 관리 도구 (대화형 메뉴)
./docker-commands.sh
```

#### 2. 로컬 개발 서버 실행

**Windows:**

```cmd
# Maven Wrapper 사용
.\mvnw.cmd spring-boot:run

# 또는 배치 파일 사용
run_backend.bat
```

**Linux:**

```bash
# 셸 스크립트 사용 (권장)
./run_backend.sh

# 또는 Maven Wrapper 사용
./mvnw spring-boot:run

# 또는 시스템 Maven 사용
mvn spring-boot:run
```

#### 3. IDE에서 실행

- **VS Code**: F5 키 또는 디버그 패널에서 "Debug Spring Boot" 실행
- **IntelliJ IDEA**: Run/Debug Configuration에서 실행

### 접속 정보

- **로컬 실행**: http://localhost:8080
- **Docker 실행**: http://localhost:5433 (포트 매핑: 5433 → 8080)
- **외부 인증 서버**: http://192.168.233.128:8000

## API 엔드포인트

### 헤더 요구사항

모든 API 호출 시 다음 헤더들을 포함해야 합니다:

```http
system_code: WAI_WEB_ADMIN 또는 WAI_WEB_VIEW
user_id: 12345
wai_lang: ko 또는 en
```

### 인증 API (MainController)

- `POST /api/main/login` - 로그인

  - **Request Body**:
    ```json
    {
      "username": "admin",
      "password": "password123",
      "system_code": "WAI_WEB_ADMIN"
    }
    ```
  - **Response**: JWT 토큰이 HttpOnly 쿠키로 설정됨
  - **Success Response**:
    ```json
    {
      "success": true,
      "status": 200,
      "message": "messages.success.loginSuccess",
      "response": {
        "user_info": {
          "user_id": "admin",
          "username": "관리자",
          "email": "admin@wai.com",
          "role": "ADMIN"
        },
        "menus": [
          {
            "menu_id": "dashboard",
            "menu_name": "대시보드",
            "menu_url": "/dashboard"
          }
        ]
      }
    }
    ```

- `POST /api/main/logout` - 로그아웃

  - **Request Body**:
    ```json
    {
      "system_code": "WAI_WEB_ADMIN"
    }
    ```
  - **Response**: 쿠키 삭제 및 외부 서버 로그아웃 호출
  - **Success Response**:
    ```json
    {
      "success": true,
      "status": 200,
      "message": "messages.success.logoutSuccess"
    }
    ```

- `POST /api/main/refresh` - 토큰 갱신

  - **Request Body**:
    ```json
    {
      "system_code": "WAI_WEB_ADMIN"
    }
    ```
  - **Request**: 쿠키에서 refresh 토큰 자동 추출
  - **Response**: 새로운 토큰들로 쿠키 업데이트
  - **Success Response**:
    ```json
    {
      "success": true,
      "status": 200,
      "message": "messages.success.tokenRefreshSuccess",
      "response": {
        "user_info": {...},
        "menus": [...]
      }
    }
    ```

- `GET /api/main/verify` - 토큰 검증

  - **Request**: 쿠키에서 access 토큰 자동 추출
  - **Response**: 토큰 유효성 검증 결과
  - **Success Response**:
    ```json
    {
      "success": true,
      "status": 200,
      "message": "Token is valid"
    }
    ```

### 사용자 관리 API (UserController)

- `POST /api/users/list` - 사용자 목록 조회

  - **Request Body**:
    ```json
    {
      "searchKeyword": "홍길동",
      "role": "USER",
      "status": "ACTIVE",
      "page": 1,
      "size": 10
    }
    ```
  - **Success Response**:
    ```json
    {
      "success": true,
      "message": "사용자 목록 조회 성공",
      "data": [
        {
          "user_id": "user001",
          "username": "홍길동",
          "email": "hong@example.com",
          "role": "USER",
          "status": "ACTIVE",
          "created_at": "2024-01-15T10:30:00.000Z"
        }
      ],
      "totalCount": 1
    }
    ```

- `POST /api/users/create` - 사용자 등록

  - **Request Body**:
    ```json
    {
      "username": "홍길동",
      "email": "hong@example.com",
      "role": "USER",
      "status": "ACTIVE"
    }
    ```

- `PATCH /api/users/update/{userId}` - 사용자 수정

  - **Request Body**:
    ```json
    {
      "username": "홍길동",
      "email": "hong@example.com",
      "role": "USER"
    }
    ```

### 기존 API 엔드포인트

#### 테스트 API

- `GET /api/test/health` - 헬스 체크
- `GET /api/test/info` - 애플리케이션 정보

#### 코드 관리 API

- `GET /api/code` - 코드 목록 조회
- `GET /api/code/{id}` - 코드 상세 조회
- `POST /api/code` - 코드 생성
- `PUT /api/code/{id}` - 코드 수정
- `DELETE /api/code/{id}` - 코드 삭제

#### 비용 관리 API

- `GET /api/cost` - 비용 목록 조회
- `GET /api/cost/{id}` - 비용 상세 조회
- `POST /api/cost` - 비용 생성
- `PUT /api/cost/{id}` - 비용 수정
- `DELETE /api/cost/{id}` - 비용 삭제
- `GET /api/cost/summary` - 비용 요약 조회

#### 대시보드 API

- `GET /api/dashboard` - 대시보드 데이터 조회
- `GET /api/dashboard/summary` - 대시보드 요약 조회
- `GET /api/dashboard/stats` - 대시보드 통계 조회
- `GET /api/dashboard/chart` - 대시보드 차트 데이터 조회

#### 기계 관리 API

- `GET /api/machine` - 기계 목록 조회
- `GET /api/machine/{id}` - 기계 상세 조회
- `POST /api/machine` - 기계 생성
- `PUT /api/machine/{id}` - 기계 수정
- `DELETE /api/machine/{id}` - 기계 삭제
- `GET /api/machine/{id}/status` - 기계 상태 조회

#### 모델 관리 API

- `GET /api/model` - 모델 목록 조회
- `GET /api/model/{id}` - 모델 상세 조회
- `POST /api/model` - 모델 생성
- `PUT /api/model/{id}` - 모델 수정
- `DELETE /api/model/{id}` - 모델 삭제
- `GET /api/model/{id}/version` - 모델 버전 조회

#### 프로세스 관리 API

- `GET /api/process` - 프로세스 목록 조회
- `GET /api/process/{id}` - 프로세스 상세 조회
- `POST /api/process` - 프로세스 생성
- `PUT /api/process/{id}` - 프로세스 수정
- `DELETE /api/process/{id}` - 프로세스 삭제
- `GET /api/process/{id}/status` - 프로세스 상태 조회
- `POST /api/process/{id}/start` - 프로세스 시작
- `POST /api/process/{id}/stop` - 프로세스 중지

#### 프로젝트 관리 API

- `GET /api/project` - 프로젝트 목록 조회
- `GET /api/project/{id}` - 프로젝트 상세 조회
- `POST /api/project` - 프로젝트 생성
- `PUT /api/project/{id}` - 프로젝트 수정
- `DELETE /api/project/{id}` - 프로젝트 삭제
- `GET /api/project/{id}/members` - 프로젝트 멤버 조회
- `GET /api/project/{id}/progress` - 프로젝트 진행률 조회

#### 사용자 관리 API

- `GET /api/user` - 사용자 목록 조회
- `GET /api/user/{id}` - 사용자 상세 조회
- `POST /api/user` - 사용자 생성
- `PUT /api/user/{id}` - 사용자 수정
- `DELETE /api/user/{id}` - 사용자 삭제
- `GET /api/user/{id}/profile` - 사용자 프로필 조회
- `GET /api/user/{id}/projects` - 사용자 프로젝트 조회

## 유틸리티 클래스

### HttpUtil

HTTP 요청을 처리하는 유틸리티 클래스입니다. 헤더에서 `system_code`를 자동으로 감지하여 적절한 쿠키를 설정합니다.

**주요 기능:**

- **헤더 기반 쿠키 관리**: `system_code` 헤더에 따라 `admin_access`/`webView_access` 쿠키 자동 설정
- **자동 토큰 추출**: 쿠키에서 access, refresh, session 토큰 자동 추출
- **Apache HttpClient 기반**: 안정적인 HTTP 통신

**주요 메서드:**

- `post(url, contentType, requestBody)` - POST 요청 (쿠키 자동 설정)
- `get(url)` - GET 요청 (쿠키 자동 설정)
- `put(url, contentType, requestBody)` - PUT 요청 (쿠키 자동 설정)
- `patch(url, contentType, requestBody)` - PATCH 요청 (쿠키 자동 설정)
- `delete(url)` - DELETE 요청 (쿠키 자동 설정)
- `delete(url, contentType, requestBody)` - DELETE 요청 (요청 본문 포함, 쿠키 자동 설정)

**사용 예시:**

```java
// POST 요청 (헤더에서 system_code 자동 감지, 쿠키 자동 설정)
HttpUtil.HttpResult result = HttpUtil.post("http://example.com/api", "application/json", "{\"key\":\"value\"}");

// GET 요청 (쿠키 자동 설정)
HttpUtil.HttpResult getResult = HttpUtil.get("http://example.com/api");

// 응답 처리
if (result.isSuccess()) {
    String response = result.getBody();
    int status = result.getStatus();
}
```

**헤더 요구사항:**

HTTP 요청 시 다음 헤더가 필요합니다:

```http
system_code: WAI_WEB_ADMIN
wai_lang: ko
user_id: 12345
```

### CommonUtil

공통 유틸리티 클래스입니다. 다양한 공통 함수들을 제공합니다.

**주요 메서드:**

#### 응답 생성

- `createResponse(boolean success, String message)` - 기본 응답 맵 생성
- `createSuccessResponse(String message)` - 성공 응답 맵 생성
- `createErrorResponse(String message)` - 실패 응답 맵 생성
- `createResponseWithstatus(boolean success, String message, int status)` - 상태 코드 포함 응답
- `createResponseWithData(boolean success, String message, Object data)` - 데이터 포함 응답
- `createFullResponse(boolean success, String message, int status, Object data)` - 완전한 응답

#### 유효성 검사

- `isEmpty(String str)` - 문자열이 null이거나 비어있는지 확인
- `isNotEmpty(String str)` - 문자열이 null이 아니고 비어있지 않은지 확인
- `isNull(Object obj)` - 객체가 null인지 확인
- `isNotNull(Object obj)` - 객체가 null이 아닌지 확인

#### 안전한 변환

- `safeToString(Object obj)` - 안전한 문자열 변환
- `safeToInt(Object obj, int defaultValue)` - 안전한 정수 변환
- `safeToInt(Object obj)` - 안전한 정수 변환 (기본값 0)

#### 맵 처리

- `getValue(Map<String, Object> map, String key)` - 맵에서 안전하게 문자열 값 추출
- `safeGetString(Map<String, Object> map, String key)` - 맵에서 안전하게 문자열 추출
- `safeGetInt(Map<String, Object> map, String key, int defaultValue)` - 맵에서 안전하게 정수 추출
- `safeGetInt(Map<String, Object> map, String key)` - 맵에서 안전하게 정수 추출 (기본값 0)

#### 디버깅

- `debugMap(Map<String, Object> map, String title)` - 디버그용 맵 출력
- `debugMap(Map<String, Object> map)` - 디버그용 맵 출력 (제목 없음)

**사용 예시:**

```java
// 응답 생성
Map<String, Object> response = CommonUtil.createSuccessResponse("처리 완료");
Map<String, Object> fullResponse = CommonUtil.createFullResponse(true, "성공", 200, data);

// 유효성 검사
if (CommonUtil.isEmpty(username)) {
    return CommonUtil.createErrorResponse("사용자명이 필요합니다.");
}

// 안전한 변환
int value = CommonUtil.safeToInt(parsedResponse.get("count"), 0);
String name = CommonUtil.getValue(parsedResponse, "name");

// 디버깅
CommonUtil.debugMap(parsedResponse, "파싱된 응답");
```

### JsonUtil

JSON 처리를 위한 유틸리티 클래스입니다.

**주요 메서드:**

- `mapToJson(Map<String, String>)` - Map을 JSON 문자열로 변환
- `objectMapToJson(Map<String, Object>)` - Object Map을 JSON 문자열로 변환
- `parseJson(String json)` - JSON 문자열을 Map으로 파싱
- `extractValue(String json, String key)` - JSON에서 특정 키의 값 추출
- `mapToString(Map<String, Object>)` - Map을 보기 좋은 JSON 문자열로 변환

**사용 예시:**

```java
Map<String, String> data = new HashMap<>();
data.put("username", "user123");
data.put("password", "pass123");
String json = JsonUtil.mapToJson(data);

Map<String, Object> parsed = JsonUtil.parseJson(json);
String username = JsonUtil.extractValue(json, "username");
```

### CommonCodeUtil

공통 코드 처리를 위한 유틸리티 클래스입니다.

**주요 기능:**

- **언어별 코드 처리**: `wai_lang` 헤더에 따라 `code_value_en` 값을 `code_value`로 자동 변환
- **외부 API 연동**: 공통 코드 조회 시 외부 서버와 연동

**주요 메서드:**

- `getCommonCodes(String codeGroup, HttpServletRequest request)` - 공통 코드 목록 조회
- `processLanguageOverride(List<Map<String, Object>> itemsList)` - 언어별 코드값 처리

**사용 예시:**

```java
// 공통 코드 조회 (헤더에서 wai_lang 자동 감지)
Map<String, Object> codes = commonCodeUtil.getCommonCodes("USER_ROLE", request);

// 영어 설정 시 code_value_en이 code_value로 자동 변환됨
```

**헤더 요구사항:**

```http
wai_lang: ko 또는 en
```

## 아키텍처 특징

### Stateless 서버 설계

- **세션 독립성**: 서버 세션에 상태 정보를 저장하지 않음
- **헤더 기반 상태 관리**: 모든 상태 정보를 HTTP 헤더로 전달
- **확장성**: 로드밸런싱 및 다중 서버 환경에 최적화
- **세션 꼬임 방지**: 동일 도메인의 다른 포트 간 독립적 동작

### JWT 토큰 관리

- **HttpOnly 쿠키**: JavaScript에서 접근 불가능한 보안 쿠키 사용
- **System Code 기반**: `WAI_WEB_ADMIN`/`WAI_WEB_VIEW`에 따른 독립적 쿠키 관리
- **동적 만료시간**: 외부 서버에서 받은 `expires_in` 값 사용
- **Access Token**: 일반적인 API 호출에 사용 (`admin_access`/`webView_access`)
- **Refresh Token**: 토큰 갱신에 사용 (`admin_refresh`/`webView_refresh`)
- **Session Token**: 세션 관리용 (`admin_session`/`webView_session`)
- **자동 삭제**: 로그아웃 시 모든 토큰 쿠키 완전 삭제

### 헤더 기반 상태 관리

- **system_code**: 시스템 구분 (`WAI_WEB_ADMIN`, `WAI_WEB_VIEW`)
- **user_id**: 사용자 식별자
- **wai_lang**: 언어 설정 (`ko`, `en`)
- **자동 처리**: HttpUtil에서 헤더 기반 쿠키 자동 설정
- **LocalStorage 연동**: 프론트엔드에서 LocalStorage → 헤더 직접 매핑

### 에러 처리

- **외부 서버 오류**: 외부 인증 서버 오류 시에도 로컬 토큰 및 쿠키 삭제
- **상세한 에러 메시지**: 외부 서버의 `message` 또는 `detail` 필드에서 에러 메시지 추출
- **HTTP 상태 코드**: 모든 응답에 HTTP 상태 코드 포함

## 개발 환경 설정

### IDE 설정

#### VS Code 설정

`.vscode/settings.json`:

```json
{
  "terminal.integrated.encoding": "utf8",
  "terminal.integrated.env.windows": {
    "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
  }
}
```

`.vscode/launch.json`:

```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Debug Spring Boot",
      "request": "launch",
      "mainClass": "com.wai.admin.WaiAdminBackendApplication",
      "vmArgs": "-Dspring.profiles.active=dev -Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
    }
  ]
}
```

### 로깅 설정

`logback-spring.xml`에서 UTF-8 인코딩으로 로그 출력을 설정합니다.

### CORS 설정

- 전역 CORS 설정으로 모든 도메인에서 접근 가능
- 허용된 HTTP 메서드: GET, POST, PUT, DELETE, OPTIONS, PATCH
- 모든 헤더 허용 및 Credentials 지원

## 빌드 및 배포

### Docker 빌드 (멀티스테이지)

**Windows:**

```cmd
# 자동 빌드 및 실행
build-and-run-docker.bat

# 수동 빌드
docker build -t wai-backend:latest .
docker-compose up -d
```

**Linux:**

```bash
# 자동 빌드 및 실행
./build-and-run-docker.sh

# 수동 빌드
docker build -t wai-backend:latest .
docker-compose up -d
```

### JAR 파일 생성 (로컬)

**Windows:**

```cmd
.\mvnw.cmd clean package
```

**Linux:**

```bash
./mvnw clean package
# 또는
mvn clean package
```

### 실행 스크립트

- **Windows**: `run_backend.bat` - UTF-8 인코딩으로 애플리케이션 실행
- **Linux**: `run_backend.sh` - UTF-8 인코딩 및 한글 지원으로 애플리케이션 실행

### Docker 관리 도구

- **Windows**: `docker-commands.bat` - Docker 컨테이너 관리를 위한 대화형 도구
- **Linux**: `docker-commands.sh` - Docker 컨테이너 관리를 위한 대화형 도구

**제공 기능:**

- 컨테이너 상태 확인
- 실시간 로그 확인
- 컨테이너 중지/재시작
- 이미지 재빌드
- 전체 정리 (컨테이너 + 이미지 삭제)
- 컨테이너 내부 접속

## 문제 해결

### 한글 깨짐 문제

**Windows에서 한글이 깨지는 경우:**

1. **PowerShell에서 실행:**

   ```powershell
   chcp 65001
   $env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
   .\mvnw.cmd spring-boot:run
   ```

2. **배치 파일 사용 (권장):**

   ```cmd
   run_backend.bat
   ```

3. **Windows Terminal 사용** (권장)

**Linux에서 한글이 깨지는 경우:**

1. **셸 스크립트 사용 (권장):**

   ```bash
   ./run_backend.sh
   ```

2. **수동 설정:**
   ```bash
   export LC_ALL=C.UTF-8
   export LANG=C.UTF-8
   export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR"
   ./mvnw spring-boot:run
   ```

### Docker 관련 문제

**Docker 빌드 실패:**

- Docker가 실행 중인지 확인
- Docker 메모리 할당량 확인 (최소 4GB 권장)
- 네트워크 연결 확인

**컨테이너 접속 불가:**

- 포트 매핑 확인: `docker-compose ps`
- 방화벽 설정 확인
- 컨테이너 로그 확인: `docker-compose logs -f wai-backend`

### 외부 서버 연결 문제

- 외부 인증 서버(`192.168.233.128:8000`) 접근 가능 여부 확인
- 네트워크 방화벽 설정 확인
- `application.yml` 파일의 `auth.server.base-url` 설정 확인

## 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다.
