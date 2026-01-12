package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.LeadState;

/**
 * Scoring rule that evaluates lead quality based on engagement level.
 *
 * <h2>Business Rationale</h2>
 * <p>A lead's progression through the sales pipeline indicates their
 * engagement and likelihood to convert. Leads further along the pipeline
 * are more valuable and should be prioritized.
 *
 * <h2>Engagement Proxy</h2>
 * <p>This implementation uses {@link LeadState} as a proxy for engagement:
 * <ul>
 *   <li>NEW = No engagement yet (just captured)</li>
 *   <li>CONTACTED = Sales rep made contact</li>
 *   <li>QUALIFIED = Customer shows genuine interest</li>
 *   <li>CONVERTED = Deal closed (highest engagement)</li>
 *   <li>LOST = Customer disengaged</li>
 * </ul>
 *
 * <h2>Scoring Tiers</h2>
 * <table border="1">
 *   <tr><th>State</th><th>Factor</th><th>Interpretation</th></tr>
 *   <tr><td>QUALIFIED/CONVERTED</td><td>1.0</td><td>Highly engaged buyer</td></tr>
 *   <tr><td>CONTACTED</td><td>0.6</td><td>Actively communicating</td></tr>
 *   <tr><td>NEW</td><td>0.2</td><td>Not yet contacted</td></tr>
 *   <tr><td>LOST</td><td>0.1</td><td>Disengaged (low priority)</td></tr>
 * </table>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Weight:</b> 15% (moderate influence on final score)</li>
 *   <li><b>Name:</b> "engagement"</li>
 * </ul>
 *
 * <h2>Future Enhancements</h2>
 * <p>This rule could be enhanced to consider additional signals:
 * <ul>
 *   <li>Number of interactions/touchpoints</li>
 *   <li>Email open rates and click-through rates</li>
 *   <li>Website visit frequency</li>
 *   <li>Response time to communications</li>
 * </ul>
 *
 * @see LeadState for lead lifecycle states
 */
public class EngagementRule implements ScoringRule {

    @Override
    public String getName() {
        return "engagement";
    }

    @Override
    public int getWeight() {
        return 15;
    }

    /**
     * Evaluates the lead based on their current pipeline state.
     *
     * @param lead The lead to evaluate
     * @return Factor from 0.1-1.0 based on engagement level
     */
    @Override
    public double evaluate(Lead lead) {
        // Handle null cases gracefully
        if (lead == null || lead.getState() == null) return 0.0;

        // Use lead state as engagement proxy
        LeadState state = lead.getState();
        switch (state) {
            case NEW:
                return 0.2;   // Not contacted yet - low engagement
            case CONTACTED:
                return 0.6;   // In communication - moderate engagement
            case QUALIFIED:
                return 1.0;   // Vetted and interested - high engagement
            case CONVERTED:
                return 1.0;   // Deal closed - highest engagement
            case LOST:
                return 0.1;   // Disengaged - very low priority
            default:
                return 0.0;   // Unknown state
        }
    }
}