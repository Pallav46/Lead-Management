package com.tekion.leadmanagement.domain.lead.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Lead is the core domain entity representing a potential customer interested in a vehicle.
 *
 * <h2>Overview</h2>
 * <p>This is an <b>aggregate root</b> in Domain-Driven Design (DDD) terms, encapsulating:
 * <ul>
 *   <li>Multi-tenant context (dealerId, tenantId, siteId) for data isolation</li>
 *   <li>Customer contact information (name, email, phone)</li>
 *   <li>Vehicle interest details (make, model, year, trade-in value)</li>
 *   <li>Lead lifecycle state machine (NEW → CONTACTED → QUALIFIED → CONVERTED)</li>
 *   <li>Computed lead score (0-100) for sales prioritization</li>
 * </ul>
 *
 * <h2>Multi-Tenant Design</h2>
 * <p>Every lead belongs to a specific dealer identified by {@code dealerId}.
 * All queries and operations MUST be scoped by dealerId to ensure complete
 * data isolation between different dealerships. This is enforced at the
 * repository layer through composite keys (dealerId:leadId).
 *
 * <h2>State Machine</h2>
 * <p>Leads follow a defined lifecycle managed by {@link LeadState}:
 * <pre>
 *   NEW → CONTACTED → QUALIFIED → CONVERTED
 *         ↓            ↓            ↓
 *        LOST ←───────←───────────←┘
 * </pre>
 *
 * @see LeadState for valid state transitions
 * @see LeadSource for lead acquisition channels
 * @see VehicleInterest for vehicle details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    /** Unique identifier for this lead (UUID format). */
    private String leadId;

    // ════════════════════════════════════════════════════════════════
    // MULTI-TENANT CONTEXT
    // These fields enable complete data isolation between dealerships.
    // Every query MUST filter by dealerId to prevent cross-tenant access.
    // ════════════════════════════════════════════════════════════════

    /** The dealership this lead belongs to. Primary tenant identifier. */
    private String dealerId;

    /** Parent tenant/organization ID (for multi-level tenancy). */
    private String tenantId;

    /** Specific site/location within the dealership. */
    private String siteId;

    // ════════════════════════════════════════════════════════════════
    // CUSTOMER INFORMATION
    // ════════════════════════════════════════════════════════════════

    /** Customer's first name. */
    private String firstName;

    /** Customer's last name. */
    private String lastName;

    /** Customer's email address (validated and normalized). */
    private Email email;

    /** Customer's phone number with country code. */
    private PhoneCoordinate phone;

    // ════════════════════════════════════════════════════════════════
    // LEAD CLASSIFICATION
    // ════════════════════════════════════════════════════════════════

    /** How the lead was acquired (WEBSITE, PHONE, WALKIN, REFERRAL). */
    private LeadSource source;

    /** Current state in the sales pipeline (NEW, CONTACTED, QUALIFIED, etc.). */
    private LeadState state;

    /** Details about the vehicle the customer is interested in. */
    private VehicleInterest vehicleInterest;

    /**
     * Lead priority score from 0 (lowest) to 100 (highest).
     * <p>Computed by {@link com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine}
     * based on multiple factors: source quality, vehicle age, trade-in value,
     * engagement level, and lead recency.
     * <p>Higher scores indicate leads more likely to convert.
     */
    private Integer score;

    // ════════════════════════════════════════════════════════════════
    // AUDIT TIMESTAMPS
    // ════════════════════════════════════════════════════════════════

    /** When this lead was first created. */
    private Instant createdAt;

    /** When this lead was last modified (state change, score update, etc.). */
    private Instant updatedAt;

    // ════════════════════════════════════════════════════════════════
    // AUDIT TRAIL
    // ════════════════════════════════════════════════════════════════

    /**
     * Complete audit trail of all state transitions.
     * Each entry records who, what, when, and why for compliance/debugging.
     */
    @Builder.Default
    private List<AuditEntry> auditTrail = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════
    // FACTORY METHOD
    // ════════════════════════════════════════════════════════════════

    /**
     * Factory method for creating a new Lead with all required fields.
     *
     * <p>This is the preferred way to create leads as it:
     * <ul>
     *   <li>Validates all required fields are present and non-blank</li>
     *   <li>Generates a unique UUID for the leadId</li>
     *   <li>Sets initial state to {@link LeadState#NEW}</li>
     *   <li>Sets createdAt and updatedAt timestamps to now</li>
     * </ul>
     *
     * @param dealerId        The dealership identifier (required, non-blank)
     * @param tenantId        The tenant/organization identifier (required, non-blank)
     * @param siteId          The site/location identifier (required, non-blank)
     * @param firstName       Customer's first name (required, non-blank)
     * @param lastName        Customer's last name (required, non-blank)
     * @param email           Customer's email (required, validated)
     * @param phone           Customer's phone (required, validated)
     * @param source          How the lead was acquired (required)
     * @param vehicleInterest Vehicle details (required)
     * @return A new Lead instance with state=NEW and generated ID
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public static Lead newLead(
            String dealerId,
            String tenantId,
            String siteId,
            String firstName,
            String lastName,
            Email email,
            PhoneCoordinate phone,
            LeadSource source,
            VehicleInterest vehicleInterest
    ) {
        Instant now = Instant.now();

        Lead lead = Lead.builder()
                .leadId(UUID.randomUUID().toString())
                .dealerId(requireNonBlank(dealerId, "dealerId"))
                .tenantId(requireNonBlank(tenantId, "tenantId"))
                .siteId(requireNonBlank(siteId, "siteId"))
                .firstName(requireNonBlank(firstName, "firstName"))
                .lastName(requireNonBlank(lastName, "lastName"))
                .email(requireNonNull(email, "email"))
                .phone(requireNonNull(phone, "phone"))
                .source(requireNonNull(source, "source"))
                .vehicleInterest(requireNonNull(vehicleInterest, "vehicleInterest"))
                .state(LeadState.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return lead;
    }

    // ════════════════════════════════════════════════════════════════
    // DOMAIN OPERATIONS
    // These methods encapsulate business logic and maintain invariants.
    // ════════════════════════════════════════════════════════════════

    /**
     * Transitions the lead to a new state in the sales pipeline.
     * Delegates to {@link #transitionTo(LeadState, String, String)} with null actor/reason.
     *
     * @param target The desired target state
     * @throws IllegalArgumentException if target is null
     * @throws IllegalStateException if current state is null or transition is invalid
     */
    public void transitionTo(LeadState target) {
        transitionTo(target, null, null);
    }

    /**
     * Transitions the lead to a new state with full audit trail.
     *
     * <p>The transition is validated by {@link LeadState#canTransitionTo(LeadState)}
     * to ensure only valid state changes are allowed. For example:
     * <ul>
     *   <li>NEW → CONTACTED (valid)</li>
     *   <li>NEW → QUALIFIED (invalid - must go through CONTACTED)</li>
     *   <li>CONVERTED → anything (invalid - terminal state)</li>
     * </ul>
     *
     * <p>Creates an {@link AuditEntry} recording the transition with timestamp,
     * actor, and reason. Also updates the {@code updatedAt} timestamp.
     *
     * @param target The desired target state
     * @param actor  Who triggered this transition (user ID, system process, etc.)
     * @param reason Optional reason or notes for the transition
     * @throws IllegalArgumentException if target is null
     * @throws IllegalStateException if current state is null or transition is invalid
     */
    public void transitionTo(LeadState target, String actor, String reason) {
        if (target == null) {
            throw new IllegalArgumentException("target state cannot be null");
        }
        if (state == null) {
            throw new IllegalStateException("lead state cannot be null");
        }
        if (!state.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid state transition: " + state + " -> " + target
            );
        }

        // Create audit entry before changing state
        LeadState fromState = this.state;
        Instant now = Instant.now();

        AuditEntry entry = AuditEntry.builder()
                .timestamp(now)
                .actor(actor != null ? actor : "SYSTEM")
                .fromState(fromState)
                .toState(target)
                .reason(reason)
                .build();

        // Initialize audit trail if null (for leads created without @Builder.Default)
        if (this.auditTrail == null) {
            this.auditTrail = new ArrayList<>();
        }
        this.auditTrail.add(entry);

        // Perform the transition
        this.state = target;
        this.updatedAt = now;
    }

    /**
     * Gets an unmodifiable view of the audit trail.
     *
     * @return Immutable list of audit entries
     */
    public List<AuditEntry> getAuditTrail() {
        if (auditTrail == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(auditTrail);
    }

    /**
     * Updates the lead's priority score.
     *
     * <p>The score must be between 0 and 100 inclusive.
     * Typically called after running the lead through {@code LeadScoringEngine}.
     *
     * @param newScore The new score value (0-100)
     * @throws IllegalArgumentException if score is outside valid range
     */
    public void updateScore(int newScore) {
        if (newScore < 0 || newScore > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        this.score = newScore;
        this.updatedAt = Instant.now();
    }

    // ════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ════════════════════════════════════════════════════════════════

    /**
     * Validates that a value is not null.
     *
     * @param value The value to check
     * @param field The field name for error messages
     * @return The value if non-null
     * @throws IllegalArgumentException if value is null
     */
    private static <T> T requireNonNull(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " cannot be null");
        return value;
    }

    /**
     * Validates that a string is not null or blank, and trims it.
     *
     * @param value The string to check
     * @param field The field name for error messages
     * @return The trimmed string if valid
     * @throws IllegalArgumentException if value is null or blank
     */
    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }
}