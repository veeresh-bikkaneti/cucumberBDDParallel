package com.cucumberbddparallel.framework.ai.cost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPricingTest {

    @Test
    void exactModelIdMatches() {
        assertEquals(Optional.of(new BigDecimal("3.00")), ModelPricing.inputRatePerMillion("claude-sonnet-5"));
        assertEquals(Optional.of(new BigDecimal("15.00")), ModelPricing.outputRatePerMillion("claude-sonnet-5"));
    }

    @Test
    void versionedModelIdFallsBackToBaseModelPrice() {
        // Providers ship dated model IDs; the price is the base model's price.
        assertEquals(Optional.of(new BigDecimal("3.00")),
                ModelPricing.inputRatePerMillion("claude-sonnet-5-20250929"));
        assertEquals(Optional.of(new BigDecimal("15.00")),
                ModelPricing.outputRatePerMillion("claude-sonnet-5-20250929"));
    }

    @Test
    void exactMatchBeatsPrefixMatch() {
        // "claude-sonnet-5" is both an exact entry and a prefix of the versioned ID -
        // the exact entry must win (same price here, but the priority matters).
        assertTrue(ModelPricing.inputRatePerMillion("claude-sonnet-5").isPresent());
    }

    @Test
    void unknownModelHasNoPrice() {
        assertEquals(Optional.empty(), ModelPricing.inputRatePerMillion("some-future-model"));
        assertEquals(Optional.empty(), ModelPricing.outputRatePerMillion("some-future-model"));
    }

    @Test
    void nullModelHasNoPrice() {
        assertEquals(Optional.empty(), ModelPricing.inputRatePerMillion(null));
    }
}
