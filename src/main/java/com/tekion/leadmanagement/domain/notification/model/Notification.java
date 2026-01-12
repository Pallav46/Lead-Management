package com.tekion.leadmanagement.domain.notification.model;

import lombok.Value;

/**
 * Value object representing a notification to be sent to a customer.
 *
 * <h2>Overview</h2>
 * <p>This is an immutable value object that encapsulates all information
 * needed to send a notification through any channel (email, SMS, push).
 *
 * <h2>Multi-Tenant Context</h2>
 * <p>Every notification carries tenant context (dealerId, tenantId, siteId) for:
 * <ul>
 *   <li>Routing to correct sender identity (dealer-specific email/phone)</li>
 *   <li>Audit logging and compliance tracking</li>
 *   <li>Rate limiting isolation between dealers</li>
 * </ul>
 *
 * <h2>Rate Limiting</h2>
 * <p>The leadId is used for per-lead rate limiting. The system enforces
 * a maximum of 3 notifications per lead per day to prevent spam.
 * Rate limit key format: "{dealerId}:{leadId}".
 *
 * <h2>Validation</h2>
 * <p>All required fields are validated on construction:
 * <ul>
 *   <li>All tenant context fields (dealerId, tenantId, siteId) required</li>
 *   <li>leadId required for rate limiting</li>
 *   <li>type cannot be null</li>
 *   <li>body and to (recipient) required</li>
 *   <li>subject is optional (mainly used for EMAIL type)</li>
 * </ul>
 *
 * @see NotificationType for available notification channels
 * @see NotificationResult for send operation outcome
 */
@Value
public class Notification {

    // ════════════════════════════════════════════════════════════════
    // MULTI-TENANT CONTEXT
    // Used for routing, auditing, and tenant-scoped rate limiting.
    // ════════════════════════════════════════════════════════════════

    /** The dealership sending this notification. */
    String dealerId;

    /** Parent tenant/organization ID. */
    String tenantId;

    /** Specific site/location within the dealership. */
    String siteId;

    // ════════════════════════════════════════════════════════════════
    // LEAD REFERENCE
    // ════════════════════════════════════════════════════════════════

    /**
     * The lead this notification is for.
     * <p>Used for per-lead rate limiting (max 3 per day per lead).
     */
    String leadId;

    // ════════════════════════════════════════════════════════════════
    // NOTIFICATION DETAILS
    // ════════════════════════════════════════════════════════════════

    /** The notification channel (EMAIL, SMS, or PUSH). */
    NotificationType type;

    /**
     * Email subject line (optional, mainly used for EMAIL type).
     * <p>For SMS/PUSH, this is typically null.
     */
    String subject;

    /** The message body content (required). */
    String body;

    /**
     * Recipient address - format depends on type:
     * <ul>
     *   <li>EMAIL: email address (e.g., "customer@example.com")</li>
     *   <li>SMS: E.164 phone number (e.g., "+14155550123")</li>
     *   <li>PUSH: device push token</li>
     * </ul>
     */
    String to;

    /**
     * Creates a new Notification with validation.
     *
     * @param dealerId  Dealership identifier (required, non-blank)
     * @param tenantId  Tenant identifier (required, non-blank)
     * @param siteId    Site identifier (required, non-blank)
     * @param leadId    Lead identifier for rate limiting (required, non-blank)
     * @param type      Notification channel (required, non-null)
     * @param subject   Email subject (optional, can be null for SMS/PUSH)
     * @param body      Message content (required, non-blank)
     * @param to        Recipient address (required, non-blank)
     * @throws IllegalArgumentException if any required field is missing or blank
     */
    public Notification(
            String dealerId,
            String tenantId,
            String siteId,
            String leadId,
            NotificationType type,
            String subject,
            String body,
            String to
    ) {
        // Validate all required fields
        if (isBlank(dealerId)) throw new IllegalArgumentException("dealerId cannot be blank");
        if (isBlank(tenantId)) throw new IllegalArgumentException("tenantId cannot be blank");
        if (isBlank(siteId)) throw new IllegalArgumentException("siteId cannot be blank");
        if (isBlank(leadId)) throw new IllegalArgumentException("leadId cannot be blank");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (isBlank(body)) throw new IllegalArgumentException("body cannot be blank");
        if (isBlank(to)) throw new IllegalArgumentException("to cannot be blank");

        // Normalize and assign (trim whitespace)
        this.dealerId = dealerId.trim();
        this.tenantId = tenantId.trim();
        this.siteId = siteId.trim();
        this.leadId = leadId.trim();
        this.type = type;
        this.subject = (subject == null ? null : subject.trim());
        this.body = body.trim();  // Trim body for consistency
        this.to = to.trim();
    }

    /**
     * Helper method to check if a string is null or blank.
     *
     * @param s The string to check
     * @return true if string is null or contains only whitespace
     */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Factory method to create an SMS notification.
     *
     * @param dealerId  Dealership identifier
     * @param tenantId  Tenant identifier
     * @param siteId    Site identifier
     * @param leadId    Lead identifier for rate limiting
     * @param body      SMS message content
     * @param phoneE164 Phone number in E.164 format (e.g., "+14155550123")
     * @return A new SMS Notification instance
     */
    public static Notification sms(String dealerId, String tenantId, String siteId,
                                   String leadId, String body, String phoneE164) {
        return new Notification(dealerId, tenantId, siteId, leadId,
                NotificationType.SMS, null, body, phoneE164);
    }

    /**
     * Factory method to create an Email notification.
     *
     * @param dealerId Dealership identifier
     * @param tenantId Tenant identifier
     * @param siteId   Site identifier
     * @param leadId   Lead identifier for rate limiting
     * @param subject  Email subject line
     * @param body     Email body content
     * @param email    Recipient email address
     * @return A new Email Notification instance
     */
    public static Notification email(String dealerId, String tenantId, String siteId,
                                     String leadId, String subject, String body, String email) {
        return new Notification(dealerId, tenantId, siteId, leadId,
                NotificationType.EMAIL, subject, body, email);
    }

    // ════════════════════════════════════════════════════════════════
    // BUILDER PATTERN
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns a new Builder for constructing Notification instances.
     *
     * <p>Example usage:
     * <pre>
     * Notification notification = Notification.builder()
     *     .dealerId("dealer-1")
     *     .tenantId("tenant-1")
     *     .siteId("site-1")
     *     .leadId("lead-123")
     *     .type(NotificationType.SMS)
     *     .body("Hello!")
     *     .to("+14155550123")
     *     .build();
     * </pre>
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing Notification instances with a fluent API.
     */
    public static class Builder {
        private String dealerId;
        private String tenantId;
        private String siteId;
        private String leadId;
        private NotificationType type;
        private String subject;
        private String body;
        private String to;

        public Builder dealerId(String dealerId) {
            this.dealerId = dealerId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder siteId(String siteId) {
            this.siteId = siteId;
            return this;
        }

        public Builder leadId(String leadId) {
            this.leadId = leadId;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Builds the Notification instance with validation.
         *
         * @return A new validated Notification instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public Notification build() {
            return new Notification(dealerId, tenantId, siteId, leadId, type, subject, body, to);
        }
    }
}