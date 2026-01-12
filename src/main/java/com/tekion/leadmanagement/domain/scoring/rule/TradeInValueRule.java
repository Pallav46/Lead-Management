package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.VehicleInterest;

/**
 * Scoring rule that evaluates lead quality based on trade-in value.
 *
 * <h2>Business Rationale</h2>
 * <p>Higher trade-in values indicate:
 * <ul>
 *   <li>More serious buyer intent (already thinking about finances)</li>
 *   <li>Higher budget for new vehicle purchase</li>
 *   <li>Ready to make a deal (has something to trade)</li>
 * </ul>
 *
 * <h2>Scoring Tiers</h2>
 * <table border="1">
 *   <tr><th>Trade-In Value</th><th>Factor</th><th>Interpretation</th></tr>
 *   <tr><td>&gt;$10,000</td><td>1.0</td><td>High value = serious buyer</td></tr>
 *   <tr><td>$5,001-$10,000</td><td>0.7</td><td>Moderate value = engaged buyer</td></tr>
 *   <tr><td>$1-$5,000</td><td>0.4</td><td>Low value = some commitment</td></tr>
 *   <tr><td>No trade-in</td><td>0.1</td><td>Still a lead but less committed</td></tr>
 * </table>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Weight:</b> 25% (high influence on final score)</li>
 *   <li><b>Name:</b> "tradeInValue"</li>
 * </ul>
 *
 * @see VehicleInterest#getTradeInValue() for trade-in value source
 */
public class TradeInValueRule implements ScoringRule {

    @Override
    public String getName() {
        return "tradeInValue";
    }

    @Override
    public int getWeight() {
        return 25;
    }

    /**
     * Evaluates the lead based on their trade-in vehicle value.
     *
     * @param lead The lead to evaluate
     * @return Factor from 0.1-1.0 based on trade-in value (higher = better)
     */
    @Override
    public double evaluate(Lead lead) {
        // Handle null lead gracefully
        if (lead == null) return 0.0;

        VehicleInterest vi = lead.getVehicleInterest();
        if (vi == null) return 0.1; // No vehicle info = minimal score

        // Get trade-in value (may be empty Optional)
        Integer tradeIn = vi.getTradeInValue().orElse(null);
        if (tradeIn == null) return 0.1; // No trade-in specified

        // Score based on trade-in value tiers
        if (tradeIn > 10_000) return 1.0;  // High value trade-in
        if (tradeIn > 5_000) return 0.7;   // Medium value trade-in
        if (tradeIn > 0) return 0.4;       // Low value trade-in
        return 0.1;                         // Zero or negative (shouldn't happen)
    }
}