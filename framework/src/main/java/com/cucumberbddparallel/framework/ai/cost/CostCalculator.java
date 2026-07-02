package com.cucumberbddparallel.framework.ai.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class CostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private CostCalculator() {
    }

    public static Optional<BigDecimal> cost(TokenUsage usage, String model) {
        Optional<BigDecimal> inputRate = ModelPricing.inputRatePerMillion(model);
        Optional<BigDecimal> outputRate = ModelPricing.outputRatePerMillion(model);
        if (inputRate.isEmpty() || outputRate.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal inputCost = inputRate.get()
                .multiply(BigDecimal.valueOf(usage.inputTokens()))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputRate.get()
                .multiply(BigDecimal.valueOf(usage.outputTokens()))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        return Optional.of(inputCost.add(outputCost));
    }
}
