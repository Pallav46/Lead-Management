package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;

import java.time.Duration;
import java.time.Instant;

/**
 * Scoring rule that prioritizes leads based on how recently they were created.
 *
 * <h2>Business Rationale</h2>
 * <p>Fresh leads are more likely to convert because:
 * <ul>
 *   <li>Customer's interest is at peak when they first inquire</li>
 *   <li>Quick follow-up creates better first impression</li>
 *   <li>Customer may be shopping at multiple dealers</li>
 *   <li>Older leads may have already purchased elsewhere</li>
 * </ul>
 *
 * <h2>Scoring Tiers</h2>
 * <table border="1">
 *   <tr><th>Age</th><th>Factor</th><th>Priority</th></tr>
 *   <tr><td>&lt;24 hours</td><td>1.0</td><td>Hot - contact immediately</td></tr>
 *   <tr><td>1-6 days</td><td>0.7</td><td>Warm - follow up quickly</td></tr>
 *   <tr><td>7-29 days</td><td>0.4</td><td>Cool - may need nurturing</td></tr>
 *   <tr><td>30+ days</td><td>0.1</td><td>Stale - low priority</td></tr>
 * </table>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Weight:</b> 15% (moderate influence on final score)</li>
 *   <li><b>Name:</b> "recency"</li>
 * </ul>
 *
 * <h2>Industry Best Practices</h2>
 * <p>Studies show that responding to leads within 5 minutes can increase
 * conversion rates by up to 9x compared to waiting 10 minutes.
 */
public class RecencyRule implements ScoringRule {

    @Override
    public String getName() {
        return "recency";
    }

    @Override
    public int getWeight() {
        return 15;
    }

    /**
     * Evaluates the lead based on how long ago it was created.
     *
     * @param lead The lead to evaluate
     * @return Factor from 0.1-1.0 based on lead age (fresher = higher)
     */
    @Override
    public double evaluate(Lead lead) {
        // Handle null cases gracefully
        if (lead == null || lead.getCreatedAt() == null) return 0.0;

        Instant now = Instant.now();

        // Calculate time since lead was created
        // Use Math.max to handle future dates (clock skew) gracefully
        long hours = Math.max(0, Duration.between(lead.getCreatedAt(), now).toHours());
        long days = Math.max(0, Duration.between(lead.getCreatedAt(), now).toDays());

        // Score based on recency tiers
        if (hours < 24) return 1.0;  // Less than 24 hours - hot lead
        if (days < 7) return 0.7;    // Less than a week - warm lead
        if (days < 30) return 0.4;   // Less than a month - cool lead
        return 0.1;                   // 30+ days old - stale lead
    }
}