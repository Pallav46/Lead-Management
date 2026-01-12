package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScoringRulesTest {

    private Lead createBaseLead(LeadSource source, int vehicleYear, Integer tradeIn) {
        Lead lead = Lead.newLead(
                "dealer-1", "tenant-1", "site-1",
                "John", "Doe",
                new Email("john@test.com"),
                new PhoneCoordinate("+1", "4155550123"),
                source,
                new VehicleInterest("Toyota", "Camry", vehicleYear, tradeIn)
        );
        lead.setCreatedAt(Instant.now()); // Fresh lead for recency
        return lead;
    }

    // ═══════════════════════════════════════════════════════════════
    // SourceQualityRule tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void sourceQualityRule_shouldReturn1ForReferral() {
        Lead lead = createBaseLead(LeadSource.REFERRAL, 2020, null);
        SourceQualityRule rule = new SourceQualityRule();

        assertEquals(1.0, rule.evaluate(lead));
        assertEquals("sourceQuality", rule.getName());
        assertEquals(20, rule.getWeight());
    }

    @Test
    void sourceQualityRule_shouldReturn0_7ForWebsite() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        assertEquals(0.7, new SourceQualityRule().evaluate(lead));
    }

    @Test
    void sourceQualityRule_shouldReturn0_5ForPhone() {
        Lead lead = createBaseLead(LeadSource.PHONE, 2020, null);
        assertEquals(0.5, new SourceQualityRule().evaluate(lead));
    }

    @Test
    void sourceQualityRule_shouldReturn0_3ForWalkin() {
        Lead lead = createBaseLead(LeadSource.WALKIN, 2020, null);
        assertEquals(0.3, new SourceQualityRule().evaluate(lead));
    }

    @Test
    void sourceQualityRule_shouldReturn0ForNull() {
        assertEquals(0.0, new SourceQualityRule().evaluate(null));
    }

    // ═══════════════════════════════════════════════════════════════
    // VehicleAgeRule tests
    // Scoring: age >= 5 years -> 1.0, age 3-4 -> 0.6, age < 3 -> 0.2
    // ═══════════════════════════════════════════════════════════════

    @Test
    void vehicleAgeRule_shouldReturn1ForOldVehicle() {
        int year = java.time.Year.now().getValue() - 6; // 6 years old
        Lead lead = createBaseLead(LeadSource.WEBSITE, year, null);
        VehicleAgeRule rule = new VehicleAgeRule();

        assertEquals(1.0, rule.evaluate(lead));
        assertEquals("vehicleAge", rule.getName());
        assertEquals(25, rule.getWeight());
    }

    @Test
    void vehicleAgeRule_shouldReturn0_6For3To4YearOld() {
        int year = java.time.Year.now().getValue() - 4; // 4 years old
        Lead lead = createBaseLead(LeadSource.WEBSITE, year, null);

        assertEquals(0.6, new VehicleAgeRule().evaluate(lead));
    }

    @Test
    void vehicleAgeRule_shouldReturn0_2ForNewVehicle() {
        int currentYear = java.time.Year.now().getValue();
        Lead lead = createBaseLead(LeadSource.WEBSITE, currentYear, null); // 0 years old

        assertEquals(0.2, new VehicleAgeRule().evaluate(lead));
    }

    @Test
    void vehicleAgeRule_shouldReturn0ForNullLead() {
        assertEquals(0.0, new VehicleAgeRule().evaluate(null));
    }

    // ═══════════════════════════════════════════════════════════════
    // TradeInValueRule tests
    // Scoring: >10k -> 1.0, >5k -> 0.7, >0 -> 0.4, null/0 -> 0.1
    // ═══════════════════════════════════════════════════════════════

    @Test
    void tradeInValueRule_shouldReturn1ForHighValue() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, 15000);
        TradeInValueRule rule = new TradeInValueRule();

        assertEquals(1.0, rule.evaluate(lead));
        assertEquals("tradeInValue", rule.getName());
        assertEquals(25, rule.getWeight());
    }

    @Test
    void tradeInValueRule_shouldReturn0_7ForMediumValue() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, 7000);

        assertEquals(0.7, new TradeInValueRule().evaluate(lead));
    }

    @Test
    void tradeInValueRule_shouldReturn0_4ForLowValue() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, 2000);

        assertEquals(0.4, new TradeInValueRule().evaluate(lead));
    }

    @Test
    void tradeInValueRule_shouldReturn0_1ForNoTradeIn() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);

        assertEquals(0.1, new TradeInValueRule().evaluate(lead));
    }

    // ═══════════════════════════════════════════════════════════════
    // EngagementRule tests
    // Scoring: QUALIFIED/CONVERTED -> 1.0, CONTACTED -> 0.6, NEW -> 0.2, LOST -> 0.1
    // ═══════════════════════════════════════════════════════════════

    @Test
    void engagementRule_shouldReturn1ForQualified() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.transitionTo(LeadState.CONTACTED);
        lead.transitionTo(LeadState.QUALIFIED);
        EngagementRule rule = new EngagementRule();

        assertEquals(1.0, rule.evaluate(lead));
        assertEquals("engagement", rule.getName());
        assertEquals(15, rule.getWeight());
    }

    @Test
    void engagementRule_shouldReturn0_6ForContacted() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.transitionTo(LeadState.CONTACTED);

        assertEquals(0.6, new EngagementRule().evaluate(lead));
    }

    @Test
    void engagementRule_shouldReturn0_2ForNew() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);

        assertEquals(0.2, new EngagementRule().evaluate(lead));
    }

    @Test
    void engagementRule_shouldReturn0_1ForLost() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.transitionTo(LeadState.LOST);

        assertEquals(0.1, new EngagementRule().evaluate(lead));
    }

    // ═══════════════════════════════════════════════════════════════
    // RecencyRule tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void recencyRule_shouldReturn1ForFreshLead() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        RecencyRule rule = new RecencyRule();

        assertEquals(1.0, rule.evaluate(lead));
        assertEquals("recency", rule.getName());
        assertEquals(15, rule.getWeight());
    }

    @Test
    void recencyRule_shouldReturn0_7For3DayOld() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.setCreatedAt(Instant.now().minus(3, ChronoUnit.DAYS));

        assertEquals(0.7, new RecencyRule().evaluate(lead));
    }

    @Test
    void recencyRule_shouldReturn0_4For15DayOld() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.setCreatedAt(Instant.now().minus(15, ChronoUnit.DAYS));

        assertEquals(0.4, new RecencyRule().evaluate(lead));
    }

    @Test
    void recencyRule_shouldReturn0_1For60DayOld() {
        Lead lead = createBaseLead(LeadSource.WEBSITE, 2020, null);
        lead.setCreatedAt(Instant.now().minus(60, ChronoUnit.DAYS));

        assertEquals(0.1, new RecencyRule().evaluate(lead));
    }
}

