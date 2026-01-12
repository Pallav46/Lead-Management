package com.tekion.leadmanagement.domain.lead.model;

import lombok.Value;

import java.util.regex.Pattern;

/**
 * Value object representing a validated email address.
 *
 * <h2>Value Object Pattern</h2>
 * <p>This is an immutable value object that:
 * <ul>
 *   <li>Validates email format on construction</li>
 *   <li>Normalizes the email (trim + lowercase)</li>
 *   <li>Is immutable after creation</li>
 *   <li>Defines equality by value, not identity</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>Cannot be null or empty</li>
 *   <li>Must contain exactly one @ symbol</li>
 *   <li>Must have non-empty local part (before @)</li>
 *   <li>Must have domain with at least one dot</li>
 *   <li>No whitespace allowed</li>
 * </ul>
 *
 * <h2>Normalization</h2>
 * <p>Email addresses are normalized by:
 * <ol>
 *   <li>Trimming leading/trailing whitespace</li>
 *   <li>Converting to lowercase (emails are case-insensitive)</li>
 * </ol>
 *
 * @see PhoneCoordinate for phone number value object
 */
@Value
public class Email {

    /**
     * Simple email validation pattern.
     * <p>Checks for: something@something.something (no whitespace allowed)
     * <p>This is intentionally simple - full RFC 5322 compliance is overkill for most use cases.
     */
    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** The normalized (lowercase, trimmed) email address. */
    String value;

    /**
     * Creates a new Email value object with validation and normalization.
     *
     * @param value The email address to validate
     * @throws IllegalArgumentException if email is null, empty, or invalid format
     */
    public Email(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        // Normalize: trim whitespace and convert to lowercase
        String normalized = value.trim().toLowerCase();

        // Validate format
        if (normalized.isEmpty() || !SIMPLE_EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }

        this.value = normalized;
    }
}