@echo off
chcp 65001 >nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
echo ========================================
echo    WAI Admin Backend 실행 스크립트
echo ========================================
echo.
echo Spring Boot 애플리케이션을 시작합니다...
echo 서버가 시작되면 http://localhost:8080 으로 접속하세요.
echo.
echo 종료하려면 Ctrl+C를 누르세요.
echo.

REM Maven Wrapper를 사용하여 Spring Boot 실행
call .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

echo.
echo 애플리케이션이 종료되었습니다.
pause 