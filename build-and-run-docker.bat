@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM 프로파일 설정 (기본값: prod)
set PROFILE=%1
if "%PROFILE%"=="" set PROFILE=prod

echo ========================================
echo WAI Backend Docker 빌드 및 실행
echo 프로파일: %PROFILE%
echo ========================================

echo.
echo Maven 확인 중...

REM Maven Wrapper 확인
if exist "mvnw.cmd" (
    echo [INFO] Maven Wrapper를 사용합니다.
    set MAVEN_CMD=.\mvnw.cmd
) else (
    echo [INFO] Maven Wrapper가 없습니다. 시스템 Maven을 사용합니다.
    set MAVEN_CMD=mvn
)

echo.
echo [1/3] 도커 이미지 빌드 시작... (멀티스테이지 빌드)
echo [INFO] Maven 빌드는 Docker 내부에서 수행됩니다.
docker build -t wai-backend:latest .

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 도커 이미지 빌드 실패!
    echo [ERROR] Docker가 실행 중인지 확인하세요.
    pause
    exit /b 1
)

echo.
echo [2/3] 기존 컨테이너 중지 및 제거...
if "%PROFILE%"=="prod" (
    docker-compose -f docker-compose.prod.yml down
) else if "%PROFILE%"=="dev" (
    docker-compose -f docker-compose.dev.yml down
) else (
    docker-compose down
)

echo.
echo [3/3] 도커 컨테이너 실행 (프로파일: %PROFILE%)...
if "%PROFILE%"=="prod" (
    docker-compose -f docker-compose.prod.yml up -d
) else if "%PROFILE%"=="dev" (
    docker-compose -f docker-compose.dev.yml up -d
) else (
    docker-compose up -d
)

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 도커 컨테이너 실행 실패!
    echo [ERROR] Docker Compose 설정을 확인하세요.
    pause
    exit /b 1
)

echo.
echo ========================================
echo [SUCCESS] 도커 컨테이너가 성공적으로 실행되었습니다!
echo 프로파일: %PROFILE%
echo ========================================
echo.
echo [INFO] 애플리케이션 URL: http://localhost:8080
echo [INFO] 헬스체크: http://localhost:8080/actuator/health
if "%PROFILE%"=="prod" (
    echo [INFO] 로그 확인: docker-compose -f docker-compose.prod.yml logs -f
    echo [INFO] 컨테이너 중지: docker-compose -f docker-compose.prod.yml down
) else if "%PROFILE%"=="dev" (
    echo [INFO] 로그 확인: docker-compose -f docker-compose.dev.yml logs -f
    echo [INFO] 컨테이너 중지: docker-compose -f docker-compose.dev.yml down
) else (
    echo [INFO] 로그 확인: docker-compose logs -f wai-backend
    echo [INFO] 컨테이너 중지: docker-compose down
)
echo [INFO] 관리 도구: docker-commands.bat
echo.
echo 사용법:
echo   build-and-run-docker.bat prod  - PROD 환경 실행
echo   build-and-run-docker.bat dev   - DEV 환경 실행
echo   build-and-run-docker.bat       - 기본(docker) 프로파일 실행
echo.
pause 