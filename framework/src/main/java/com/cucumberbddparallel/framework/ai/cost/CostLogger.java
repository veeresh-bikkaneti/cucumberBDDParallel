package com.cucumberbddparallel.framework.ai.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Logs the dollar cost of every AI healing call, plus a running total for the whole JVM run.
 *
 * This is deliberately "just logging," not a dashboard or a database - the point is that if
 * you're scrolling through CI output (or your local console) and healing kicked in, you can
 * see exactly what it cost right there next to the healing attempt, and see the session
 * total when the run finishes. See PLAYBOOK.md for how CI pulls these same log lines into
 * the job summary.
 */
public final class CostLogger {

    private static final Logger LOG = LoggerFactory.getLogger(CostLogger.class);
    private static final AtomicReference<BigDecimal> SESSION_TOTAL = new AtomicReference<>(BigDecimal.ZERO);
    private static volatile boolean shutdownHookRegistered = false;

    private CostLogger() {
    }

    /** Call this once per healing attempt - logs the cost of that one call and adds it to the running total. */
    public static void logHealCall(String elementName, String model, TokenUsage usage) {
        registerShutdownHookOnce();
        CostCalculator.cost(usage, model).ifPresentOrElse(
                cost -> {
                    SESSION_TOTAL.updateAndGet(total -> total.add(cost));
                    LOG.info("AI locator heal: element={} model={} in={} out={} cost=${}",
                            elementName, model, usage.inputTokens(), usage.outputTokens(), cost);
                },
                // Still worth logging even when we can't price it - you at least see that
                // healing happened and how many tokens it used, just not the dollar figure.
                () -> LOG.warn("AI locator heal: element={} model={} in={} out={} cost=unknown, "
                                + "no pricing entry for this model",
                        elementName, model, usage.inputTokens(), usage.outputTokens())
        );
    }

    // A test suite can call logHealCall() many times, from many test threads, all within
    // the same JVM - we only want ONE shutdown hook logging the total once at the very end,
    // not one per call. The volatile check outside the lock is just a fast path so most
    // calls (after the very first) skip the synchronized block entirely; the check is
    // repeated inside the lock because two threads could both pass the outside check before
    // either one grabs the lock.
    private static void registerShutdownHookOnce() {
        if (shutdownHookRegistered) {
            return;
        }
        synchronized (CostLogger.class) {
            if (shutdownHookRegistered) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
                    LOG.info("AI locator healing session total: ${}", SESSION_TOTAL.get())));
            shutdownHookRegistered = true;
        }
    }
}
