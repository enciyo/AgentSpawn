# syntax=docker/dockerfile:1
# =============================================================================
# AgentSpawn — Kotlin Multiplatform
# Server ve Web ayrı builder stage'leri: webpack server image'ına girmez.
# BuildKit cache mount ile Gradle/Kotlin cache kalır (--no-build-cache YOK).
# =============================================================================

FROM eclipse-temurin:23-jdk AS base

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradlew gradlew.bat ./
RUN chmod +x gradlew

ENV GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m"

# -----------------------------------------------------------------------------
# Server builder — sadece :server:installDist (webpack yok)
# -----------------------------------------------------------------------------
FROM base AS server-builder

COPY server server
COPY shared shared
# settings.gradle.kts include(":composeApp") nedeniyle gerekli
COPY composeApp composeApp

RUN --mount=type=cache,id=agentspawn-gradle-server,target=/root/.gradle \
    --mount=type=cache,id=agentspawn-kotlin-server,target=/root/.kotlin \
    ./gradlew :server:installDist --no-daemon --console=plain

# -----------------------------------------------------------------------------
# Web builder — sadece Compose JS production webpack
# -----------------------------------------------------------------------------
FROM base AS web-builder

COPY server server
COPY shared shared
COPY composeApp composeApp

# Yarn/npm TTY yoksa sessiz kalır; CI=true progress'i kapatır
ENV CI=false \
    npm_config_loglevel=http \
    YARN_SILENT=0

# Ayrı layer: yarn paket indirme — --info her paketi basar
RUN --mount=type=cache,id=agentspawn-gradle-web,target=/root/.gradle \
    --mount=type=cache,id=agentspawn-kotlin-web,target=/root/.kotlin \
    stdbuf -oL -eL ./gradlew :kotlinNpmInstall --no-daemon --console=plain --info

RUN --mount=type=cache,id=agentspawn-gradle-web,target=/root/.gradle \
    --mount=type=cache,id=agentspawn-kotlin-web,target=/root/.kotlin \
    stdbuf -oL -eL ./gradlew :composeApp:jsBrowserProductionWebpack --no-daemon --console=plain

# Kotlin/JS çıktı yolu sürümler arası değişebilir — normalize et
RUN set -eux; \
    mkdir -p /out; \
    if [ -d composeApp/build/dist/js/productionExecutable ]; then \
      cp -a composeApp/build/dist/js/productionExecutable/. /out/; \
    elif [ -d composeApp/build/kotlin-webpack/js/productionExecutable ]; then \
      cp -a composeApp/build/kotlin-webpack/js/productionExecutable/. /out/; \
    else \
      echo "Webpack output not found"; \
      find composeApp/build -name 'index.html' -print || true; \
      exit 1; \
    fi; \
    # webMain resources (index.html/styles.css) webpack dist'e her zaman düşmeyebilir
    cp -f composeApp/src/webMain/resources/index.html /out/index.html; \
    if [ -f composeApp/src/webMain/resources/styles.css ]; then \
      cp -f composeApp/src/webMain/resources/styles.css /out/styles.css; \
    fi

# -----------------------------------------------------------------------------
# Server runtime — Ktor API (JRE)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jre AS server

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl git bash sqlite3 && \
    curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
      -o /usr/share/keyrings/githubcli-archive-keyring.gpg && \
    chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
      > /etc/apt/sources.list.d/github-cli.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends gh && \
    curl -fsSL https://cursor.com/install | bash && \
    rm -rf /var/lib/apt/lists/*

# Install script: ~/.local/bin (yeni) veya ~/.cursor/bin (eski)
ENV PATH="/root/.local/bin:/root/.cursor/bin:${PATH}"
RUN agent --version

WORKDIR /app
COPY --from=server-builder /app/server/build/install/server /app

ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=5s --start-period=30s --retries=12 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

# -----------------------------------------------------------------------------
# Web runtime — static nginx (Gradle runtime YOK)
# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS web

COPY deploy/nginx-web.conf /etc/nginx/conf.d/default.conf
COPY --from=web-builder /out/ /usr/share/nginx/html/

EXPOSE 3000

# -----------------------------------------------------------------------------
# Cursor — geliştirme (varsayılan: docker build .)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:23-jdk AS cursor

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl bash ca-certificates && \
    curl -fsSL https://cursor.com/install | bash && \
    rm -rf /var/lib/apt/lists/*
ENV PATH="/root/.local/bin:/root/.cursor/bin:${PATH}"
RUN agent --version

WORKDIR /app
COPY --from=base /app /app
COPY server server
COPY shared shared
COPY composeApp composeApp

CMD ["bash"]
