package com.tekion.leadmanagement.domain.lead.model;

/**
 * Represents the lifecycle state of a Lead in the sales pipeline.
 *
 * <h2>State Machine</h2>
 * <p>Leads progress through a defined workflow:
 * <pre>
 *   ┌─────────────────────────────────────────────────────────┐
 *   │                    LEAD STATE MACHINE                   │
 *   ├─────────────────────────────────────────────────────────┤
 *   │                                                         │
 *   │   NEW ──→ CONTACTED ──→ QUALIFIED ──→ CONVERTED        │
 *   │    │          │             │           (terminal)      │
 *   │    │          │             │                           │
 *   │    └──────────┴─────────────┴────────→ LOST            │
 *   │                                        (terminal)       │
 *   │                                                         │
 *   └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Valid Transitions</h2>
 * <ul>
 *   <li>NEW → CONTACTED (sales rep made first contact)</li>
 *   <li>NEW → LOST (lead unresponsive or unqualified)</li>
 *   <li>CONTACTED → QUALIFIED (customer shows genuine interest)</li>
 *   <li>CONTACTED → LOST (customer not interested)</li>
 *   <li>QUALIFIED → CONVERTED (deal closed successfully)</li>
 *   <li>QUALIFIED → LOST (deal fell through)</li>
 * </ul>
 *
 * <h2>Terminal States</h2>
 * <p>CONVERTED and LOST are terminal states - no further transitions allowed.
 * This prevents accidental reactivation of closed leads.
 *
 * <h2>Idempotent Transitions</h2>
 * <p>Transitioning to the same state (e.g., NEW → NEW) is always allowed
 * for idempotency in distributed systems.
 */
public enum LeadState {

    /** Initial state when lead is first created. Sales rep has not made contact yet. */
    NEW(false, "New"),

    /** Sales rep has made initial contact with the customer. */
    CONTACTED(false, "Contacted"),

    /** Customer has been vetted and shows genuine purchase intent. */
    QUALIFIED(false, "Qualified"),

    /** Deal successfully closed. Terminal state - no further transitions. */
    CONVERTED(true, "Converted"),

    /** Lead was lost (unresponsive, not interested, etc.). Terminal state. */
    LOST(true, "Lost");

    /** Whether this state is terminal (no further transitions allowed). */
    private final boolean terminal;

    /** Human-readable name for UI display. */
    private final String displayName;

    LeadState(boolean terminal, String displayName) {
        this.terminal = terminal;
        this.displayName = displayName;
    }

    /**
     * Returns whether this is a terminal (final) state.
     *
     * @return true if no further transitions are allowed from this state
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return The display name (e.g., "New", "Contacted")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines if a transition from this state to the target state is valid.
     *
     * <h3>Rules:</h3>
     * <ol>
     *   <li>Null target is never valid</li>
     *   <li>Same-state transitions are always valid (idempotency)</li>
     *   <li>Terminal states cannot transition to anything else</li>
     *   <li>Specific allowed transitions are defined per state</li>
     * </ol>
     *
     * @param target The desired target state
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(LeadState target) {
        // Rule 1: null target is never valid
        if (target == null) return false;

        // Rule 2: idempotent transition (same state) is always allowed
        if (this == target) return true;

        // Rule 3: terminal states cannot change
        if (this.terminal) return false;

        // Rule 4: specific transition rules per state
        switch (this) {
            case NEW:
                // From NEW: can contact the lead or mark as lost
                return target == CONTACTED || target == LOST;
            case CONTACTED:
                // From CONTACTED: can qualify the lead or mark as lost
                return target == QUALIFIED || target == LOST;
            case QUALIFIED:
                // From QUALIFIED: can convert (close deal) or mark as lost
                return target == CONVERTED || target == LOST;
            case CONVERTED:
            case LOST:
                // Terminal states - already handled above but included for completeness
                return false;
            default:
                // Unknown state - should never happen
                return false;
        }
    }
}