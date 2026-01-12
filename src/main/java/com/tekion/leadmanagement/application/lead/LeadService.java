package com.tekion.leadmanagement.application.lead;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.LeadState;
import com.tekion.leadmanagement.domain.lead.port.LeadPersistencePort;
import com.tekion.leadmanagement.domain.scoring.model.ScoringResult;
import com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine;

import java.util.Optional;

/**
 * Application service for lead management operations.
 *
 * <h2>Overview</h2>
 * <p>This service orchestrates lead lifecycle operations including:
 * <ul>
 *   <li>Lead creation and persistence</li>
 *   <li>Lead retrieval with tenant isolation</li>
 *   <li>State transitions through the sales pipeline</li>
 *   <li>Lead scoring computation and persistence</li>
 * </ul>
 *
 * <h2>Hexagonal Architecture</h2>
 * <p>This is an <b>application service</b> that sits between the driving
 * adapters (REST controllers, CLI, etc.) and the domain layer. It:
 * <ul>
 *   <li>Coordinates domain operations</li>
 *   <li>Manages transactions (when applicable)</li>
 *   <li>Enforces application-level validation</li>
 * </ul>
 *
 * <h2>Multi-Tenant Design</h2>
 * <p>All operations require {@code dealerId} to ensure tenant isolation.
 * A lead from one dealer cannot be accessed or modified by another dealer.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>{@link LeadPersistencePort} - For lead storage operations</li>
 *   <li>{@link LeadScoringEngine} - For computing lead priority scores</li>
 * </ul>
 *
 * @see Lead for the lead domain model
 * @see LeadScoringEngine for scoring logic
 */
public class LeadService {

    private final LeadPersistencePort persistencePort;
    private final LeadScoringEngine scoringEngine;

    /**
     * Creates a new LeadService with required dependencies.
     *
     * @param persistencePort Port for lead persistence operations
     * @param scoringEngine   Engine for computing lead scores
     * @throws IllegalArgumentException if any dependency is null
     */
    public LeadService(LeadPersistencePort persistencePort, LeadScoringEngine scoringEngine) {
        if (persistencePort == null) throw new IllegalArgumentException("persistencePort cannot be null");
        if (scoringEngine == null) throw new IllegalArgumentException("scoringEngine cannot be null");
        this.persistencePort = persistencePort;
        this.scoringEngine = scoringEngine;
    }

    /**
     * Creates a new lead in the system.
     *
     * <p>The lead must have valid dealerId and leadId set before calling.
     * Initial state should typically be {@link LeadState#NEW}.
     *
     * @param lead The lead to create
     * @return The persisted lead
     * @throws IllegalArgumentException if lead is null or has invalid fields
     */
    public Lead create(Lead lead) {
        if (lead == null) throw new IllegalArgumentException("lead cannot be null");
        return persistencePort.save(lead);
    }

    /**
     * Finds a lead by ID within a specific dealer's scope.
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @return Optional containing the lead if found
     */
    public Optional<Lead> findByIdAndDealerId(String leadId, String dealerId) {
        return persistencePort.findByIdAndDealerId(leadId, dealerId);
    }

    /**
     * Transitions a lead to a new state in the sales pipeline.
     *
     * <p>Valid transitions are enforced by the {@link Lead#transitionTo(LeadState)}
     * method. Invalid transitions will throw an exception.
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @param target   The target state to transition to
     * @return The updated lead with new state
     * @throws IllegalArgumentException if lead not found or transition invalid
     */
    public Lead transitionState(String leadId, String dealerId, LeadState target) {
        Lead lead = persistencePort.findByIdAndDealerId(leadId, dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        lead.transitionTo(target);
        return persistencePort.save(lead);
    }

    /**
     * Computes the score for a lead without persisting it.
     *
     * <p>Useful for previewing what a lead's score would be, or for
     * debugging scoring rules.
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @return Detailed scoring result with breakdown by rule
     * @throws IllegalArgumentException if lead not found
     */
    public ScoringResult scoreLead(String leadId, String dealerId) {
        Lead lead = persistencePort.findByIdAndDealerId(leadId, dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        return scoringEngine.score(lead);
    }

    /**
     * Computes and persists the score for a lead.
     *
     * <p>This is the primary method for updating lead scores. It:
     * <ol>
     *   <li>Retrieves the lead from storage</li>
     *   <li>Runs all scoring rules via the scoring engine</li>
     *   <li>Updates the lead's score field</li>
     *   <li>Persists the updated lead</li>
     * </ol>
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @throws IllegalArgumentException if lead not found
     */
    public void computeAndPersistScore(String leadId, String dealerId) {
        Lead lead = persistencePort.findByIdAndDealerId(leadId, dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        ScoringResult scoringResult = scoringEngine.score(lead);
        lead.updateScore(scoringResult.getFinalScore());
        persistencePort.save(lead);
    }
}