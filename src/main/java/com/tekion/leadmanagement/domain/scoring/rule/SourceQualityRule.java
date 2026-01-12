package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.LeadSource;

/**
 * Scoring rule that evaluates lead quality based on acquisition channel.
 *
 * <h2>Business Rationale</h2>
 * <p>Different lead sources have different conversion rates based on
 * the customer's intent and trust level:
 *
 * <table border="1">
 *   <tr><th>Source</th><th>Factor</th><th>Reasoning</th></tr>
 *   <tr><td>REFERRAL</td><td>1.0</td><td>Personal recommendation = highest trust</td></tr>
 *   <tr><td>WEBSITE</td><td>0.7</td><td>Active research shows intent</td></tr>
 *   <tr><td>PHONE</td><td>0.5</td><td>Inbound calls show moderate interest</td></tr>
 *   <tr><td>WALKIN</td><td>0.3</td><td>May be casual browsing</td></tr>
 * </table>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Weight:</b> 20% (moderate influence on final score)</li>
 *   <li><b>Name:</b> "sourceQuality"</li>
 * </ul>
 *
 * @see LeadSource for available lead sources
 */
public class SourceQualityRule implements ScoringRule {

    @Override
    public String getName() {
        return "sourceQuality";
    }

    @Override
    public int getWeight() {
        return 20;
    }

    /**
     * Evaluates the lead based on its acquisition source.
     *
     * @param lead The lead to evaluate
     * @return Factor from 0.0-1.0 based on source quality
     */
    @Override
    public double evaluate(Lead lead) {
        // Handle null cases gracefully
        if (lead == null || lead.getSource() == null) return 0.0;

        LeadSource source = lead.getSource();
        switch (source) {
            case REFERRAL:
                return 1.0;   // Best: personal recommendation
            case WEBSITE:
                return 0.7;   // Good: active online research
            case PHONE:
                return 0.5;   // Medium: inbound phone inquiry
            case WALKIN:
                return 0.3;   // Lower: may be casual browser
            default:
                return 0.0;   // Unknown source
        }
    }
}