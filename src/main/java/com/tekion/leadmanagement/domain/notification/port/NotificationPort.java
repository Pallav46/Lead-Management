package com.tekion.leadmanagement.domain.notification.port;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;

/**
 * Port interface for notification delivery adapters.
 *
 * <h2>Hexagonal Architecture</h2>
 * <p>This is a <b>driven port</b> (secondary port) in hexagonal architecture.
 * Each implementation represents a different notification channel or vendor.
 *
 * <h2>Strategy Pattern</h2>
 * <p>The {@code NotificationRouter} uses multiple implementations of this
 * interface to support:
 * <ul>
 *   <li>Channel selection (email vs SMS vs push)</li>
 *   <li>Vendor failover (if primary fails, try secondary)</li>
 *   <li>A/B testing different providers</li>
 * </ul>
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@code EmailNotificationAdapter} - Sends email via SMTP/API</li>
 *   <li>{@code SmsNotificationAdapter} - Sends SMS via Twilio/SNS</li>
 *   <li>Future: {@code PushNotificationAdapter} for mobile push</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>Implementations should NOT throw exceptions for delivery failures.
 * Instead, return a {@link NotificationResult} with success=false and
 * an appropriate error message. This allows the router to attempt failover.
 *
 * @see com.tekion.leadmanagement.application.notification.NotificationRouter
 * @see NotificationResult for operation outcomes
 */
public interface NotificationPort {

    /**
     * Sends a notification through this adapter's channel.
     *
     * <p>The implementation should:
     * <ol>
     *   <li>Validate the notification is appropriate for this channel</li>
     *   <li>Transform the notification to vendor-specific format</li>
     *   <li>Call the vendor API</li>
     *   <li>Return success or failure result</li>
     * </ol>
     *
     * @param notification The notification to send (never null when called by router)
     * @return Result indicating success/failure with vendor details
     */
    NotificationResult send(Notification notification);

    /**
     * Checks if this adapter can handle the given notification type.
     *
     * <p>The router uses this to filter which adapters to try for a given
     * notification. An adapter can support multiple types if desired.
     *
     * @param type The notification channel type
     * @return true if this adapter can send notifications of this type
     */
    boolean supports(NotificationType type);
}