# 멀티스테이지 빌드: 빌드 스테이지
FROM eclipse-temurin:17-jdk-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Maven 설치 (Alpine 기반)
RUN apk add --no-cache maven

# 프로젝트 파일 복사
COPY pom.xml .

# 의존성 다운로드 (캐시 최적화)
RUN mvn dependency:go-offline -B

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 스킵)
RUN mvn clean package -DskipTests

# 실행 스테이지
FROM eclipse-temurin:17-jre-alpine

# 한글 폰트 및 타임존 설정 (Alpine 기반)
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    tzdata

# 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 스테이지에서 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행 (UTF-8 인코딩 및 메모리 최적화)
# SPRING_PROFILES_ACTIVE 환경 변수로 프로파일 설정 가능 (기본값: prod)
# JAVA_OPTS 환경 변수로 JVM 옵션 설정 가능 (docker-compose에서 설정)
# PROD 환경 권장: -Xmx2048m -Xms1024m
# DEV 환경 권장: -Xmx1024m -Xms512m
ENTRYPOINT ["sh", "-c", "java \
    -Dfile.encoding=UTF-8 \
    -Duser.language=ko \
    -Duser.country=KR \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    ${JAVA_OPTS:--Xmx1024m -Xms512m} \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -jar app.jar"] 