package com.tekion.leadmanagement.domain.lead.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Immutable audit trail entry for tracking lead state transitions.
 *
 * <h2>Purpose</h2>
 * <p>Records all state changes for compliance, debugging, and analytics:
 * <ul>
 *   <li>Who made the change (actor)</li>
 *   <li>What changed (from/to states)</li>
 *   <li>When it happened (timestamp)</li>
 *   <li>Why it changed (optional reason)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * AuditEntry entry = AuditEntry.builder()
 *     .timestamp(Instant.now())
 *     .actor("sales-rep-123")
 *     .fromState(LeadState.NEW)
 *     .toState(LeadState.CONTACTED)
 *     .reason("Initial phone call completed")
 *     .build();
 * }</pre>
 *
 * @see Lead#transitionTo(LeadState, String, String) for creating audit entries
 */
@Value
@Builder
public class AuditEntry {

    /**
     * When the state transition occurred.
     */
    Instant timestamp;

    /**
     * Who triggered the transition (user ID, system process, etc.).
     */
    String actor;

    /**
     * The previous state before transition.
     */
    LeadState fromState;

    /**
     * The new state after transition.
     */
    LeadState toState;

    /**
     * Optional reason or notes for the transition.
     */
    String reason;

    /**
     * Creates a formatted log message for this audit entry.
     *
     * @return Human-readable audit log line
     */
    public String toLogMessage() {
        return String.format("[%s] %s: %s → %s%s",
                timestamp,
                actor != null ? actor : "SYSTEM",
                fromState,
                toState,
                reason != null ? " (" + reason + ")" : "");
    }
}

