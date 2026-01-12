package com.tekion.leadmanagement.adapter.notification.sms;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Twilio-based SMS notification adapter using the Messages API.
 *
 * <h2>Overview</h2>
 * <p>This adapter integrates with Twilio's Messages API to send custom
 * SMS messages to customers. It requires a Twilio phone number configured
 * in the TwilioConfig.
 *
 * <h2>Twilio Messages API</h2>
 * <p>The Messages API allows:
 * <ul>
 *   <li>Sending custom text messages</li>
 *   <li>Tracking delivery status</li>
 *   <li>Two-way SMS conversations</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * TwilioConfig config = new TwilioConfig(
 *     "AC...",      // Account SID
 *     "xxx",        // Auth Token
 *     "+1234567890" // Your Twilio Phone Number
 * );
 * TwilioSmsAdapter adapter = new TwilioSmsAdapter(config);
 * NotificationResult result = adapter.send(notification);
 * }</pre>
 *
 * @see TwilioConfig for configuration
 * @see SmsNotificationAdapter for mock implementation
 */
public class TwilioSmsAdapter implements NotificationPort {

    private static final String VENDOR_NAME = "twilio-sms";

    private final TwilioConfig config;
    /** Volatile for proper visibility in double-checked locking pattern. */
    private volatile boolean initialized = false;

    /**
     * Creates a new Twilio SMS adapter with the given configuration.
     *
     * @param config Twilio API configuration
     */
    public TwilioSmsAdapter(TwilioConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
    }

    /**
     * Initializes the Twilio SDK (lazy initialization).
     */
    private synchronized void ensureInitialized() {
        if (!initialized) {
            Twilio.init(config.getAccountSid(), config.getAuthToken());
            initialized = true;
        }
    }

    /**
     * Sends an SMS message via Twilio Messages API.
     *
     * @param notification The notification containing recipient phone and message
     * @return Result with message SID on success, or error details on failure
     */
    @Override
    public NotificationResult send(Notification notification) {
        // Validate input
        if (notification == null) {
            return NotificationResult.failure(VENDOR_NAME, "notification was null");
        }
        if (!supports(notification.getType())) {
            return NotificationResult.failure(VENDOR_NAME,
                    "unsupported type: " + notification.getType());
        }
        if (notification.getBody() == null || notification.getBody().isBlank()) {
            return NotificationResult.failure(VENDOR_NAME, "message body is required");
        }

        try {
            // Initialize Twilio SDK if needed
            ensureInitialized();

            // Send SMS via Twilio Messages API
            Message message = Message.creator(
                    new PhoneNumber(notification.getTo()),           // To
                    new PhoneNumber(config.getFromPhoneNumber()),    // From (your Twilio number)
                    notification.getBody()                           // Message body
            ).create();

            // Return success with message SID
            return NotificationResult.success(VENDOR_NAME, message.getSid());

        } catch (Exception e) {
            // Return failure with error message
            return NotificationResult.failure(VENDOR_NAME,
                    "Twilio API error: " + e.getMessage());
        }
    }

    /**
     * This adapter only supports SMS notifications.
     *
     * @param type The notification type to check
     * @return true only for SMS type
     */
    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }
}

