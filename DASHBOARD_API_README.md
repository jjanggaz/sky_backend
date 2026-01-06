# 서버 정보 대시보드 API

## 개요

이 API는 서버의 시스템 정보, 프로젝트 통계, 3D 모델 통계, 승인 통계 등을 포함한 종합적인 대시보드 데이터를 제공합니다.

## API 엔드포인트

### 1. 서버 정보 대시보드 조회

```
GET /api/dashboard/server
```

**응답 예시:**

```json
{
  "success": true,
  "message": "대시보드 데이터를 성공적으로 가져왔습니다.",
  "data": {
    "serverStatus": {
      "capacity": {
        "total": 107374182400,
        "used": 53687091200,
        "free": 53687091200
      },
      "cpu": {
        "usage": 25,
        "cores": 8,
        "temperature": 45
      },
      "ram": {
        "total": 17179869184,
        "used": 8589934592,
        "free": 8589934592,
        "usage": 50
      },
      "system": {
        "uptime": 86400,
        "loadAverage": [1.2, 1.1, 0.9],
        "osInfo": "Windows 10 10.0"
      }
    },
    "projectStats": {
      "total": 10,
      "inProgress": 5,
      "completed": 3
    },
    "modelStats": {
      "total": 25,
      "pending": 8,
      "approved": 15
    },
    "approvalStats": {
      "total": 30,
      "pending": 12,
      "approved": 15,
      "rejected": 3
    }
  }
}
```

## 수집되는 정보

### 서버 상태 (ServerStatus)

- **용량 (Capacity)**: 디스크 총 용량, 사용량, 여유 공간
- **CPU**: 사용률, 코어 수, 온도 (Linux 환경에서만)
- **RAM**: 총 메모리, 사용량, 여유 공간, 사용률
- **시스템**: 가동 시간, 로드 평균, 운영체제 정보

### 프로젝트 통계 (ProjectStats)

- 총 프로젝트 수
- 진행 중인 프로젝트 수
- 완료된 프로젝트 수

### 3D 모델 통계 (ModelStats)

- 총 모델 수
- 대기 중인 모델 수
- 승인된 모델 수

### 승인 통계 (ApprovalStats)

- 총 승인 요청 수
- 대기 중인 승인 요청 수
- 승인된 요청 수
- 거부된 요청 수

## 주의사항

1. **CPU 온도**: Linux 환경에서만 실제 값을 가져올 수 있으며, Windows 환경에서는 0으로 표시됩니다.

2. **메모리 정보**: Java 9+ 환경에서 `com.sun.management.OperatingSystemMXBean`을 통해 수집됩니다.

3. **디스크 정보**:

   - Windows: C: 드라이브 정보
   - Linux/Unix: 루트 디렉토리 정보

4. **로드 평균**: OS에서 제공하는 제한적인 정보를 기반으로 1분, 5분, 15분 평균을 시뮬레이션합니다.

## 에러 처리

API 호출 시 오류가 발생하면 다음과 같은 응답을 받습니다:

```json
{
  "success": false,
  "message": "서버 내부 오류가 발생했습니다.",
  "error": "오류 상세 내용"
}
```

## 사용 예시

### JavaScript (Fetch API)

```javascript
fetch("/api/dashboard/server")
  .then((response) => response.json())
  .then((data) => {
    if (data.success) {
      console.log("서버 CPU 사용률:", data.data.serverStatus.cpu.usage + "%");
      console.log("메모리 사용률:", data.data.serverStatus.ram.usage + "%");
      console.log(
        "디스크 사용률:",
        Math.round(
          (data.data.serverStatus.capacity.used /
            data.data.serverStatus.capacity.total) *
            100
        ) + "%"
      );
    }
  })
  .catch((error) => console.error("Error:", error));
```

### cURL

```bash
curl -X GET "http://localhost:8080/api/dashboard/server" \
  -H "Accept: application/json"
```

## 개발 환경

- Java 17+
- Spring Boot 3.2.0
- Spring Web MVC
- SLF4J 로깅

## 향후 개선 사항

1. 실제 데이터베이스 연동 (프로젝트, 모델, 승인 통계)
2. 실시간 모니터링을 위한 WebSocket 지원
3. 히스토리 데이터 저장 및 추이 분석
4. 알림 기능 (임계값 초과 시)
5. 더 정확한 CPU 온도 정보 수집 (Windows 환경)
