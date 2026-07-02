package com.cucumberbddparallel.framework.ai.cost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostCalculatorTest {

    @Test
    void computesCostForKnownModel() {
        TokenUsage usage = new TokenUsage(1000, 200);

        BigDecimal cost = CostCalculator.cost(usage, "claude-sonnet-5").orElseThrow();

        // 1000 input tokens @ $3/million + 200 output tokens @ $15/million
        BigDecimal expected = new BigDecimal("3.00").multiply(new BigDecimal("1000"))
                .divide(new BigDecimal("1000000"))
                .add(new BigDecimal("15.00").multiply(new BigDecimal("200"))
                        .divide(new BigDecimal("1000000")));
        assertTrue(cost.subtract(expected).abs().compareTo(new BigDecimal("0.000001")) < 0);
    }

    @Test
    void returnsEmptyForUnknownModel() {
        TokenUsage usage = new TokenUsage(1000, 200);

        assertEquals(java.util.Optional.empty(), CostCalculator.cost(usage, "some-future-model"));
    }
}
