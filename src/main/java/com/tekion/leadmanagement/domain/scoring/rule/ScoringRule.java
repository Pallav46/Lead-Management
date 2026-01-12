package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;

/**
 * Interface for pluggable lead scoring rules.
 *
 * <h2>Strategy Pattern</h2>
 * <p>Each implementation of this interface represents a specific criterion
 * for evaluating lead quality. The {@code LeadScoringEngine} aggregates
 * multiple rules to compute a final weighted score.
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Each rule has a name (for identification) and weight (importance)</li>
 *   <li>The {@code evaluate()} method returns a factor from 0.0 to 1.0</li>
 *   <li>Final score = Σ(factor × weight) / Σ(weights) × 100</li>
 * </ol>
 *
 * <h2>Built-in Rules</h2>
 * <table border="1">
 *   <tr><th>Rule</th><th>Weight</th><th>What it evaluates</th></tr>
 *   <tr><td>SourceQualityRule</td><td>10%</td><td>Lead acquisition channel quality</td></tr>
 *   <tr><td>VehicleAgeRule</td><td>25%</td><td>How old is customer's current vehicle</td></tr>
 *   <tr><td>TradeInValueRule</td><td>25%</td><td>Trade-in value (higher = more serious)</td></tr>
 *   <tr><td>EngagementRule</td><td>15%</td><td>Lead state progression</td></tr>
 *   <tr><td>RecencyRule</td><td>25%</td><td>How fresh is the lead</td></tr>
 * </table>
 *
 * <h2>Custom Rules</h2>
 * <p>To add a custom scoring criterion:
 * <pre>
 * public class CreditScoreRule implements ScoringRule {
 *     public String getName() { return "creditScore"; }
 *     public int getWeight() { return 20; }
 *     public double evaluate(Lead lead) {
 *         // Return 0.0 to 1.0 based on credit score
 *     }
 * }
 * </pre>
 *
 * @see com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine
 * @see com.tekion.leadmanagement.domain.scoring.model.ScoringResult
 */
public interface ScoringRule {

    /**
     * Returns the unique identifier for this rule.
     * <p>Used as the key in the scoring breakdown map.
     *
     * @return The rule name (e.g., "sourceQuality", "vehicleAge")
     */
    String getName();

    /**
     * Returns the weight of this rule as a percentage (0 to 100).
     *
     * <p>Higher weight means this rule has more influence on the final score.
     * The sum of all rule weights doesn't need to equal 100 - they're normalized.
     *
     * <p>Examples:
     * <ul>
     *   <li>20 = This rule contributes 20% of total weight</li>
     *   <li>0 = This rule is disabled but still calculated</li>
     * </ul>
     *
     * @return Weight as percentage (must be >= 0)
     */
    int getWeight();

    /**
     * Evaluates a lead and returns a score factor.
     *
     * <p>The returned value should be between 0.0 (lowest) and 1.0 (highest).
     * Values outside this range will be clamped by the scoring engine.
     *
     * <p>Examples:
     * <ul>
     *   <li>1.0 = Lead scores perfectly on this criterion</li>
     *   <li>0.5 = Lead scores average on this criterion</li>
     *   <li>0.0 = Lead scores poorly on this criterion</li>
     * </ul>
     *
     * @param lead The lead to evaluate (may be null - handle gracefully)
     * @return Score factor from 0.0 to 1.0
     */
    double evaluate(Lead lead);
}