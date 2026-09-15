package com.cucumberbddparallel.framework.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * The HTTP plumbing both hand-rolled LLM clients share: request timeouts and bounded
 * retries for rate-limit / server-error responses.
 *
 * Kept separate from the clients so the retry policy (how many attempts, how long to
 * wait) lives in exactly one place - if we ever tune it, both providers change together.
 */
final class LlmHttp {

    private static final Logger LOG = LoggerFactory.getLogger(LlmHttp.class);

    /** How many times a single LLM call is attempted before giving up. */
    static final int MAX_ATTEMPTS = 3;
    /** First retry waits this long, doubling on each subsequent attempt, plus jitter. */
    static final Duration RETRY_BASE_DELAY = Duration.ofMillis(500);
    /** Upper bound on the random jitter added to each backoff, so parallel retries don't line up. */
    private static final Duration RETRY_JITTER = Duration.ofMillis(250);

    private static final String TIMEOUT_ENV_VAR = "AI_HEALING_TIMEOUT_SECONDS";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private LlmHttp() {
    }

    /**
     * How long to wait for the LLM response: {@code AI_HEALING_TIMEOUT_SECONDS} from the
     * environment (or the same-named system property), defaulting to 30 seconds. Garbage
     * values fall back to the default with a warning instead of blowing up the test run.
     */
    static Duration requestTimeout() {
        return requestTimeout(key -> {
            String env = System.getenv(key);
            return env != null ? env : System.getProperty(key);
        });
    }

    // Package-private so tests can feed values (including garbage) without touching the
    // real environment.
    static Duration requestTimeout(Function<String, String> lookup) {
        String raw = lookup.apply(TIMEOUT_ENV_VAR);
        if (raw != null) {
            try {
                long seconds = Long.parseLong(raw.trim());
                if (seconds > 0) {
                    return Duration.ofSeconds(seconds);
                }
                LOG.warn("Ignoring non-positive {}='{}'; using default {}", TIMEOUT_ENV_VAR, raw, DEFAULT_TIMEOUT);
            } catch (NumberFormatException notANumber) {
                LOG.warn("Ignoring invalid {}='{}'; using default {}", TIMEOUT_ENV_VAR, raw, DEFAULT_TIMEOUT);
            }
        }
        return DEFAULT_TIMEOUT;
    }

    /**
     * Sends the request, retrying HTTP 429 (rate limited) and 5xx (server error) with
     * exponential backoff + jitter. Transport failures ({@link IOException}) and
     * interrupts are NOT retried - those mean the network or the thread is gone, not that
     * the server is merely busy.
     */
    static HttpResponse<String> sendWithRetry(HttpClient client, HttpRequest request)
            throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            attempt++;
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (isRetryable(status) && attempt < MAX_ATTEMPTS) {
                Duration delay = backoffDelay(attempt);
                LOG.warn("LLM API returned HTTP {} (attempt {}/{}); retrying in {}ms",
                        status, attempt, MAX_ATTEMPTS, delay.toMillis());
                sleepInterruptibly(delay);
                continue;
            }
            return response;
        }
    }

    private static boolean isRetryable(int status) {
        return status == 429 || (status >= 500 && status < 600);
    }

    private static Duration backoffDelay(int attempt) {
        long base = RETRY_BASE_DELAY.toMillis() << (attempt - 1);
        long jitter = ThreadLocalRandom.current().nextLong(RETRY_JITTER.toMillis() + 1);
        return Duration.ofMillis(base + jitter);
    }

    private static void sleepInterruptibly(Duration delay) throws InterruptedException {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            // Our own sleep was interrupted - restore the flag (callers rely on it) and
            // propagate so the retry loop stops instead of hammering the API.
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }
}
