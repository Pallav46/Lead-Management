package com.tekion.leadmanagement.domain.notification.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    private Notification createValidNotification(NotificationType type) {
        return new Notification(
                "dealer-1",
                "tenant-1",
                "site-1",
                "lead-1",
                type,
                "Test Subject",
                "Test body message",
                "recipient@test.com"
        );
    }

    @Test
    void shouldCreateValidEmailNotification() {
        Notification n = createValidNotification(NotificationType.EMAIL);

        assertEquals("dealer-1", n.getDealerId());
        assertEquals("tenant-1", n.getTenantId());
        assertEquals("site-1", n.getSiteId());
        assertEquals("lead-1", n.getLeadId());
        assertEquals(NotificationType.EMAIL, n.getType());
        assertEquals("Test Subject", n.getSubject());
        assertEquals("Test body message", n.getBody());
        assertEquals("recipient@test.com", n.getTo());
    }

    @Test
    void shouldCreateValidSmsNotification() {
        Notification n = new Notification(
                "dealer-1", "tenant-1", "site-1", "lead-1",
                NotificationType.SMS,
                null, // SMS doesn't need subject
                "Your appointment is confirmed",
                "+14155550123"
        );

        assertEquals(NotificationType.SMS, n.getType());
        assertNull(n.getSubject());
    }

    @Test
    void shouldTrimAllStringFields() {
        Notification n = new Notification(
                "  dealer-1  ",
                "  tenant-1  ",
                "  site-1  ",
                "  lead-1  ",
                NotificationType.EMAIL,
                "  Subject  ",
                "Body",
                "  to@test.com  "
        );

        assertEquals("dealer-1", n.getDealerId());
        assertEquals("tenant-1", n.getTenantId());
        assertEquals("site-1", n.getSiteId());
        assertEquals("lead-1", n.getLeadId());
        assertEquals("Subject", n.getSubject());
        assertEquals("to@test.com", n.getTo());
    }

    @Test
    void shouldRejectBlankDealerId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("  ", "t", "s", "l", NotificationType.EMAIL, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectNullDealerId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification(null, "t", "s", "l", NotificationType.EMAIL, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectBlankTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "  ", "s", "l", NotificationType.EMAIL, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectBlankSiteId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "t", "  ", "l", NotificationType.EMAIL, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectBlankLeadId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "t", "s", "  ", NotificationType.EMAIL, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "t", "s", "l", null, null, "body", "to@test.com")
        );
    }

    @Test
    void shouldRejectBlankBody() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "t", "s", "l", NotificationType.EMAIL, null, "  ", "to@test.com")
        );
    }

    @Test
    void shouldRejectBlankTo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("d", "t", "s", "l", NotificationType.EMAIL, null, "body", "  ")
        );
    }
}

