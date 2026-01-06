#!/bin/bash

# UTF-8 인코딩 및 한글 지원 설정
export LC_ALL=C.UTF-8
export LANG=C.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

echo "========================================"
echo "WAI Backend 개발 서버 실행 (Linux)"
echo "========================================"
echo ""
echo "[INFO] UTF-8 인코딩으로 Spring Boot 애플리케이션을 시작합니다..."
echo "[INFO] 개발 프로필(dev)을 사용합니다."
echo ""

# Maven Wrapper 확인
if [ -f "./mvnw" ]; then
    echo "[INFO] Maven Wrapper(Unix)를 사용합니다."
    chmod +x ./mvnw
    MAVEN_CMD="./mvnw"
elif [ -f "./mvnw.cmd" ]; then
    echo "[WARNING] Windows용 Maven Wrapper만 발견되었습니다."
    echo "[INFO] 시스템 Maven을 사용합니다."
    MAVEN_CMD="mvn"
elif command -v mvn &> /dev/null; then
    echo "[INFO] 시스템 Maven을 사용합니다."
    MAVEN_CMD="mvn"
else
    echo "[ERROR] Maven을 찾을 수 없습니다!"
    echo "[ERROR] Maven을 설치하거나 Maven Wrapper를 설정하세요."
    exit 1
fi

echo ""
echo "========================================"
echo "[INFO] Spring Boot 애플리케이션을 시작합니다..."
echo "[INFO] 종료하려면 Ctrl+C를 누르세요."
echo "========================================"
echo ""

# Spring Boot 실행
$MAVEN_CMD spring-boot:run -Dspring-boot.run.profiles=dev
