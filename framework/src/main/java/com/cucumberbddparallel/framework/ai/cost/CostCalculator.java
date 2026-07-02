package com.cucumberbddparallel.framework.ai.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Turns a token count into a dollar amount, using the rates in {@link ModelPricing}.
 *
 * Returns {@link Optional#empty()} instead of a dollar amount when we don't have a price
 * for the given model, rather than guessing or throwing. That matters because
 * ANTHROPIC_MODEL is an environment override - someone could point it at a brand-new model
 * we haven't added a price for yet, and "silently report $0.00" would be worse than "we
 * genuinely don't know." See {@code CostLogger} for how that empty case gets logged.
 */
public final class CostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private CostCalculator() {
    }

    /** Dollar cost for this many input/output tokens at {@code model}'s rates, or empty if the model is unpriced. */
    public static Optional<BigDecimal> cost(TokenUsage usage, String model) {
        Optional<BigDecimal> inputRate = ModelPricing.inputRatePerMillion(model);
        Optional<BigDecimal> outputRate = ModelPricing.outputRatePerMillion(model);
        if (inputRate.isEmpty() || outputRate.isEmpty()) {
            return Optional.empty();
        }
        // Pricing is quoted per million tokens, so scale down after multiplying. 6 decimal
        // places keeps single-cent-and-below costs (the common case for one heal call)
        // from rounding away to nothing.
        BigDecimal inputCost = inputRate.get()
                .multiply(BigDecimal.valueOf(usage.inputTokens()))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputRate.get()
                .multiply(BigDecimal.valueOf(usage.outputTokens()))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        return Optional.of(inputCost.add(outputCost));
    }
}
