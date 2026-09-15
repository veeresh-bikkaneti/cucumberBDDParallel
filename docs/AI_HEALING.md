# AI self-healing locators

When a `@FindBy` locator can't find its element — usually because the
page markup changed — the framework can ask a large language model for
a replacement CSS selector and retry once before failing the step.
This is **opt-in and off by default**: with no provider configured,
nothing ever leaves your machine.

## How it works

1. A page-object field's `@FindBy` lookup throws `NoSuchElementException`.
2. `AiLocatorHealer` sends the page HTML plus a description of the
   intended element to the configured LLM.
3. The LLM replies with a CSS selector; the framework retries the
   lookup with it.
4. The healed selector is cached for the rest of the run, so the same
   broken locator doesn't trigger a new LLM call on every poll.
5. If healing fails (network error, bad selector, timeout), the
   **original** `NoSuchElementException` is rethrown with the healing
   failure attached as a suppressed exception.

See [Architecture](ARCHITECTURE.md#ai-self-healing-end-to-end) for the
sequence diagram.

## Providers

You pick the route — nothing is locked to one vendor. The clients are
hand-rolled on the JDK `HttpClient` (no SDK dependencies); Ollama
works because it speaks the OpenAI-compatible chat API.

| Provider | `AI_HEALING_PROVIDER` | Needs | Default model |
|---|---|---|---|
| Anthropic (BYOK) | `anthropic` | `AI_HEALING_API_KEY` | `claude-sonnet-5` |
| OpenAI or compatible gateway (BYOK) | `openai` | `AI_HEALING_API_KEY` | `gpt-4o-mini` |
| Local Ollama | `ollama` | Ollama running, model pulled | `llama3.2` |

### Anthropic

```bash
export AI_HEALING_PROVIDER=anthropic
export AI_HEALING_API_KEY=sk-ant-...
export AI_HEALING_MODEL=claude-sonnet-5   # optional
```

Legacy `ANTHROPIC_API_KEY` / `ANTHROPIC_MODEL` variables are still
honored.

### OpenAI-compatible

```bash
export AI_HEALING_PROVIDER=openai
export AI_HEALING_API_KEY=sk-...
export AI_HEALING_MODEL=gpt-4o-mini        # optional
# export AI_HEALING_BASE_URL=https://my-gateway.example/v1   # optional
```

### Ollama

```bash
ollama pull llama3.2
export AI_HEALING_PROVIDER=ollama
export AI_HEALING_MODEL=llama3.2
# export OLLAMA_HOST=http://127.0.0.1:11434   # if not on localhost:11434
```

Or via Docker Compose: `docker compose -f docker-compose.ollama.yml up -d`.

## Configuration reference

| Variable | System property | Default | Purpose |
|---|---|---|---|
| `AI_HEALING_PROVIDER` | `ai.healing.provider` | — (healing off) | `anthropic`, `openai`, or `ollama` |
| `AI_HEALING_API_KEY` | `ai.healing.apiKey` | — | API key (BYOK). Not needed for Ollama |
| `AI_HEALING_MODEL` | `ai.healing.model` | per-provider default | Model to ask for selectors |
| `AI_HEALING_BASE_URL` | `ai.healing.baseUrl` | provider default | Override the API endpoint (gateways, proxies) |
| `AI_HEALING_TIMEOUT_SECONDS` | `ai.healing.timeoutSeconds` | `30` | HTTP request timeout per healing call |
| `AI_HEALING_DEMO_LIVE` | — | `false` | Demo-only: run `LiveAiHealingDemoTest` against a real provider |
| — | `ai.healing.enabled` | `true` when configured | Set `-Dai.healing.enabled=false` to force healing off |
| `ANTHROPIC_API_KEY` / `ANTHROPIC_MODEL` | — | — | Legacy Anthropic variables (still work) |
| `OPENAI_API_KEY` | — | — | Fallback key for the `openai` provider |
| `OLLAMA_HOST` | — | `http://127.0.0.1:11434` | Where Ollama listens |

Never commit API keys to source control. The demo module ships
`.env.example.*` files as templates.

## Reliability behavior

- **Timeouts:** every healing HTTP call has a 30s request timeout
  (configurable via `AI_HEALING_TIMEOUT_SECONDS`); connect timeout is 10s.
- **Retries:** HTTP 429/5xx responses are retried up to 3 times with
  exponential backoff + jitter. Other failures fail fast.
- **Interrupt handling:** thread interrupts are propagated, not swallowed.
- **Cache:** a healed selector is reused for the rest of the JVM run.
  If it breaks again later, the framework re-heals and updates the cache.

## Cost tracking

Every healing call logs one SLF4J line:

```
AI locator heal: element=searchInput model=claude-sonnet-5 in=1842 out=12 cost=$0.005706
```

A shutdown hook logs the JVM session total on exit:

```
AI locator healing session total: $0.005706
```

Pricing comes from `ModelPricing` (a snapshot, not live data — check
the provider's pricing page for budget planning; versioned model IDs
like `claude-sonnet-5-20250929` match their base entries). In CI, the
`e2e-with-ai` job extracts these lines into the GitHub step summary.

Rough math: at Sonnet rates, a typical heal (~2k input tokens of page
HTML, a few dozen output tokens) costs well under a cent. The real
cost driver is how often your markup changes, not the per-call price.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Healing never triggers | No provider configured | Set `AI_HEALING_PROVIDER` + key (or Ollama) |
| `NoSuchElementException` with suppressed healing error | LLM unreachable / bad selector | Check the suppressed exception; verify key, base URL, network |
| Slow scenarios | LLM latency on many broken locators | Fix the locators — healing is a safety net, not a strategy |
| Healing fires on every wait poll | Cached selector went stale (page changed again) | Expected: the stale entry is dropped and re-healed once |
| `cost=$unknown` in logs | Model not in `ModelPricing` | Add a one-line entry to the pricing table |

## Demo

`examples/ai-healing-demo` proves the whole flow deterministically:

```bash
# Mock LLM, no key needed — this is what CI runs
./mvnw -pl examples/ai-healing-demo -am test

# Live provider (needs credentials)
./scripts/run-ai-demo-anthropic.sh   # or -openai / -ollama
# Windows: .\scripts\run-ai-demo-anthropic.ps1 (etc.)
```

See `examples/ai-healing-demo/README.md` for the full matrix.
