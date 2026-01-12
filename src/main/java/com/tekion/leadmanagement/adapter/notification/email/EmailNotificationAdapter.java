package com.tekion.leadmanagement.adapter.notification.email;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;

import java.util.UUID;

/**
 * Notification adapter for sending email notifications.
 *
 * <h2>Overview</h2>
 * <p>This adapter handles email delivery through an email service provider.
 * Currently implements a mock version for development/testing.
 *
 * <h2>Fallback Support</h2>
 * <p>This adapter supports both EMAIL and SMS notification types.
 * For SMS, it can send an email-to-SMS via carrier gateways or as
 * a fallback when the primary SMS vendor is unavailable.
 *
 * <h2>Production Implementation</h2>
 * <p>In production, the {@code send()} method would:
 * <ol>
 *   <li>Build the email message (from, to, subject, body, HTML template)</li>
 *   <li>Call email service (SendGrid, AWS SES, Mailgun, etc.)</li>
 *   <li>Handle retry logic for transient failures</li>
 *   <li>Return vendor message ID for tracking</li>
 * </ol>
 *
 * <h2>Vendor Identifier</h2>
 * <p>Returns "email-adapter" as the vendor name in results.
 *
 * @see NotificationPort for the interface contract
 * @see SmsNotificationAdapter for SMS-specific delivery
 */
public class EmailNotificationAdapter implements NotificationPort {

    /** Vendor identifier returned in NotificationResult. */
    private static final String VENDOR_NAME = "email-adapter";

    /**
     * Sends an email notification.
     *
     * <p>Current implementation is a mock that always succeeds.
     * Production would integrate with actual email service.
     *
     * @param notification The notification to send
     * @return Success result with generated message ID, or failure result
     */
    @Override
    public NotificationResult send(Notification notification) {
        // Validate input
        if (notification == null) {
            return NotificationResult.failure(VENDOR_NAME, "notification was null");
        }
        if (!supports(notification.getType())) {
            return NotificationResult.failure(VENDOR_NAME, "unsupported type: " + notification.getType());
        }

        // In production: integrate with email service provider here
        // Example: sendGridClient.send(buildEmail(notification));

        // Mock implementation: generate fake message ID
        String messageId = "email-" + UUID.randomUUID();
        return NotificationResult.success(VENDOR_NAME, messageId);
    }

    /**
     * Checks if this adapter supports the given notification type.
     *
     * <p>Supports EMAIL (primary) and SMS (as fallback via email-to-SMS gateway).
     *
     * @param type The notification type to check
     * @return true for EMAIL and SMS types
     */
    @Override
    public boolean supports(NotificationType type) {
        // Email adapter supports EMAIL primarily, plus SMS as fallback
        // (email-to-SMS gateways like number@vtext.com for Verizon)
        return type == NotificationType.EMAIL || type == NotificationType.SMS;
    }
}