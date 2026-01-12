package com.tekion.leadmanagement.adapter.notification.sms;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;

import java.util.UUID;

/**
 * Notification adapter for sending SMS notifications.
 *
 * <h2>Overview</h2>
 * <p>This adapter handles SMS delivery through an SMS service provider.
 * Currently implements a mock version for development/testing with
 * configurable failure simulation.
 *
 * <h2>Failure Simulation</h2>
 * <p>The {@code fail} constructor parameter allows simulating vendor outages
 * for testing failover behavior. When enabled, all send attempts return failure.
 *
 * <h2>Production Implementation</h2>
 * <p>In production, the {@code send()} method would:
 * <ol>
 *   <li>Format the phone number (E.164 format)</li>
 *   <li>Build the SMS message (respecting 160 char limit or multipart)</li>
 *   <li>Call SMS service (Twilio, Bandwidth, AWS SNS, etc.)</li>
 *   <li>Handle retry logic for transient failures</li>
 *   <li>Return vendor message SID for tracking</li>
 * </ol>
 *
 * <h2>Vendor Identifier</h2>
 * <p>Returns "sms-adapter" as the vendor name in results.
 *
 * @see NotificationPort for the interface contract
 * @see EmailNotificationAdapter for email delivery (also supports SMS fallback)
 */
public class SmsNotificationAdapter implements NotificationPort {

    /** Vendor identifier returned in NotificationResult. */
    private static final String VENDOR_NAME = "sms-adapter";

    /**
     * When true, all send attempts will fail.
     * Useful for testing failover to email adapter.
     */
    private final boolean fail;

    /**
     * Creates an SMS adapter that operates normally.
     */
    public SmsNotificationAdapter() {
        this(false);
    }

    /**
     * Creates an SMS adapter with configurable failure mode.
     *
     * @param fail If true, all send attempts will return failure
     */
    public SmsNotificationAdapter(boolean fail) {
        this.fail = fail;
    }

    /**
     * Sends an SMS notification.
     *
     * <p>Current implementation is a mock that succeeds unless failure mode
     * is enabled. Production would integrate with actual SMS service.
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

        // Simulate vendor outage if failure mode is enabled
        if (fail) {
            return NotificationResult.failure(VENDOR_NAME, "simulated SMS vendor failure");
        }

        // In production: integrate with SMS service provider here
        // Example: twilioClient.messages().create(buildMessage(notification));

        // Mock implementation: generate fake message SID
        String messageId = "sms-" + UUID.randomUUID();
        return NotificationResult.success(VENDOR_NAME, messageId);
    }

    /**
     * Checks if this adapter supports the given notification type.
     *
     * <p>Only supports SMS type. For email-to-SMS fallback, use EmailNotificationAdapter.
     *
     * @param type The notification type to check
     * @return true only for SMS type
     */
    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }
}