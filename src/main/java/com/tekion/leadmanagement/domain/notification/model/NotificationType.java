package com.tekion.leadmanagement.domain.notification.model;

/**
 * Represents the available notification delivery channels.
 *
 * <h2>Channel Characteristics</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Recipient Format</th><th>Subject</th><th>Use Case</th></tr>
 *   <tr><td>EMAIL</td><td>Email address</td><td>Required</td><td>Detailed info, documents</td></tr>
 *   <tr><td>SMS</td><td>E.164 phone</td><td>N/A</td><td>Urgent, brief messages</td></tr>
 *   <tr><td>PUSH</td><td>Device token</td><td>Optional</td><td>Mobile app notifications</td></tr>
 * </table>
 *
 * <h2>Adapter Selection</h2>
 * <p>The {@code NotificationRouter} uses this type to select appropriate adapters.
 * Each adapter implements {@code supports(NotificationType)} to indicate which
 * types it can handle.
 *
 * @see com.tekion.leadmanagement.domain.notification.port.NotificationAdapter
 * @see com.tekion.leadmanagement.application.notification.NotificationRouter
 */
public enum NotificationType {

    /**
     * Email notification - supports rich content and attachments.
     * <p>Requires subject line. Recipient should be valid email address.
     */
    EMAIL,

    /**
     * SMS text message - limited to 160 characters for single SMS.
     * <p>No subject line. Recipient should be E.164 format phone number.
     */
    SMS,

    /**
     * Mobile push notification - delivered via device-specific tokens.
     * <p>Subject is optional (used as title). Recipient is push token.
     */
    PUSH
}