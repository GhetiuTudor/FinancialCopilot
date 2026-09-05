# 🧠 Financial Copilot

**An AI-powered financial advisor for SMEs** — ask questions about your company's finances in plain language and get instant, grounded answers backed by your real ledger data, Romanian tax law, and ML-powered VAT forecasting.

> Built as a Bachelor's Degree project in Computer Science.

---

## 📸 Overview

Financial Copilot is a full-stack, multi-service application that combines:

- A **React + TypeScript** SPA with a chat interface and financial dashboard
- A **Spring Boot** backend that manages users, ledger data, SAF-T imports, and AI orchestration
- A **Python FastAPI** AI agent powered by **LangGraph** with tool-calling capabilities
- A **RAG pipeline** over Romanian fiscal legislation (Codul Fiscal)
- A **TFT (Temporal Fusion Transformer)** model for VAT/cash-flow forecasting
- **PostgreSQL** as the single source of truth, managed by Flyway migrations
- Full **Docker Compose** deployment

---

## 🗂️ Repository Structure

```
ProjectFinancialCopilot/
├── frontend/               # React 19 + TypeScript + Tailwind v4 SPA
├── backend/                # Spring Boot 3.4 REST API + JPA + Security
├── ai-service/             # Python FastAPI — LangGraph agent + RAG + TFT
│   ├── agent/              # LangGraph graph, tools, config
│   ├── rag-module/         # ChromaDB ingestion + retrieval pipeline
│   ├── tft_module/         # Temporal Fusion Transformer forecasting
│   ├── renderers/          # Pluggable LLM response post-processors
│   └── data/               # Romanian fiscal law PDFs (RAG source)
├── infra/                  # Docker Compose + environment templates
├── contracts/              # OpenAPI spec for the internal AI service contract
└── .env.example            # Root environment template (copy → .env)
```

---

## 🏗️ System Architecture

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': {'primaryColor': '#1e1e2e', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#89b4fa', 'lineColor': '#89dceb', 'secondaryColor': '#181825', 'tertiaryColor': '#11111b', 'edgeLabelBackground': '#1e1e2e', 'nodeTextColor': '#cdd6f4', 'clusterBkg': '#181825', 'clusterBorder': '#45475a'}}}%%
flowchart TD
    Browser(["🌐 Browser\nReact 19 + TypeScript\nTailwind CSS v4"])

    subgraph FE["FRONTEND  :3000  —  nginx + Vite SPA"]
        direction TB
        Login["🔐 LoginPage\nRegisterPage"]
        Dashboard["📊 Dashboard\nKPI Cards + Charts"]
        Chat["💬 Chat Component\nmessage threads\ntools_used badges"]
        Ledger["📒 LedgerPage\nInvoice list + filters"]
        Integrations["🔌 IntegrationsPage\nSAF-T / SmartBill / FGO / Tink"]
        Profile["👤 ProfilePage\nCompany settings"]
        APIClient["api/client.ts\nAxios + JWT Bearer\nauto-attach token"]
    end

    subgraph BE["BACKEND  :8080  —  Spring Boot 3.4  Java 21"]
        direction TB
        subgraph Security["Spring Security Layer"]
            JwtFilter["JwtAuthFilter\nvalidate Bearer token\nextract org_id + user_id"]
            JwtSvc["JwtService\nHS256  •  7-day TTL\nBCrypt password hash"]
        end

        subgraph Controllers["REST Controllers"]
            AuthCtrl["AuthController\nPOST /auth/register\nPOST /auth/login"]
            ChatCtrl["ChatController\nPOST /chat/threads\nPOST /chat/{id}/messages\nGET  /chat/{id}"]
            DashCtrl["DashboardController\nGET /dashboard/summary"]
            LedgerCtrl["LedgerController\nGET /ledger"]
            SaftCtrl["SaftImportController\nPOST /saft/upload\nmultipart XML"]
            IntCtrl["IntegrationController\nGET/POST /integrations"]
        end

        subgraph Services["Service Layer"]
            ChatSvc["ChatService\nbuild FinancialContext\nforward to AI\npersist reply"]
            AiProxy["AiService\nWebClient\nPOST /ai/v1/analyze\nX-Request-Id header\nX-Correlation-Id header"]
            DashSvc["DashboardService"]
            MetricsSvc["FinancialMetricsService\nrevenue / expenses\nprofit / margin\ncash position"]
            TaxSvc["TaxEngineService\nVAT 19% + 9% + 5%\nIncome Tax 16%\nMicroenterprise 3%"]
            LedgerAgg["LedgerAggregationService\njournal aggregation\nbalance sheets"]
            Reconcile["InvoiceReconciliationService\nbank tx ↔ invoice matching"]
            SaftSvc["SaftXmlParserService\nJackson XML → entities\naccounts, invoices\ncounterparties, journal"]
            IntSvc["IntegrationService\nadapter dispatch\nsync run tracking"]
        end

        subgraph IntAdapters["Integration Adapters"]
            SmartBill["SmartbillAdapterStub"]
            FGO["FgoAdapterStub"]
            Tink["TinkAdapterStub"]
            SaftAdapter["SaftAdapterStub"]
        end

        subgraph Context["FinancialContext Builder"]
            CtxBuilder["LedgerContextPackBuilder\nfacts map\ncoverage windows\nraw_refs\nprofile\ntime_range"]
        end

        subgraph Internal["Internal Endpoints — AI callbacks only"]
            IntLedger["InternalLedgerController\nGET /internal/ledger/records"]
            IntDash["InternalDashboardController\nGET /internal/dashboard/summary"]
            IntForecast["InternalForecastDataController\nGET /internal/forecast/data"]
        end

        DB[("🐘 PostgreSQL 16\nFlyway V1–V8 migrations\nchat_threads · chat_messages\nusers · organizations\ninvoices · journal_entries\npayments · counterparties\nintegration_connections\nsaft_imports · company_profile")]
    end

    subgraph AI["AI SERVICE  :8000  —  FastAPI  Python 3.11"]
        direction TB
        FastAPI_App["FastAPI Application\nversion 2.0.0\nGET  /ai/v1/health\nPOST /ai/v1/analyze"]

        subgraph AgentCore["LangGraph Agent — ReAct Loop"]
            State["AgentState\nTypedDict\nmessages: list\ntools_used: list\niterations: int\ncompany_id: str"]

            subgraph Nodes["Graph Nodes"]
                LLM_Node["llm_node\nChatOpenAI.bind_tools\ninvoke messages\nincrement iterations"]
                ToolExec["tool_executor\nToolNode wrapper\ntrack tools_used list\nlog tool name + args"]
            end

            subgraph Router["Conditional Router"]
                ShouldCont{"should_continue\n\nhas tool_calls?\niterations >= MAX?"}
            end

            Start(["START"]) --> LLM_Node
            LLM_Node --> ShouldCont
            ShouldCont -->|"tool_calls present"| ToolExec
            ShouldCont -->|"no calls OR max iter"| End(["END"])
            ToolExec --> LLM_Node
        end

        subgraph Prompts["System Prompts"]
            SysPrompt["AGENT_SYSTEM_PROMPT\nRomanian financial advisor\nadvisory-only constraint\ncurrency RON\nconfidence calibration"]
            TaxGuardrail["TAX_GUARDRAIL_PROMPT\ninjected if is_tax_intent()\nlegislation citation required\nno tax advice without RAG"]
        end

        subgraph ToolRegistry["Tool Registry — ALL_TOOLS"]
            T1["🔧 get_ledger_records\ndate_from, date_to\n→ GET /internal/ledger/records\nreturns: invoices + journal rows"]
            T2["📈 get_financial_metrics\nyear, month\n→ GET /internal/dashboard/summary\nreturns: revenue, expenses, profit\nmargin, cash, receivables"]
            T3["🧾 get_tax_summary\nyear, month\n→ TaxEngineService (via backend)\nreturns: VAT, income_tax, total"]
            T4["🔮 forecast_vat\nmonths_ahead\n→ TFT Module (local Python)\nreturns: P10/P50/P90 quantiles"]
            T5["📚 search_legislation\nquery string\n→ RAG Module (local Python)\nreturns: ranked chunks + scores"]
        end

        subgraph Renderer["Response Renderer — Pluggable"]
            RFactory["RendererFactory\ncreate_renderer(type)"]
            GPT_R["GptRenderer\npass-through\nno post-processing"]
            Llama_R["LlamaRenderer\nOllama HTTP client\nllama3 stylistic rewrite\ntimeout: 10s"]
        end

        subgraph MsgBuilder["Message Builder"]
            BuildMsg["_build_agent_messages\nSystemMessage: system prompt\n+ tax guardrail if needed\nSystemMessage: financial context\n+ company_id injection\nHumanMessage/AIMessage: history\nHumanMessage: current question"]
        end
    end

    subgraph RAG_Box["RAG MODULE"]
        Chroma[("ChromaDB\nvector store\nall-MiniLM-L6-v2 embeddings")]
        BM25_Store["BM25 Index\nrank-bm25"]
    end

    subgraph TFT_Box["TFT MODULE"]
        TFT_Model["Temporal Fusion\nTransformer\npytorch-forecasting"]
    end

    Browser -->|"HTTP"| FE
    FE --> APIClient
    APIClient -->|"REST + JWT"| JwtFilter
    JwtFilter --> Controllers
    Controllers --> Services
    Services --> DB
    ChatSvc --> CtxBuilder
    CtxBuilder --> AiProxy
    AiProxy -->|"POST /ai/v1/analyze\nFinancialContext payload"| FastAPI_App
    FastAPI_App --> MsgBuilder
    MsgBuilder --> AgentCore
    Prompts --> MsgBuilder
    AgentCore --> ToolRegistry
    T1 & T2 & T3 -->|"Internal REST"| Internal
    Internal --> Services
    T4 --> TFT_Box
    T5 --> RAG_Box
    AgentCore --> Renderer
    RFactory --> GPT_R & Llama_R
    IntSvc --> IntAdapters
```

---

## 🤖 AI Agent — Deep Dive

The agent is built on **LangGraph** with a two-node reasoning loop.

### Agent Graph

```mermaid
flowchart LR
    START --> llm_node
    llm_node -->|"has tool_calls?"| should_continue{should_continue}
    should_continue -->|"yes"| tool_executor
    should_continue -->|"no / max iter"| END
    tool_executor --> llm_node
```

The `should_continue` router also enforces a hard cap (`MAX_AGENT_ITERATIONS`, default 5) to prevent runaway loops.

### Agent Tools

| Tool | Description | Calls |
|------|-------------|-------|
| `get_ledger_records` | Fetches invoices & journal entries for a date range | Java backend |
| `get_financial_metrics` | Returns computed KPIs: revenue, expenses, profit, cash position | Java backend |
| `get_tax_summary` | Returns VAT, income tax and total tax liability breakdown | Java backend |
| `forecast_vat` | Runs the TFT model to predict future monthly VAT obligations | Python (TFT module) |
| `search_legislation` | Semantic + BM25 hybrid search over Romanian Codul Fiscal PDFs | Python (RAG module) |

### RAG Pipeline

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': {'primaryColor': '#1e1e2e', 'primaryTextColor': '#cdd6f4', 'primaryBorderColor': '#a6e3a1', 'lineColor': '#a6e3a1', 'secondaryColor': '#181825', 'tertiaryColor': '#11111b', 'edgeLabelBackground': '#1e1e2e', 'nodeTextColor': '#cdd6f4', 'clusterBkg': '#181825', 'clusterBorder': '#45475a'}}}%%
flowchart TD
    subgraph INGEST["━━━  OFFLINE INGESTION PIPELINE  ━━━  python ingest.py"]
        direction TB

        PDFs["📄 Source Documents\nai-service/data/\n15× Romanian Codul Fiscal PDFs\n~8 MB total\nTITLUL I–XI + CAPITOLUL"]

        subgraph DocProc["DocumentProcessor\nrag/document_processor.py"]
            PyPDF["pypdf >= 4.0\npage-by-page text extraction\nUTF-8 decode\nRomanian diacritics preserved"]
            Splitter["Recursive Text Splitter\nchunk_size = 1000 chars\nchunk_overlap = 200 chars\nsplit on: paragraph → sentence → word"]
            Meta["Metadata tagging\nsource: filename\nchunk_id: uuid4\npage_num: int"]
        end

        subgraph EmbedModel["EmbeddingModel\nrag/embeddings.py"]
            SentTrans["sentence-transformers >= 2.2\nModel: all-MiniLM-L6-v2\n384-dimensional dense vectors\nmax_seq_length: 256 tokens\nCPU inference (no GPU needed)\nbatch_size: 32"]
        end

        subgraph VecDB["VectorStore\nrag/vector_store.py"]
            Chroma["ChromaDB >= 0.4\nPersistent local store\nrag-module/data/vector_store/\ncollection: legislation\nHNSW index (cosine similarity)\nstores: embeddings + raw text + metadata"]
        end

        subgraph BM25_Build["BM25 Index\nrag/keyword_search.py"]
            BM25["rank-bm25 >= 0.2.2\nBM25Okapi\ntokenize: lower + split\nbuilt over all chunk texts\nin-memory at query time"]
        end

        PDFs --> PyPDF --> Splitter --> Meta
        Meta --> SentTrans
        SentTrans -->|"384-dim vectors"| Chroma
        Meta -->|"raw text"| BM25
    end

    subgraph QUERY["━━━  ONLINE QUERY PIPELINE  ━━━  search_legislation(query)"]
        direction TB

        UserQ["🗣️ User Question\ne.g. 'Care este cota TVA\npentru servicii medicale?'"]

        subgraph Expand["QueryExpander\nrag/query_expander.py"]
            QExp["Query Expansion\nsynonym expansion\nRomanian legal term mapping\ne.g. TVA → taxa pe valoarea adaugata\ngenerate 2–3 query variants"]
        end

        subgraph DenseRet["Dense Retrieval\nChromaDB cosine search"]
            QEmbed["Embed query\nall-MiniLM-L6-v2\n→ 384-dim vector"]
            TopK_Dense["Top-K Dense Results\nk=10\nHNSW approximate nearest-neighbor\nreturns: chunk_text + cosine_score + metadata"]
        end

        subgraph SparseRet["Sparse Retrieval\nBM25 keyword search"]
            BM25Q["BM25Okapi.get_scores(query)\ntokenized query against index\nterm frequency × IDF weighting\nreturns: top-10 chunks by BM25 score"]
        end

        subgraph Rerank["HybridReranker\nrag/reranker.py"]
            Normalize["Score Normalization\ndense: min-max scale [0,1]\nsparse: min-max scale [0,1]"]
            RRF["Reciprocal Rank Fusion\nalpha = 0.5  (dense weight)\n1-alpha = 0.5  (sparse weight)\nfinal_score = α·dense + (1-α)·BM25"]
            TopN["Top-N Selection\nn=5 final chunks\nde-duplicated by chunk_id\nsorted by hybrid score desc"]
        end

        subgraph Output["RAG Tool Output → Agent"]
            Chunks["Retrieved Chunks\n{\n  text: str,\n  source: filename,\n  chunk_id: uuid,\n  score: float 0–1\n}\n× 5 results"]
            Injected["Injected into Agent Context\nas ToolMessage content\nagent cites source in answer"]
        end

        UserQ --> Expand
        Expand -->|"expanded queries"| QEmbed
        Expand -->|"expanded queries"| BM25Q
        QEmbed --> TopK_Dense
        BM25Q --> SparseRet
        TopK_Dense --> Normalize
        SparseRet --> Normalize
        Normalize --> RRF --> TopN --> Chunks --> Injected
    end

    INGEST -.->|"vector store on disk\n(run once)"| QUERY
```

Run ingestion once before starting the agent:
```bash
cd ai-service/rag-module
python ingest.py
```

### TFT Forecasting Module

The **Temporal Fusion Transformer** (`tft_module/`) is trained on a synthetic Romanian financial dataset and predicts `monthly_vat_target` 1 month ahead using:

- **Static categoricals**: business sector
- **Time-varying known**: month, quarter, day-of-week, holiday flag, macro index
- **Time-varying unknown**: revenue, expenses, profit, VAT due, tax due

The forecast tool is called by the agent when the user asks questions like *"What will my VAT be next month?"*

---

## 🗄️ Backend — Deep Dive

Built with **Spring Boot 3.4**, **Java 21**, **Spring Data JPA**, and **Spring Security**.

### Key Domain Areas

| Package | Responsibility |
|---------|---------------|
| `controller/` | REST endpoints (Auth, Chat, Dashboard, Ledger, SAF-T, Integrations) |
| `service/` | Business logic, AI orchestration, ledger aggregation, reconciliation |
| `entity/` | JPA entities: Users, Organizations, ChatThreads, Invoices, JournalEntries, Payments… |
| `security/` | JWT filter, `JwtService`, `SecurityConfig` |
| `dto/saft/` | Jackson XML mapping for SAF-T (Standard Audit File for Tax) |
| `integration/` | Adapter stubs for SmartBill, FGO, Tink, SAF-T connectors |

### Authentication Flow

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Spring Boot
    participant DB as PostgreSQL

    FE->>BE: POST /auth/register {email, password, company}
    BE->>DB: Save Organization + User (BCrypt password)
    BE-->>FE: { token, userId }

    FE->>BE: POST /auth/login {email, password}
    BE->>DB: Load user, verify BCrypt
    BE-->>FE: JWT (7-day TTL)

    FE->>BE: GET /ledger (Authorization: Bearer <token>)
    BE->>BE: JwtAuthFilter validates token
    BE-->>FE: Ledger data
```

### Chat → AI Flow

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Spring Boot
    participant AI as Python AI Service

    FE->>BE: POST /chat/{threadId}/messages {question}
    BE->>BE: Build FinancialContext (ledger, KPIs, profile)
    BE->>AI: POST /ai/v1/analyze\n(X-Request-Id, X-Correlation-Id headers)
    AI->>AI: LangGraph agent runs (tool calls as needed)
    AI-->>BE: AiAnalyzeResponse {answer_text, tools_used, usage}
    BE->>DB: Persist ChatMessage
    BE-->>FE: MessageDto {content, toolsUsed}
```

### SAF-T Import

The backend can parse **SAF-T XML** files (Romanian standard for accounting data exchange). Importing a SAF-T file populates the ledger with:
- Chart of accounts
- Customers and suppliers (counterparties)
- Sales and purchase invoices
- General ledger journal entries
- Tax codes

Upload via: `POST /api/saft/upload` (multipart XML)

---

## 💻 Frontend — Deep Dive

Built with **React 19**, **TypeScript**, **Vite 5**, and **Tailwind CSS v4**.

### Pages

| Page | Route | Description |
|------|-------|-------------|
| Login | `/login` | JWT-based authentication |
| Register | `/register` | Company + user registration |
| Dashboard | `/dashboard` | KPI cards, revenue/expense charts, AI chat sidebar |
| Ledger | `/ledger` | Invoice list with filtering and payment status |
| Integrations | `/integrations` | Connect data sources (SAF-T, SmartBill, FGO, Tink) |
| Profile | `/profile` | Company profile management |

### Component Architecture

```
src/
├── App.tsx                 # Router setup
├── main.tsx                # React root + auth context
├── api/                    # Typed fetch wrappers per domain
├── auth/                   # Auth context + protected route HOC
├── components/
│   ├── Chat.tsx            # Full chat interface with streaming display
│   ├── Dashboard.tsx       # KPI dashboard with charts
│   └── DashboardCard.tsx   # Reusable KPI card
├── layout/                 # App shell, navbar, sidebar
├── pages/                  # Page-level components
└── types/                  # Shared TypeScript interfaces
```

---

## 🚀 Running Locally

### Prerequisites

| Tool | Version |
|------|---------|
| Docker + Docker Compose | v2+ |
| Node.js | ≥ 20.19 (frontend dev only) |
| Python | 3.11+ (ai-service dev only) |
| Java | 21 (backend dev only) |
| OpenAI API Key | Required for the agent |

---

### Option 1 — Full Stack with Docker Compose (recommended)

```bash
# 1. Clone the repo
git clone https://github.com/your-org/financial-copilot.git
cd financial-copilot

# 2. Configure environment
cp .env.example infra/.env
# Edit infra/.env and set OPENAI_API_KEY

# 3. (First time only) Ingest the RAG documents
cd ai-service/rag-module
pip install -r ../requirements.txt
python ingest.py
cd ../..

# 4. Start all services
cd infra
docker compose up --build
```

Services will be available at:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| AI Service | http://localhost:8000 |
| PostgreSQL | localhost:5432 |

---

### Option 2 — Individual Services (development)

#### Backend (Spring Boot)

```bash
cd backend

# Requires PostgreSQL on localhost:5432 (use Docker for just the DB)
docker run -d --name pg \
  -e POSTGRES_DB=copilot -e POSTGRES_USER=copilot -e POSTGRES_PASSWORD=copilot \
  -p 5432:5432 postgres:16

./mvnw spring-boot:run
# API available at http://localhost:8080
```

#### AI Service (Python)

```bash
cd ai-service

python -m venv venv && source venv/bin/activate
pip install -r requirements.txt

# Copy and fill in env
cp ../.env.example .env
# Set OPENAI_API_KEY and JAVA_BACKEND_URL=http://localhost:8080

# (First time) Ingest RAG documents
cd rag-module && python ingest.py && cd ..

uvicorn agent.api:app --reload --port 8000
# API docs at http://localhost:8000/docs
```

#### Frontend (React)

```bash
cd frontend
npm install
npm run dev
# SPA at http://localhost:5173
```

---

## 🔑 Environment Variables

Copy `.env.example` to `.env` (or `infra/.env` for Docker Compose) and fill in your values.

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OPENAI_API_KEY` | ✅ | — | Your OpenAI API key |
| `OPENAI_MODEL` | | `gpt-4o-mini` | Model to use for the agent |
| `LLM_TEMPERATURE` | | `0.2` | LLM sampling temperature |
| `LLM_MAX_TOKENS` | | `2000` | Max response tokens |
| `MAX_AGENT_ITERATIONS` | | `5` | Max LangGraph reasoning steps |
| `JAVA_BACKEND_URL` | | `http://localhost:8080` | Backend URL (from AI service) |
| `RESPONSE_RENDERER` | | `gpt_only` | `gpt_only` or `llama_post_process` |
| `OLLAMA_URL` | | `http://localhost:11434` | Ollama URL (if using Llama renderer) |
| `OLLAMA_MODEL` | | `llama3` | Ollama model name |
| `OLLAMA_TIMEOUT` | | `10` | Ollama request timeout (seconds) |
| `SPRING_FLYWAY_VALIDATE_ON_MIGRATE` | | `false` | Flyway schema validation |
| `JWT_SECRET` | | dev default | JWT signing secret (change in production!) |

---

## 🛡️ Design Decisions

### Advisory-Only AI
The AI service is **strictly read-only** — it has no database access and cannot mutate any state. All write operations go through the Spring Boot backend. This is enforced via the `AiConstraints.advisory_only = true` field in every request and documented in `contracts/internal-ai.openapi.yaml`.

### Internal API Contract
The Java backend and Python AI service communicate via a versioned internal OpenAPI contract (`contracts/internal-ai.openapi.yaml`). This ensures both sides agree on request/response shapes independently of implementation language.

### Multi-tenant Architecture
Every entity in the database is scoped to an `Organization`. JWT tokens carry the `organizationId` claim, which is validated at the service layer so users can only ever see their own company's data.

### Pluggable Response Rendering
After the agent produces an answer, it passes through a **renderer** (`renderers/`). The default `gpt_only` renderer is a pass-through. Optionally, `llama_post_process` sends the text to a local Ollama/Llama instance for stylistic post-processing — useful for matching a specific tone without re-running the full expensive agent call.

---

## 📦 Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite 5, Tailwind CSS v4 |
| Backend | Spring Boot 3.4, Java 21, Spring Security, Flyway |
| AI Agent | Python 3.11, FastAPI, LangGraph, LangChain, OpenAI |
| RAG | ChromaDB, sentence-transformers, BM25 (rank-bm25), PyPDF |
| Forecasting | PyTorch Forecasting (TFT), PyTorch Lightning, pandas |
| Database | PostgreSQL 16 |
| Infra | Docker, Docker Compose, nginx (frontend) |
| Auth | JWT (jjwt 0.12), BCrypt |

---

## 📄 License

This project was developed as a Bachelor's thesis project. All rights reserved.
