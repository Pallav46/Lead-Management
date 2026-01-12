package com.tekion.leadmanagement.application.notification;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CircuitBreakerNotificationAdapter.
 */
class CircuitBreakerNotificationAdapterTest {

    private CircuitBreaker breaker;
    private TestNotificationPort delegate;
    private CircuitBreakerNotificationAdapter adapter;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        breaker = new CircuitBreaker("test", 2, Duration.ofMillis(100));
        delegate = new TestNotificationPort();
        adapter = new CircuitBreakerNotificationAdapter(delegate, breaker);
        testNotification = new Notification("d1", "t1", "s1", "l1", 
                NotificationType.SMS, null, "Test message", "+1234567890");
    }

    @Test
    @DisplayName("Should pass through to delegate when circuit is closed")
    void shouldPassThroughWhenClosed() {
        NotificationResult result = adapter.send(testNotification);
        
        assertTrue(result.isSuccess());
        assertEquals(1, delegate.sendCount);
    }

    @Test
    @DisplayName("Should fail fast when circuit is open")
    void shouldFailFastWhenOpen() {
        // Open the circuit
        delegate.shouldFail = true;
        adapter.send(testNotification);
        adapter.send(testNotification);
        
        assertEquals(CircuitBreaker.State.OPEN, adapter.getCircuitState());
        
        // Next request should fail fast without calling delegate
        int countBefore = delegate.sendCount;
        NotificationResult result = adapter.send(testNotification);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Circuit is OPEN"));
        assertEquals(countBefore, delegate.sendCount); // delegate not called
    }

    @Test
    @DisplayName("Should record success and keep circuit closed")
    void shouldRecordSuccess() {
        adapter.send(testNotification);
        adapter.send(testNotification);
        
        assertEquals(CircuitBreaker.State.CLOSED, adapter.getCircuitState());
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @DisplayName("Should record failures and open circuit")
    void shouldRecordFailures() {
        delegate.shouldFail = true;
        
        adapter.send(testNotification);
        assertEquals(1, breaker.getFailureCount());
        
        adapter.send(testNotification);
        assertEquals(CircuitBreaker.State.OPEN, adapter.getCircuitState());
    }

    @Test
    @DisplayName("Should delegate supports check")
    void shouldDelegateSupports() {
        assertTrue(adapter.supports(NotificationType.SMS));
        assertFalse(adapter.supports(NotificationType.EMAIL));
    }

    @Test
    @DisplayName("Should reject null delegate")
    void shouldRejectNullDelegate() {
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreakerNotificationAdapter(null, breaker));
    }

    @Test
    @DisplayName("Should reject null circuit breaker")
    void shouldRejectNullBreaker() {
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreakerNotificationAdapter(delegate, null));
    }

    /**
     * Test implementation of NotificationPort.
     */
    private static class TestNotificationPort implements NotificationPort {
        int sendCount = 0;
        boolean shouldFail = false;

        @Override
        public NotificationResult send(Notification notification) {
            sendCount++;
            if (shouldFail) {
                return NotificationResult.failure("test", "simulated failure");
            }
            return NotificationResult.success("test", "msg-123");
        }

        @Override
        public boolean supports(NotificationType type) {
            return type == NotificationType.SMS;
        }
    }
}

