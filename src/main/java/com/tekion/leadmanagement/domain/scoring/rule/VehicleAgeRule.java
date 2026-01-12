package com.tekion.leadmanagement.domain.scoring.rule;

import com.tekion.leadmanagement.domain.lead.model.Lead;
import com.tekion.leadmanagement.domain.lead.model.VehicleInterest;

/**
 * Scoring rule that evaluates lead priority based on current vehicle age.
 *
 * <h2>Business Rationale</h2>
 * <p>Customers with older vehicles have higher urgency to purchase:
 * <ul>
 *   <li>Older cars = more maintenance issues, reliability concerns</li>
 *   <li>May be out of warranty, facing expensive repairs</li>
 *   <li>Higher motivation to trade in before value drops further</li>
 * </ul>
 *
 * <h2>Scoring Tiers</h2>
 * <table border="1">
 *   <tr><th>Vehicle Age</th><th>Factor</th><th>Priority</th></tr>
 *   <tr><td>5+ years</td><td>1.0</td><td>High - likely ready to buy</td></tr>
 *   <tr><td>3-4 years</td><td>0.6</td><td>Medium - exploring options</td></tr>
 *   <tr><td>0-2 years</td><td>0.2</td><td>Low - recent purchase</td></tr>
 * </table>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Weight:</b> 25% (high influence on final score)</li>
 *   <li><b>Name:</b> "vehicleAge"</li>
 * </ul>
 *
 * @see VehicleInterest#getCurrentVehicleAge() for age calculation
 */
public class VehicleAgeRule implements ScoringRule {

    @Override
    public String getName() {
        return "vehicleAge";
    }

    @Override
    public int getWeight() {
        return 25;
    }

    /**
     * Evaluates the lead based on their current vehicle's age.
     *
     * @param lead The lead to evaluate
     * @return Factor from 0.0-1.0 based on vehicle age (older = higher)
     */
    @Override
    public double evaluate(Lead lead) {
        // Handle null cases gracefully
        if (lead == null) return 0.0;

        VehicleInterest vi = lead.getVehicleInterest();
        if (vi == null) return 0.0;

        // Calculate vehicle age in years
        int age = vi.getCurrentVehicleAge();

        // Score based on age tiers
        if (age >= 5) return 1.0;   // High priority: 5+ year old vehicle
        if (age >= 3) return 0.6;   // Medium priority: 3-4 years old
        return 0.2;                  // Low priority: newer vehicle (0-2 years)
    }
}