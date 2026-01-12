package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldNormalizeToLowercaseAndTrim() {
        Email email = new Email("  Priya@Tekion.com  ");
        assertEquals("priya@tekion.com", email.getValue());
    }

    @Test
    void shouldRejectNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Email(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be null"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("a@b"));
        assertThrows(IllegalArgumentException.class, () -> new Email(" "));
    }
}

