package com.tekion.leadmanagement.adapter.notification;

import com.tekion.leadmanagement.adapter.notification.sms.SmsNotificationAdapter;
import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsNotificationAdapterTest {

    private Notification createSmsNotification() {
        return new Notification(
                "dealer-1", "tenant-1", "site-1", "lead-1",
                NotificationType.SMS,
                null,
                "Your appointment is confirmed",
                "+14155550123"
        );
    }

    @Test
    void shouldSupportSmsType() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();
        assertTrue(adapter.supports(NotificationType.SMS));
    }

    @Test
    void shouldNotSupportEmailType() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();
        assertFalse(adapter.supports(NotificationType.EMAIL));
    }

    @Test
    void shouldNotSupportPushType() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();
        assertFalse(adapter.supports(NotificationType.PUSH));
    }

    @Test
    void shouldSendSmsSuccessfully() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter(false);

        NotificationResult result = adapter.send(createSmsNotification());

        assertTrue(result.isSuccess());
        assertEquals("sms-adapter", result.getVendor());
        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("sms-"));
    }

    @Test
    void shouldFailWhenConfiguredToFail() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter(true);

        NotificationResult result = adapter.send(createSmsNotification());

        assertFalse(result.isSuccess());
        assertEquals("sms-adapter", result.getVendor());
        assertTrue(result.getErrorMessage().contains("simulated"));
    }

    @Test
    void shouldFailForNullNotification() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();

        NotificationResult result = adapter.send(null);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    void shouldFailForUnsupportedType() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();
        Notification emailNotification = new Notification(
                "dealer-1", "tenant-1", "site-1", "lead-1",
                NotificationType.EMAIL,
                "Subject",
                "Body",
                "test@example.com"
        );

        NotificationResult result = adapter.send(emailNotification);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("unsupported"));
    }

    @Test
    void shouldUseDefaultConstructorWithNoFail() {
        SmsNotificationAdapter adapter = new SmsNotificationAdapter();

        NotificationResult result = adapter.send(createSmsNotification());

        assertTrue(result.isSuccess());
    }
}

