package com.tekion.leadmanagement.adapter.notification.sms;

/**
 * Configuration holder for Twilio API credentials.
 *
 * <h2>Security Note</h2>
 * <p>In production, these values should come from:
 * <ul>
 *   <li>Environment variables</li>
 *   <li>AWS Secrets Manager / HashiCorp Vault</li>
 *   <li>Spring Boot application.properties with encryption</li>
 * </ul>
 *
 * <h2>Twilio Messages API</h2>
 * <p>This configuration uses the Twilio Messages API for sending
 * custom SMS messages. Requires a Twilio phone number.
 *
 * @see TwilioSmsAdapter for the adapter using this config
 */
public class TwilioConfig {

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;

    /**
     * Creates a new Twilio configuration.
     *
     * @param accountSid      Twilio Account SID (starts with AC)
     * @param authToken       Twilio Auth Token
     * @param fromPhoneNumber Your Twilio phone number (E.164 format, e.g., +1234567890)
     */
    public TwilioConfig(String accountSid, String authToken, String fromPhoneNumber) {
        if (accountSid == null || accountSid.isBlank()) {
            throw new IllegalArgumentException("accountSid cannot be blank");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("authToken cannot be blank");
        }
        if (fromPhoneNumber == null || fromPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("fromPhoneNumber cannot be blank");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhoneNumber = fromPhoneNumber;
    }

    /**
     * Creates a config from environment variables.
     *
     * <p>Expected environment variables:
     * <ul>
     *   <li>TWILIO_ACCOUNT_SID</li>
     *   <li>TWILIO_AUTH_TOKEN</li>
     *   <li>TWILIO_FROM_PHONE_NUMBER</li>
     * </ul>
     *
     * @return TwilioConfig from environment
     * @throws IllegalArgumentException if any env var is missing
     */
    public static TwilioConfig fromEnvironment() {
        return new TwilioConfig(
                System.getenv("TWILIO_ACCOUNT_SID"),
                System.getenv("TWILIO_AUTH_TOKEN"),
                System.getenv("TWILIO_FROM_PHONE_NUMBER")
        );
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getFromPhoneNumber() {
        return fromPhoneNumber;
    }
}

