# Active Context — AgentSpawn

## Teşhis (2026-10-18) — sürekli `agent status --print`
- Kaynak: `ChatViewModel.pollAgents()` → `refreshProviderGroups()` → `client.statusAgent` → `GET /agents/status` → `agent status --print`
- Interval: **10s** (`poll(KEY_AGENTS, 10_000)`) — 3s’den yükseltildi
- Amaç: drawer’da provider login + chat listesini güncel tutmak

## Son odak (2026-10-18)
MCP JSON EditText: basit syntax highlight (key/string/number/keyword).

## UI (2026-10-18) — JSON highlight (A)
- `JsonSyntaxHighlightVisualTransformation` + `AppTextField.visualTransformation`
- Geçersiz JSON’da border error rengi

## Önceki odak (2026-10-18)
Agent list boş: sync wipe bug + nested Cursor chats path.

## Bugfix (2026-10-18) — No Agents Yet
- DB `agent` tablosu 0 row; drawer sadece DB’den okur (ham Cursor IDE chat’leri değil).
- `sync()`: `listChats` fail → `emptyList` → tüm agent’ları siliyordu.
- Fix: list fail/empty iken wipe yok; `listChats` → `~/.cursor/chats/*/*/` basename.

## Önceki odak (2026-10-18)
CORS: PUT/DELETE eklendi — MCP Save "Failed to fetch" fix.

## Bugfix (2026-10-18) — MCP Save Failed to fetch
- Kök: CORS default’ta PUT yok → browser preflight fail.
- Fix: `CorsConfig` → allow Put + Delete (+ Get/Post/Options).

## Önceki odak (2026-10-18)
Customize MCP tab: ham `mcp.json` EditText + Save (`GET/PUT /mcp`).

## UI (2026-10-18) — MCP JSON editor
- `Client.getMcp` / `putMcp`
- `CustomizeViewModel`: load → pretty JSON; save → parse + PUT
- MCP tab: monospace multiline field + Save (dirty)

## Önceki odak (2026-10-18)
MCP file-backed API: `~/.agentspawn/mcp.json` tek kaynak.

## API (2026-10-18) — MCP CRUD (1A/2B/3A)
- Canonical: `~/.agentspawn/mcp.json` (Cursor-uyumlu `mcpServers` map)
- Routes: `GET/PUT /mcp`, `POST /mcp/servers`, `DELETE /mcp/servers/{name}`
- Write-through: her yazmada `~/.cursor/mcp.json`
- Eski `GET /agents/mcp` + `AgentCli.getMcpConfigs` kaldırıldı
- FE/Customize henüz bağlı değil

## Önceki odak (2026-10-18)
Drawer footer version `x.x.x.x` — last digit build counter.

## UI (2026-10-18) — drawer version
- `composeApp/version.properties` (major/minor/patch/build)
- Gradle `generateAppVersion`: her compile öncesi build +1, `AppVersion.NAME` üretir
- Drawer en altta `AppVersion.NAME` gösterir

## Önceki odak (2026-10-18)
New Agent: drawer chat listesi flicker fix.

## Bugfix (2026-10-18) — New Agent list flicker
- Kök: `onNewAgent` tüm jobs’ı iptal + boş `DrawerState` + `loadInitialData` → liste silinip yeniden geliyordu.
- Fix: `KEY_AGENT` iptal; `providerGroups` korunur; sadece `selectedAgentId`/messages/input sıfırlanır; `loadRepositories()` çağrılır.

## Önceki odak (2026-10-18)
Drawer → Customize: sol drawer kalır, ortadaki chat pane Customize’a geçer.

## UI (2026-10-18) — Customize pane (iskelet)
- `MainPane` (Chat | Customize) `ChatUiState` içinde.
- Drawer: Home → **Customize** → New Agent; Customize seçiliyken highlight.
- Orta alan: `CustomizeScreen` — TabRow MCP | Skills (placeholder metin).
- Agent / New Agent / provider seçimi → `MainPane.Chat`’e döner.
- API bağlanmadı (MCP server endpoint var, FE henüz yok).

## Önceki odak (2026-10-11)
New agent StatusLine: modelden önce provider seçici.

## UI (2026-10-11) — StatusLine provider before model
- Provider dropdown model’in solunda; new agent’ta editable, chat başladıysa disable.
- Provider değişince modeller clear + yeniden yüklenir.
- Model selector sadece provider seçiliyken görünür.

## Önceki (2026-10-11)
Drawer: gruplama toggle + boş provider gruplarını gizle.

## UI (2026-10-11) — drawer grouping toggle
- `DrawerState.isGrouped` (default true) + `onToggleGrouping`.
- Toggle kapalı → flat agent listesi.
- Gruplu modda sadece `agents.isNotEmpty()` gruplar görünür (Coming soon / empty yok).

## Önceki (2026-10-11)
Multi-provider drawer — FE iskeleti implement edildi (2. provider CLI yok).

## Implement (2026-10-11) — multi-provider drawer (FE-only)
- `ProviderCatalog`: Cursor (wired) + ChatGPT / Claude Code / Gemini (Coming soon section).
- `ChatViewModel.refreshProviderGroups`: paralel `statusAgent` + tek `listAgents` → `ProviderGroup` listesi.
- Drawer: provider section header + chat list / Not connected / Coming soon.
- `selectedProviderId`: agent seçimi / section tıklama / create+models buna bağlı.
- Yeni provider eklemek için: enum+CLI sonra `ProviderCatalog` satırında `agentProvider` set et.
- API değişmedi. Server `sync()` hâlâ Cursor-only (ileride multi-sync gerekir).

## Önceki odak (2026-10-11)
StatusLine loading iken kilitleniyor.

## UI (2026-10-11) — StatusLine lock on loading
- `isLoading` = message loading / status active / pending / input disabled.
- Loading’de repo/branch/target/model disable; send gizlenir.
- ViewModel `isStatusLoading()` guard.

## Önceki (2026-10-11)
TargetBranch width bug: repo/branch scroll dışına kaçıyordu.

## Bugfix (2026-10-11) — input yazınca repo/branch kaybolması
- Kök neden: `TargetBranch` `Int.MAX_VALUE.dp` + `horizontalScroll` unbounded → repo/branch ekran dışına kayıyordu.
- Fix: max 200.dp, density-aware büyüyen width; Int.MAX_VALUE kaldırıldı.

## Önceki (2026-10-11)
StatusLine repo/branch/targetBranch enable kuralları (isChatStarted).

## UI (2026-10-11) — StatusLine edit rules
- Repo/branch: chat başlamadıysa enable; başladıysa disable ama görünür; veri yoksa gizle.
- Target branch: chat öncesi set edilir; chat sonrası read-only (`isEnabled = !isChatStarted`).
- ViewModel guard: `isChatStarted` iken select/expand/change ignore.

## Önceki (2026-10-11)
StatusLine repo/branch tıklanamama bugfix.

## Bugfix (2026-10-11) — dropdown disable
- Kök neden: `pollAgent` `repositories/branches = emptyList()` yapıyordu; chip `isEnable = list.isNotEmpty()` ile kilitleniyordu (görünür ama tıklanamaz).
- Fix: listeleri poll'da silme; chip her zaman enable; açılınca liste boşsa `onExpandRepo/Branch` ile yeniden yükle.

## UI (2026-10-11) — StatusLine
- Alt bar `StatusLine.kt` olarak chat'ten ayrıldı (`StatusLineActions`).
- Sol: repo / branch / target branch; sağ: model dropdown + send.
- `InputScreen` sadece text input; `ChatRoute` içinde `InputScreen` + `StatusLine` kardeş.

## Önceki odak (2026-10-11)
Android Studio'da iki Gradle Run aynı anda çalışmıyor.

## Teşhis
- Tek proje (`AgentSpawn`) üzerinde Gradle exclusive project lock kullanır.
- `:server:run` (Gradle Application) çalışırken daemon BUSY kalır ve kilidi bırakmaz.
- İkinci Gradle görevi (composeApp / Android sync-build) kilit bekler → "iki gradle aynı anda yok".
- Anlık durum örneği: daemon `9812 BUSY`, server process Gradle child olarak ayakta.

## Uygulanan çözüm (2026-10-11)
- Android Studio Application run config: `.idea/runConfigurations/Server_Application.xml`
- Adı: **Server** — main: `org.enciyo.agentspawn.ApplicationKt`, module: `AgentSpawn.server.main`
- Make ile kısa compile sonrası JVM kendi process'inde koşar → Gradle proje kilidi serbest kalır
- composeApp / Android Gradle Run ile paralel kullanılabilir
- Not: `.idea/` gitignore'da; config local. Repo'ya almak istenirse `.idea/runConfigurations/` exception eklenebilir.

## Takılma teşhisi (wasmJs loading)
- Belirti: `Executing 'wasmJsBrowserDevelopmentRun'...` sonra sessiz loading.
- Kullanıcı `AgentSpawn [:server:...ApplicationKt.main()]` ile açtı → bu **Gradle** run; tab adında `Gradle.` / `:server:` var.
- Daemon BUSY + server child of GradleDaemon → wasmJs kilit bekler.
- Fix: Gradle server Stop → dropdown'dan **Server (IDEA)** → sonra wasmJs.
- `.idea/gradle.xml`: `delegatedBuild=false` (Build and run using IntelliJ IDEA) — gutter Run'ın Gradle'a düşmesini azaltır.

## Not
`gradle/gradle-daemon-jvm.properties` local değişmiş (toolchain URL + `toolchainVendor=JETBRAINS` kaldırılmış); karışık daemon JVM (AS JBR vs JDK 25) yan etki olabilir, ana sebep değil.

## Web research (2026-10-11) — paralel Gradle run
Kaynaklar: Google 396841270, YouTrack KMT-1342, Gradle #34113, SO/Reddit.
- Android Studio bilerek Gradle run config'leri serialize eder (concurrent Gradle unsafe).
- IntelliJ IDEA bazen paralel Gradle başlatır; Google/JB: AS doğru, IJ riskli/verimsiz.
- Gradle short/medium fix yok. JB KMP: ideal çözüm otomatik JVM Application run config.
- Community: installDist + dış script; terminal/--no-daemon; server'ı IDEA Application ile çalıştır; Allow multiple instances işe yaramaz.

## Web research (2026-10-11) — paralel Gradle run
Kaynaklar: Google [396841270](https://issuetracker.google.com/issues/396841270), YouTrack [KMT-1342](https://youtrack.jetbrains.com/issue/KMT-1342), Gradle [#34113](https://github.com/gradle/gradle/issues/34113), SO/Reddit.
- Android Studio bilerek Gradle run config'leri serialize eder (concurrent Gradle unsafe).
- IntelliJ IDEA bazen paralel Gradle başlatır; Google/JB: AS doğru, IJ riskli/verimsiz.
- Gradle short/medium fix yok. JB KMP: ideal çözüm otomatik **JVM Application** run config.
- Community workaround: `installDist` + dış script; terminal/`--no-daemon`; server'ı IDEA Application ile çalıştır; Allow multiple instances işe yaramaz.
