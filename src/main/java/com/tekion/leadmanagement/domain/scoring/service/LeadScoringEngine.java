package com.tekion.leadmanagement.domain.scoring.service;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.scoring.model.ScoringResult;
import com.tekion.leadmanagement.domain.scoring.rule.ScoringRule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine that computes lead priority scores using configurable rules.
 *
 * <h2>Overview</h2>
 * <p>This service applies a collection of {@link ScoringRule} implementations
 * to evaluate a lead and produce a normalized score from 0-100.
 *
 * <h2>Scoring Algorithm</h2>
 * <p>The final score is computed as a weighted average:
 * <pre>
 *   finalScore = (Σ (factor_i × weight_i) / Σ weight_i) × 100
 * </pre>
 *
 * <p>Where:
 * <ul>
 *   <li>factor_i = value returned by rule i (clamped to 0.0-1.0)</li>
 *   <li>weight_i = importance of rule i (0-100)</li>
 * </ul>
 *
 * <h2>Example Calculation</h2>
 * <pre>
 *   Rules:
 *   - SourceQuality: weight=10, factor=1.0 (referral)     → contributes 10
 *   - VehicleAge:    weight=25, factor=0.6 (4 years old)  → contributes 15
 *   - TradeIn:       weight=25, factor=1.0 ($12k)         → contributes 25
 *   - Engagement:    weight=15, factor=0.2 (NEW state)    → contributes 3
 *   - Recency:       weight=25, factor=1.0 (fresh lead)   → contributes 25
 *
 *   Total weight: 100
 *   Weighted sum: 10 + 15 + 25 + 3 + 25 = 78
 *   Final score: (78 / 100) × 100 = 78
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe if all rules are thread-safe. The engine
 * holds no mutable state between calls to {@code score()}.
 *
 * @see ScoringRule for implementing custom scoring criteria
 * @see ScoringResult for the output structure
 */
public class LeadScoringEngine {

    /** The list of scoring rules to apply (injected at construction). */
    private final List<ScoringRule> rules;

    /**
     * Creates a new scoring engine with the given rules.
     *
     * @param rules List of scoring rules to apply (must not be null or empty)
     * @throws IllegalArgumentException if rules is null or empty
     */
    public LeadScoringEngine(List<ScoringRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("rules cannot be null/empty");
        }
        this.rules = rules;
    }

    /**
     * Scores a lead by applying all configured rules.
     *
     * <p>Each rule's factor is clamped to 0.0-1.0 range, then weighted
     * according to the rule's configured weight. The final score is
     * normalized to 0-100 range.
     *
     * @param lead The lead to score (must not be null)
     * @return ScoringResult containing final score and per-rule breakdown
     * @throws IllegalArgumentException if lead is null or any rule has negative weight
     */
    public ScoringResult score(Lead lead) {
        if (lead == null) {
            throw new IllegalArgumentException("lead cannot be null");
        }

        // LinkedHashMap preserves insertion order for consistent breakdown display
        Map<String, Double> breakdown = new LinkedHashMap<>();
        double weightedSum = 0.0;
        int totalWeight = 0;

        // Apply each rule and accumulate weighted scores
        for (ScoringRule rule : rules) {
            // Evaluate the rule and clamp to valid range
            double factor = clamp01(rule.evaluate(lead));
            breakdown.put(rule.getName(), factor);

            // Validate weight is non-negative
            int weight = rule.getWeight();
            if (weight < 0) {
                throw new IllegalArgumentException("Negative weight for rule: " + rule.getName());
            }

            // Accumulate weighted contribution
            weightedSum += factor * weight;
            totalWeight += weight;
        }

        // Calculate final normalized score
        int finalScore;
        if (totalWeight == 0) {
            // Edge case: all rules have zero weight
            finalScore = 0;
        } else {
            // Normalize to 0-100 range and round
            finalScore = (int) Math.round((weightedSum / totalWeight) * 100.0);
        }

        // Return immutable result - wrap breakdown in unmodifiable map
        return ScoringResult.builder()
                .finalScore(finalScore)
                .breakdown(Map.copyOf(breakdown))
                .build();
    }

    /**
     * Scores multiple leads in batch for improved performance.
     *
     * <p>This method processes leads in parallel using Java's parallel streams,
     * which can significantly improve throughput when scoring many leads.
     *
     * <h2>Performance</h2>
     * <p>For large batches (100+ leads), parallel processing can provide
     * 2-4x speedup depending on available CPU cores.
     *
     * <h2>Thread Safety</h2>
     * <p>This method is thread-safe if all rules are thread-safe.
     *
     * @param leads List of leads to score (must not be null)
     * @return Map of leadId → ScoringResult for each lead
     * @throws IllegalArgumentException if leads is null
     */
    public Map<String, ScoringResult> scoreBatch(List<Lead> leads) {
        if (leads == null) {
            throw new IllegalArgumentException("leads cannot be null");
        }
        if (leads.isEmpty()) {
            return Map.of();
        }

        // Use parallel stream for batch processing
        return leads.parallelStream()
                .filter(lead -> lead != null && lead.getLeadId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Lead::getLeadId,
                        this::score
                ));
    }

    /**
     * Scores multiple leads and updates their score field in-place.
     *
     * <p>This is a convenience method that combines scoring with updating
     * the lead's score property.
     *
     * @param leads List of leads to score and update
     * @return The same list with scores updated
     */
    public List<Lead> scoreAndUpdateBatch(List<Lead> leads) {
        if (leads == null) {
            throw new IllegalArgumentException("leads cannot be null");
        }

        leads.parallelStream()
                .filter(lead -> lead != null)
                .forEach(lead -> {
                    ScoringResult result = score(lead);
                    lead.updateScore(result.getFinalScore());
                });

        return leads;
    }

    /**
     * Clamps a value to the 0.0-1.0 range.
     *
     * <p>This ensures rule implementations that return out-of-range values
     * don't break the scoring calculation.
     *
     * @param v The value to clamp
     * @return Value clamped to [0.0, 1.0] range
     */
    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}