#!/bin/bash

# UTF-8 인코딩 설정
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

# 프로파일 설정 (기본값: prod)
PROFILE=${1:-prod}

echo "========================================"
echo "WAI Backend Docker 빌드 및 실행"
echo "프로파일: $PROFILE"
echo "========================================"

echo ""
echo "[1/3] 도커 이미지 빌드 시작... (멀티스테이지 빌드)"
echo "[INFO] Maven 빌드는 Docker 내부에서 수행됩니다."
docker build -t wai-backend:latest .

if [ $? -ne 0 ]; then
    echo "[ERROR] 도커 이미지 빌드 실패!"
    echo "[ERROR] Docker가 실행 중인지 확인하세요."
    exit 1
fi

echo ""
echo "[2/3] 기존 컨테이너 중지 및 제거..."
if [ "$PROFILE" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml down
elif [ "$PROFILE" == "dev" ]; then
    docker-compose -f docker-compose.dev.yml down
else
    docker-compose down
fi

echo ""
echo "[3/3] 도커 컨테이너 실행 (프로파일: $PROFILE)..."
if [ "$PROFILE" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml up -d
elif [ "$PROFILE" == "dev" ]; then
    docker-compose -f docker-compose.dev.yml up -d
else
    docker-compose up -d
fi

if [ $? -ne 0 ]; then
    echo "[ERROR] 도커 컨테이너 실행 실패!"
    echo "[ERROR] Docker Compose 설정을 확인하세요."
    exit 1
fi

echo ""
echo "========================================"
echo "[SUCCESS] 도커 컨테이너가 성공적으로 실행되었습니다!"
echo "프로파일: $PROFILE"
echo "========================================"
echo ""
echo "[INFO] 애플리케이션 URL: http://localhost:8080"
echo "[INFO] 헬스체크: http://localhost:8080/actuator/health"
if [ "$PROFILE" == "prod" ]; then
    echo "[INFO] 로그 확인: docker-compose -f docker-compose.prod.yml logs -f"
    echo "[INFO] 컨테이너 중지: docker-compose -f docker-compose.prod.yml down"
elif [ "$PROFILE" == "dev" ]; then
    echo "[INFO] 로그 확인: docker-compose -f docker-compose.dev.yml logs -f"
    echo "[INFO] 컨테이너 중지: docker-compose -f docker-compose.dev.yml down"
else
    echo "[INFO] 로그 확인: docker-compose logs -f wai-backend"
    echo "[INFO] 컨테이너 중지: docker-compose down"
fi
echo "[INFO] 관리 도구: ./docker-commands.sh"
echo ""
echo "사용법:"
echo "  ./build-and-run-docker.sh prod  - PROD 환경 실행"
echo "  ./build-and-run-docker.sh dev   - DEV 환경 실행"
echo "  ./build-and-run-docker.sh       - 기본(docker) 프로파일 실행"
echo ""
