# Docker 배포 가이드

이 문서는 Docker를 사용하여 애플리케이션을 배포할 때 PROD와 DEV 환경을 구분하는 방법을 설명합니다.

## 프로파일 설정 방법

### 1. docker-compose 파일 사용 (권장)

#### PROD 환경 실행

```bash
# 빌드 및 실행
docker-compose -f docker-compose.prod.yml up -d --build

# 로그 확인
docker-compose -f docker-compose.prod.yml logs -f

# 중지
docker-compose -f docker-compose.prod.yml down
```

#### DEV 환경 실행

```bash
# 빌드 및 실행
docker-compose -f docker-compose.dev.yml up -d --build

# 로그 확인
docker-compose -f docker-compose.dev.yml logs -f

# 중지
docker-compose -f docker-compose.dev.yml down
```

#### 기본 docker 프로파일 실행

```bash
# docker-compose.yml 사용 (docker 프로파일)
docker-compose up -d --build
```

### 2. docker run 명령어 사용

#### PROD 환경

```bash
# 이미지 빌드
docker build -t wai-backend:latest .

# 컨테이너 실행
docker run -d \
  --name wai-backend-prod \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  -v $(pwd)/logs:/app/logs \
  --restart unless-stopped \
  wai-backend:latest
```

#### DEV 환경

```bash
# 이미지 빌드
docker build -t wai-backend:latest .

# 컨테이너 실행
docker run -d \
  --name wai-backend-dev \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  -v $(pwd)/logs:/app/logs \
  --restart unless-stopped \
  wai-backend:latest
```

### 3. 환경 변수 파일 사용 (.env)

#### .env.prod 파일 생성

```env
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xmx512m -Xms256m
TZ=Asia/Seoul
```

#### .env.dev 파일 생성

```env
SPRING_PROFILES_ACTIVE=dev
JAVA_OPTS=-Xmx512m -Xms256m
TZ=Asia/Seoul
```

#### 사용 방법

```bash
# PROD 환경
docker-compose --env-file .env.prod -f docker-compose.prod.yml up -d

# DEV 환경
docker-compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

## 프로파일별 설정 파일

각 프로파일은 다음 설정 파일을 사용합니다:

- **PROD**: `application-prod.yml` → `api-prod.properties`
- **DEV**: `application-dev.yml` → `api-dev.properties` (또는 `api.properties`)
- **DOCKER**: `application-docker.yml` → `api.properties`

## 환경 변수 우선순위

Spring Boot는 다음 순서로 프로파일을 결정합니다:

1. `SPRING_PROFILES_ACTIVE` 환경 변수
2. `-Dspring.profiles.active` JVM 옵션
3. `application.yml`의 기본 프로파일

## 확인 방법

컨테이너 실행 후 로그에서 프로파일 확인:

```bash
# docker-compose 사용 시
docker-compose -f docker-compose.prod.yml logs | grep "The following profiles are active"

# docker run 사용 시
docker logs wai-backend-prod | grep "The following profiles are active"
```

예상 출력:

```
The following profiles are active: prod
```

## 주의사항

1. **PROD 환경 배포 전 확인사항:**

   - `api-prod.properties`의 실제 운영 서버 주소 설정 확인
   - `auth.server.base-url` 값 확인
   - 로깅 레벨이 INFO로 설정되어 있는지 확인

2. **포트 충돌 방지:**

   - PROD와 DEV를 동시에 실행할 경우 포트를 다르게 설정
   - 예: PROD는 8080, DEV는 8081

3. **볼륨 마운트:**
   - 로그 파일은 호스트와 공유하여 확인 가능
   - DEV 환경에서는 소스 코드 마운트로 핫 리로드 가능 (선택사항)

## 스크립트 사용 (Windows)

### build-and-run-docker.bat 수정 예시

```batch
@echo off
set PROFILE=%1
if "%PROFILE%"=="" set PROFILE=prod

echo Building and running with profile: %PROFILE%

if "%PROFILE%"=="prod" (
    docker-compose -f docker-compose.prod.yml up -d --build
) else if "%PROFILE%"=="dev" (
    docker-compose -f docker-compose.dev.yml up -d --build
) else (
    docker-compose up -d --build
)

echo Container started with profile: %PROFILE%
```

사용법:

```batch
build-and-run-docker.bat prod
build-and-run-docker.bat dev
```

## 스크립트 사용 (Linux/Mac)

### build-and-run-docker.sh 수정 예시

```bash
#!/bin/bash
PROFILE=${1:-prod}

echo "Building and running with profile: $PROFILE"

if [ "$PROFILE" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml up -d --build
elif [ "$PROFILE" == "dev" ]; then
    docker-compose -f docker-compose.dev.yml up -d --build
else
    docker-compose up -d --build
fi

echo "Container started with profile: $PROFILE"
```

사용법:

```bash
chmod +x build-and-run-docker.sh
./build-and-run-docker.sh prod
./build-and-run-docker.sh dev
```
