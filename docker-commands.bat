@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo WAI Backend Docker 관리 도구
echo ========================================

:menu
echo.
echo 1. 컨테이너 상태 확인
echo 2. 로그 확인
echo 3. 컨테이너 재시작
echo 4. 컨테이너 중지
echo 5. 컨테이너 및 이미지 삭제
echo 6. 컨테이너 내부 접속
echo 7. 종료
echo.
set /p choice="선택하세요 (1-7): "

if "%choice%"=="1" goto status
if "%choice%"=="2" goto logs
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto stop
if "%choice%"=="5" goto cleanup
if "%choice%"=="6" goto exec
if "%choice%"=="7" goto exit
goto menu

:status
echo.
echo [INFO] 컨테이너 상태 확인 중...
docker-compose ps
echo.
pause
goto menu

:logs
echo.
echo [INFO] 로그 확인 중... (Ctrl+C로 종료)
docker-compose logs -f wai-backend
goto menu

:restart
echo.
echo [INFO] 컨테이너 재시작 중...
docker-compose restart
echo [SUCCESS] 재시작 완료!
pause
goto menu

:stop
echo.
echo [INFO] 컨테이너 중지 중...
docker-compose down
echo [SUCCESS] 중지 완료!
pause
goto menu

:cleanup
echo.
echo [INFO] 컨테이너 및 이미지 삭제 중...
docker-compose down --rmi all --volumes --remove-orphans
echo [SUCCESS] 삭제 완료!
pause
goto menu

:exec
echo.
echo [INFO] 컨테이너 내부 접속 중...
docker-compose exec wai-backend /bin/bash
goto menu

:exit
echo.
echo [INFO] 종료합니다.
exit /b 0 