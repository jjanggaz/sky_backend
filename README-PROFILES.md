# 프로파일 설정 가이드

이 프로젝트는 Spring Boot 프로파일을 사용하여 환경별 설정을 관리합니다.

## 프로파일 종류

- **local**: 로컬 개발 환경
- **dev**: 개발 환경
- **prod**: 운영 환경
- **docker**: Docker 환경

## 프로파일별 Properties 파일

### api-local.properties
로컬 개발 환경용 설정 파일
- 외부 인증 서버: `http://192.168.233.128:8000`
- 디버그 로깅 활성화

### api-prod.properties
운영 환경용 설정 파일
- 운영 서버 주소 설정 필요
- 프로덕션 로깅 레벨

## 프로파일 활성화 방법

### 1. IDE에서 실행 시

**IntelliJ IDEA:**
1. Run/Debug Configurations 열기
2. Active profiles에 `local` 또는 `prod` 입력
3. 또는 VM options에 `-Dspring.profiles.active=local` 추가

**Eclipse:**
1. Run Configurations 열기
2. Arguments 탭에서 VM arguments에 `-Dspring.profiles.active=local` 추가

### 2. 명령줄에서 실행 시

```bash
# LOCAL 프로파일
java -jar -Dspring.profiles.active=local wai-admin-backend.jar

# PROD 프로파일
java -jar -Dspring.profiles.active=prod wai-admin-backend.jar

# 여러 프로파일 동시 활성화
java -jar -Dspring.profiles.active=local,dev wai-admin-backend.jar
```

### 3. Maven으로 실행 시

```bash
# LOCAL 프로파일
mvn spring-boot:run -Dspring-boot.run.profiles=local

# PROD 프로파일
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. 환경 변수로 설정

```bash
# Windows
set SPRING_PROFILES_ACTIVE=local

# Linux/Mac
export SPRING_PROFILES_ACTIVE=local
```

### 5. application.yml에서 기본 프로파일 설정

```yaml
spring:
  profiles:
    active: local  # 기본 프로파일
```

## 프로파일별 설정 파일 우선순위

Spring Boot는 다음 순서로 설정 파일을 로드합니다:

1. `application.yml` (기본 설정)
2. `application-{profile}.yml` (프로파일별 설정, 기본 설정 오버라이드)
3. Properties 파일은 YAML 설정에서 import됨

## 설정 파일 구조

```
src/main/resources/
├── application.yml              # 기본 설정
├── application-local.yml        # LOCAL 프로파일 설정
├── application-prod.yml         # PROD 프로파일 설정
├── application-dev.yml          # DEV 프로파일 설정
├── application-docker.yml       # DOCKER 프로파일 설정
├── api.properties               # 기본 API 설정 (fallback)
├── api-local.properties         # LOCAL API 설정
└── api-prod.properties          # PROD API 설정
```

## 주의사항

1. **api-prod.properties** 파일의 실제 운영 서버 주소를 반드시 수정하세요.
   - `auth.server.base-url`: 운영 인증 서버 URL
   - `api.base-url`: 운영 API 서버 URL

2. 프로파일이 활성화되지 않으면 기본적으로 `api.properties`가 사용됩니다.

3. 프로파일별 설정은 기본 설정을 오버라이드합니다.

## 프로파일 확인

애플리케이션 실행 시 로그에서 활성화된 프로파일을 확인할 수 있습니다:

```
The following profiles are active: local
```

또는 `/api/test/config` 엔드포인트를 호출하여 현재 설정을 확인할 수 있습니다.

