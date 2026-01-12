package com.tekion.leadmanagement.application.lead;

import com.tekion.leadmanagement.adapter.persistence.inmemory.InMemoryLeadRepository;
import com.tekion.leadmanagement.domain.lead.model.*;
import com.tekion.leadmanagement.domain.lead.port.LeadPersistencePort;
import com.tekion.leadmanagement.domain.scoring.rule.*;
import com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LeadServiceTest {

    private LeadPersistencePort repo;
    private LeadScoringEngine scoringEngine;
    private LeadService leadService;

    @BeforeEach
    void setUp() {
        repo = new InMemoryLeadRepository();
        scoringEngine = new LeadScoringEngine(List.of(
                new SourceQualityRule(),
                new VehicleAgeRule(),
                new TradeInValueRule(),
                new EngagementRule(),
                new RecencyRule()
        ));
        leadService = new LeadService(repo, scoringEngine);
    }

    private Lead createTestLead(String dealerId, String firstName) {
        return Lead.newLead(
                dealerId,
                "tenant-1",
                "site-1",
                firstName,
                "Test",
                new Email(firstName.toLowerCase() + "@test.com"),
                new PhoneCoordinate("+1", "4155550123"),
                LeadSource.WEBSITE,
                new VehicleInterest("Toyota", "Camry", 2020, 15000)
        );
    }

    @Test
    void shouldCreateLead() {
        Lead lead = createTestLead("dealer-1", "John");

        Lead saved = leadService.create(lead);

        assertNotNull(saved);
        assertEquals(lead.getLeadId(), saved.getLeadId());
    }

    @Test
    void shouldThrowWhenCreatingNullLead() {
        assertThrows(IllegalArgumentException.class, () -> leadService.create(null));
    }

    @Test
    void shouldFindLeadByIdAndDealerId() {
        Lead lead = createTestLead("dealer-1", "Jane");
        leadService.create(lead);

        Optional<Lead> found = leadService.findByIdAndDealerId(lead.getLeadId(), "dealer-1");

        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
    }

    @Test
    void shouldReturnEmptyWhenLeadNotFound() {
        Optional<Lead> found = leadService.findByIdAndDealerId("non-existent", "dealer-1");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldTransitionState() {
        Lead lead = createTestLead("dealer-1", "Bob");
        leadService.create(lead);
        assertEquals(LeadState.NEW, lead.getState());

        Lead transitioned = leadService.transitionState(lead.getLeadId(), "dealer-1", LeadState.CONTACTED);

        assertEquals(LeadState.CONTACTED, transitioned.getState());
    }

    @Test
    void shouldThrowOnInvalidStateTransition() {
        Lead lead = createTestLead("dealer-1", "Alice");
        leadService.create(lead);

        // NEW -> CONVERTED is not allowed (must go through CONTACTED -> QUALIFIED first)
        assertThrows(IllegalStateException.class, () ->
                leadService.transitionState(lead.getLeadId(), "dealer-1", LeadState.CONVERTED)
        );
    }

    @Test
    void shouldThrowWhenTransitioningNonExistentLead() {
        assertThrows(IllegalArgumentException.class, () ->
                leadService.transitionState("non-existent", "dealer-1", LeadState.CONTACTED)
        );
    }

    @Test
    void shouldComputeAndPersistScore() {
        Lead lead = createTestLead("dealer-1", "Charlie");
        leadService.create(lead);
        assertNull(lead.getScore()); // Initially no score

        leadService.computeAndPersistScore(lead.getLeadId(), "dealer-1");

        Lead updated = leadService.findByIdAndDealerId(lead.getLeadId(), "dealer-1").orElseThrow();
        assertNotNull(updated.getScore());
        assertTrue(updated.getScore() >= 0 && updated.getScore() <= 100);
    }

    @Test
    void shouldScoreLeadWithoutPersisting() {
        Lead lead = createTestLead("dealer-1", "Diana");
        leadService.create(lead);

        var result = leadService.scoreLead(lead.getLeadId(), "dealer-1");

        assertNotNull(result);
        assertNotNull(result.getBreakdown());
        assertEquals(5, result.getBreakdown().size());

        // Lead should still have no persisted score
        Lead fetched = leadService.findByIdAndDealerId(lead.getLeadId(), "dealer-1").orElseThrow();
        assertNull(fetched.getScore());
    }

    @Test
    void shouldEnforceMultiTenantIsolation() {
        Lead dealer1Lead = createTestLead("dealer-1", "User1");
        Lead dealer2Lead = createTestLead("dealer-2", "User2");
        leadService.create(dealer1Lead);
        leadService.create(dealer2Lead);

        // Dealer-1 cannot access Dealer-2's lead
        Optional<Lead> crossAccess = leadService.findByIdAndDealerId(dealer2Lead.getLeadId(), "dealer-1");
        assertFalse(crossAccess.isPresent());

        // Dealer-2 cannot access Dealer-1's lead
        Optional<Lead> reverseAccess = leadService.findByIdAndDealerId(dealer1Lead.getLeadId(), "dealer-2");
        assertFalse(reverseAccess.isPresent());
    }
}

