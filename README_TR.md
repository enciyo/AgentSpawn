<div align="center">

# AgentSpawn

**Yapay zeka kodlama ajanlarını programlanabilir HTTP servislerine dönüştürün.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.4.0-087CFA?logo=ktor&logoColor=white)](https://ktor.io)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.3-4285F4?logo=jetbrains&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org)

[English](README.md) | Türkçe

</div>

---

## AgentSpawn Nedir?

**Cursor**, **Claude Code** ve **Cline** gibi yapay zeka kodlama ajanları güçlüdür — ancak CLI araçlarına ve yerel terminallere bağımlıdırlar. Bir iş akışından çağıramazsınız, bir pipeline'a ekleyemezsiniz ya da ölçeklendiremezsiniz.

**AgentSpawn**, bu CLI ajanlarını bir REST API arkasına sarar. Bir prompt ve depo gönderin — AgentSpawn depoyu klonlar, ajanı başlatır, kod yazdırır, değişiklikleri commit eder, dalı push eder ve pull request açar. Tek bir API çağrısıyla tam otomasyon.

```
POST /agents/create
{
  "prompt": "Kullanıcı kayıt endpoint'ine giriş doğrulaması ekle",
  "repository": "https://github.com/your-org/your-repo",
  "ref": "main",
  "branchName": "feat/input-validation",
  "provider": "Cursor",
  "model": "auto"
}

→ Ajan depoyu klonlar
→ Dal oluşturur
→ Ajan ile kod yazar (ACP üzerinden Cursor veya Claude Code)
→ Commit eder ve push eder
→ Pull Request açar
```

Bu sayede yapay zeka kodlama ajanları **n8n**, **Temporal** veya herhangi bir HTTP istemcisi ile kullanılabilir hale gelir — yerel CLI araçlarını ölçeklenebilir, otomatikleştirilebilir servislere dönüştürür.

---

## Temel Özellikler

| | Özellik | Açıklama |
|---|---------|----------|
| 🔌 | **CLI Ajanları için REST API** | Cursor ve Claude Code'u (yakında Cline) HTTP uç noktaları olarak sunun |
| 🧩 | **Agent Client Protocol** | Tüm sağlayıcılar tek bir ortak istemciyle [ACP](https://agentclientprotocol.com) (stdio üzerinden JSON-RPC) ile yönetilir |
| 📡 | **Canlı Akış** | Ajan çıktısı (mesajlar, düşünceler, araç çağrıları) Server-Sent Events olarak akar |
| 🛠️ | **MCP Sunucuları** | Model Context Protocol sunucularını `/mcp` ile yönetin; ajanlara otomatik iletilir |
| 🔄 | **Tam Git Otomasyonu** | Klonla → dal oluştur → kodla → commit → push → PR, tek bir API çağrısıyla |
| 🤖 | **Ajan Yaşam Döngüsü** | Ajanları kalıcı durum ile oluşturun, devam ettirin, izleyin ve yönetin |
| 🐙 | **GitHub Entegrasyonu** | Depo klonlama, dal yönetimi ve otomatik PR oluşturma |
| 🔗 | **Orkestrasyon Desteği** | n8n, Temporal veya herhangi bir iş akışı motoru ile REST üzerinden kullanın |
| 💬 | **Web Panosu** | Gerçek zamanlı sohbet ve ajan izleme için Compose Multiplatform arayüzü |
| 🐘 | **PostgreSQL Kalıcılığı** | Flyway geçiş dosyalarıyla ajan durumu ve sohbet geçmişi |
| 🔐 | **API Anahtarı Doğrulama** | Uç noktaları güvence altına almak için isteğe bağlı API anahtarı |
| 🐳 | **Docker Compose** | PostgreSQL, API sunucu ve web arayüzü tek komutla ayağa kalkar |

---

## Nasıl Çalışır?

```
                         ┌─────────────────────┐
  n8n / HTTP İstemci ───▶│   REST API (Ktor)   │
                         └────────┬────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │      AgentService          │
                    │  provision → resumeProvision│
                    └─────────────┬─────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                    │
     ┌────────▼───────┐  ┌───────▼────────┐  ┌───────▼────────┐
     │  GitHub CLI     │  │  Agent CLI     │  │  PostgreSQL    │
     │  klonla, dal    │  │  Cursor        │  │  durum ve      │
     │  commit, push   │  │  Claude Code   │  │  sohbet        │
     │  PR oluştur     │  │  (ACP ile)     │  │  geçmişi       │
     └────────────────┘  └────────────────┘  └────────────────┘
```

**Provision akışı (ajan oluşturulduğunda ne olur):**

1. Depoyu bir çalışma alanına klonla
2. Belirtilen referanstan yeni bir dal oluştur
3. Bir ACP oturumu aç (veya yükle) ve prompt'u gönder; ilerleme `/agents/{id}/stream` üzerinden akar
4. Ajanın değişiklik yapıp yapmadığını kontrol et
5. Ajan aracılığıyla commit mesajı oluştur
6. Commit et ve dala push et
7. Ajan aracılığıyla PR başlığı ve açıklaması oluştur
8. GitHub'da pull request aç

**Devam ettirme akışı:** Mevcut bir ajana takip prompt'u gönderin — dalı checkout eder, konuşmanın devam etmesi için ajanın ACP oturumunu yeniden yükler (`session/load`) ve yeni değişiklikleri push eder.

`/agents/create` ve `/agents/resume` hemen döner; çalışma arka planda sürer. Canlı takip için `/agents/{id}/stream`'e bağlanın — geç bağlanan bir istemci de çalışmanın tamamını baştan alır.

---

## Mimari

```
┌─────────────────────────────────────────────┐
│           composeApp (Web Arayüzü)          │
│   Compose Multiplatform · JS/Wasm hedefleri │
└──────────────────┬──────────────────────────┘
                   │ HTTP (Ktor Client)
┌──────────────────▼──────────────────────────┐
│           server (Ktor Backend)             │
│  Route: Agent · Shell · GitHub · MCP        │
│  CLI: AgentCli · AcpAgent · GitCli ·        │
│       GitHubCli · ShellRunner · FileCli     │
│  DI: Koin  ·  FP: Arrow                    │
└──────────────────┬──────────────────────────┘
                   │ JDBC (Exposed + Flyway)
┌──────────────────▼──────────────────────────┐
│           PostgreSQL Veritabanı             │
└─────────────────────────────────────────────┘

         ┌─────────────────────┐
         │   shared (KMP DTO)  │
         │  Hem UI hem sunucu  │
         │  tarafından kullanılır│
         └─────────────────────┘
```

### Sağlayıcı Sistemi

Tüm sohbet, geçmiş ve model işlemleri [Agent Client Protocol](https://agentclientprotocol.com) üzerinden yapılır. Bir sağlayıcı yalnızca ACP ajanını başlatan komutu ve ACP'nin kapsamadığı hesap/dosya işlemlerini sağlar:

```kotlin
interface AgentCli {
    val acp: AcpAgent  // ACP üzerinden sohbet, geçmiş, modeller

    suspend fun login(): Either<AppError, ShellResult>
    suspend fun logout(): Either<AppError, ShellResult>
    suspend fun status(): Either<AppError, ShellResult>
    suspend fun listChats(): Either<AppError, List<String>>
    suspend fun clear()
}
```

Yeni sağlayıcılar `AgentCli`'yi uygular ve `DefaultAgentProviderFactory` içinde kayıt edilir.

| Sağlayıcı | ACP ajan komutu |
|-----------|-----------------|
| **Cursor** | `agent acp` (Cursor CLI'a dahil) |
| **Claude Code** | `npx -y @agentclientprotocol/claude-agent-acp` (resmi adaptör) |

Sırada: **Cline**.

ACP istemcisi `server/cli/acp` altındadır: `AcpConnection` ajanın stdio'su üzerinden JSON-RPC 2.0'ı yönetir (izin istekleri otomatik onaylanır), `AcpAgent` ise bunun üzerine `prompt`, `history`, `ask` ve `models` sunar. İstek ve cevaplar tipli `@Serializable` data class'lardır; metot adları `AcpMethod` enum'undan gelir.

### Modüller

| Modül | Yol | Amaç |
|-------|-----|------|
| `composeApp` | `/composeApp` | Compose Multiplatform web ön yüz (JS + Wasm) |
| `server` | `/server` | Ajan, kabuk, GitHub ve MCP özellikleriyle Ktor REST API |
| `shared` | `/shared` | Kotlin Multiplatform paylaşımlı DTO, enum ve veri modelleri |
| `docs` | `/docs` | Otomatik oluşturulan Swagger/OpenAPI dokümantasyonu |

### Sunucu Katmanlama

Sunucu **Route → Service → Repository/CLI** desenini izler. Her özellik alanı (Agent, Shell, GitHub, MCP) kendi route, service ve veri erişim katmanıyla bağımsızdır.

---

## API Referansı

> Sunucu çalışırken `/swagger` ve `/redoc` adreslerinde etkileşimli dokümantasyona ulaşabilirsiniz.

### Ajan Uç Noktaları — `/agents`

| Metot | Yol | Açıklama |
|-------|-----|----------|
| `POST` | `/agents/create` | Ajan oluştur: depoyu klonla, CLI ajanını çalıştır, push et ve PR aç |
| `POST` | `/agents/resume` | Takip prompt'u ile ajana devam et |
| `GET` | `/agents/list` | Tüm ajanları listele |
| `GET` | `/agents/{id}` | Sohbet geçmişiyle (ACP oturumundan yeniden oynatılır) ajan detayını getir |
| `GET` | `/agents/{id}/stream` | Ajan çalışmasının Server-Sent Events akışı (`AgentStreamEvent`) |
| `GET` | `/agents/status?provider=Cursor` | Ajan sağlayıcı doğrulama durumunu kontrol et |
| `GET` | `/agents/login?provider=Cursor` | Ajan sağlayıcısına giriş yap |
| `GET` | `/agents/logout?provider=Cursor` | Ajan sağlayıcısından çıkış yap |
| `GET` | `/agents/models?provider=Cursor` | Kullanılabilir modelleri listele |
| `GET` | `/agents/clear` | Tüm ajanları temizle |

`provider` değeri `Cursor` veya `Claude` olabilir.

**Akış olayları** (`AgentStreamEvent`): `Update` (bir ACP `session/update`: mesaj/düşünce parçaları, araç çağrıları), `TurnStarted`, `TurnEnded` (ACP `stopReason` ile), `Error` ve `Done` (commit/push/PR dahil çalışma bitti).

### Kabuk Uç Noktaları — `/shell`

| Metot | Yol | Açıklama |
|-------|-----|----------|
| `POST` | `/shell/create` | İsteğe bağlı zaman aşımıyla kabuk komutu çalıştır |

### GitHub Uç Noktaları — `/github`

| Metot | Yol | Açıklama |
|-------|-----|----------|
| `GET` | `/github/status` | GitHub CLI doğrulama durumu |
| `GET` | `/github/login` | GitHub CLI ile giriş yap |
| `GET` | `/github/logout` | GitHub CLI'dan çıkış yap |
| `GET` | `/github/repo` | Depoları listele |
| `GET` | `/github/repo/search?q=&limit=` | Depo ara |
| `POST` | `/github/branch` | Bir depo için dal ara |
| `GET` | `/github/token` | GitHub doğrulama belirtecini al |

### MCP Uç Noktaları — `/mcp`

| Metot | Yol | Açıklama |
|-------|-----|----------|
| `GET` | `/mcp` | MCP yapılandırmasını getir |
| `PUT` | `/mcp` | MCP yapılandırmasını değiştir |
| `POST` | `/mcp/servers` | MCP sunucusu ekle veya güncelle |
| `DELETE` | `/mcp/servers/{name}` | MCP sunucusunu kaldır |

Yapılandırma `~/.agentspawn/mcp.json` dosyasında tutulur ve ACP `session/new` / `session/load` ile Claude Code'a iletilir; ayrıca Cursor'ın kendisinin okuduğu `~/.cursor/mcp.json` dosyasına da yazılır.

---

## Yol Haritası

- [x] **Claude Code sağlayıcısı** — Resmi ACP adaptörü üzerinden Claude Code
- [x] **Agent Client Protocol** — Tüm sağlayıcılar ACP ile yönetiliyor
- [ ] **Cline sağlayıcısı** — Cline CLI için `AgentCli` uygulaması
- [x] **MCP desteği** — Eklenebilir Model Context Protocol sunucuları
- [ ] **Beceriler ve Kurallar** — Yapılandırılabilir ajan becerileri ve kural setleri
- [ ] **Kod Tabanı İndeksleme** — Daha ucuz, bağlam farkındalıklı ajan kullanımı için depo indeksleme
- [x] **Canlı akış** — Server-Sent Events ile gerçek zamanlı ajan çıktısı
- [ ] **Çoklu kullanıcı desteği** — Rol tabanlı erişim denetimi
- [ ] **Görev kuyruğu ve zamanlama** — Ajan görevlerini kuyruğa alma ve zamanlama
- [ ] **Pano analizleri** — Belirteç kullanımı, başarı oranları, ajan performansı

---

## Başlangıç

### Ön Koşullar

- **JDK 23+** (Eclipse Temurin önerilir)
- **Docker** (isteğe bağlı, konteyner tabanlı dağıtım için)
- **PostgreSQL 17** (veya Docker Compose kullanın)
- **Cursor CLI** (Cursor sağlayıcısı için; `agent acp` komutunu sağlar)
- **Claude Code** + **Node.js / npx** (Claude sağlayıcısı için; ACP adaptörü `npx` ile çalışır)
- Gradle wrapper dahildir — ayrı kurulum gerekmez

### Docker Compose ile Hızlı Başlangıç

```bash
docker compose up --build
```

Bu komut şunları başlatır:
- **PostgreSQL** — port `5432`
- **Sunucu (Ktor API)** — port `8080`
- **Web Arayüzü (Compose JS)** — port `3000`

### Manuel Kurulum

```bash
# Sunucuyu başlat
./gradlew :server:run

# Web arayüzünü başlat (birini seçin)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Wasm (daha hızlı, modern tarayıcılar)
./gradlew :composeApp:jsBrowserDevelopmentRun        # JS (daha geniş destek)
```

### Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `PORT` | `8080` | Sunucu portu |
| `KTOR_ENV` | `development` | Ktor ortamı (`development` / `production`) |
| `POSTGRES_HOST` | `localhost` | Veritabanı sunucusu |
| `POSTGRES_PORT` | `5432` | Veritabanı portu |
| `POSTGRES_DB` | `ktor_tutorial_db` | Veritabanı adı |
| `POSTGRES_USER` | `postgresql` | Veritabanı kullanıcısı |
| `POSTGRES_PASSWORD` | `password` | Veritabanı şifresi |
| `API_KEY` | `default` | API doğrulama anahtarı |

### Docker (Tekli İmajlar)

```bash
docker build --target server -t agentspawn-server .
docker build --target web -t agentspawn-web .
docker build -t agentspawn-cursor .    # Cursor CLI içeren geliştirme imajı
```

---

## Teknoloji Yığını

| Teknoloji | Sürüm | Amaç |
|-----------|-------|------|
| Kotlin | 2.3.0 | Programlama dili |
| Compose Multiplatform | 1.10.3 | Web arayüz çerçevesi |
| Ktor | 3.4.0 | HTTP sunucu ve istemci |
| Koin | 4.2.0 | Bağımlılık enjeksiyonu |
| Arrow | 2.2.2 | Fonksiyonel hata yönetimi |
| Exposed | 1.1.1 | Kotlin SQL ORM |
| Flyway | 11.8.2 | Veritabanı geçişleri |
| PostgreSQL | 42.7.7 | Veritabanı sürücüsü |
| kotlinx-serialization | 1.10.0 | JSON serileştirme |

## Proje Yapısı

```
AgentSpawn/
├── composeApp/                # Compose Multiplatform Web Arayüzü
│   └── src/webMain/           # Web hedefi (JS + Wasm)
│       ├── ui/features/       # Chat, Splash, Bileşenler
│       ├── data/              # HTTP istemci (Ktor)
│       └── di/                # Koin DI modülleri
├── server/                    # Ktor Backend
│   ├── features/
│   │   ├── agent/             # Ajan CRUD ve yaşam döngüsü
│   │   ├── shell/             # Kabuk komutu çalıştırma
│   │   ├── github/            # GitHub entegrasyonu
│   │   └── mcp/               # MCP sunucu yapılandırması
│   ├── cli/                   # CLI soyutlamaları
│   │   ├── acp/               # Agent Client Protocol istemcisi (stdio üzerinden JSON-RPC)
│   │   ├── agent/             # AgentCli arayüzü ve fabrika
│   │   └── providers/         # Sağlayıcı uygulamaları (Cursor, Claude)
│   ├── configs/               # Ktor eklenti yapılandırmaları
│   └── resources/             # application.conf, Flyway geçişleri
├── shared/                    # KMP Paylaşımlı Modeller
│   └── api/                   # DTO, enum, istek/yanıt modelleri
├── docs/                      # OpenAPI dokümantasyonu
└── gradle/                    # Derleme yapılandırması
```

---

## Katkıda Bulunma

1. Depoyu **fork** edin
2. Bir **dal oluşturun** (`git checkout -b feature/harika-ozellik`)
3. Değişikliklerinizi **commit** edin
4. **Push** yapın ve bir **Pull Request** açın

### Kod Kuralları

- Sunucu modülünde hata yönetimi için **Arrow** (`Either<AppError, T>`) kullanın
- **Route → Service → Repository/CLI** mimari desenine bağlı kalın
- Yeni ajan sağlayıcıları `AgentCli` arayüzünü uygulamalı ve `DefaultAgentProviderFactory` içinde kayıt edilmelidir

---

<div align="center">

Kotlin, Ktor ve Compose Multiplatform ile geliştirilmiştir

</div>
