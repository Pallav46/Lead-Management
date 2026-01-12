package com.tekion.leadmanagement.domain.lead.model;

import lombok.Value;

import java.time.Year;
import java.util.Optional;

/**
 * Value object representing a customer's vehicle interest and optional trade-in.
 *
 * <h2>Value Object Pattern</h2>
 * <p>This is an immutable value object that encapsulates:
 * <ul>
 *   <li>Vehicle identification (make, model, year)</li>
 *   <li>Optional trade-in value for existing vehicle</li>
 *   <li>Computed property for vehicle age</li>
 * </ul>
 *
 * <h2>Scoring Impact</h2>
 * <p>Vehicle interest data is used in lead scoring:
 * <table border="1">
 *   <tr><th>Factor</th><th>High Score</th><th>Low Score</th></tr>
 *   <tr><td>Vehicle Age</td><td>Older vehicles (5+ years)</td><td>New vehicles (0-2 years)</td></tr>
 *   <tr><td>Trade-In Value</td><td>$10,000+</td><td>No trade-in</td></tr>
 * </table>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>Make and model cannot be blank</li>
 *   <li>Year must be between 1900 and next year (allows pre-orders)</li>
 *   <li>Trade-in value cannot be negative (null = no trade-in)</li>
 * </ul>
 *
 * @see com.tekion.leadmanagement.domain.scoring.rule.VehicleAgeRule
 * @see com.tekion.leadmanagement.domain.scoring.rule.TradeInValueRule
 */
@Value
public class VehicleInterest {

    /** Vehicle manufacturer name, e.g., "Toyota", "Ford", "BMW". */
    String make;

    /** Vehicle model name, e.g., "Camry", "F-150", "X5". */
    String model;

    /** Model year of the vehicle (e.g., 2022). */
    int year;

    /**
     * Estimated trade-in value in dollars, or null if no trade-in.
     * <p>Stored as nullable Integer but exposed as Optional via getter.
     */
    Integer tradeInValue;

    /**
     * Creates a new VehicleInterest with validation.
     *
     * @param make         Vehicle manufacturer (required, non-blank)
     * @param model        Vehicle model name (required, non-blank)
     * @param year         Model year (1900 to current year + 1)
     * @param tradeInValue Estimated trade-in value in dollars (null for no trade-in)
     * @throws IllegalArgumentException if validation fails
     */
    public VehicleInterest(String make, String model, int year, Integer tradeInValue) {
        // Validate make is not blank
        if (make == null || make.trim().isEmpty()) {
            throw new IllegalArgumentException("make cannot be blank");
        }

        // Validate model is not blank
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be blank");
        }

        // Validate year is reasonable (1900 to next year for pre-orders)
        int currentYear = Year.now().getValue();
        if (year < 1900 || year > currentYear + 1) {
            throw new IllegalArgumentException("Invalid vehicle year: " + year);
        }

        // Validate trade-in value is non-negative if provided
        if (tradeInValue != null && tradeInValue < 0) {
            throw new IllegalArgumentException("tradeInValue cannot be negative: " + tradeInValue);
        }

        this.make = make.trim();
        this.model = model.trim();
        this.year = year;
        this.tradeInValue = tradeInValue;
    }

    /**
     * Returns the trade-in value as an Optional.
     *
     * <p>This provides a safer API than nullable fields,
     * encouraging explicit handling of the "no trade-in" case.
     *
     * @return Optional containing trade-in value, or empty if no trade-in
     */
    public Optional<Integer> getTradeInValue() {
        return Optional.ofNullable(tradeInValue);
    }

    /**
     * Calculates the current age of the vehicle in years.
     *
     * <p>Used by {@code VehicleAgeRule} in lead scoring.
     * Older vehicles score higher as they indicate more urgent need.
     *
     * @return Age in years (current year - vehicle year)
     */
    public int getCurrentVehicleAge() {
        return Year.now().getValue() - year;
    }
}