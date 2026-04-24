# Stage 1: Build (pode usar AMD64 para compilar)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (ARM64)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Configuração de timezone
RUN apt-get update && \
    apt-get install -y tzdata && \
    ln -sf /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copiar JAR do stage de build
COPY --from=builder /build/target/*.jar app.jar

# Criar usuário não-root
RUN useradd -r -u 1001 -g root rappidrive && \
    chown -R rappidrive:root /app

USER rappidrive

# Configuração JVM otimizada para ARM64
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
