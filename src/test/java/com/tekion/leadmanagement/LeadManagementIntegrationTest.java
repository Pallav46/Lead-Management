package com.tekion.leadmanagement;

import com.tekion.leadmanagement.adapter.notification.email.EmailNotificationAdapter;
import com.tekion.leadmanagement.adapter.notification.sms.SmsNotificationAdapter;
import com.tekion.leadmanagement.adapter.persistence.inmemory.InMemoryLeadRepository;
import com.tekion.leadmanagement.application.lead.LeadService;
import com.tekion.leadmanagement.application.notification.CircuitBreaker;
import com.tekion.leadmanagement.application.notification.CircuitBreakerNotificationAdapter;
import com.tekion.leadmanagement.application.notification.NotificationRouter;
import com.tekion.leadmanagement.domain.lead.model.*;
import com.tekion.leadmanagement.domain.lead.port.LeadPersistencePort;
import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.notification.port.NotificationPort;
import com.tekion.leadmanagement.domain.scoring.model.ScoringResult;
import com.tekion.leadmanagement.domain.scoring.rule.*;
import com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test for Lead Management System.
 * 
 * This test file validates ALL components working together:
 * - Domain Model (Lead, Value Objects, State Machine)
 * - Hexagonal Architecture (Ports & Adapters)
 * - Scoring Engine (5 Rules)
 * - Notification Router (Failover + Rate Limiting)
 * - Circuit Breaker
 * - Audit Trail
 * - Bulk Operations
 */
@DisplayName("Lead Management System - Full Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeadManagementIntegrationTest {

    // ═══════════════════════════════════════════════════════════════════
    // TEST FIXTURES
    // ═══════════════════════════════════════════════════════════════════
    
    private LeadPersistencePort repository;
    private LeadScoringEngine scoringEngine;
    private LeadService leadService;
    private NotificationRouter notificationRouter;
    
    private static final String DEALER_ID = "dealer-integration-test";
    private static final String TENANT_ID = "tenant-001";
    private static final String SITE_ID = "site-001";

    @BeforeEach
    void setUp() {
        // Setup Hexagonal Architecture components
        repository = new InMemoryLeadRepository();
        
        List<ScoringRule> rules = List.of(
            new SourceQualityRule(),
            new VehicleAgeRule(),
            new TradeInValueRule(),
            new EngagementRule(),
            new RecencyRule()
        );
        scoringEngine = new LeadScoringEngine(rules);
        leadService = new LeadService(repository, scoringEngine);
        
        // Setup Notification Router with failover
        List<NotificationPort> adapters = List.of(
            new SmsNotificationAdapter(),
            new EmailNotificationAdapter()
        );
        notificationRouter = new NotificationRouter(adapters);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 1. DOMAIN MODEL TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("1.1 Value Objects - Email validation and normalization")
    void testEmailValueObject() {
        Email email = new Email("  TEST@Example.COM  ");
        assertEquals("test@example.com", email.getValue());
        
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid"));
        assertThrows(IllegalArgumentException.class, () -> new Email("no@domain"));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 Value Objects - Phone E.164 formatting")
    void testPhoneValueObject() {
        PhoneCoordinate phone = new PhoneCoordinate("+1", "2125551234");
        assertEquals("+12125551234", phone.toE164());

        assertThrows(IllegalArgumentException.class, () -> new PhoneCoordinate(null, "123")); // Too short
        assertThrows(IllegalArgumentException.class, () -> new PhoneCoordinate("+1", null));
        assertThrows(IllegalArgumentException.class, () -> new PhoneCoordinate("US", "2125551234")); // No + prefix
    }

    @Test
    @Order(3)
    @DisplayName("1.3 Value Objects - VehicleInterest with trade-in")
    void testVehicleInterestValueObject() {
        VehicleInterest vehicle = new VehicleInterest("Toyota", "Camry", 2020, 15000);

        assertEquals("Toyota", vehicle.getMake());
        assertEquals(6, vehicle.getCurrentVehicleAge()); // 2026 - 2020 = 6
        assertTrue(vehicle.getTradeInValue().isPresent());
        assertEquals(15000, vehicle.getTradeInValue().get());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 Lead Factory - Creates valid lead with all fields")
    void testLeadFactory() {
        Lead lead = createTestLead("John", "Doe", LeadSource.WEBSITE);
        
        assertNotNull(lead.getLeadId());
        assertEquals(DEALER_ID, lead.getDealerId());
        assertEquals(LeadState.NEW, lead.getState());
        assertNotNull(lead.getCreatedAt());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. STATE MACHINE & AUDIT TRAIL TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("2.1 State Machine - Valid transitions")
    void testValidStateTransitions() {
        Lead lead = createTestLead("Jane", "Smith", LeadSource.REFERRAL);
        
        assertEquals(LeadState.NEW, lead.getState());
        
        lead.transitionTo(LeadState.CONTACTED, "sales-rep-1", "Initial contact made");
        assertEquals(LeadState.CONTACTED, lead.getState());
        
        lead.transitionTo(LeadState.QUALIFIED, "sales-rep-1", "Budget confirmed");
        assertEquals(LeadState.QUALIFIED, lead.getState());
        
        lead.transitionTo(LeadState.CONVERTED, "sales-rep-1", "Deal closed!");
        assertEquals(LeadState.CONVERTED, lead.getState());
    }

    @Test
    @Order(6)
    @DisplayName("2.2 State Machine - Invalid transitions rejected")
    void testInvalidStateTransitions() {
        Lead lead = createTestLead("Bob", "Jones", LeadSource.PHONE);

        // NEW -> QUALIFIED is invalid (must go through CONTACTED)
        assertThrows(IllegalStateException.class,
            () -> lead.transitionTo(LeadState.QUALIFIED));

        // NEW -> CONVERTED is invalid
        assertThrows(IllegalStateException.class,
            () -> lead.transitionTo(LeadState.CONVERTED));
    }

    @Test
    @Order(7)
    @DisplayName("2.3 Audit Trail - Records all transitions")
    void testAuditTrail() {
        Lead lead = createTestLead("Alice", "Brown", LeadSource.WEBSITE);

        lead.transitionTo(LeadState.CONTACTED, "rep-1", "Called customer");
        lead.transitionTo(LeadState.QUALIFIED, "rep-1", "Budget approved");

        List<AuditEntry> audit = lead.getAuditTrail();
        assertEquals(2, audit.size());

        AuditEntry first = audit.get(0);
        assertEquals("rep-1", first.getActor());
        assertEquals(LeadState.NEW, first.getFromState());
        assertEquals(LeadState.CONTACTED, first.getToState());
        assertEquals("Called customer", first.getReason());
        assertNotNull(first.getTimestamp());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. HEXAGONAL ARCHITECTURE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("3.1 Persistence Port - Save and retrieve lead")
    void testPersistencePort() {
        Lead lead = createTestLead("Charlie", "Davis", LeadSource.REFERRAL);

        Lead saved = leadService.create(lead);
        assertNotNull(saved);

        Optional<Lead> found = leadService.findByIdAndDealerId(lead.getLeadId(), DEALER_ID);
        assertTrue(found.isPresent());
        assertEquals(lead.getLeadId(), found.get().getLeadId());
    }

    @Test
    @Order(9)
    @DisplayName("3.2 Persistence Port - Tenant isolation")
    void testTenantIsolation() {
        Lead lead = createTestLead("David", "Wilson", LeadSource.PHONE);
        leadService.create(lead);

        // Same lead ID, different dealer = not found
        Optional<Lead> notFound = leadService.findByIdAndDealerId(
            lead.getLeadId(), "different-dealer");
        assertTrue(notFound.isEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("3.3 Service Layer - State transition via service")
    void testServiceStateTransition() {
        Lead lead = createTestLead("Eve", "Taylor", LeadSource.WEBSITE);
        leadService.create(lead);

        Lead updated = leadService.transitionState(
            lead.getLeadId(), DEALER_ID, LeadState.CONTACTED);

        assertEquals(LeadState.CONTACTED, updated.getState());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. SCORING ENGINE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    @DisplayName("4.1 Scoring Engine - All 5 rules applied")
    void testAllScoringRules() {
        Lead lead = Lead.builder()
            .leadId(UUID.randomUUID().toString())
            .dealerId(DEALER_ID)
            .tenantId(TENANT_ID)
            .siteId(SITE_ID)
            .firstName("Frank")
            .lastName("Miller")
            .email(new Email("frank@example.com"))
            .phone(new PhoneCoordinate("+1", "5551234567"))
            .source(LeadSource.REFERRAL)
            .vehicleInterest(new VehicleInterest("BMW", "X5", 2024, 25000))
            .state(LeadState.QUALIFIED) // Higher engagement state
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        ScoringResult result = scoringEngine.score(lead);

        // All 5 rules should be applied
        assertEquals(5, result.getBreakdown().size());
        assertTrue(result.getFinalScore() >= 0, "Score should be non-negative");

        // Verify rule names (using actual rule names from implementations)
        Set<String> ruleNames = result.getBreakdown().keySet();
        assertTrue(ruleNames.contains("sourceQuality"), "Should have sourceQuality rule");
        assertTrue(ruleNames.contains("vehicleAge"), "Should have vehicleAge rule");
        assertTrue(ruleNames.contains("tradeInValue"), "Should have tradeInValue rule");
        assertTrue(ruleNames.contains("engagement"), "Should have engagement rule");
        assertTrue(ruleNames.contains("recency"), "Should have recency rule");
    }

    @Test
    @Order(12)
    @DisplayName("4.2 Scoring Engine - Compute and persist score")
    void testComputeAndPersistScore() {
        Lead lead = createTestLead("Grace", "Lee", LeadSource.WEBSITE);
        leadService.create(lead);

        leadService.computeAndPersistScore(lead.getLeadId(), DEALER_ID);

        Lead updated = leadService.findByIdAndDealerId(lead.getLeadId(), DEALER_ID).orElseThrow();
        assertNotNull(updated.getScore());
        assertTrue(updated.getScore() > 0);
    }

    @Test
    @Order(13)
    @DisplayName("4.3 Bulk Scoring - Multiple leads scored")
    void testBulkScoring() {
        List<Lead> leads = List.of(
            createTestLead("Lead1", "Test", LeadSource.WEBSITE),
            createTestLead("Lead2", "Test", LeadSource.REFERRAL),
            createTestLead("Lead3", "Test", LeadSource.PHONE)
        );

        Map<String, ScoringResult> results = scoringEngine.scoreBatch(leads);

        assertEquals(3, results.size());
        results.values().forEach(result -> assertTrue(result.getFinalScore() > 0));
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. NOTIFICATION ROUTER TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(14)
    @DisplayName("5.1 Notification Router - SMS routing")
    void testSmsRouting() {
        Notification sms = new Notification(
            DEALER_ID, TENANT_ID, SITE_ID, "lead-001",
            NotificationType.SMS, null, "Test SMS message", "+12125551234"
        );

        NotificationResult result = notificationRouter.route(sms);
        assertTrue(result.isSuccess());
        assertEquals("sms-adapter", result.getVendor());
    }

    @Test
    @Order(15)
    @DisplayName("5.2 Notification Router - Email routing")
    void testEmailRouting() {
        Notification email = new Notification(
            DEALER_ID, TENANT_ID, SITE_ID, "lead-002",
            NotificationType.EMAIL, "Test Subject", "Test email body", "customer@example.com"
        );

        NotificationResult result = notificationRouter.route(email);
        assertTrue(result.isSuccess());
        assertEquals("email-adapter", result.getVendor());
    }

    @Test
    @Order(16)
    @DisplayName("5.3 Notification Router - Rate limiting (max 3/day)")
    void testRateLimiting() {
        String leadId = "rate-limit-lead-" + UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            Notification sms = new Notification(
                DEALER_ID, TENANT_ID, SITE_ID, leadId,
                NotificationType.SMS, null, "Message " + i, "+12125551234"
            );
            assertTrue(notificationRouter.route(sms).isSuccess(), "Message " + i + " should succeed");
        }

        // 4th message should be rate limited
        Notification fourth = new Notification(
            DEALER_ID, TENANT_ID, SITE_ID, leadId,
            NotificationType.SMS, null, "Message 4", "+12125551234"
        );

        NotificationResult result = notificationRouter.route(fourth);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("rate limit"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. CIRCUIT BREAKER TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(17)
    @DisplayName("6.1 Circuit Breaker - Opens after failures")
    void testCircuitBreakerOpens() {
        CircuitBreaker cb = new CircuitBreaker("test-cb", 3, Duration.ofMillis(100));

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.allowRequest());
    }

    @Test
    @Order(18)
    @DisplayName("6.2 Circuit Breaker - Transitions to HALF_OPEN after timeout")
    void testCircuitBreakerHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("test-cb", 2, Duration.ofMillis(50));

        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        Thread.sleep(100);

        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    @Order(19)
    @DisplayName("6.3 Circuit Breaker - Closes on success in HALF_OPEN")
    void testCircuitBreakerCloses() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("test-cb", 2, Duration.ofMillis(50));

        cb.recordFailure();
        cb.recordFailure();
        Thread.sleep(100);
        cb.allowRequest();

        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    @Order(20)
    @DisplayName("6.4 Circuit Breaker Adapter - Wraps notification port")
    void testCircuitBreakerAdapter() {
        NotificationPort smsAdapter = new SmsNotificationAdapter();
        CircuitBreaker cb = new CircuitBreaker("sms-cb");
        CircuitBreakerNotificationAdapter wrappedAdapter =
            new CircuitBreakerNotificationAdapter(smsAdapter, cb);

        Notification sms = new Notification(
            DEALER_ID, TENANT_ID, SITE_ID, "lead-cb-test",
            NotificationType.SMS, null, "Test message", "+12125551234"
        );

        NotificationResult result = wrappedAdapter.send(sms);
        assertTrue(result.isSuccess());
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. END-TO-END WORKFLOW TEST
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Order(21)
    @DisplayName("7.1 End-to-End - Complete lead lifecycle")
    void testCompleteLeadLifecycle() {
        // 1. Create lead
        Lead lead = Lead.newLead(
            DEALER_ID, TENANT_ID, SITE_ID,
            "Integration", "Test",
            new Email("integration@test.com"),
            new PhoneCoordinate("+1", "5559876543"),
            LeadSource.WEBSITE,
            new VehicleInterest("Honda", "Accord", 2023, 10000)
        );

        Lead saved = leadService.create(lead);
        assertNotNull(saved.getLeadId());
        assertEquals(LeadState.NEW, saved.getState());

        // 2. Score the lead
        leadService.computeAndPersistScore(saved.getLeadId(), DEALER_ID);
        Lead scored = leadService.findByIdAndDealerId(saved.getLeadId(), DEALER_ID).orElseThrow();
        assertNotNull(scored.getScore());

        // 3. Transition through states
        leadService.transitionState(scored.getLeadId(), DEALER_ID, LeadState.CONTACTED);
        leadService.transitionState(scored.getLeadId(), DEALER_ID, LeadState.QUALIFIED);
        leadService.transitionState(scored.getLeadId(), DEALER_ID, LeadState.CONVERTED);

        Lead converted = leadService.findByIdAndDealerId(scored.getLeadId(), DEALER_ID).orElseThrow();
        assertEquals(LeadState.CONVERTED, converted.getState());

        // 4. Send notification
        Notification sms = new Notification(
            DEALER_ID, TENANT_ID, SITE_ID, converted.getLeadId(),
            NotificationType.SMS, null, "Congratulations on your new Honda Accord!",
            converted.getPhone().toE164()
        );

        NotificationResult result = notificationRouter.route(sms);
        assertTrue(result.isSuccess());
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private Lead createTestLead(String firstName, String lastName, LeadSource source) {
        return Lead.newLead(
            DEALER_ID, TENANT_ID, SITE_ID,
            firstName, lastName,
            new Email(firstName.toLowerCase() + "@example.com"),
            new PhoneCoordinate("+1", "555" + String.format("%07d", System.nanoTime() % 10000000)),
            source,
            new VehicleInterest("Toyota", "Camry", 2022, null)
        );
    }
}
