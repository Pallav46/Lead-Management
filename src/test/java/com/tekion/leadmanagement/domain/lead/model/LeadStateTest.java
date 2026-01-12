package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeadStateTest {

    @Test
    void shouldAllowValidTransitions() {
        assertTrue(LeadState.NEW.canTransitionTo(LeadState.CONTACTED));
        assertTrue(LeadState.NEW.canTransitionTo(LeadState.LOST));

        assertTrue(LeadState.CONTACTED.canTransitionTo(LeadState.QUALIFIED));
        assertTrue(LeadState.CONTACTED.canTransitionTo(LeadState.LOST));

        assertTrue(LeadState.QUALIFIED.canTransitionTo(LeadState.CONVERTED));
        assertTrue(LeadState.QUALIFIED.canTransitionTo(LeadState.LOST));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(LeadState.NEW.canTransitionTo(LeadState.CONVERTED));
        assertFalse(LeadState.NEW.canTransitionTo(LeadState.QUALIFIED));

        assertFalse(LeadState.CONTACTED.canTransitionTo(LeadState.CONVERTED));

        assertFalse(LeadState.CONVERTED.canTransitionTo(LeadState.NEW));
        assertFalse(LeadState.LOST.canTransitionTo(LeadState.CONTACTED));
    }

    @Test
    void terminalStatesShouldBeTerminal() {
        assertTrue(LeadState.CONVERTED.isTerminal());
        assertTrue(LeadState.LOST.isTerminal());

        assertFalse(LeadState.NEW.isTerminal());
        assertFalse(LeadState.CONTACTED.isTerminal());
        assertFalse(LeadState.QUALIFIED.isTerminal());
    }

    @Test
    void shouldHaveDisplayNames() {
        assertEquals("New", LeadState.NEW.getDisplayName());
        assertEquals("Contacted", LeadState.CONTACTED.getDisplayName());
    }
}

