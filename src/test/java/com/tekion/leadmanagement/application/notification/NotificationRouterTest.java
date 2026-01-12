package com.tekion.leadmanagement.application.notification;

import com.tekion.leadmanagement.adapter.notification.email.EmailNotificationAdapter;
import com.tekion.leadmanagement.adapter.notification.sms.SmsNotificationAdapter;
import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRouterTest {

    private Notification createNotification(String dealerId, String leadId, NotificationType type) {
        return new Notification(
                dealerId, "tenant-1", "site-1",
                leadId,
                type,
                "Subject",
                "Body",
                "recipient@test.com"
        );
    }

    @Test
    void shouldFailoverWhenPrimaryVendorFails() {
        NotificationRouter router = new NotificationRouter(List.of(
                new SmsNotificationAdapter(true),   // fail
                new EmailNotificationAdapter()      // succeed
        ));

        Notification n = new Notification(
                "dealer-1", "tenant-1", "site-1",
                "lead-1",
                NotificationType.SMS,
                null,
                "hello",
                "+14155550123"
        );

        NotificationResult result = router.route(n);

        assertTrue(result.isSuccess());
        assertEquals("email-adapter", result.getVendor()); // failover happened
        assertNotNull(result.getMessageId());
    }

    @Test
    void shouldEnforceRateLimitMax3PerLeadPerDay() {
        NotificationRouter router = new NotificationRouter(List.of(
                new EmailNotificationAdapter()
        ));

        Notification n = new Notification(
                "dealer-1", "tenant-1", "site-1",
                "lead-1",
                NotificationType.EMAIL,
                "subj",
                "hello",
                "a@b.com"
        );

        assertTrue(router.route(n).isSuccess());
        assertTrue(router.route(n).isSuccess());
        assertTrue(router.route(n).isSuccess());

        NotificationResult fourth = router.route(n);
        assertFalse(fourth.isSuccess());
        assertTrue(fourth.getErrorMessage().toLowerCase().contains("rate limit"));
    }

    @Test
    void shouldFailForNullNotification() {
        NotificationRouter router = new NotificationRouter(List.of(
                new EmailNotificationAdapter()
        ));

        NotificationResult result = router.route(null);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    void shouldFailWhenNoAdapterSupportsType() {
        NotificationRouter router = new NotificationRouter(List.of(
                new SmsNotificationAdapter() // Only supports SMS
        ));

        Notification emailNotification = createNotification("dealer-1", "lead-1", NotificationType.EMAIL);

        NotificationResult result = router.route(emailNotification);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("no adapter supports"));
    }

    @Test
    void shouldThrowOnEmptyAdapterList() {
        assertThrows(IllegalArgumentException.class, () ->
                new NotificationRouter(List.of())
        );
    }

    @Test
    void shouldThrowOnNullAdapterList() {
        assertThrows(IllegalArgumentException.class, () ->
                new NotificationRouter(null)
        );
    }

    @Test
    void shouldTrackRateLimitPerLeadSeparately() {
        NotificationRouter router = new NotificationRouter(List.of(
                new EmailNotificationAdapter()
        ));

        Notification lead1 = createNotification("dealer-1", "lead-1", NotificationType.EMAIL);
        Notification lead2 = createNotification("dealer-1", "lead-2", NotificationType.EMAIL);

        // Send 3 to lead-1
        assertTrue(router.route(lead1).isSuccess());
        assertTrue(router.route(lead1).isSuccess());
        assertTrue(router.route(lead1).isSuccess());

        // 4th to lead-1 should fail
        assertFalse(router.route(lead1).isSuccess());

        // But lead-2 should still work (separate rate limit)
        assertTrue(router.route(lead2).isSuccess());
    }

    @Test
    void shouldTrackRateLimitPerDealerSeparately() {
        NotificationRouter router = new NotificationRouter(List.of(
                new EmailNotificationAdapter()
        ));

        Notification dealer1Lead = createNotification("dealer-1", "lead-1", NotificationType.EMAIL);
        Notification dealer2Lead = createNotification("dealer-2", "lead-1", NotificationType.EMAIL);

        // Send 3 to dealer-1's lead-1
        assertTrue(router.route(dealer1Lead).isSuccess());
        assertTrue(router.route(dealer1Lead).isSuccess());
        assertTrue(router.route(dealer1Lead).isSuccess());
        assertFalse(router.route(dealer1Lead).isSuccess()); // 4th fails

        // But dealer-2's lead-1 is a different rate limit key
        assertTrue(router.route(dealer2Lead).isSuccess());
    }

    @Test
    void shouldSuccessfullyRouteWhenPrimaryAdapterWorks() {
        NotificationRouter router = new NotificationRouter(List.of(
                new SmsNotificationAdapter(false) // SMS works
        ));

        Notification smsNotification = new Notification(
                "dealer-1", "tenant-1", "site-1", "lead-1",
                NotificationType.SMS,
                null,
                "Your appointment is confirmed",
                "+14155550123"
        );

        NotificationResult result = router.route(smsNotification);

        assertTrue(result.isSuccess());
        assertEquals("sms-adapter", result.getVendor());
    }
}

