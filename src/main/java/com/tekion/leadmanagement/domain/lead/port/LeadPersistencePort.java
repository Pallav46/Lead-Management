package com.tekion.leadmanagement.domain.lead.port;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.LeadState;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for lead persistence operations.
 *
 * <h2>Hexagonal Architecture</h2>
 * <p>This is a <b>driven port</b> (secondary port) in hexagonal architecture.
 * It defines the contract for persisting and retrieving leads, allowing the
 * domain layer to remain agnostic of the actual storage implementation.
 *
 * <h2>Multi-Tenant Design</h2>
 * <p>All methods require {@code dealerId} as a parameter to enforce tenant
 * isolation. Implementations MUST ensure that leads from different dealers
 * cannot be accessed or mixed.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@code InMemoryLeadRepository} - For testing and development</li>
 *   <li>{@code MongoLeadRepository} - For production (future)</li>
 *   <li>{@code JpaLeadRepository} - For SQL databases (future)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe for concurrent access.
 *
 * @see com.tekion.leadmanagement.adapter.persistence.inmemory.InMemoryLeadRepository
 */
public interface LeadPersistencePort {

    /**
     * Persists a lead (insert or update).
     *
     * <p>The implementation should use the composite key (dealerId:leadId)
     * to identify the lead uniquely.
     *
     * @param lead The lead to persist (must not be null)
     * @return The persisted lead (may be same instance or copy)
     * @throws IllegalArgumentException if lead is null or has invalid fields
     */
    Lead save(Lead lead);

    /**
     * Finds a lead by ID within a specific dealer's scope.
     *
     * <p>This method enforces tenant isolation by requiring both leadId and
     * dealerId. A lead from one dealer cannot be retrieved using another
     * dealer's ID.
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @return Optional containing the lead if found, empty otherwise
     */
    Optional<Lead> findByIdAndDealerId(String leadId, String dealerId);

    /**
     * Finds all leads for a dealer in a specific state.
     *
     * <p>Useful for:
     * <ul>
     *   <li>Getting all NEW leads for follow-up queue</li>
     *   <li>Finding QUALIFIED leads for sales manager review</li>
     *   <li>Listing LOST leads for win-back campaigns</li>
     * </ul>
     *
     * @param dealerId The dealer to query
     * @param state    The lead state to filter by
     * @return List of matching leads (empty if none found)
     */
    List<Lead> findByDealerIdAndState(String dealerId, LeadState state);

    /**
     * Finds top-scored leads for a dealer, ordered by score descending.
     *
     * <p>This is the primary method for building prioritized lead queues.
     * Leads with higher scores appear first. Null scores are treated as
     * lowest priority.
     *
     * @param dealerId The dealer to query
     * @param limit    Maximum number of leads to return
     * @return List of leads ordered by score (highest first)
     */
    List<Lead> findByDealerIdOrderByScore(String dealerId, int limit);
}