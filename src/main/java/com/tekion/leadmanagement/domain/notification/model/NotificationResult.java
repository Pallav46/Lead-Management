package com.tekion.leadmanagement.domain.notification.model;

import lombok.Value;

/**
 * Value object representing the outcome of a notification send operation.
 *
 * <h2>Overview</h2>
 * <p>This immutable value object captures the result of attempting to send
 * a notification through any channel. It follows the Result Pattern to
 * explicitly model success and failure cases.
 *
 * <h2>Success Case</h2>
 * <ul>
 *   <li>{@code success} = true</li>
 *   <li>{@code vendor} = which adapter handled it (e.g., "email-adapter")</li>
 *   <li>{@code messageId} = vendor-assigned ID for tracking/debugging</li>
 *   <li>{@code errorMessage} = null</li>
 * </ul>
 *
 * <h2>Failure Case</h2>
 * <ul>
 *   <li>{@code success} = false</li>
 *   <li>{@code vendor} = which adapter failed (may be null if routing failed)</li>
 *   <li>{@code messageId} = null</li>
 *   <li>{@code errorMessage} = description of what went wrong</li>
 * </ul>
 *
 * <h2>Failover Tracking</h2>
 * <p>When primary vendor fails and failover succeeds, the {@code vendor} field
 * will indicate the fallback vendor that actually delivered the message.
 *
 * @see Notification for the notification request
 * @see com.tekion.leadmanagement.application.notification.NotificationRouter for routing logic
 */
@Value
public class NotificationResult {

    /** Whether the notification was sent successfully. */
    boolean success;

    /**
     * Which adapter/vendor handled the notification.
     * <p>Examples: "email-adapter", "sms-adapter"
     * <p>May be null if routing failed before reaching any adapter.
     */
    String vendor;

    /**
     * Vendor-assigned message ID for tracking.
     * <p>Can be used for debugging, delivery confirmation, etc.
     * <p>Only present when success=true.
     */
    String messageId;

    /**
     * Error description when send fails.
     * <p>Examples: "Rate limit exceeded", "Invalid recipient", "Vendor timeout"
     * <p>Only present when success=false.
     */
    String errorMessage;

    /**
     * Factory method for successful notification sends.
     *
     * @param vendor    The adapter that handled the notification
     * @param messageId The vendor-assigned tracking ID
     * @return A success result with the given vendor and messageId
     */
    public static NotificationResult success(String vendor, String messageId) {
        return new NotificationResult(true, vendor, messageId, null);
    }

    /**
     * Factory method for failed notification sends.
     *
     * @param vendor       The adapter that failed (may be null)
     * @param errorMessage Description of what went wrong
     * @return A failure result with the given error details
     */
    public static NotificationResult failure(String vendor, String errorMessage) {
        return new NotificationResult(false, vendor, null, errorMessage);
    }
}