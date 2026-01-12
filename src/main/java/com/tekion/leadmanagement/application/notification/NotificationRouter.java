package com.tekion.leadmanagement.application.notification;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes notifications to appropriate adapters with failover and rate limiting.
 *
 * <h2>Overview</h2>
 * <p>The NotificationRouter is responsible for:
 * <ul>
 *   <li>Selecting the appropriate notification adapter based on type</li>
 *   <li>Implementing failover when primary adapter fails</li>
 *   <li>Enforcing rate limits to prevent notification spam</li>
 * </ul>
 *
 * <h2>Failover Strategy</h2>
 * <p>Adapters are tried in priority order (as provided in constructor).
 * If the first adapter fails, the router tries the next one that supports
 * the notification type. This enables:
 * <ul>
 *   <li>Vendor failover (e.g., Twilio fails → try Bandwidth)</li>
 *   <li>Channel fallback (e.g., SMS fails → try email-to-SMS)</li>
 * </ul>
 *
 * <h2>Rate Limiting</h2>
 * <p>Enforces a maximum of 3 notifications per lead per day to prevent:
 * <ul>
 *   <li>Customer annoyance from excessive messages</li>
 *   <li>Compliance issues (TCPA, CAN-SPAM)</li>
 *   <li>Vendor cost overruns</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Uses {@link ConcurrentHashMap} for rate limit tracking, making it
 * safe for concurrent use from multiple threads.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * List<NotificationPort> adapters = List.of(
 *     new SmsNotificationAdapter(),      // Primary
 *     new EmailNotificationAdapter()     // Fallback
 * );
 * NotificationRouter router = new NotificationRouter(adapters);
 *
 * Notification sms = new Notification(...);
 * NotificationResult result = router.route(sms);
 * }</pre>
 *
 * @see NotificationPort for adapter interface
 * @see NotificationResult for operation outcomes
 */
public class NotificationRouter {

    /** Maximum notifications allowed per lead per day. */
    private static final int MAX_NOTIFICATIONS_PER_LEAD_PER_DAY = 3;

    /** Adapters to try, in priority order (first = highest priority). */
    private final List<NotificationPort> portsInPriorityOrder;

    /**
     * Rate limit tracking: (dealerId:leadId:date) → count.
     * Uses ConcurrentHashMap for thread-safe concurrent access.
     */
    private final Map<String, Integer> perLeadPerDayCount = new ConcurrentHashMap<>();

    /**
     * Creates a router with the given adapters in priority order.
     *
     * @param portsInPriorityOrder Adapters to use, first = highest priority
     * @throws IllegalArgumentException if list is null or empty
     */
    public NotificationRouter(List<NotificationPort> portsInPriorityOrder) {
        if (portsInPriorityOrder == null || portsInPriorityOrder.isEmpty()) {
            throw new IllegalArgumentException("portsInPriorityOrder cannot be null/empty");
        }
        this.portsInPriorityOrder = portsInPriorityOrder;
    }

    /**
     * Routes a notification to an appropriate adapter.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Validate the notification is not null</li>
     *   <li>Atomically check and reserve rate limit slot</li>
     *   <li>Try each adapter in priority order until one succeeds</li>
     *   <li>Release rate limit slot on failure</li>
     *   <li>Return result (success or last failure)</li>
     * </ol>
     *
     * <p>Thread-safe: Uses atomic operations for rate limiting.
     *
     * @param notification The notification to send
     * @return Result indicating success/failure with details
     */
    public NotificationResult route(Notification notification) {
        // Validate input
        if (notification == null) {
            return NotificationResult.failure("router", "notification was null");
        }

        // Atomically check and reserve rate limit slot
        String rateKey = rateLimitKey(notification.getDealerId(), notification.getLeadId(), LocalDate.now());

        // Use compute() for atomic check-and-increment to prevent race conditions
        boolean[] rateLimitExceeded = {false};
        perLeadPerDayCount.compute(rateKey, (key, currentCount) -> {
            int count = (currentCount == null) ? 0 : currentCount;
            if (count >= MAX_NOTIFICATIONS_PER_LEAD_PER_DAY) {
                rateLimitExceeded[0] = true;
                return count; // Don't increment if limit exceeded
            }
            return count + 1; // Reserve slot by incrementing
        });

        if (rateLimitExceeded[0]) {
            return NotificationResult.failure("router",
                    "rate limit exceeded (max " + MAX_NOTIFICATIONS_PER_LEAD_PER_DAY + " per lead per day)");
        }

        // Try adapters in priority order
        NotificationResult lastFailure = null;

        for (NotificationPort port : portsInPriorityOrder) {
            // Skip adapters that don't support this notification type
            if (!port.supports(notification.getType())) {
                continue;
            }

            // Attempt to send via this adapter
            NotificationResult result = port.send(notification);
            if (result.isSuccess()) {
                // Success! Rate limit slot already reserved in compute() above
                return result;
            }

            // Keep track of last failure for visibility
            lastFailure = result;
            // In production: log failure here for monitoring/alerting
        }

        // All adapters failed - release the reserved rate limit slot
        perLeadPerDayCount.compute(rateKey, (key, currentCount) -> {
            if (currentCount == null || currentCount <= 0) return 0;
            return currentCount - 1;
        });

        // Return the last failure or no-adapter error
        if (lastFailure != null) {
            return lastFailure;
        }
        return NotificationResult.failure("router", "no adapter supports type: " + notification.getType());
    }

    /**
     * Generates the rate limit tracking key.
     *
     * @param dealerId The dealer identifier
     * @param leadId   The lead identifier
     * @param date     The date for rate limiting
     * @return Composite key for rate limit map
     */
    private String rateLimitKey(String dealerId, String leadId, LocalDate date) {
        return dealerId + ":" + leadId + ":" + date;
    }
}