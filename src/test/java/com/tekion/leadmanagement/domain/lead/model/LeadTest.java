package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LeadTest {

    private Lead createValidLead() {
        return Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "John",
                "Doe",
                new Email("john@test.com"),
                new PhoneCoordinate("+1", "4155550123"),
                LeadSource.WEBSITE,
                new VehicleInterest("Toyota", "Camry", 2020, 15000)
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // newLead factory tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldCreateLeadWithAllRequiredFields() {
        Lead lead = createValidLead();

        assertNotNull(lead.getLeadId());
        assertEquals("dealer-1", lead.getDealerId());
        assertEquals("tenant-1", lead.getTenantId());
        assertEquals("site-1", lead.getSiteId());
        assertEquals("John", lead.getFirstName());
        assertEquals("Doe", lead.getLastName());
        assertEquals(LeadState.NEW, lead.getState());
        assertNotNull(lead.getCreatedAt());
        assertNotNull(lead.getUpdatedAt());
    }

    @Test
    void shouldRejectBlankDealerId() {
        assertThrows(IllegalArgumentException.class, () ->
                Lead.newLead("  ", "tenant-1", "site-1", "John", "Doe",
                        new Email("john@test.com"),
                        new PhoneCoordinate("+1", "4155550123"),
                        LeadSource.WEBSITE,
                        new VehicleInterest("Toyota", "Camry", 2020, null))
        );
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                Lead.newLead("dealer-1", "tenant-1", "site-1", "John", "Doe",
                        null,
                        new PhoneCoordinate("+1", "4155550123"),
                        LeadSource.WEBSITE,
                        new VehicleInterest("Toyota", "Camry", 2020, null))
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // transitionTo tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldTransitionFromNewToContacted() {
        Lead lead = createValidLead();
        Instant beforeTransition = lead.getUpdatedAt();

        lead.transitionTo(LeadState.CONTACTED);

        assertEquals(LeadState.CONTACTED, lead.getState());
        assertTrue(lead.getUpdatedAt().isAfter(beforeTransition) || lead.getUpdatedAt().equals(beforeTransition));
    }

    @Test
    void shouldTransitionFromNewToLost() {
        Lead lead = createValidLead();
        lead.transitionTo(LeadState.LOST);
        assertEquals(LeadState.LOST, lead.getState());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Lead lead = createValidLead();
        // NEW -> CONVERTED is not allowed
        assertThrows(IllegalStateException.class, () -> lead.transitionTo(LeadState.CONVERTED));
    }

    @Test
    void shouldRejectNullTargetState() {
        Lead lead = createValidLead();
        assertThrows(IllegalArgumentException.class, () -> lead.transitionTo(null));
    }

    @Test
    void shouldCompleteFullHappyPath() {
        Lead lead = createValidLead();

        lead.transitionTo(LeadState.CONTACTED);
        assertEquals(LeadState.CONTACTED, lead.getState());

        lead.transitionTo(LeadState.QUALIFIED);
        assertEquals(LeadState.QUALIFIED, lead.getState());

        lead.transitionTo(LeadState.CONVERTED);
        assertEquals(LeadState.CONVERTED, lead.getState());
    }

    // ═══════════════════════════════════════════════════════════════
    // updateScore tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldUpdateScoreWithValidValue() {
        Lead lead = createValidLead();
        Instant beforeUpdate = lead.getUpdatedAt();

        lead.updateScore(75);

        assertEquals(75, lead.getScore());
        assertTrue(lead.getUpdatedAt().isAfter(beforeUpdate) || lead.getUpdatedAt().equals(beforeUpdate));
    }

    @Test
    void shouldAcceptBoundaryScores() {
        Lead lead = createValidLead();

        lead.updateScore(0);
        assertEquals(0, lead.getScore());

        lead.updateScore(100);
        assertEquals(100, lead.getScore());
    }

    @Test
    void shouldRejectNegativeScore() {
        Lead lead = createValidLead();
        assertThrows(IllegalArgumentException.class, () -> lead.updateScore(-1));
    }

    @Test
    void shouldRejectScoreOver100() {
        Lead lead = createValidLead();
        assertThrows(IllegalArgumentException.class, () -> lead.updateScore(101));
    }
}

