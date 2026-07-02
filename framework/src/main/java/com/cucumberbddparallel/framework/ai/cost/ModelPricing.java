package com.cucumberbddparallel.framework.ai.cost;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Dollar-per-million-token rates by model. These are a snapshot, not live
 * data - check https://www.anthropic.com/pricing before using them for
 * real budget planning.
 *
 * This is intentionally just a lookup table, not an if/else chain full of model name
 * checks. Adding a new model, or updating a price when Anthropic changes one, is a
 * one-line change here - nothing else in the codebase needs to know or care.
 */
public final class ModelPricing {

    private record Rate(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
    }

    private static final Map<String, Rate> RATES = Map.of(
            "claude-opus-4-8", new Rate(new BigDecimal("15.00"), new BigDecimal("75.00")),
            "claude-sonnet-5", new Rate(new BigDecimal("3.00"), new BigDecimal("15.00")),
            "claude-haiku-4-5", new Rate(new BigDecimal("0.80"), new BigDecimal("4.00"))
    );

    private ModelPricing() {
    }

    /** Empty if we don't have a price for this model - see {@link CostCalculator} for how that's handled. */
    public static Optional<BigDecimal> inputRatePerMillion(String model) {
        return Optional.ofNullable(RATES.get(model)).map(Rate::inputPerMillion);
    }

    public static Optional<BigDecimal> outputRatePerMillion(String model) {
        return Optional.ofNullable(RATES.get(model)).map(Rate::outputPerMillion);
    }
}
