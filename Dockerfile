# =============================================================================
# AgentSpawn — Kotlin Multiplatform
# Server (Ktor API) ve Web (Compose JS) ayrı servisler.
# Varsayılan build: son stage (cursor). docker-compose ile server + web ayağa kalkar.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Builder — server + Compose JS web build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jdk AS builder

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradlew gradlew.bat ./
RUN chmod +x gradlew

COPY server server
COPY shared shared
COPY composeApp composeApp

ENV GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m"

RUN ./gradlew :server:installDist :composeApp:jsBrowserProductionWebpack --no-daemon --no-build-cache --parallel --console=plain

# -----------------------------------------------------------------------------
# Stage 2: Server — sadece Ktor API (JRE)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jre AS server

WORKDIR /app
COPY --from=builder /app/server/build/install/server /app

ENV SERVER_PORT=8080
EXPOSE 8080

# -----------------------------------------------------------------------------
# Stage 3: Web — Compose Multiplatform JS (Gradle ile çalıştırma)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jdk AS web

WORKDIR /app
COPY --from=builder /app .

ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

EXPOSE 3000

# Jetpack Compose KMP web: Gradle run task (webpack dev server)
# --console=plain: Docker'da log tamponlamasını kapatır, ilerleme görünür
CMD ["./gradlew", ":composeApp:jsBrowserProductionRun", "--no-daemon", "--console=plain"]

# -----------------------------------------------------------------------------
# Stage 4: Cursor — geliştirme (varsayılan: docker build .)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jdk AS cursor

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl bash && \
    rm -rf /var/lib/apt/lists/*
RUN curl -fsSL https://cursor.com/install | bash
ENV PATH="/root/.cursor/bin:${PATH}"
RUN agent --version

WORKDIR /app
COPY --from=builder /app /app

CMD ["bash"]
