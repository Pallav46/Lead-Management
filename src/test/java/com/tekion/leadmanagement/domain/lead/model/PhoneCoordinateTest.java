package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneCoordinateTest {

    @Test
    void shouldNormalizePhoneNumber() {
        PhoneCoordinate phone = new PhoneCoordinate("+1", "(415) 555-0123");

        assertEquals("+1", phone.getCountryCode());
        assertEquals("4155550123", phone.getNumber());
    }

    @Test
    void shouldFormatToE164() {
        PhoneCoordinate phone = new PhoneCoordinate("+1", "4155550123");

        assertEquals("+14155550123", phone.toE164());
    }

    @Test
    void shouldDefaultCountryCodeToUS() {
        PhoneCoordinate phone = new PhoneCoordinate(null, "4155550123");

        assertEquals("+1", phone.getCountryCode());
    }

    @Test
    void shouldDefaultEmptyCountryCodeToUS() {
        PhoneCoordinate phone = new PhoneCoordinate("  ", "4155550123");

        assertEquals("+1", phone.getCountryCode());
    }

    @Test
    void shouldAcceptInternationalCountryCode() {
        PhoneCoordinate phone = new PhoneCoordinate("+44", "7911123456");

        assertEquals("+44", phone.getCountryCode());
        assertEquals("7911123456", phone.getNumber());
        assertEquals("+447911123456", phone.toE164());
    }

    @Test
    void shouldRejectCountryCodeWithoutPlus() {
        assertThrows(IllegalArgumentException.class, () ->
                new PhoneCoordinate("1", "4155550123")
        );
    }

    @Test
    void shouldRejectNullPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new PhoneCoordinate("+1", null)
        );
    }

    @Test
    void shouldRejectShortPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new PhoneCoordinate("+1", "123456789") // 9 digits
        );
    }

    @Test
    void shouldAcceptExactly10Digits() {
        PhoneCoordinate phone = new PhoneCoordinate("+1", "1234567890");

        assertEquals("1234567890", phone.getNumber());
    }

    @Test
    void shouldStripNonDigits() {
        PhoneCoordinate phone = new PhoneCoordinate("+1", "+1 (415) 555-0123 ext 100");

        assertEquals("14155550123100", phone.getNumber());
    }
}

