package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lead audit trail functionality.
 */
class AuditTrailTest {

    private Lead lead;

    @BeforeEach
    void setUp() {
        lead = Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "John",
                "Doe",
                new Email("john@example.com"),
                new PhoneCoordinate("+1", "5551234567"),
                LeadSource.WEBSITE,
                new VehicleInterest("Toyota", "Camry", 2022, null)
        );
    }

    @Test
    @DisplayName("Should create audit entry on state transition with actor")
    void shouldCreateAuditEntryWithActor() {
        lead.transitionTo(LeadState.CONTACTED, "sales-rep-123", "Initial phone call");

        List<AuditEntry> trail = lead.getAuditTrail();
        assertEquals(1, trail.size());

        AuditEntry entry = trail.get(0);
        assertEquals("sales-rep-123", entry.getActor());
        assertEquals(LeadState.NEW, entry.getFromState());
        assertEquals(LeadState.CONTACTED, entry.getToState());
        assertEquals("Initial phone call", entry.getReason());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    @DisplayName("Should use SYSTEM as default actor when null")
    void shouldUseSystemAsDefaultActor() {
        lead.transitionTo(LeadState.CONTACTED, null, null);

        AuditEntry entry = lead.getAuditTrail().get(0);
        assertEquals("SYSTEM", entry.getActor());
    }

    @Test
    @DisplayName("Should preserve audit trail through multiple transitions")
    void shouldPreserveAuditTrailThroughTransitions() {
        lead.transitionTo(LeadState.CONTACTED, "rep-1", "Called customer");
        lead.transitionTo(LeadState.QUALIFIED, "rep-1", "Customer interested");
        lead.transitionTo(LeadState.CONVERTED, "manager-1", "Deal closed");

        List<AuditEntry> trail = lead.getAuditTrail();
        assertEquals(3, trail.size());

        assertEquals(LeadState.NEW, trail.get(0).getFromState());
        assertEquals(LeadState.CONTACTED, trail.get(0).getToState());

        assertEquals(LeadState.CONTACTED, trail.get(1).getFromState());
        assertEquals(LeadState.QUALIFIED, trail.get(1).getToState());

        assertEquals(LeadState.QUALIFIED, trail.get(2).getFromState());
        assertEquals(LeadState.CONVERTED, trail.get(2).getToState());
    }

    @Test
    @DisplayName("Should work with overloaded transitionTo without actor")
    void shouldWorkWithoutActor() {
        lead.transitionTo(LeadState.CONTACTED);

        List<AuditEntry> trail = lead.getAuditTrail();
        assertEquals(1, trail.size());
        assertEquals("SYSTEM", trail.get(0).getActor());
    }

    @Test
    @DisplayName("Audit trail should be unmodifiable")
    void auditTrailShouldBeUnmodifiable() {
        lead.transitionTo(LeadState.CONTACTED);

        List<AuditEntry> trail = lead.getAuditTrail();
        assertThrows(UnsupportedOperationException.class, 
                () -> trail.add(AuditEntry.builder().build()));
    }

    @Test
    @DisplayName("AuditEntry toLogMessage should format correctly")
    void auditEntryToLogMessageShouldFormat() {
        lead.transitionTo(LeadState.CONTACTED, "rep-1", "Test reason");

        AuditEntry entry = lead.getAuditTrail().get(0);
        String logMessage = entry.toLogMessage();

        assertTrue(logMessage.contains("rep-1"));
        assertTrue(logMessage.contains("NEW"));
        assertTrue(logMessage.contains("CONTACTED"));
        assertTrue(logMessage.contains("Test reason"));
    }

    @Test
    @DisplayName("Empty audit trail should return empty list")
    void emptyAuditTrailShouldReturnEmptyList() {
        // New lead has no transitions yet
        List<AuditEntry> trail = lead.getAuditTrail();
        assertNotNull(trail);
        assertTrue(trail.isEmpty());
    }
}

