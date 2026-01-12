package com.tekion.leadmanagement.application.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CircuitBreaker implementation.
 */
class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        // 3 failures to open, 1 second timeout for fast tests
        breaker = new CircuitBreaker("test-breaker", 3, Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Should start in CLOSED state")
    void shouldStartClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertTrue(breaker.allowRequest());
    }

    @Test
    @DisplayName("Should stay CLOSED after failures below threshold")
    void shouldStayClosedBelowThreshold() {
        breaker.recordFailure();
        breaker.recordFailure();
        
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertTrue(breaker.allowRequest());
    }

    @Test
    @DisplayName("Should OPEN after reaching failure threshold")
    void shouldOpenAfterThreshold() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertFalse(breaker.allowRequest());
    }

    @Test
    @DisplayName("Should transition to HALF_OPEN after timeout")
    void shouldTransitionToHalfOpenAfterTimeout() throws InterruptedException {
        // Open the circuit
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        
        // Wait for timeout
        Thread.sleep(150);
        
        // Should transition to HALF_OPEN on next request
        assertTrue(breaker.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
    }

    @Test
    @DisplayName("Should CLOSE after success in HALF_OPEN state")
    void shouldCloseAfterSuccessInHalfOpen() throws InterruptedException {
        // Open the circuit
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        
        // Wait for timeout and transition to HALF_OPEN
        Thread.sleep(150);
        breaker.allowRequest();
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
        
        // Record success
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    @DisplayName("Should reopen after failure in HALF_OPEN state")
    void shouldReopenAfterFailureInHalfOpen() throws InterruptedException {
        // Open the circuit
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        
        // Wait and transition to HALF_OPEN
        Thread.sleep(150);
        breaker.allowRequest();
        
        // Record failure - should reopen
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    }

    @Test
    @DisplayName("Should reset failure count on success")
    void shouldResetFailureCountOnSuccess() {
        breaker.recordFailure();
        breaker.recordFailure();
        assertEquals(2, breaker.getFailureCount());
        
        breaker.recordSuccess();
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @DisplayName("Should reset to CLOSED state")
    void shouldReset() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        
        breaker.reset();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @DisplayName("Default constructor should use sensible defaults")
    void defaultConstructorShouldWork() {
        CircuitBreaker defaultBreaker = new CircuitBreaker("default");
        assertEquals("default", defaultBreaker.getName());
        assertEquals(CircuitBreaker.State.CLOSED, defaultBreaker.getState());
    }
}

