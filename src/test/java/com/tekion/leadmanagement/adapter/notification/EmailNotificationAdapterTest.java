package com.tekion.leadmanagement.adapter.notification;

import com.tekion.leadmanagement.adapter.notification.email.EmailNotificationAdapter;
import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailNotificationAdapterTest {

    private EmailNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmailNotificationAdapter();
    }

    private Notification createNotification(NotificationType type) {
        return new Notification(
                "dealer-1", "tenant-1", "site-1", "lead-1",
                type,
                "Test Subject",
                "Test body",
                "test@example.com"
        );
    }

    @Test
    void shouldSupportEmailType() {
        assertTrue(adapter.supports(NotificationType.EMAIL));
    }

    @Test
    void shouldSupportSmsTypeAsFallback() {
        assertTrue(adapter.supports(NotificationType.SMS));
    }

    @Test
    void shouldNotSupportPushType() {
        assertFalse(adapter.supports(NotificationType.PUSH));
    }

    @Test
    void shouldSendEmailSuccessfully() {
        Notification n = createNotification(NotificationType.EMAIL);

        NotificationResult result = adapter.send(n);

        assertTrue(result.isSuccess());
        assertEquals("email-adapter", result.getVendor());
        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("email-"));
    }

    @Test
    void shouldSendSmsAsFallbackSuccessfully() {
        Notification n = createNotification(NotificationType.SMS);

        NotificationResult result = adapter.send(n);

        assertTrue(result.isSuccess());
        assertEquals("email-adapter", result.getVendor());
    }

    @Test
    void shouldFailForNullNotification() {
        NotificationResult result = adapter.send(null);

        assertFalse(result.isSuccess());
        assertEquals("email-adapter", result.getVendor());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    void shouldFailForUnsupportedType() {
        Notification n = createNotification(NotificationType.PUSH);

        NotificationResult result = adapter.send(n);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("unsupported"));
    }
}

