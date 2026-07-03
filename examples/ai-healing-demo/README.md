# AI healing demo

Deterministic proof that locator healing works — without hitting google.com or a real LLM in CI.

## What this shows

| Test | When | What it proves |
|------|------|----------------|
| `MockAiHealingDemoTest` | Always (CI) | Broken `@FindBy` → mock LLM returns `#logo` → test passes |
| `LiveAiHealingDemoTest` | Opt-in | Same flow against your Anthropic / OpenAI / Ollama provider |

Fixture HTML (`fixtures/demo-page.html`) has `<div id="logo">`. The page object uses `#hplogo` on purpose.

## Quick run (mock — no API key)

From repo root:

```bash
./mvnw -pl examples/ai-healing-demo -am test
```

Windows:

```powershell
.\mvnw.cmd -pl examples/ai-healing-demo -am test
```

Expected: **2 tests pass** in `MockAiHealingDemoTest`.

## Live provider runs

Set credentials, then use the scripts under `scripts/` or run Maven directly.

**Anthropic BYOK:**

```bash
export AI_HEALING_PROVIDER=anthropic
export AI_HEALING_API_KEY=sk-ant-...
export AI_HEALING_DEMO_LIVE=true
./scripts/run-ai-demo-anthropic.sh
```

**OpenAI BYOK:**

```bash
export AI_HEALING_PROVIDER=openai
export AI_HEALING_API_KEY=sk-...
export AI_HEALING_DEMO_LIVE=true
./scripts/run-ai-demo-openai.sh
```

**Local Ollama:**

```bash
ollama pull llama3.2
export AI_HEALING_PROVIDER=ollama
export AI_HEALING_MODEL=llama3.2
export AI_HEALING_DEMO_LIVE=true
./scripts/run-ai-demo-ollama.sh
```

Or with Docker:

```bash
docker compose -f docker-compose.ollama.yml up -d
export AI_HEALING_PROVIDER=ollama
export OLLAMA_HOST=http://127.0.0.1:11434
export AI_HEALING_DEMO_LIVE=true
./scripts/run-ai-demo-ollama.sh
```

Copy `.env.example.*` files in this folder as a starting point (never commit real keys).