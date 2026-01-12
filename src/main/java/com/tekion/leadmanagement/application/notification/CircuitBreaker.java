package com.tekion.leadmanagement.application.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker implementation for fault tolerance in notification adapters.
 *
 * <h2>Circuit Breaker Pattern</h2>
 * <p>Prevents cascading failures by temporarily disabling calls to a failing service.
 * The circuit has three states:
 * <ul>
 *   <li><b>CLOSED:</b> Normal operation, requests pass through</li>
 *   <li><b>OPEN:</b> Service is failing, requests are rejected immediately</li>
 *   <li><b>HALF_OPEN:</b> Testing if service has recovered</li>
 * </ul>
 *
 * <h2>State Transitions</h2>
 * <pre>
 *   CLOSED --[failure threshold reached]--> OPEN
 *   OPEN --[timeout expires]--> HALF_OPEN
 *   HALF_OPEN --[success]--> CLOSED
 *   HALF_OPEN --[failure]--> OPEN
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Uses atomic operations for thread-safe state management.
 *
 * @see CircuitBreakerNotificationAdapter for usage with notification ports
 */
public class CircuitBreaker {

    /**
     * Circuit breaker states.
     */
    public enum State {
        CLOSED,     // Normal operation
        OPEN,       // Failing, reject requests
        HALF_OPEN   // Testing recovery
    }

    private final String name;
    private final int failureThreshold;
    private final Duration openTimeout;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(Instant.MIN);

    /**
     * Creates a circuit breaker with default settings.
     * Default: 3 failures to open, 30 seconds timeout.
     *
     * @param name Identifier for logging/monitoring
     */
    public CircuitBreaker(String name) {
        this(name, 3, Duration.ofSeconds(30));
    }

    /**
     * Creates a circuit breaker with custom settings.
     *
     * @param name             Identifier for logging/monitoring
     * @param failureThreshold Number of failures before opening circuit
     * @param openTimeout      Time to wait before attempting recovery
     */
    public CircuitBreaker(String name, int failureThreshold, Duration openTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openTimeout = openTimeout;
    }

    /**
     * Checks if the circuit allows requests to pass through.
     *
     * @return true if request should be allowed, false if circuit is open
     */
    public boolean allowRequest() {
        State currentState = state.get();
        
        if (currentState == State.CLOSED) {
            return true;
        }
        
        if (currentState == State.OPEN) {
            // Check if timeout has expired
            if (Instant.now().isAfter(lastFailureTime.get().plus(openTimeout))) {
                // Transition to HALF_OPEN to test recovery
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    successCount.set(0);
                }
                return true;
            }
            return false; // Still in timeout period
        }
        
        // HALF_OPEN: allow limited requests to test recovery
        return true;
    }

    /**
     * Records a successful operation.
     * In HALF_OPEN state, closes the circuit after success.
     */
    public void recordSuccess() {
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Recovery confirmed, close the circuit
            state.set(State.CLOSED);
            failureCount.set(0);
            successCount.set(0);
        } else if (currentState == State.CLOSED) {
            // Reset failure count on success
            failureCount.set(0);
        }
        
        successCount.incrementAndGet();
    }

    /**
     * Records a failed operation.
     * Opens the circuit if failure threshold is reached.
     */
    public void recordFailure() {
        lastFailureTime.set(Instant.now());
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Recovery failed, reopen circuit
            state.set(State.OPEN);
            return;
        }
        
        if (currentState == State.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                state.set(State.OPEN);
            }
        }
    }

    /**
     * Gets the current circuit state.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Gets the circuit breaker name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the current failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Resets the circuit breaker to closed state.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
    }
}

