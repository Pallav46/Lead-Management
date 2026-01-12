package com.tekion.leadmanagement.adapter.notification.sms;

import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;

/**
 * Twilio Verify adapter for sending OTP verification codes via SMS.
 * 
 * <p>This adapter uses Twilio's Verify API which automatically generates
 * and sends a 6-digit OTP code to the recipient's phone number.
 * 
 * <p>Unlike the Messages API, Verify doesn't require a Twilio phone number -
 * it uses a Verify Service SID instead.
 */
public class TwilioVerifyAdapter implements NotificationPort {

    private static final String VENDOR_NAME = "twilio-verify";

    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;
    /** Volatile for proper visibility in double-checked locking pattern. */
    private volatile boolean initialized = false;

    /**
     * Creates a new Twilio Verify adapter.
     *
     * @param accountSid       Twilio Account SID (starts with AC)
     * @param authToken        Twilio Auth Token
     * @param verifyServiceSid Twilio Verify Service SID (starts with VA)
     */
    public TwilioVerifyAdapter(String accountSid, String authToken, String verifyServiceSid) {
        if (accountSid == null || accountSid.isBlank()) {
            throw new IllegalArgumentException("accountSid cannot be blank");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("authToken cannot be blank");
        }
        if (verifyServiceSid == null || verifyServiceSid.isBlank()) {
            throw new IllegalArgumentException("verifyServiceSid cannot be blank");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.verifyServiceSid = verifyServiceSid;
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            Twilio.init(accountSid, authToken);
            initialized = true;
        }
    }

    /**
     * Sends an OTP verification code via Twilio Verify API.
     *
     * @param notification The notification (phone number in 'to' field)
     * @return Result with verification SID on success
     */
    @Override
    public NotificationResult send(Notification notification) {
        if (notification == null) {
            return NotificationResult.failure(VENDOR_NAME, "notification was null");
        }
        if (!supports(notification.getType())) {
            return NotificationResult.failure(VENDOR_NAME,
                    "unsupported type: " + notification.getType());
        }

        try {
            ensureInitialized();

            // Send OTP via Twilio Verify API
            Verification verification = Verification.creator(
                    verifyServiceSid,
                    notification.getTo(),  // Phone in E.164 format
                    "sms"
            ).create();

            return NotificationResult.success(VENDOR_NAME, verification.getSid());

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("  ⚠️  Twilio Verify failed: " + e.getMessage());
            return NotificationResult.failure(VENDOR_NAME,
                    "Twilio Verify error: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }
}

