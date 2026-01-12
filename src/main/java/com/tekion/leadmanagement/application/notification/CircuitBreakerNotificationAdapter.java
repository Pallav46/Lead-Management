package com.tekion.leadmanagement.application.notification;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;

/**
 * Decorator that wraps a NotificationPort with circuit breaker protection.
 *
 * <h2>Purpose</h2>
 * <p>Protects the system from cascading failures when a notification vendor
 * is experiencing issues. When the circuit opens, requests fail fast without
 * attempting the actual call.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * NotificationPort smsAdapter = new TwilioSmsAdapter(config);
 * CircuitBreaker breaker = new CircuitBreaker("twilio-sms", 3, Duration.ofSeconds(30));
 * NotificationPort protectedAdapter = new CircuitBreakerNotificationAdapter(smsAdapter, breaker);
 * 
 * // Use protectedAdapter in NotificationRouter
 * }</pre>
 *
 * @see CircuitBreaker for the circuit breaker implementation
 */
public class CircuitBreakerNotificationAdapter implements NotificationPort {

    private final NotificationPort delegate;
    private final CircuitBreaker circuitBreaker;

    /**
     * Creates a circuit-breaker-protected notification adapter.
     *
     * @param delegate       The underlying notification adapter
     * @param circuitBreaker The circuit breaker instance to use
     */
    public CircuitBreakerNotificationAdapter(NotificationPort delegate, CircuitBreaker circuitBreaker) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("circuitBreaker cannot be null");
        }
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Sends a notification through the protected adapter.
     *
     * <p>If the circuit is open, returns failure immediately without
     * attempting the actual send operation.
     *
     * @param notification The notification to send
     * @return Result from delegate or circuit-open failure
     */
    @Override
    public NotificationResult send(Notification notification) {
        // Check if circuit allows the request
        if (!circuitBreaker.allowRequest()) {
            return NotificationResult.failure(
                    circuitBreaker.getName() + "-circuit-breaker",
                    "Circuit is OPEN - service temporarily unavailable (will retry after timeout)"
            );
        }

        // Attempt the actual send
        NotificationResult result = delegate.send(notification);

        // Record result in circuit breaker
        if (result.isSuccess()) {
            circuitBreaker.recordSuccess();
        } else {
            circuitBreaker.recordFailure();
        }

        return result;
    }

    /**
     * Delegates support check to underlying adapter.
     */
    @Override
    public boolean supports(NotificationType type) {
        return delegate.supports(type);
    }

    /**
     * Gets the current circuit breaker state.
     */
    public CircuitBreaker.State getCircuitState() {
        return circuitBreaker.getState();
    }

    /**
     * Gets the circuit breaker instance.
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}

