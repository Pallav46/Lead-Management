package com.tekion.leadmanagement.adapter.persistence.inmemory;

import com.tekion.leadmanagement.domain.lead.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLeadRepositoryTest {

    private InMemoryLeadRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryLeadRepository();
    }

    private Lead createLead(String dealerId, String firstName, LeadSource source) {
        return Lead.newLead(
                dealerId, "tenant-1", "site-1",
                firstName, "Test",
                new Email(firstName.toLowerCase() + "@test.com"),
                new PhoneCoordinate("+1", "4155550123"),
                source,
                new VehicleInterest("Toyota", "Camry", 2020, 15000)
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // save() tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldSaveAndReturnLead() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);

        Lead saved = repo.save(lead);

        assertNotNull(saved);
        assertEquals(lead.getLeadId(), saved.getLeadId());
    }

    @Test
    void shouldUpdateExistingLead() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        repo.save(lead);

        lead.updateScore(85);
        repo.save(lead);

        Lead fetched = repo.findByIdAndDealerId(lead.getLeadId(), "dealer-1").orElseThrow();
        assertEquals(85, fetched.getScore());
    }

    @Test
    void shouldThrowOnNullLead() {
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
    }

    @Test
    void shouldThrowOnBlankDealerId() {
        // Lead.newLead validates dealerId, so we test that validation is enforced
        assertThrows(IllegalArgumentException.class, () ->
                Lead.newLead("   ", "t", "s", "F", "L",
                        new Email("a@b.com"),
                        new PhoneCoordinate("+1", "1234567890"),
                        LeadSource.WEBSITE,
                        new VehicleInterest("T", "M", 2020, null))
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // findByIdAndDealerId() tests - Multi-Tenant Isolation
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldFindLeadByIdAndDealerId() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        repo.save(lead);

        Optional<Lead> found = repo.findByIdAndDealerId(lead.getLeadId(), "dealer-1");

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
    }

    @Test
    void shouldReturnEmptyWhenLeadNotFound() {
        Optional<Lead> found = repo.findByIdAndDealerId("non-existent", "dealer-1");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldEnforceMultiTenantIsolation_CannotAccessOtherDealerLead() {
        Lead dealer1Lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        Lead dealer2Lead = createLead("dealer-2", "Jane", LeadSource.REFERRAL);
        repo.save(dealer1Lead);
        repo.save(dealer2Lead);

        // Dealer-1 cannot access Dealer-2's lead
        Optional<Lead> crossAccess = repo.findByIdAndDealerId(dealer2Lead.getLeadId(), "dealer-1");
        assertFalse(crossAccess.isPresent());

        // Dealer-2 cannot access Dealer-1's lead
        Optional<Lead> reverseAccess = repo.findByIdAndDealerId(dealer1Lead.getLeadId(), "dealer-2");
        assertFalse(reverseAccess.isPresent());
    }

    @Test
    void shouldReturnEmptyForNullInputs() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        repo.save(lead);

        assertFalse(repo.findByIdAndDealerId(null, "dealer-1").isPresent());
        assertFalse(repo.findByIdAndDealerId(lead.getLeadId(), null).isPresent());
        assertFalse(repo.findByIdAndDealerId(null, null).isPresent());
    }

    // ═══════════════════════════════════════════════════════════════
    // findByDealerIdAndState() tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldFindLeadsByDealerIdAndState() {
        Lead lead1 = createLead("dealer-1", "John", LeadSource.WEBSITE);
        Lead lead2 = createLead("dealer-1", "Jane", LeadSource.REFERRAL);
        lead2.transitionTo(LeadState.CONTACTED);
        repo.save(lead1);
        repo.save(lead2);

        List<Lead> newLeads = repo.findByDealerIdAndState("dealer-1", LeadState.NEW);
        List<Lead> contactedLeads = repo.findByDealerIdAndState("dealer-1", LeadState.CONTACTED);

        assertEquals(1, newLeads.size());
        assertEquals("John", newLeads.get(0).getFirstName());
        assertEquals(1, contactedLeads.size());
        assertEquals("Jane", contactedLeads.get(0).getFirstName());
    }

    @Test
    void shouldReturnEmptyListForNoMatchingState() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        repo.save(lead);

        List<Lead> qualified = repo.findByDealerIdAndState("dealer-1", LeadState.QUALIFIED);

        assertTrue(qualified.isEmpty());
    }

    @Test
    void shouldOnlyReturnLeadsForSpecificDealer() {
        Lead dealer1Lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        Lead dealer2Lead = createLead("dealer-2", "Jane", LeadSource.WEBSITE);
        repo.save(dealer1Lead);
        repo.save(dealer2Lead);

        List<Lead> dealer1Leads = repo.findByDealerIdAndState("dealer-1", LeadState.NEW);
        List<Lead> dealer2Leads = repo.findByDealerIdAndState("dealer-2", LeadState.NEW);

        assertEquals(1, dealer1Leads.size());
        assertEquals("John", dealer1Leads.get(0).getFirstName());
        assertEquals(1, dealer2Leads.size());
        assertEquals("Jane", dealer2Leads.get(0).getFirstName());
    }

    @Test
    void shouldReturnEmptyListForNullInputs() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        repo.save(lead);

        assertTrue(repo.findByDealerIdAndState(null, LeadState.NEW).isEmpty());
        assertTrue(repo.findByDealerIdAndState("dealer-1", null).isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════
    // findByDealerIdOrderByScore() tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void shouldReturnLeadsOrderedByScoreDesc() {
        Lead l1 = createLead("dealer-1", "Low", LeadSource.WEBSITE);
        l1.setUpdatedAt(Instant.now().minusSeconds(10));
        l1.updateScore(20);

        Lead l2 = createLead("dealer-1", "High", LeadSource.REFERRAL);
        l2.setUpdatedAt(Instant.now());
        l2.updateScore(90);

        Lead l3 = createLead("dealer-1", "NoScore", LeadSource.PHONE);
        l3.setUpdatedAt(Instant.now().plusSeconds(5));
        // score intentionally not set (null) -> should come last

        repo.save(l1);
        repo.save(l2);
        repo.save(l3);

        List<Lead> top = repo.findByDealerIdOrderByScore("dealer-1", 10);

        assertEquals(3, top.size());
        assertEquals("High", top.get(0).getFirstName());    // 90
        assertEquals("Low", top.get(1).getFirstName());     // 20
        assertEquals("NoScore", top.get(2).getFirstName()); // null score last
    }

    @Test
    void shouldRespectLimit() {
        for (int i = 0; i < 10; i++) {
            Lead lead = createLead("dealer-1", "User" + i, LeadSource.WEBSITE);
            lead.updateScore(i * 10);
            repo.save(lead);
        }

        List<Lead> top3 = repo.findByDealerIdOrderByScore("dealer-1", 3);

        assertEquals(3, top3.size());
        assertEquals(90, top3.get(0).getScore()); // highest
        assertEquals(80, top3.get(1).getScore());
        assertEquals(70, top3.get(2).getScore());
    }

    @Test
    void shouldOnlyReturnLeadsForRequestedDealer() {
        Lead dealer1Lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        dealer1Lead.updateScore(50);
        Lead dealer2Lead = createLead("dealer-2", "Jane", LeadSource.WEBSITE);
        dealer2Lead.updateScore(90);
        repo.save(dealer1Lead);
        repo.save(dealer2Lead);

        List<Lead> dealer1Top = repo.findByDealerIdOrderByScore("dealer-1", 10);

        assertEquals(1, dealer1Top.size());
        assertEquals("John", dealer1Top.get(0).getFirstName());
    }

    @Test
    void shouldReturnEmptyListForInvalidInputs() {
        Lead lead = createLead("dealer-1", "John", LeadSource.WEBSITE);
        lead.updateScore(50);
        repo.save(lead);

        assertTrue(repo.findByDealerIdOrderByScore(null, 10).isEmpty());
        assertTrue(repo.findByDealerIdOrderByScore("dealer-1", 0).isEmpty());
        assertTrue(repo.findByDealerIdOrderByScore("dealer-1", -1).isEmpty());
    }

    @Test
    void shouldHandleTiesByUpdatedAtDesc() {
        Lead l1 = createLead("dealer-1", "Earlier", LeadSource.WEBSITE);
        l1.updateScore(50);
        l1.setUpdatedAt(Instant.now().minusSeconds(100));

        Lead l2 = createLead("dealer-1", "Later", LeadSource.WEBSITE);
        l2.updateScore(50); // same score
        l2.setUpdatedAt(Instant.now());

        repo.save(l1);
        repo.save(l2);

        List<Lead> top = repo.findByDealerIdOrderByScore("dealer-1", 10);

        assertEquals(2, top.size());
        assertEquals("Later", top.get(0).getFirstName());   // more recent
        assertEquals("Earlier", top.get(1).getFirstName()); // older
    }
}
