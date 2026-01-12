package com.tekion.leadmanagement.domain.scoring.service;

import com.tekion.leadmanagement.domain.lead.model.*;
import com.tekion.leadmanagement.domain.scoring.model.ScoringResult;
import com.tekion.leadmanagement.domain.scoring.rule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for bulk scoring operations in LeadScoringEngine.
 */
class BulkScoringTest {

    private LeadScoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LeadScoringEngine(List.of(
                new SourceQualityRule(),
                new VehicleAgeRule(),
                new TradeInValueRule(),
                new EngagementRule(),
                new RecencyRule()
        ));
    }

    @Test
    @DisplayName("Should score batch of leads")
    void shouldScoreBatch() {
        List<Lead> leads = createTestLeads(5);

        Map<String, ScoringResult> results = engine.scoreBatch(leads);

        assertEquals(5, results.size());
        for (Lead lead : leads) {
            assertTrue(results.containsKey(lead.getLeadId()));
            ScoringResult result = results.get(lead.getLeadId());
            assertTrue(result.getFinalScore() >= 0 && result.getFinalScore() <= 100);
        }
    }

    @Test
    @DisplayName("Should return empty map for empty list")
    void shouldReturnEmptyForEmptyList() {
        Map<String, ScoringResult> results = engine.scoreBatch(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should reject null leads list")
    void shouldRejectNullList() {
        assertThrows(IllegalArgumentException.class, () -> engine.scoreBatch(null));
    }

    @Test
    @DisplayName("Should skip null leads in batch")
    void shouldSkipNullLeads() {
        List<Lead> leads = new ArrayList<>();
        leads.add(createLead("lead-1"));
        leads.add(null);
        leads.add(createLead("lead-2"));

        Map<String, ScoringResult> results = engine.scoreBatch(leads);

        assertEquals(2, results.size());
        assertTrue(results.containsKey("lead-1"));
        assertTrue(results.containsKey("lead-2"));
    }

    @Test
    @DisplayName("Should score and update batch in-place")
    void shouldScoreAndUpdateBatch() {
        List<Lead> leads = createTestLeads(3);

        // Verify scores are null initially
        for (Lead lead : leads) {
            assertNull(lead.getScore());
        }

        List<Lead> result = engine.scoreAndUpdateBatch(leads);

        assertSame(leads, result);
        for (Lead lead : leads) {
            assertNotNull(lead.getScore());
            assertTrue(lead.getScore() >= 0 && lead.getScore() <= 100);
        }
    }

    @Test
    @DisplayName("Should reject null list for scoreAndUpdateBatch")
    void shouldRejectNullForScoreAndUpdate() {
        assertThrows(IllegalArgumentException.class, () -> engine.scoreAndUpdateBatch(null));
    }

    @Test
    @DisplayName("Bulk scoring should produce same results as individual scoring")
    void bulkShouldMatchIndividual() {
        List<Lead> leads = createTestLeads(10);

        // Score individually
        Map<String, Integer> individualScores = new java.util.HashMap<>();
        for (Lead lead : leads) {
            individualScores.put(lead.getLeadId(), engine.score(lead).getFinalScore());
        }

        // Score in batch
        Map<String, ScoringResult> batchResults = engine.scoreBatch(leads);

        // Compare
        for (Lead lead : leads) {
            assertEquals(
                    individualScores.get(lead.getLeadId()),
                    batchResults.get(lead.getLeadId()).getFinalScore()
            );
        }
    }

    private List<Lead> createTestLeads(int count) {
        List<Lead> leads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            leads.add(createLead("lead-" + i));
        }
        return leads;
    }

    private Lead createLead(String leadId) {
        Lead lead = Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "First" + leadId,
                "Last",
                new Email(leadId + "@example.com"),
                new PhoneCoordinate("+1", "555" + Math.abs(leadId.hashCode())),
                LeadSource.WEBSITE,
                new VehicleInterest("Toyota", "Camry", 2022, 5000)
        );
        // Override the auto-generated ID for predictable testing
        lead.setLeadId(leadId);
        return lead;
    }
}

