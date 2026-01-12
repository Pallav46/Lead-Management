package com.tekion.leadmanagement.domain.scoring.service;

import com.tekion.leadmanagement.domain.lead.model.*;
import com.tekion.leadmanagement.domain.scoring.model.ScoringResult;
import com.tekion.leadmanagement.domain.scoring.rule.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeadScoringEngineTest {

    private Lead createTestLead() {
        return Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "Priya",
                "Shah",
                new Email("priya@tekion.com"),
                new PhoneCoordinate("+1", "4155550123"),
                LeadSource.REFERRAL,
                new VehicleInterest("Toyota", "Camry", 2016, 12000)
        );
    }

    @Test
    void shouldComputeScoreAndBreakdown() {
        LeadScoringEngine engine = new LeadScoringEngine(List.of(
                new SourceQualityRule(),
                new VehicleAgeRule(),
                new TradeInValueRule(),
                new EngagementRule(),
                new RecencyRule()
        ));

        Lead lead = createTestLead();
        lead.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        lead.setState(LeadState.QUALIFIED);

        ScoringResult result = engine.score(lead);

        assertNotNull(result);
        assertTrue(result.getFinalScore() >= 0 && result.getFinalScore() <= 100);

        assertNotNull(result.getBreakdown());
        assertEquals(5, result.getBreakdown().size());

        assertEquals(1.0, result.getBreakdown().get("sourceQuality"));
        assertNotNull(result.getBreakdown().get("vehicleAge"));
        assertEquals(1.0, result.getBreakdown().get("tradeInValue"));
        assertEquals(1.0, result.getBreakdown().get("engagement"));
        assertEquals(1.0, result.getBreakdown().get("recency"));
    }

    @Test
    void shouldThrowOnNullRules() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeadScoringEngine(null)
        );
    }

    @Test
    void shouldThrowOnEmptyRules() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeadScoringEngine(List.of())
        );
    }

    @Test
    void shouldThrowOnNullLead() {
        LeadScoringEngine engine = new LeadScoringEngine(List.of(new SourceQualityRule()));

        assertThrows(IllegalArgumentException.class, () ->
                engine.score(null)
        );
    }

    @Test
    void shouldClampNegativeFactorsToZero() {
        // Create a mock rule that returns negative value
        ScoringRule negativeRule = new ScoringRule() {
            @Override
            public String getName() { return "negative"; }
            @Override
            public int getWeight() { return 10; }
            @Override
            public double evaluate(Lead lead) { return -0.5; }
        };

        LeadScoringEngine engine = new LeadScoringEngine(List.of(negativeRule));
        Lead lead = createTestLead();

        ScoringResult result = engine.score(lead);

        assertEquals(0.0, result.getBreakdown().get("negative"));
        assertEquals(0, result.getFinalScore());
    }

    @Test
    void shouldClampFactorsAbove1ToOne() {
        // Create a mock rule that returns > 1
        ScoringRule overOneRule = new ScoringRule() {
            @Override
            public String getName() { return "overOne"; }
            @Override
            public int getWeight() { return 10; }
            @Override
            public double evaluate(Lead lead) { return 1.5; }
        };

        LeadScoringEngine engine = new LeadScoringEngine(List.of(overOneRule));
        Lead lead = createTestLead();

        ScoringResult result = engine.score(lead);

        assertEquals(1.0, result.getBreakdown().get("overOne"));
        assertEquals(100, result.getFinalScore());
    }

    @Test
    void shouldComputeWeightedAverage() {
        // Rule with weight 60, factor 1.0
        ScoringRule rule1 = new ScoringRule() {
            @Override
            public String getName() { return "rule1"; }
            @Override
            public int getWeight() { return 60; }
            @Override
            public double evaluate(Lead lead) { return 1.0; }
        };

        // Rule with weight 40, factor 0.0
        ScoringRule rule2 = new ScoringRule() {
            @Override
            public String getName() { return "rule2"; }
            @Override
            public int getWeight() { return 40; }
            @Override
            public double evaluate(Lead lead) { return 0.0; }
        };

        LeadScoringEngine engine = new LeadScoringEngine(List.of(rule1, rule2));
        Lead lead = createTestLead();

        ScoringResult result = engine.score(lead);

        // (1.0*60 + 0.0*40) / 100 = 0.6 -> 60
        assertEquals(60, result.getFinalScore());
    }

    @Test
    void shouldThrowOnNegativeWeight() {
        ScoringRule negativeWeightRule = new ScoringRule() {
            @Override
            public String getName() { return "badWeight"; }
            @Override
            public int getWeight() { return -10; }
            @Override
            public double evaluate(Lead lead) { return 1.0; }
        };

        LeadScoringEngine engine = new LeadScoringEngine(List.of(negativeWeightRule));
        Lead lead = createTestLead();

        assertThrows(IllegalArgumentException.class, () -> engine.score(lead));
    }

    @Test
    void shouldReturnZeroWhenTotalWeightIsZero() {
        ScoringRule zeroWeightRule = new ScoringRule() {
            @Override
            public String getName() { return "zeroWeight"; }
            @Override
            public int getWeight() { return 0; }
            @Override
            public double evaluate(Lead lead) { return 1.0; }
        };

        LeadScoringEngine engine = new LeadScoringEngine(List.of(zeroWeightRule));
        Lead lead = createTestLead();

        ScoringResult result = engine.score(lead);

        assertEquals(0, result.getFinalScore());
    }
}

