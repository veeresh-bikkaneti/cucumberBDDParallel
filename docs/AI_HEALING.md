# AI locator healing — provider configuration

Locator healing is optional. When enabled, a broken `@FindBy` triggers one
LLM call to suggest a new CSS selector, then a single retry.

## Environment variables

| Variable | Purpose |
|----------|---------|
| `AI_HEALING_PROVIDER` | `anthropic`, `openai`, or `ollama` (recommended — explicit choice) |
| `AI_HEALING_MODEL` | Model name for the selected provider |
| `AI_HEALING_API_KEY` | BYOK API key (works with any provider that needs auth) |
| `AI_HEALING_BASE_URL` | OpenAI-compatible base URL (`.../v1`) for gateways or custom hosts |
| `ai.healing.enabled` | JVM property; set `false` to disable even when credentials exist |

### Legacy / provider-specific (still supported)

| Variable | Provider |
|----------|----------|
| `ANTHROPIC_API_KEY` | Anthropic (auto-selects `anthropic` when set) |
| `ANTHROPIC_MODEL` | Anthropic model override |
| `OPENAI_API_KEY` | OpenAI-compatible (auto-selects `openai` when set) |
| `OPENAI_MODEL` | OpenAI model override |
| `OLLAMA_HOST` | Ollama host, e.g. `http://127.0.0.1:11434` (appends `/v1`) |
| `OLLAMA_MODEL` | Ollama model override |
| `AI_HEALING_OLLAMA` | Set `true` to enable Ollama without other keys |

## Defaults when unset

| Provider | Default model | Default base URL |
|----------|---------------|------------------|
| `anthropic` | `claude-sonnet-5` | `https://api.anthropic.com/v1` |
| `openai` | `gpt-4o-mini` | `https://api.openai.com/v1` |
| `ollama` | `llama3.2` | `http://127.0.0.1:11434/v1` |

## Examples

### Anthropic BYOK

```bash
export AI_HEALING_PROVIDER=anthropic
export AI_HEALING_API_KEY="$ANTHROPIC_API_KEY"
export AI_HEALING_MODEL=claude-sonnet-5
```

### OpenAI BYOK

```bash
export AI_HEALING_PROVIDER=openai
export AI_HEALING_API_KEY="$OPENAI_API_KEY"
```

### Local Ollama

```bash
ollama pull llama3.2
export AI_HEALING_PROVIDER=ollama
export AI_HEALING_MODEL=llama3.2
```

### Custom OpenAI-compatible gateway

```bash
export AI_HEALING_PROVIDER=openai
export AI_HEALING_API_KEY=your-gateway-key
export AI_HEALING_BASE_URL=https://llm.internal.company/v1
export AI_HEALING_MODEL=your-model-id
```

## Auto-detection (when `AI_HEALING_PROVIDER` is unset)

1. `ANTHROPIC_API_KEY` present → Anthropic
2. Else `OPENAI_API_KEY` or `AI_HEALING_API_KEY` → OpenAI-compatible
3. Else `OLLAMA_HOST` or `AI_HEALING_OLLAMA=true` → Ollama
4. Else healing stays **off**

If multiple credential sets are present (e.g. both `ANTHROPIC_API_KEY` and
`OPENAI_API_KEY`), Anthropic wins unless you set `AI_HEALING_PROVIDER` explicitly.

Prefer setting `AI_HEALING_PROVIDER` explicitly so CI and teammates know
which backend you intended.

## Run the demo

Deterministic mock (CI-safe, no cloud key):

```bash
./mvnw -pl examples/ai-healing-demo -am test
```

Live provider (Anthropic, OpenAI, or Ollama):

```bash
export AI_HEALING_DEMO_LIVE=true
# plus provider vars — see examples/ai-healing-demo/README.md
./mvnw -pl examples/ai-healing-demo -am test -Plive-ai-demo
```

Scripts: `scripts/run-ai-demo-mock.sh`, `run-ai-demo-anthropic.sh`,
`run-ai-demo-openai.sh`, `run-ai-demo-ollama.sh`.