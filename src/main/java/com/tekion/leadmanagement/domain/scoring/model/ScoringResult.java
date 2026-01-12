package com.tekion.leadmanagement.domain.scoring.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Value object representing the result of lead scoring.
 *
 * <h2>Overview</h2>
 * <p>This immutable value object contains both the final computed score
 * and a detailed breakdown showing how each rule contributed.
 *
 * <h2>Score Interpretation</h2>
 * <table border="1">
 *   <tr><th>Score Range</th><th>Priority</th><th>Action</th></tr>
 *   <tr><td>80-100</td><td>Hot Lead</td><td>Immediate follow-up</td></tr>
 *   <tr><td>60-79</td><td>Warm Lead</td><td>Follow-up within 24 hours</td></tr>
 *   <tr><td>40-59</td><td>Cool Lead</td><td>Add to nurture campaign</td></tr>
 *   <tr><td>0-39</td><td>Cold Lead</td><td>Low priority, nurture over time</td></tr>
 * </table>
 *
 * <h2>Breakdown Usage</h2>
 * <p>The breakdown map shows each rule's contribution:
 * <pre>
 * {
 *   "sourceQuality": 1.0,    // Referral source = max score
 *   "vehicleAge": 0.6,       // 4-year-old vehicle = medium score
 *   "tradeInValue": 0.7,     // $7k trade-in = good score
 *   "engagement": 0.2,       // NEW state = low engagement
 *   "recency": 1.0           // Created within 24 hours
 * }
 * </pre>
 *
 * @see com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine
 * @see com.tekion.leadmanagement.domain.scoring.rule.ScoringRule
 */
@Value
@Builder
public class ScoringResult {

    /**
     * Final computed score normalized to 0-100 range.
     *
     * <p>Calculated as: Σ(factor × weight) / Σ(weights) × 100
     * <p>Higher scores indicate leads more likely to convert.
     */
    int finalScore;

    /**
     * Detailed breakdown of scores by rule.
     *
     * <p>Map keys are rule names (e.g., "sourceQuality", "vehicleAge").
     * <p>Map values are the raw factor (0.0 to 1.0) returned by each rule.
     *
     * <p>Useful for:
     * <ul>
     *   <li>Debugging why a lead scored high/low</li>
     *   <li>Displaying score breakdown in UI</li>
     *   <li>Identifying areas where a lead is weak</li>
     * </ul>
     */
    Map<String, Double> breakdown;
}