<div align="center">

# AgentSpawn

**Turn AI coding agents into programmable HTTP services.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.4.0-087CFA?logo=ktor&logoColor=white)](https://ktor.io)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.3-4285F4?logo=jetbrains&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org)

English | [Türkçe](README_TR.md)

</div>

---

## What Is AgentSpawn?

AI coding agents like **Cursor**, **Claude Code**, and **Cline** are powerful — but they're locked behind CLIs and local terminals. You can't call them from a workflow, chain them in a pipeline, or manage them at scale.

**AgentSpawn** wraps these CLI agents behind a REST API. Send a prompt and a repository — AgentSpawn clones the repo, spins up the agent, lets it write code, commits the changes, pushes to a branch, and opens a pull request. One API call, full automation.

```
POST /agents/create
{
  "prompt": "Add input validation to the user registration endpoint",
  "repository": "https://github.com/your-org/your-repo",
  "ref": "main",
  "branchName": "feat/input-validation",
  "provider": "Cursor",
  "model": "auto"
}

→ Agent clones repo
→ Creates branch
→ Writes code via the agent (Cursor or Claude Code, over ACP)
→ Commits & pushes
→ Opens a Pull Request
```

This makes AI coding agents usable from orchestration tools like **n8n**, **Temporal**, or any HTTP client — turning local CLI tools into scalable, automatable services.

---

## Key Features

| | Feature | Description |
|---|---------|-------------|
| 🔌 | **REST API for CLI Agents** | Expose Cursor and Claude Code (soon Cline) as HTTP endpoints |
| 🧩 | **Agent Client Protocol** | Every provider is driven over [ACP](https://agentclientprotocol.com) (JSON-RPC over stdio) with one shared client |
| 📡 | **Live Streaming** | Agent output (messages, thoughts, tool calls) streamed as Server-Sent Events |
| 🛠️ | **MCP Servers** | Manage Model Context Protocol servers via `/mcp`; they are passed to agents automatically |
| 🔄 | **Full Git Automation** | Clone → branch → code → commit → push → PR in a single API call |
| 🤖 | **Agent Lifecycle** | Create, resume, monitor, and manage agents with persistent state |
| 🐙 | **GitHub Integration** | Repository cloning, branch management, and automatic PR creation |
| 🔗 | **Orchestration Ready** | Use with n8n, Temporal, or any workflow engine via REST |
| 💬 | **Web Dashboard** | Compose Multiplatform UI for real-time chat and agent monitoring |
| 🐘 | **PostgreSQL Persistence** | Agent state and chat history persisted with Flyway migrations |
| 🔐 | **API Key Auth** | Optional API key authentication for securing endpoints |
| 🐳 | **Docker Compose** | One-command deployment with PostgreSQL, API server, and web UI |

---

## How It Works

```
                         ┌─────────────────────┐
  n8n / HTTP Client ────▶│   REST API (Ktor)   │
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
     │  clone, branch  │  │  Cursor        │  │  state &       │
     │  commit, push   │  │  Claude Code   │  │  chat history  │
     │  PR creation    │  │  (over ACP)    │  │                │
     └────────────────┘  └────────────────┘  └────────────────┘
```

**Provision flow (what happens when you create an agent):**

1. Clone the repository to a workspace
2. Create a new branch from the specified ref
3. Open (or load) an ACP session and send your prompt; progress is streamed to `/agents/{id}/stream`
4. Check if the agent made changes
5. Generate a commit message via the agent
6. Commit and push to the branch
7. Generate a PR title and body via the agent
8. Create a pull request on GitHub

**Resume flow:** Send a follow-up prompt to an existing agent — it checks out the branch, reloads the agent's ACP session (`session/load`) so the conversation continues, and pushes new changes.

`/agents/create` and `/agents/resume` return immediately; the run continues in the background. Connect to `/agents/{id}/stream` to follow it live — a client that connects late still receives the whole run from the start.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│           composeApp (Web UI)               │
│   Compose Multiplatform · JS/Wasm targets   │
└──────────────────┬──────────────────────────┘
                   │ HTTP (Ktor Client)
┌──────────────────▼──────────────────────────┐
│           server (Ktor Backend)             │
│  Routes: Agent · Shell · GitHub · MCP       │
│  CLI: AgentCli · AcpAgent · GitCli ·        │
│       GitHubCli · ShellRunner · FileCli     │
│  DI: Koin  ·  FP: Arrow                    │
└──────────────────┬──────────────────────────┘
                   │ JDBC (Exposed + Flyway)
┌──────────────────▼──────────────────────────┐
│           PostgreSQL Database                │
└─────────────────────────────────────────────┘

         ┌─────────────────────┐
         │   shared (KMP DTOs) │
         │  Used by both UI &  │
         │   server modules    │
         └─────────────────────┘
```

### Provider System

All chat, history and model operations go through the [Agent Client Protocol](https://agentclientprotocol.com). A provider only supplies the command that starts its ACP agent plus the account/file operations ACP doesn't cover:

```kotlin
interface AgentCli {
    val acp: AcpAgent  // chat, history, models over ACP

    suspend fun login(): Either<AppError, ShellResult>
    suspend fun logout(): Either<AppError, ShellResult>
    suspend fun status(): Either<AppError, ShellResult>
    suspend fun listChats(): Either<AppError, List<String>>
    suspend fun clear()
}
```

New providers implement `AgentCli` and register in `DefaultAgentProviderFactory`.

| Provider | ACP agent command |
|----------|-------------------|
| **Cursor** | `agent acp` (built into Cursor CLI) |
| **Claude Code** | `npx -y @agentclientprotocol/claude-agent-acp` (official adapter) |

Next: **Cline**.

The ACP client lives in `server/cli/acp`: `AcpConnection` handles JSON-RPC 2.0 over the agent's stdio (permission requests are auto-approved), and `AcpAgent` exposes `prompt`, `history`, `ask` and `models` on top of it. Requests and responses are typed `@Serializable` data classes, and method names come from the `AcpMethod` enum.

### Modules

| Module | Path | Purpose |
|--------|------|---------|
| `composeApp` | `/composeApp` | Compose Multiplatform web frontend (JS + Wasm) |
| `server` | `/server` | Ktor REST API with agent, shell, GitHub, and MCP features |
| `shared` | `/shared` | Kotlin Multiplatform shared DTOs, enums, and data models |
| `docs` | `/docs` | Auto-generated Swagger/OpenAPI documentation |

### Server Layering

The server follows a **Route → Service → Repository/CLI** pattern. Each feature domain (Agent, Shell, GitHub, MCP) is self-contained with its own route, service, and data access layer.

---

## API Reference

> Interactive docs available at `/swagger` and `/redoc` when the server is running.

### Agent Endpoints — `/agents`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/agents/create` | Create an agent: clone repo, run CLI agent, push & open PR |
| `POST` | `/agents/resume` | Resume an agent with a follow-up prompt |
| `GET` | `/agents/list` | List all agents |
| `GET` | `/agents/{id}` | Get agent detail with chat history (replayed from the ACP session) |
| `GET` | `/agents/{id}/stream` | Server-Sent Events stream of the agent's run (`AgentStreamEvent`) |
| `GET` | `/agents/status?provider=Cursor` | Check agent provider auth status |
| `GET` | `/agents/login?provider=Cursor` | Log in to agent provider |
| `GET` | `/agents/logout?provider=Cursor` | Log out from agent provider |
| `GET` | `/agents/models?provider=Cursor` | List available models |
| `GET` | `/agents/clear` | Clear all agents |

`provider` is `Cursor` or `Claude`.

**Stream events** (`AgentStreamEvent`): `Update` (an ACP `session/update`: message/thought chunks, tool calls), `TurnStarted`, `TurnEnded` (with the ACP `stopReason`), `Error`, and `Done` (the run, including commit/push/PR, has finished).

### Shell Endpoints — `/shell`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/shell/create` | Execute a shell command with optional timeout |

### GitHub Endpoints — `/github`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/github/status` | GitHub CLI auth status |
| `GET` | `/github/login` | Log in via GitHub CLI |
| `GET` | `/github/logout` | Log out from GitHub CLI |
| `GET` | `/github/repo` | List repositories |
| `GET` | `/github/repo/search?q=&limit=` | Search repositories |
| `POST` | `/github/branch` | Search branches for a repository |
| `GET` | `/github/token` | Get GitHub auth token |

### MCP Endpoints — `/mcp`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/mcp` | Get the MCP configuration |
| `PUT` | `/mcp` | Replace the MCP configuration |
| `POST` | `/mcp/servers` | Add or update an MCP server |
| `DELETE` | `/mcp/servers/{name}` | Remove an MCP server |

The configuration is stored in `~/.agentspawn/mcp.json` and passed to Claude Code in ACP `session/new` / `session/load`; it is also written to `~/.cursor/mcp.json`, which Cursor reads itself.

---

## Roadmap

- [x] **Claude Code provider** — Claude Code over the official ACP adapter
- [x] **Agent Client Protocol** — All providers driven over ACP
- [ ] **Cline provider** — `AgentCli` implementation for Cline CLI
- [x] **MCP support** — Pluggable Model Context Protocol servers
- [ ] **Skills & Rules** — Configurable agent skills and rule sets
- [ ] **Codebase Indexing** — Index repositories for cheaper, context-aware agent usage
- [x] **Live streaming** — Real-time agent output over Server-Sent Events
- [ ] **Multi-user support** — Role-based access control
- [ ] **Task queue & scheduling** — Queue and schedule agent tasks
- [ ] **Dashboard analytics** — Token usage, success rates, agent performance

---

## Getting Started

### Prerequisites

- **JDK 23+** (Eclipse Temurin recommended)
- **Docker** (optional, for containerized deployment)
- **PostgreSQL 17** (or use Docker Compose)
- **Cursor CLI** (for the Cursor provider; provides `agent acp`)
- **Claude Code** + **Node.js / npx** (for the Claude provider; the ACP adapter runs via `npx`)
- Gradle wrapper is included — no separate installation needed

### Quick Start with Docker Compose

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** on port `5432`
- **Server (Ktor API)** on port `8080`
- **Web UI (Compose JS)** on port `3000`

### Manual Setup

```bash
# Start the server
./gradlew :server:run

# Start the web UI (pick one)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Wasm (faster, modern browsers)
./gradlew :composeApp:jsBrowserDevelopmentRun        # JS (broader support)
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Server port |
| `KTOR_ENV` | `development` | Ktor environment (`development` / `production`) |
| `POSTGRES_HOST` | `localhost` | Database host |
| `POSTGRES_PORT` | `5432` | Database port |
| `POSTGRES_DB` | `ktor_tutorial_db` | Database name |
| `POSTGRES_USER` | `postgresql` | Database user |
| `POSTGRES_PASSWORD` | `password` | Database password |
| `API_KEY` | `default` | API authentication key |

### Docker (Individual Images)

```bash
docker build --target server -t agentspawn-server .
docker build --target web -t agentspawn-web .
docker build -t agentspawn-cursor .    # Development image with Cursor CLI
```

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.3.0 | Language |
| Compose Multiplatform | 1.10.3 | Web UI framework |
| Ktor | 3.4.0 | HTTP server & client |
| Koin | 4.2.0 | Dependency injection |
| Arrow | 2.2.2 | Functional error handling |
| Exposed | 1.1.1 | Kotlin SQL ORM |
| Flyway | 11.8.2 | Database migrations |
| PostgreSQL | 42.7.7 | Database driver |
| kotlinx-serialization | 1.10.0 | JSON serialization |

## Project Structure

```
AgentSpawn/
├── composeApp/                # Compose Multiplatform Web UI
│   └── src/webMain/           # Web target (JS + Wasm)
│       ├── ui/features/       # Chat, Splash, Components
│       ├── data/              # HTTP client (Ktor)
│       └── di/                # Koin DI modules
├── server/                    # Ktor Backend
│   ├── features/
│   │   ├── agent/             # Agent CRUD & lifecycle
│   │   ├── shell/             # Shell command execution
│   │   ├── github/            # GitHub integration
│   │   └── mcp/               # MCP server configuration
│   ├── cli/                   # CLI abstractions
│   │   ├── acp/               # Agent Client Protocol client (JSON-RPC over stdio)
│   │   ├── agent/             # AgentCli interface & factory
│   │   └── providers/         # Provider implementations (Cursor, Claude)
│   ├── configs/               # Ktor plugin configurations
│   └── resources/             # application.conf, Flyway migrations
├── shared/                    # KMP Shared Models
│   └── api/                   # DTOs, enums, request/response models
├── docs/                      # OpenAPI documentation
└── gradle/                    # Build configuration
```

---

## Contributing

1. **Fork** the repository
2. **Create a branch** (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes
4. **Push** and open a **Pull Request**

### Code Guidelines

- Use **Arrow** (`Either<AppError, T>`) for error handling in the server module
- Follow the **Route → Service → Repository/CLI** architecture pattern
- New agent providers implement the `AgentCli` interface and register in `DefaultAgentProviderFactory`

---

<div align="center">

Built with Kotlin, Ktor, and Compose Multiplatform

</div>
