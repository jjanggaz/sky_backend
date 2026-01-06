#!/bin/bash

# UTF-8 인코딩 설정
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

show_menu() {
    echo "========================================"
    echo "WAI Backend Docker 관리 도구 (Linux)"
    echo "========================================"
    echo ""
    echo "1. 컨테이너 상태 확인"
    echo "2. 컨테이너 로그 확인"
    echo "3. 컨테이너 중지"
    echo "4. 컨테이너 재시작"
    echo "5. 이미지 재빌드"
    echo "6. 전체 정리 (컨테이너 + 이미지 삭제)"
    echo "7. 컨테이너 내부 접속"
    echo "8. 종료"
    echo ""
    echo -n "선택하세요 (1-8): "
}

while true; do
    show_menu
    read choice
    
    case $choice in
        1)
            echo ""
            echo "=== 컨테이너 상태 확인 ==="
            docker-compose ps
            echo ""
            ;;
        2)
            echo ""
            echo "=== 실시간 로그 확인 (Ctrl+C로 종료) ==="
            docker-compose logs -f wai-backend
            echo ""
            ;;
        3)
            echo ""
            echo "=== 컨테이너 중지 ==="
            docker-compose down
            echo "컨테이너가 중지되었습니다."
            echo ""
            ;;
        4)
            echo ""
            echo "=== 컨테이너 재시작 ==="
            docker-compose restart wai-backend
            echo "컨테이너가 재시작되었습니다."
            echo ""
            ;;
        5)
            echo ""
            echo "=== 이미지 재빌드 및 컨테이너 재시작 ==="
            docker-compose down
            docker build -t wai-backend:latest .
            if [ $? -eq 0 ]; then
                docker-compose up -d
                echo "이미지 재빌드 및 컨테이너 재시작이 완료되었습니다."
            else
                echo "이미지 빌드에 실패했습니다."
            fi
            echo ""
            ;;
        6)
            echo ""
            echo "=== 전체 정리 ==="
            echo -n "모든 컨테이너와 이미지를 삭제하시겠습니까? (y/N): "
            read confirm
            if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
                docker-compose down --rmi all --volumes --remove-orphans
                echo "전체 정리가 완료되었습니다."
            else
                echo "취소되었습니다."
            fi
            echo ""
            ;;
        7)
            echo ""
            echo "=== 컨테이너 내부 접속 ==="
            echo "컨테이너가 실행 중인지 확인합니다..."
            if docker-compose ps | grep -q "wai-backend.*Up"; then
                echo "컨테이너에 접속합니다... (exit 명령으로 나가기)"
                docker-compose exec wai-backend /bin/sh
            else
                echo "컨테이너가 실행 중이지 않습니다."
                echo "먼저 'docker-compose up -d' 명령으로 컨테이너를 시작하세요."
            fi
            echo ""
            ;;
        8)
            echo ""
            echo "프로그램을 종료합니다."
            exit 0
            ;;
        *)
            echo ""
            echo "잘못된 선택입니다. 1-8 중에서 선택하세요."
            echo ""
            ;;
    esac
done
