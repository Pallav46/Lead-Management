package com.tekion.leadmanagement.domain.lead.model;

import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VehicleInterestTest {

    @Test
    void shouldCreateWithAllFields() {
        VehicleInterest vi = new VehicleInterest("Toyota", "Camry", 2020, 15000);

        assertEquals("Toyota", vi.getMake());
        assertEquals("Camry", vi.getModel());
        assertEquals(2020, vi.getYear());
        assertEquals(Optional.of(15000), vi.getTradeInValue());
    }

    @Test
    void shouldAllowNullTradeInValue() {
        VehicleInterest vi = new VehicleInterest("Honda", "Accord", 2018, null);

        assertEquals(Optional.empty(), vi.getTradeInValue());
    }

    @Test
    void shouldTrimMakeAndModel() {
        VehicleInterest vi = new VehicleInterest("  Toyota  ", "  Camry  ", 2020, null);

        assertEquals("Toyota", vi.getMake());
        assertEquals("Camry", vi.getModel());
    }

    @Test
    void shouldCalculateVehicleAge() {
        int currentYear = Year.now().getValue();
        VehicleInterest vi = new VehicleInterest("Toyota", "Camry", 2020, null);

        assertEquals(currentYear - 2020, vi.getCurrentVehicleAge());
    }

    @Test
    void shouldRejectBlankMake() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("  ", "Camry", 2020, null)
        );
    }

    @Test
    void shouldRejectNullMake() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest(null, "Camry", 2020, null)
        );
    }

    @Test
    void shouldRejectBlankModel() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("Toyota", "  ", 2020, null)
        );
    }

    @Test
    void shouldRejectNullModel() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("Toyota", null, 2020, null)
        );
    }

    @Test
    void shouldRejectYearBefore1900() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("Ford", "Model T", 1899, null)
        );
    }

    @Test
    void shouldRejectYearTooFarInFuture() {
        int futureYear = Year.now().getValue() + 2;
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("Tesla", "Model S", futureYear, null)
        );
    }

    @Test
    void shouldAcceptNextYearVehicle() {
        int nextYear = Year.now().getValue() + 1;
        VehicleInterest vi = new VehicleInterest("Toyota", "Camry", nextYear, null);

        assertEquals(nextYear, vi.getYear());
    }

    @Test
    void shouldRejectNegativeTradeInValue() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInterest("Toyota", "Camry", 2020, -1000)
        );
    }

    @Test
    void shouldAcceptZeroTradeInValue() {
        VehicleInterest vi = new VehicleInterest("Toyota", "Camry", 2020, 0);

        assertEquals(Optional.of(0), vi.getTradeInValue());
    }
}

