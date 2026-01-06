# WAI Backend Docker 배포 가이드

## 📋 개요

WAI Backend 애플리케이션을 Docker 컨테이너로 배포하기 위한 설정 파일들과 사용 방법을 정리한 문서입니다.

## 📋 사전 요구사항

- Docker Desktop 설치
- Docker Compose 설치
- Java 17 이상
- Maven 3.6 이상

## 📁 파일 구조

```
wai_backend/
├── Dockerfile                    # 도커 이미지 빌드 설정
├── .dockerignore                 # 도커 빌드 시 제외 파일
├── docker-compose.yml            # 컨테이너 오케스트레이션
├── build-and-run-docker.bat      # 자동 빌드/실행 스크립트
├── docker-commands.bat           # 도커 관리 도구
├── docker.md                     # 이 문서
├── src/main/resources/
│   └── application-docker.yml    # 도커 환경 설정
└── logs/                         # 로그 디렉토리 (볼륨 마운트)
```

## 🚀 빠른 시작

### 1. 자동 빌드 및 실행

```bash
# Windows
build-and-run-docker.bat

# 또는 수동으로
mvn clean package -DskipTests
docker-compose up -d
```

### 2. 수동 빌드 및 실행

```bash
# 1. Maven 빌드
mvn clean package -DskipTests

# 2. 도커 이미지 빌드
docker build -t wai-backend:latest .

# 3. 컨테이너 실행
docker-compose up -d
```

## 🐳 Dockerfile

### 목적

Spring Boot 애플리케이션을 도커 이미지로 빌드하기 위한 설정 파일

### 주요 설정

```dockerfile
# OpenJDK 17 베이스 이미지 사용
FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 애플리케이션 JAR 파일 복사
COPY target/*.jar app.jar

# 포트 노출 (Spring Boot 기본 포트)
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 특징

- **OpenJDK 17**: 최신 LTS 버전 사용
- **Slim 이미지**: 용량 최적화
- **포트 8080**: Spring Boot 기본 포트
- **JAR 실행**: 표준 Spring Boot 실행 방식

## 🚫 .dockerignore

### 목적

도커 빌드 시 불필요한 파일들을 제외하여 빌드 속도 향상 및 이미지 크기 최적화

### 주요 제외 항목

- Git 관련 파일 (`.git`, `.gitignore`)
- IDE 설정 파일 (`.idea`, `.vscode`)
- Maven 빌드 파일 (`target/`, `pom.xml.*`)
- 로그 파일 (`logs/`, `*.log`)
- OS 관련 파일 (`.DS_Store`, `Thumbs.db`)
- 문서 파일 (`README.md`, `*.md`)
- 배치 파일 (`*.bat`, `*.sh`)

## 🐙 docker-compose.yml

### 목적

도커 컨테이너의 오케스트레이션 및 환경 설정 관리

### 주요 설정

```yaml
version: "3.8"

services:
  wai-backend:
    build: . # 현재 디렉토리에서 빌드
    container_name: wai-backend # 컨테이너 이름
    ports:
      - "8080:8080" # 외부포트:내부포트
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xmx512m -Xms256m
    volumes:
      - ./logs:/app/logs # 로그 볼륨 마운트
    restart: unless-stopped # 자동 재시작
    networks:
      - wai-network # 네트워크 설정

networks:
  wai-network:
    driver: bridge
```

### 포트 매핑 설명

- **왼쪽 `8080`**: 외부 포트 (호스트에서 접근)
- **오른쪽 `8080`**: 내부 포트 (컨테이너 내부)

### 환경 변수

- `SPRING_PROFILES_ACTIVE=docker`: 도커 환경 프로파일 활성화
- `JAVA_OPTS=-Xmx512m -Xms256m`: JVM 메모리 설정

### 볼륨 마운트

- `./logs:/app/logs`: 호스트의 logs 디렉토리를 컨테이너의 /app/logs에 마운트

## 🚀 build-and-run-docker.bat

### 목적

Maven 빌드부터 도커 컨테이너 실행까지 전체 과정을 자동화

### 실행 과정

1. **Maven 빌드**: `mvn clean package -DskipTests`
2. **도커 이미지 빌드**: `docker build -t wai-backend:latest .`
3. **기존 컨테이너 정리**: `docker-compose down`
4. **새 컨테이너 실행**: `docker-compose up -d`

### 사용 방법

```bash
# Windows에서 실행
build-and-run-docker.bat
```

### 특징

- **자동화**: 수동 명령어 입력 불필요
- **에러 처리**: 각 단계별 실패 시 중단
- **사용자 친화적**: 진행 상황 표시

## 🛠️ docker-commands.bat

### 목적

도커 컨테이너 관리를 위한 대화형 도구

### 메뉴 구성

```
========================================
WAI Backend Docker 관리 도구
========================================

1. 컨테이너 상태 확인
2. 로그 확인
3. 컨테이너 재시작
4. 컨테이너 중지
5. 컨테이너 및 이미지 삭제
6. 컨테이너 내부 접속
7. 종료
```

### 각 기능 설명

#### 1. 컨테이너 상태 확인

```bash
docker-compose ps
```

- 실행 중인 컨테이너 목록과 상태 표시

#### 2. 로그 확인

```bash
docker-compose logs -f wai-backend
```

- 실시간 로그 스트리밍 (Ctrl+C로 종료)

#### 3. 컨테이너 재시작

```bash
docker-compose restart
```

- 애플리케이션 재시작 (설정 변경 후 유용)

#### 4. 컨테이너 중지

```bash
docker-compose down
```

- 컨테이너 중지 (데이터는 유지)

#### 5. 컨테이너 및 이미지 삭제

```bash
docker-compose down --rmi all --volumes --remove-orphans
```

- 완전 삭제 (이미지, 볼륨, 네트워크 모두 제거)

#### 6. 컨테이너 내부 접속

```bash
docker-compose exec wai-backend /bin/bash
```

- 컨테이너 내부로 터미널 접속

## ⚙️ application-docker.yml

### 목적

도커 환경에서 실행될 Spring Boot 애플리케이션의 설정

### 주요 설정

```yaml
server:
  port: 8080

spring:
  application:
    name: wai-admin-backend

  logging:
    level:
      com.wai.admin: DEBUG
      org.springframework.web: DEBUG
    pattern:
      console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
      file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file:
      name: logs/application.log
      max-size: 10MB
      max-history: 30

api:
  base-url: ${API_BASE_URL:http://localhost:3000}
  timeout: 30000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### 특징

- **로깅 설정**: 파일 및 콘솔 로깅
- **API 설정**: 외부 API 연결 설정
- **헬스체크**: 컨테이너 상태 모니터링
- **메트릭스**: 성능 모니터링

## 🔧 주요 명령어

### 컨테이너 관리

```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f wai-backend

# 컨테이너 재시작
docker-compose restart

# 컨테이너 중지
docker-compose down

# 컨테이너 및 이미지 삭제
docker-compose down --rmi all --volumes --remove-orphans
```

### 컨테이너 내부 접속

```bash
docker-compose exec wai-backend /bin/bash
```

### 리소스 모니터링

```bash
# 리소스 사용량 확인
docker stats wai-backend

# 최근 로그 확인
docker-compose logs --tail=100 wai-backend
```

## 🌐 접속 정보

### 기본 접속

- **애플리케이션 URL**: http://localhost:8080
- **헬스체크**: http://localhost:8080/actuator/health
- **메트릭스**: http://localhost:8080/actuator/metrics

### 포트 변경 시

```yaml
# docker-compose.yml에서 포트 변경
ports:
  - "9090:8080" # 외부 9090 포트로 변경
```

- **새 접속 URL**: http://localhost:9090

## ⚙️ 환경 변수

`docker-compose.yml`에서 다음 환경 변수를 설정할 수 있습니다:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - JAVA_OPTS=-Xmx512m -Xms256m
  - API_BASE_URL=http://your-api-server:3000
```

## 📊 모니터링

### 로그 확인

```bash
# 실시간 로그
docker-compose logs -f wai-backend

# 최근 100줄 로그
docker-compose logs --tail=100 wai-backend

# 특정 시간 이후 로그
docker-compose logs --since="2024-01-01T00:00:00" wai-backend

# 에러 로그만 확인
docker-compose logs wai-backend | grep ERROR
```

### 성능 모니터링

```bash
# 리소스 사용량 확인
docker stats wai-backend

# 컨테이너 정보
docker inspect wai-backend
```

## 🔍 문제 해결

### 1. 포트 충돌

```bash
# 8080 포트 사용 중인 프로세스 확인
netstat -ano | findstr :8080

# 다른 포트로 변경 (docker-compose.yml 수정)
ports:
  - "8081:8080"
```

### 2. 메모리 부족

```bash
# JVM 힙 메모리 조정 (docker-compose.yml)
environment:
  - JAVA_OPTS=-Xmx1024m -Xms512m
```

### 3. 로그 디렉토리 권한

```bash
# Windows에서 로그 디렉토리 생성
mkdir logs
```

### 4. 컨테이너 시작 실패

```bash
# 상세 로그 확인
docker-compose logs wai-backend

# 컨테이너 재빌드
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## 🧹 정리

### 완전 삭제

```bash
# 컨테이너, 이미지, 볼륨 모두 삭제
docker-compose down --rmi all --volumes --remove-orphans
docker system prune -a
```

### 로그 정리

```bash
# 로그 파일 삭제
rm -rf logs/*
```

### 도커 시스템 정리

```bash
# 사용하지 않는 리소스 정리
docker system prune
```

## 📝 참고사항

### 성능 최적화

- **메모리 설정**: 애플리케이션 크기에 맞게 조정
- **로그 로테이션**: 디스크 공간 절약
- **이미지 크기**: 멀티스테이지 빌드 고려

### 보안 고려사항

- **네트워크 격리**: 필요한 포트만 노출
- **환경 변수**: 민감한 정보는 환경 변수로 관리
- **권한 최소화**: 필요한 권한만 부여

### 개발 환경 vs 운영 환경

- **개발**: 디버그 로그, 자동 재시작
- **운영**: 프로덕션 로그, 리소스 제한

### 도커 컨테이너 특징

- 도커 컨테이너는 `wai-backend`라는 이름으로 실행됩니다
- 로그는 `./logs` 디렉토리에 마운트됩니다
- 컨테이너는 자동으로 재시작됩니다 (`restart: unless-stopped`)
- 개발 환경에서는 `application-docker.yml` 프로파일이 사용됩니다

## 🎯 사용 시나리오

### 개발 환경

1. `build-and-run-docker.bat` 실행
2. `docker-commands.bat`로 로그 확인
3. 코드 수정 후 재빌드

### 운영 환경

1. `docker-compose up -d` 실행
2. 헬스체크로 상태 확인
3. 로그 모니터링

### 문제 해결

1. `docker-commands.bat`로 상태 확인
2. 로그 분석
3. 필요시 컨테이너 재시작

## 🚀 실행 예시

### 전체 과정 예시

```bash
# 1. 자동 빌드 및 실행
build-and-run-docker.bat

# 2. 상태 확인
docker-compose ps

# 3. 로그 확인
docker-compose logs -f wai-backend

# 4. 브라우저에서 접속
# http://localhost:8080

# 5. 헬스체크 확인
# http://localhost:8080/actuator/health
```

### 문제 발생 시

```bash
# 1. 관리 도구 실행
docker-commands.bat

# 2. 메뉴에서 적절한 옵션 선택
# - 상태 확인: 1번
# - 로그 확인: 2번
# - 재시작: 3번
# - 완전 삭제: 5번
```
