# Java 21 JRE 기반, 경량 Alpine
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리
WORKDIR /app

# 빌드된 JAR 복사
COPY build/libs/app.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-Xms512m", "-Xmx768m", "-jar", "/app/app.jar"]