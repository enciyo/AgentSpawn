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
→ Cursor CLI ile kod yazar
→ Commit eder ve push eder
→ Pull Request açar
```

Bu sayede yapay zeka kodlama ajanları **n8n**, **Temporal** veya herhangi bir HTTP istemcisi ile kullanılabilir hale gelir — yerel CLI araçlarını ölçeklenebilir, otomatikleştirilebilir servislere dönüştürür.

---

## Temel Özellikler

| | Özellik | Açıklama |
|---|---------|----------|
| 🔌 | **CLI Ajanları için REST API** | Cursor'ı (yakında Claude Code, Cline) HTTP uç noktaları olarak sunun |
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
     │  commit, push   │  │  (Claude Code) │  │  sohbet        │
     │  PR oluştur     │  │  (Cline)       │  │  geçmişi       │
     └────────────────┘  └────────────────┘  └────────────────┘
```

**Provision akışı (ajan oluşturulduğunda ne olur):**

1. Depoyu bir çalışma alanına klonla
2. Belirtilen referanstan yeni bir dal oluştur
3. CLI ajanını prompt ile çalıştır (ör. `cursor agent --resume`)
4. Ajanın değişiklik yapıp yapmadığını kontrol et
5. Ajan aracılığıyla commit mesajı oluştur
6. Commit et ve dala push et
7. Ajan aracılığıyla PR başlığı ve açıklaması oluştur
8. GitHub'da pull request aç

**Devam ettirme akışı:** Mevcut bir ajana takip prompt'u gönderin — dalı checkout eder, ajanı tekrar çalıştırır ve yeni değişiklikleri push eder.

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
│  Route: Agent · Shell · GitHub              │
│  CLI: AgentCli · GitCli · GitHubCli ·       │
│       ShellRunner · FileCli · SqlLiteCli    │
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

Yeni CLI ajanları, `AgentCli` arayüzü uygulanarak ve `DefaultAgentProviderFactory` içinde kayıt edilerek eklenir:

```kotlin
interface AgentCli {
    suspend fun login(): Either<AppError, ShellResult>
    suspend fun models(): Either<AppError, List<String>>
    suspend fun createChat(): Either<AppError, ShellResult>
    suspend fun resumeChat(id: String, prompt: String, model: String): Either<AppError, ShellResult>
    suspend fun prompt(id: String, prompt: String): Either<AppError, ShellResult>
    // ...
}
```

Şu anda desteklenen: **Cursor**. Sırada: **Claude Code**, **Cline**.

### Modüller

| Modül | Yol | Amaç |
|-------|-----|------|
| `composeApp` | `/composeApp` | Compose Multiplatform web ön yüz (JS + Wasm) |
| `server` | `/server` | Ajan, kabuk ve GitHub özellikleriyle Ktor REST API |
| `shared` | `/shared` | Kotlin Multiplatform paylaşımlı DTO, enum ve veri modelleri |
| `docs` | `/docs` | Otomatik oluşturulan Swagger/OpenAPI dokümantasyonu |

### Sunucu Katmanlama

Sunucu **Route → Service → Repository/CLI** desenini izler. Her özellik alanı (Agent, Shell, GitHub) kendi route, service ve veri erişim katmanıyla bağımsızdır.

---

## API Referansı

> Sunucu çalışırken `/swagger` ve `/redoc` adreslerinde etkileşimli dokümantasyona ulaşabilirsiniz.

### Ajan Uç Noktaları — `/agents`

| Metot | Yol | Açıklama |
|-------|-----|----------|
| `POST` | `/agents/create` | Ajan oluştur: depoyu klonla, CLI ajanını çalıştır, push et ve PR aç |
| `POST` | `/agents/resume` | Takip prompt'u ile ajana devam et |
| `GET` | `/agents/list` | Tüm ajanları listele |
| `GET` | `/agents/{id}` | Sohbet mesajlarıyla ajan detayını getir |
| `GET` | `/agents/status?provider=Cursor` | Ajan sağlayıcı doğrulama durumunu kontrol et |
| `GET` | `/agents/login?provider=Cursor` | Ajan sağlayıcısına giriş yap |
| `GET` | `/agents/logout?provider=Cursor` | Ajan sağlayıcısından çıkış yap |
| `GET` | `/agents/models?provider=Cursor` | Kullanılabilir modelleri listele |
| `GET` | `/agents/clear` | Tüm ajanları temizle |

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

---

## Yol Haritası

- [ ] **Claude Code sağlayıcısı** — Claude Code CLI için `AgentCli` uygulaması
- [ ] **Cline sağlayıcısı** — Cline CLI için `AgentCli` uygulaması
- [ ] **MCP desteği** — Ajan başına eklenebilir Model Context Protocol sunucuları
- [ ] **Beceriler ve Kurallar** — Yapılandırılabilir ajan becerileri ve kural setleri
- [ ] **Kod Tabanı İndeksleme** — Daha ucuz, bağlam farkındalıklı ajan kullanımı için depo indeksleme
- [ ] **WebSocket akışı** — Gerçek zamanlı ajan çıktı akışı
- [ ] **Çoklu kullanıcı desteği** — Rol tabanlı erişim denetimi
- [ ] **Görev kuyruğu ve zamanlama** — Ajan görevlerini kuyruğa alma ve zamanlama
- [ ] **Pano analizleri** — Belirteç kullanımı, başarı oranları, ajan performansı

---

## Başlangıç

### Ön Koşullar

- **JDK 23+** (Eclipse Temurin önerilir)
- **Docker** (isteğe bağlı, konteyner tabanlı dağıtım için)
- **PostgreSQL 17** (veya Docker Compose kullanın)
- **Cursor CLI** (Cursor sağlayıcısı için)
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
│   │   └── github/            # GitHub entegrasyonu
│   ├── cli/                   # CLI soyutlamaları
│   │   ├── agent/             # AgentCli arayüzü ve fabrika
│   │   └── providers/         # Sağlayıcı uygulamaları (Cursor)
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
