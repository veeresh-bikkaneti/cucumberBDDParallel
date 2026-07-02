package com.cucumberbddparallel.framework.ai.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public final class CostLogger {

    private static final Logger LOG = LoggerFactory.getLogger(CostLogger.class);
    private static final AtomicReference<BigDecimal> SESSION_TOTAL = new AtomicReference<>(BigDecimal.ZERO);
    private static volatile boolean shutdownHookRegistered = false;

    private CostLogger() {
    }

    public static void logHealCall(String elementName, String model, TokenUsage usage) {
        registerShutdownHookOnce();
        CostCalculator.cost(usage, model).ifPresentOrElse(
                cost -> {
                    SESSION_TOTAL.updateAndGet(total -> total.add(cost));
                    LOG.info("AI locator heal: element={} model={} in={} out={} cost=${}",
                            elementName, model, usage.inputTokens(), usage.outputTokens(), cost);
                },
                () -> LOG.warn("AI locator heal: element={} model={} in={} out={} cost=unknown, "
                                + "no pricing entry for this model",
                        elementName, model, usage.inputTokens(), usage.outputTokens())
        );
    }

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
