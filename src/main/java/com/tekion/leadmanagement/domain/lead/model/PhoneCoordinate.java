package com.tekion.leadmanagement.domain.lead.model;

import lombok.Value;

/**
 * Value object representing a phone number with country code.
 *
 * <h2>Value Object Pattern</h2>
 * <p>This is an immutable value object that:
 * <ul>
 *   <li>Validates phone number format on construction</li>
 *   <li>Normalizes the number (strips non-digits)</li>
 *   <li>Supports E.164 international format output</li>
 *   <li>Is immutable after creation</li>
 * </ul>
 *
 * <h2>E.164 Format</h2>
 * <p>E.164 is the international standard for phone number formatting:
 * <ul>
 *   <li>Starts with '+' followed by country code</li>
 *   <li>Contains only digits after the '+'</li>
 *   <li>Example: +14155550123 (US number)</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>Country code defaults to "+1" (US) if not provided</li>
 *   <li>Country code must start with '+'</li>
 *   <li>Phone number must have at least 10 digits</li>
 *   <li>Non-digit characters are stripped from the number</li>
 * </ul>
 *
 * @see Email for email value object
 */
@Value
public class PhoneCoordinate {

    /**
     * International country calling code with '+' prefix.
     * Example: "+1" (US/Canada), "+44" (UK), "+91" (India)
     */
    String countryCode;

    /**
     * Phone number containing only digits.
     * Example: "4155550123" (10-digit US number)
     */
    String number;

    /**
     * Creates a new PhoneCoordinate with validation and normalization.
     *
     * @param countryCode The country calling code (defaults to "+1" if null/empty)
     * @param number      The phone number (non-digits will be stripped)
     * @throws IllegalArgumentException if country code doesn't start with '+'
     *                                  or number has fewer than 10 digits
     */
    public PhoneCoordinate(String countryCode, String number) {
        // Default to US country code if not provided
        String cc = (countryCode == null || countryCode.trim().isEmpty())
                ? "+1"
                : countryCode.trim();

        // Validate country code format
        if (!cc.startsWith("+")) {
            throw new IllegalArgumentException("countryCode must start with '+': " + countryCode);
        }

        // Validate number is not null
        if (number == null) {
            throw new IllegalArgumentException("Phone number cannot be null");
        }

        // Normalize: strip all non-digit characters
        String normalizedNumber = number.replaceAll("[^0-9]", "");

        // Validate minimum length
        if (normalizedNumber.length() < 10) {
            throw new IllegalArgumentException("Invalid phone number (need at least 10 digits): " + number);
        }

        this.countryCode = cc;
        this.number = normalizedNumber;
    }

    /**
     * Returns the phone number in E.164 international format.
     *
     * <p>E.164 format is the standard for international phone numbers,
     * used by most telecom APIs (Twilio, AWS SNS, etc.).
     *
     * @return The full phone number with country code, e.g., "+14155550123"
     */
    public String toE164() {
        return countryCode + number;
    }
}