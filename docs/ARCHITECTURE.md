# Architecture

How `cucumberBDDParallel` is put together, how tests flow through it in
parallel, and where the AI self-healing fits in. If you just want to run
things, start with the [README](../README.md). If you want the reasoning
behind the design decisions, see the [Playbook](../PLAYBOOK.md).

## Module map

Five Maven modules. One rule: **everything depends on `framework`;
`example-tests` also depends on the `example-app` it drives;
nothing else depends on anything.**

```mermaid
flowchart TD
    F[framework<br/>reusable core: driver lifecycle,<br/>waits, pages, AI healing]
    E1[example-tests<br/>Cucumber + TestNG BDD suite<br/>against the example app]
    E2[examples/ai-healing-demo<br/>deterministic healing demo<br/>mock + live providers]
    E3[examples/web-patterns-demo<br/>tables, drag-drop, upload/download,<br/>PDF, QR, OCR]
    E4[examples/example-app<br/>self-contained demo web app<br/>embedded server, zero dependencies]

    E1 --> F
    E1 --> E4
    E2 --> F
    E3 --> F
```

| Module | Packaging | Depends on | Purpose |
|---|---|---|---|
| `framework` | jar | Selenium, Cucumber, TestNG, WebDriverManager, SLF4J | The reusable library. Driver lifecycle, explicit waits, base page, opt-in AI healing locator, cost tracking. |
| `example-tests` | jar (test code) | `framework`, `example-app` | A real BDD suite: Gherkin features, step definitions, page objects, TestNG runners — parallelized with Cucable. The app-under-test is `example-app` (see [EXAMPLE_APP.md](EXAMPLE_APP.md)). |
| `examples/example-app` | jar | none (JDK only) | The self-contained demo web app the examples drive: embedded `HttpServer`, teaching pages for search, tables, drag-drop, upload, login, dynamic content. Full route table in [EXAMPLE_APP.md](EXAMPLE_APP.md). |
| `examples/ai-healing-demo` | jar (test code) | `framework` | Proves locator healing works: a deliberately broken `@FindBy`, a mock LLM for CI, live runs for Anthropic/OpenAI/Ollama. |
| `examples/web-patterns-demo` | jar (test code) | `framework` | Tricky-web-pattern recipes with local fixtures: tables, HTML5 drag-drop, file upload/download, PDF text, QR decode, OCR. |

The module boundary is deliberate: `example-tests` can only use what
`framework` exposes publicly, exactly like any external consumer.

## How a parallel run flows

`example-tests` parallelizes with [Cucable](https://github.com/trivago/cucable-plugin):
each feature file becomes its own generated TestNG runner, and Maven
Failsafe runs those runners across forked JVMs.

```mermaid
flowchart TD
    subgraph build [Maven build — example-tests]
        FEA[*.feature files<br/>src/test/resources/features]
        CUC[cucable-plugin<br/>generate-test-resources]
        GEN[generated runners *IT<br/>+ split features<br/>target/parallel]
        BH[build-helper<br/>add-test-source]
        FEA --> CUC --> GEN --> BH
    end
    subgraph run [Maven verify — integration-test]
        FS[maven-failsafe-plugin<br/>forkCount=2]
        TNG[TestNG]
        CUK[cucumber-testng]
        GLUE[glue: Setup, TearDown,<br/>step definitions]
        PAGE[page objects<br/>BasePage]
        BH --> FS --> TNG --> CUK --> GLUE --> PAGE
    end
    REP[cluecumber + cucumber-reporting<br/>HTML reports]
    FS --> REP
```

Hand-written runners (`*Test` in the `runner` package) are **excluded
from Surefire** — they exist for IDE runs. Only the Cucable-generated
`*IT` runners execute under Failsafe, so nothing runs twice and
nothing runs zero times.

## One scenario, one browser, one thread

Parallelism is built on a `ThreadLocal<WebDriver>`. Each scenario gets
a fresh browser on its own thread; threads never share drivers.

```mermaid
sequenceDiagram
    participant FSF as Failsafe fork (JVM)
    participant TN as TestNG thread
    participant SU as Setup @Before
    participant DM as DriverManager<br/>(ThreadLocal)
    participant ST as Step definitions
    participant TD as TearDown @After

    FSF->>TN: run scenario
    TN->>SU: setWebDriver()
    SU->>SU: WebDriverManager resolves driver binary
    SU->>SU: new ChromeDriver / FirefoxDriver<br/>(headless + no-sandbox when needed)
    SU->>DM: set(driver)
    TN->>ST: execute steps
    ST->>DM: get() → this thread's driver
    TN->>TD: quitDriver(scenario)
    TD->>TD: screenshot on failure
    TD->>DM: quit() in finally<br/>(remove ThreadLocal first,<br/>then driver.quit())
```

Failure safety is the point of the `finally`: even if screenshotting
throws, the browser is quit and the thread-local is cleared, so the
next scenario on a pooled thread never inherits a dead driver.

```mermaid
flowchart TD
    subgraph thread [One TestNG thread]
        TL[ThreadLocal slot]
        D1[ChromeDriver A]
        D2[ChromeDriver B]
        TL -.->|scenario 1| D1
        TL -.->|scenario 2| D2
    end
    note[quit() clears the slot<br/>before closing the browser]
```

## Page objects and waits

```mermaid
flowchart TD
    BP[BasePage<br/>holds WebDriver + Wait]
    PF[PageFactory.initElements]
    ELF{AI healing<br/>configured?}
    DEF[DefaultElementLocatorFactory<br/>plain @FindBy lookup]
    SHE[AiElementLocatorFactory<br/>self-healing lookup]
    W[Wait<br/>explicit waits]

    BP --> PF
    PF --> ELF
    ELF -->|no| DEF
    ELF -->|yes| SHE
    BP --> W
```

- `BasePage` is the superclass for all page objects. It wires `@FindBy`
  fields through Selenium's `PageFactory`, picking the locator factory
  based on whether AI healing is configured.
- `Wait` wraps `WebDriverWait` with explicit-wait helpers that return
  the found element(s), so callers never re-find and race with stale
  elements.
- `TableHelper`, `DragDropHelper`, `FileUploadHelper` cover the
  awkward web patterns (grids, HTML5 drag-drop via injected
  `DataTransfer` events, hidden file inputs).

## AI self-healing, end to end

When a `@FindBy` locator stops matching (markup changed), the healing
locator asks a configured LLM for a replacement CSS selector, retries
once, and caches the healed selector so a page with many broken
locators doesn't re-bill the same fix.

```mermaid
sequenceDiagram
    participant Step as Step definition
    participant Page as Page object
    participant Loc as SelfHealingElementLocator
    participant Cache as Healed-selector cache
    participant Heal as AiLocatorHealer
    participant Cli as LlmMessagesClient
    participant LLM as Configured LLM API
    participant Cost as CostLogger

    Step->>Page: page.search("…")
    Page->>Loc: findElement()
    Loc->>Cache: healed selector for this field?
    Cache-->>Loc: yes → try it first
    Loc->>Loc: default @FindBy lookup
    Loc-->>Loc: NoSuchElementException
    Loc->>Heal: heal(driver, field, cause)
    Heal->>Cli: POST chat payload<br/>(system prompt + page HTML + description)
    Cli->>LLM: HTTP (30s timeout,<br/>retry 429/5xx with backoff)
    LLM-->>Cli: CSS selector + token usage
    Cli-->>Heal: selector
    Heal->>Cost: logHealCall(element, model, tokens, $)
    Heal->>Loc: retry findElement with healed selector
    Loc->>Cache: store healed selector
    Loc-->>Page: WebElement
    Note over Heal,Loc: If the healed selector also fails,<br/>the ORIGINAL NoSuchElementException<br/>is rethrown (healing error suppressed).
```

Design points worth knowing:

- **Healing is off by default.** No provider credentials in the
  environment → `AiConfig.isHealingEnabled()` is `false` → plain
  `DefaultElementLocatorFactory`. Nothing phones home.
- **The original exception always wins.** If the LLM is unreachable or
  its selector is invalid, you get the original
  `NoSuchElementException` with the healing failure attached as a
  suppressed exception — never a confusing LLM error in place of the
  real one.
- **Cost is tracked per call and per session.** Every heal logs
  `element / model / input+output tokens / $`; a JVM shutdown hook
  logs the session total. See [AI healing](AI_HEALING.md).

## AI provider abstraction

Three providers, one interface. The hand-rolled `HttpClient`
implementations are intentional — zero extra dependencies.

```mermaid
flowchart TD
    subgraph config [Configuration]
        ENV[env vars / system properties<br/>AI_HEALING_PROVIDER, AI_HEALING_API_KEY, ...]
        AC[AiConfig<br/>reads env, isHealingEnabled]
        AHS[AiHealingSettings<br/>record: provider, model, apiKey, baseUrl]
        ENV --> AC --> AHS
    end
    subgraph factory [Factory]
        AP{AiProvider<br/>ANTHROPIC | OPENAI | OLLAMA}
        LF[LlmClientFactory]
        AHS --> AP --> LF
    end
    subgraph clients [Clients]
        I[LlmMessagesClient<br/>interface]
        AN[AnthropicHttpClient<br/>POST /v1/messages]
        OP[OpenAiCompatibleHttpClient<br/>POST /v1/chat/completions]
        LF --> I
        I --> AN
        I --> OP
        note2[Ollama speaks the<br/>OpenAI-compatible API]
    end
    subgraph parse [Response handling]
        SRP[SelectorResponseParser<br/>strips code fences]
        JE[JsonEscaping<br/>encode/decode]
        AN --> SRP
        OP --> SRP
        AN --> JE
        OP --> JE
    end
    subgraph cost [Cost tracking]
        TU[TokenUsage]
        CC[CostCalculator]
        MP[ModelPricing<br/>$/1M tokens]
        CL[CostLogger]
        TU --> CC --> MP
        CC --> CL
    end
```

## CI pipeline

```mermaid
flowchart TD
    TRIG[push to main / pull_request] --> UT[unit-tests<br/>framework JUnit 5 suite<br/>no browser, no network]
    UT --> AHD[ai-healing-demo<br/>MockAiHealingDemoTest<br/>deterministic, no API key]
    UT --> WPD[web-patterns-demo<br/>local fixtures<br/>headless Chrome]
    UT --> E2E[e2e-no-ai<br/>example-tests, healing off]
    UT --> EAI[e2e-with-ai<br/>example-tests, healing on]
    EAI --> KEY{ANTHROPIC_API_KEY<br/>secret set?}
    KEY -->|no| SKIP[skip gracefully<br/>note in job summary]
    KEY -->|yes| RUN[run with healing<br/>cost → job summary]
```

The `e2e-with-ai` job greps the Maven log for the heal/cost lines and
publishes them to the GitHub step summary, so the dollar cost of the
run is visible without digging through logs.

## Docker topology

```mermaid
flowchart TD
    subgraph img [cucumberbddparallel-examples image]
        MVN[maven:3.9.16-eclipse-temurin-21]
        CHR[google-chrome-stable]
        TES[tesseract-ocr + eng data]
    end
    SVC1[cucumber-examples<br/>runs both demo modules<br/>shm 2gb]
    SVC2[cucumber-examples-ocr<br/>profile: ocr<br/>OcrValidationTest only]
    SVC3[cucumber-integration<br/>profile: integration<br/>example-tests verify]
    OLL[ollama<br/>profile via docker-compose.ollama.yml<br/>ollama/ollama:0.34.0]
```

Chrome runs headless with `--no-sandbox`/`--disable-dev-shm-usage`
inside the container (it runs as root, and build-time `/dev/shm` is
tiny). Point `OLLAMA_HOST` at the ollama service for local-LLM
healing runs.

## Key files

| Area | Files |
|---|---|
| Driver lifecycle | `framework/.../driver/DriverManager.java`, `Setup.java`, `TearDown.java` |
| Pages & waits | `framework/.../page/BasePage.java`, `framework/.../wait/Wait.java` |
| Web patterns | `framework/.../interaction/TableHelper.java`, `DragDropHelper.java`, `FileUploadHelper.java` |
| AI healing | `framework/.../ai/AiLocatorHealer.java`, `AiElementLocatorFactory.java`, `AiConfig.java`, `AiHealingSettings.java`, `AiProvider.java`, `LlmClientFactory.java`, `LlmMessagesClient.java`, `LlmHttp.java` (shared timeout/retry), `AnthropicHttpClient.java`, `OpenAiCompatibleHttpClient.java`, `SelectorResponseParser.java`, `JsonEscaping.java` |
| Cost tracking | `framework/.../ai/cost/CostLogger.java`, `CostCalculator.java`, `ModelPricing.java`, `TokenUsage.java` |
| Parallel BDD | `example-tests/pom.xml` (Cucable + Failsafe), `example-tests/src/test/resources/cucable.template`, `example-tests/.../support/AppServerHooks.java` (starts/stops the example app) |
| Example app | `examples/example-app` — `ExampleAppServer.java`, `SearchPageRenderer.java`, `StaticPageHandler.java`, `src/main/resources/app-pages/*.html` |
| Demos | `examples/ai-healing-demo`, `examples/web-patterns-demo` |
| CI / Docker | `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`, `docker-compose.ollama.yml` |
| Docs | `README.md`, `PLAYBOOK.md`, `docs/AI_HEALING.md`, `docs/EXAMPLE_APP.md`, `docs/MCP_PLAYBOOK.md` |
