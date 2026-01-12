package com.tekion.leadmanagement.adapter.persistence.inmemory;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.LeadState;
import com.tekion.leadmanagement.domain.lead.port.LeadPersistencePort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of lead persistence for testing and development.
 *
 * <h2>Overview</h2>
 * <p>This adapter provides a simple, thread-safe, in-memory storage for leads.
 * It's perfect for:
 * <ul>
 *   <li>Unit and integration testing</li>
 *   <li>Local development without database setup</li>
 *   <li>Prototyping new features</li>
 * </ul>
 *
 * <h2>Multi-Tenant Isolation</h2>
 * <p>Data is stored using composite keys: {@code dealerId:leadId}.
 * This ensures complete isolation between different dealers' data,
 * even though all data resides in the same in-memory map.
 *
 * <h2>Thread Safety</h2>
 * <p>Uses {@link ConcurrentHashMap} for thread-safe concurrent access.
 * All read and write operations are atomic at the individual key level.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Data is lost when the application restarts</li>
 *   <li>Not suitable for production or distributed systems</li>
 *   <li>No transaction support</li>
 *   <li>Linear scan for filtered queries (not scalable)</li>
 * </ul>
 *
 * @see LeadPersistencePort for the interface contract
 */
public class InMemoryLeadRepository implements LeadPersistencePort {

    /**
     * Main storage for leads.
     *
     * <p>Key format: "{dealerId}:{leadId}" to enforce tenant-aware storage.
     * <p>Using ConcurrentHashMap for thread-safe concurrent access.
     */
    private final ConcurrentHashMap<String, Lead> store = new ConcurrentHashMap<>();

    /**
     * Saves a lead to the in-memory store.
     *
     * <p>This is an upsert operation - it will insert if the key doesn't
     * exist or update if it does.
     *
     * @param lead The lead to persist
     * @return The saved lead (same instance)
     * @throws IllegalArgumentException if lead is null or has blank dealerId/leadId
     */
    @Override
    public Lead save(Lead lead) {
        // Validate input
        if (lead == null) {
            throw new IllegalArgumentException("lead cannot be null");
        }
        if (lead.getDealerId() == null || lead.getDealerId().trim().isEmpty()) {
            throw new IllegalArgumentException("dealerId cannot be blank");
        }
        if (lead.getLeadId() == null || lead.getLeadId().trim().isEmpty()) {
            throw new IllegalArgumentException("leadId cannot be blank");
        }

        // Store using composite key for tenant isolation
        store.put(key(lead.getDealerId(), lead.getLeadId()), lead);
        return lead;
    }

    /**
     * Finds a lead by its ID within a specific dealer's scope.
     *
     * <p>O(1) lookup using the composite key.
     *
     * @param leadId   The lead's unique identifier
     * @param dealerId The dealer the lead belongs to
     * @return Optional containing the lead if found
     */
    @Override
    public Optional<Lead> findByIdAndDealerId(String leadId, String dealerId) {
        if (leadId == null || dealerId == null) return Optional.empty();
        return Optional.ofNullable(store.get(key(dealerId, leadId)));
    }

    /**
     * Finds all leads for a dealer in a specific state.
     *
     * <p>O(n) linear scan through all stored leads.
     * In production, this would use indexed database queries.
     *
     * @param dealerId The dealer to query
     * @param state    The lead state to filter by
     * @return List of matching leads
     */
    @Override
    public List<Lead> findByDealerIdAndState(String dealerId, LeadState state) {
        if (dealerId == null || state == null) return List.of();

        List<Lead> result = new ArrayList<>();
        for (Lead lead : store.values()) {
            if (dealerId.equals(lead.getDealerId()) && state == lead.getState()) {
                result.add(lead);
            }
        }
        return result;
    }

    /**
     * Finds top-scored leads for a dealer, ordered by score descending.
     *
     * <p>Sorting priority:
     * <ol>
     *   <li>Higher score first (null scores go last)</li>
     *   <li>More recently updated first (tie-breaker)</li>
     * </ol>
     *
     * @param dealerId The dealer to query
     * @param limit    Maximum number of leads to return
     * @return Sorted list of top leads
     */
    @Override
    public List<Lead> findByDealerIdOrderByScore(String dealerId, int limit) {
        if (dealerId == null || limit <= 0) return List.of();

        // Filter leads by dealer
        List<Lead> result = new ArrayList<>();
        for (Lead lead : store.values()) {
            if (dealerId.equals(lead.getDealerId())) {
                result.add(lead);
            }
        }

        // Sort: higher score first, null scores last, then by updatedAt desc
        result.sort(
                Comparator
                        .comparing((Lead l) -> l.getScore(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Lead::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        // Apply limit
        return result.size() <= limit ? result : result.subList(0, limit);
    }

    /**
     * Generates the composite storage key for tenant isolation.
     *
     * @param dealerId The dealer identifier
     * @param leadId   The lead identifier
     * @return Composite key in format "dealerId:leadId"
     */
    private String key(String dealerId, String leadId) {
        return dealerId + ":" + leadId;
    }
}