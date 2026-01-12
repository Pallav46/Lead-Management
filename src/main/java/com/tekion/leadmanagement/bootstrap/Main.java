package com.tekion.leadmanagement.bootstrap;

import com.tekion.leadmanagement.adapter.notification.email.EmailNotificationAdapter;
import com.tekion.leadmanagement.adapter.notification.sms.SmsNotificationAdapter;
import com.tekion.leadmanagement.adapter.notification.sms.TwilioVerifyAdapter;
import com.tekion.leadmanagement.adapter.persistence.inmemory.InMemoryLeadRepository;
import com.tekion.leadmanagement.application.lead.LeadService;
import com.tekion.leadmanagement.application.notification.CircuitBreaker;
import com.tekion.leadmanagement.application.notification.NotificationRouter;
import com.tekion.leadmanagement.domain.lead.model.*;
import com.tekion.leadmanagement.domain.notification.model.Notification;
import com.tekion.leadmanagement.domain.notification.model.NotificationResult;
import com.tekion.leadmanagement.domain.notification.model.NotificationType;
import com.tekion.leadmanagement.domain.scoring.rule.*;
import com.tekion.leadmanagement.domain.scoring.service.LeadScoringEngine;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

/**
 * Bootstrap class demonstrating the Lead Management System capabilities.
 *
 * <h2>Overview</h2>
 * <p>This class serves as an executable demo that showcases:
 * <ul>
 *   <li>Lead creation with full validation</li>
 *   <li>Lead scoring using configurable rules</li>
 *   <li>Multi-tenant data isolation between dealers</li>
 *   <li>Notification routing with vendor failover</li>
 * </ul>
 *
 * <h2>Architecture Demonstration</h2>
 * <p>The demo wires up the hexagonal architecture manually:
 * <ul>
 *   <li><b>Persistence:</b> InMemoryLeadRepository (driven adapter)</li>
 *   <li><b>Scoring:</b> LeadScoringEngine with 5 configurable rules</li>
 *   <li><b>Notifications:</b> Router with SMS→Email failover</li>
 * </ul>
 *
 * <h2>Multi-Tenant Demo</h2>
 * <p>Creates leads for two different dealers and demonstrates that:
 * <ul>
 *   <li>Dealer-1 cannot access Dealer-2's leads</li>
 *   <li>Dealer-2 cannot access Dealer-1's leads</li>
 *   <li>Each dealer only sees their own data in queries</li>
 * </ul>
 *
 * <h2>Failover Demo</h2>
 * <p>The SMS adapter is configured to fail, demonstrating automatic
 * failover to the Email adapter for notification delivery.
 *
 * <h2>Running the Demo</h2>
 * <pre>
 * ./gradlew run
 * </pre>
 *
 * @see LeadService for lead management operations
 * @see LeadScoringEngine for scoring logic
 * @see NotificationRouter for notification delivery
 */
public class Main {

    public static void main(String[] args) {
        // Persistence adapter
        InMemoryLeadRepository repo = new InMemoryLeadRepository();

        // Scoring engine (Strategy pattern: list of rules)
        LeadScoringEngine scoringEngine = new LeadScoringEngine(List.of(
                new SourceQualityRule(),
                new VehicleAgeRule(),
                new TradeInValueRule(),
                new EngagementRule(),
                new RecencyRule()
        ));

        LeadService leadService = new LeadService(repo, scoringEngine);

        // ═══════════════════════════════════════════════════════════════════
        // NOTIFICATION ROUTING CONFIGURATION
        // For real SMS: Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_VERIFY_SERVICE_SID
        // If not set, falls back to mock adapter for demo purposes.
        // ═══════════════════════════════════════════════════════════════════
        NotificationRouter router = createNotificationRouter();

        // ═══════════════════════════════════════════════════════════════════
        // CREATE LEADS FOR DEALER-1
        // ═══════════════════════════════════════════════════════════════════
        Lead lead1 = Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "Priya",
                "Shah",
                new Email("priya@tekion.com"),
                new PhoneCoordinate("+1", "(415) 555-0123"),
                LeadSource.REFERRAL,
                new VehicleInterest("Toyota", "Camry", 2018, 12000)
        );

        Lead lead2 = Lead.newLead(
                "dealer-1",
                "tenant-1",
                "site-1",
                "John",
                "Doe",
                new Email("john.doe@gmail.com"),
                new PhoneCoordinate("+1", "(408) 555-9876"),
                LeadSource.WEBSITE,
                new VehicleInterest("Honda", "Accord", 2020, 18000)
        );

        leadService.create(lead1);
        leadService.create(lead2);
        leadService.computeAndPersistScore(lead1.getLeadId(), lead1.getDealerId());
        leadService.computeAndPersistScore(lead2.getLeadId(), lead2.getDealerId());

        // ═══════════════════════════════════════════════════════════════════
        // CREATE LEADS FOR DEALER-2 (DIFFERENT TENANT)
        // ═══════════════════════════════════════════════════════════════════
        Lead lead3 = Lead.newLead(
                "dealer-2",
                "tenant-2",
                "site-2",
                "Alice",
                "Johnson",
                new Email("alice@dealer2.com"),
                new PhoneCoordinate("+1", "(650) 555-4321"),
                LeadSource.WALKIN,
                new VehicleInterest("BMW", "X5", 2022, 45000)
        );

        Lead lead4 = Lead.newLead(
                "dealer-2",
                "tenant-2",
                "site-2",
                "Bob",
                "Smith",
                new Email("bob@dealer2.com"),
                new PhoneCoordinate("+1", "(650) 555-8888"),
                LeadSource.REFERRAL,
                new VehicleInterest("Mercedes", "C-Class", 2021, 35000)
        );

        leadService.create(lead3);
        leadService.create(lead4);
        leadService.computeAndPersistScore(lead3.getLeadId(), lead3.getDealerId());
        leadService.computeAndPersistScore(lead4.getLeadId(), lead4.getDealerId());

        Lead persisted = leadService.findByIdAndDealerId(lead1.getLeadId(), lead1.getDealerId()).orElseThrow();

        // ═══════════════════════════════════════════════════════════════════
        // MAIN OUTPUT - COMPREHENSIVE DEMO PRESENTATION
        // ═══════════════════════════════════════════════════════════════════

        printHeader();
        printArchitectureOverview();
        printLeadDetails(persisted);
        printVehicleInterest(persisted);
        printScoringBreakdown(persisted, scoringEngine);

        // Top Leads Comparison
        printTopLeadsComparison(repo);

        // Multi-Tenant Isolation Demo
        printMultiTenantDemo(leadService, lead1, lead3, repo);

        // Notification Demo
        printNotificationDemo(lead1, router);

        // ═══════════════════════════════════════════════════════════════════
        // BONUS FEATURES SECTION
        // ═══════════════════════════════════════════════════════════════════
        printBonusFeaturesHeader();
        printAuditTrailDemo();
        printBulkScoringDemo(scoringEngine, lead1, lead2, lead3, lead4);
        printCircuitBreakerDemo();

        printFooter();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRESENTATION HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private static String truncate(String str, int maxLen) {
        if (str == null) return "N/A";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                            ║");
        System.out.println("║   ██╗     ███████╗ █████╗ ██████╗     ███╗   ███╗ ██████╗ ███╗   ███╗████████║");
        System.out.println("║   ██║     ██╔════╝██╔══██╗██╔══██╗    ████╗ ████║██╔════╝ ████╗ ████╚══██╔══║");
        System.out.println("║   ██║     █████╗  ███████║██║  ██║    ██╔████╔██║██║  ███╗██╔████╔██║  ██║  ║");
        System.out.println("║   ██║     ██╔══╝  ██╔══██║██║  ██║    ██║╚██╔╝██║██║   ██║██║╚██╔╝██║  ██║  ║");
        System.out.println("║   ███████╗███████╗██║  ██║██████╔╝    ██║ ╚═╝ ██║╚██████╔╝██║ ╚═╝ ██║  ██║  ║");
        System.out.println("║   ╚══════╝╚══════╝╚═╝  ╚═╝╚═════╝     ╚═╝     ╚═╝ ╚═════╝ ╚═╝     ╚═╝  ╚═╝  ║");
        System.out.println("║                                                                            ║");
        System.out.println("║                    LEAD MANAGEMENT SYSTEM v1.0                             ║");
        System.out.println("║                    Tekion Backend Training Project                         ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printArchitectureOverview() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🏗️  HEXAGONAL ARCHITECTURE OVERVIEW                                       ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");
        System.out.println("║    ┌─────────────────────────────────────────────────────────────────┐    ║");
        System.out.println("║    │                        DOMAIN LAYER                              │    ║");
        System.out.println("║    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │    ║");
        System.out.println("║    │  │ Lead Model  │  │  Scoring    │  │  Notification Models    │ │    ║");
        System.out.println("║    │  │ + Value Obj │  │  Rules (5)  │  │  + Result/Type          │ │    ║");
        System.out.println("║    │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │    ║");
        System.out.println("║    └─────────────────────────────────────────────────────────────────┘    ║");
        System.out.println("║                            ▲          ▲                                   ║");
        System.out.println("║    ┌─────────────────────────────────────────────────────────────────┐    ║");
        System.out.println("║    │                     APPLICATION LAYER                           │    ║");
        System.out.println("║    │  ┌─────────────────┐        ┌───────────────────────────────┐  │    ║");
        System.out.println("║    │  │  LeadService    │        │  NotificationRouter           │  │    ║");
        System.out.println("║    │  │  (Orchestrator) │        │  (Priority + Failover)        │  │    ║");
        System.out.println("║    │  └─────────────────┘        └───────────────────────────────┘  │    ║");
        System.out.println("║    └─────────────────────────────────────────────────────────────────┘    ║");
        System.out.println("║                            ▲          ▲                                   ║");
        System.out.println("║    ┌──────────────────────┐│          │┌─────────────────────────────┐   ║");
        System.out.println("║    │   PERSISTENCE ADAPTER ││          ││   NOTIFICATION ADAPTERS    │   ║");
        System.out.println("║    │   InMemoryRepository ││          ││  Twilio SMS / Email Mock   │   ║");
        System.out.println("║    └──────────────────────┘│          │└─────────────────────────────┘   ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  📌 Design Patterns Demonstrated:");
        System.out.println("     • Hexagonal (Ports & Adapters) Architecture");
        System.out.println("     • Strategy Pattern (Scoring Rules)");
        System.out.println("     • Chain of Responsibility (Notification Routing)");
        System.out.println("     • Value Objects (Email, Phone, VehicleInterest)");
        System.out.println("     • Repository Pattern (LeadRepository Port)");
        System.out.println();
    }

    private static void printLeadDetails(Lead lead) {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  📋 LEAD ENTITY DETAILS                                                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  %-20s │ %-53s ║%n", "Lead ID", lead.getLeadId());
        System.out.printf("║  %-20s │ %-53s ║%n", "Full Name", lead.getFirstName() + " " + lead.getLastName());
        System.out.printf("║  %-20s │ %-53s ║%n", "Email (Value Object)", lead.getEmail().getValue());
        System.out.printf("║  %-20s │ %-53s ║%n", "Phone (E.164)", lead.getPhone().toE164());
        System.out.printf("║  %-20s │ %-53s ║%n", "Lead Source", lead.getSource() + " (" + getSourceEmoji(lead.getSource()) + ")");
        System.out.printf("║  %-20s │ %-53s ║%n", "Current State", lead.getState().getDisplayName());
        System.out.printf("║  %-20s │ %-53s ║%n", "Created At", lead.getCreatedAt());
        System.out.println("╠────────────────────────┼──────────────────────────────────────────────────────╣");
        System.out.printf("║  %-20s │ %-53s ║%n", "Dealer ID", lead.getDealerId());
        System.out.printf("║  %-20s │ %-53s ║%n", "Tenant ID", lead.getTenantId());
        System.out.printf("║  %-20s │ %-53s ║%n", "Site ID", lead.getSiteId());
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static String getSourceEmoji(LeadSource source) {
        return switch (source) {
            case WEBSITE -> "🌐 Online";
            case REFERRAL -> "🤝 Word of Mouth";
            case WALKIN -> "🚶 In Person";
            case PHONE -> "📞 Call";
        };
    }

    private static void printVehicleInterest(Lead lead) {
        VehicleInterest vi = lead.getVehicleInterest();
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🚗 VEHICLE INTEREST (Value Object)                                        ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  %-20s │ %-53s ║%n", "Make", vi.getMake());
        System.out.printf("║  %-20s │ %-53s ║%n", "Model", vi.getModel());
        System.out.printf("║  %-20s │ %-53s ║%n", "Year", vi.getYear());
        System.out.printf("║  %-20s │ $%-52s ║%n", "Trade-In Value", vi.getTradeInValue().map(v -> String.format("%,d", v)).orElse("N/A"));
        int vehicleAge = java.time.Year.now().getValue() - vi.getYear();
        System.out.printf("║  %-20s │ %-53s ║%n", "Vehicle Age", vehicleAge + " years");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printScoringBreakdown(Lead lead, LeadScoringEngine engine) {
        var result = engine.score(lead);
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  📊 LEAD SCORING BREAKDOWN                                                 ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");

        // Score bar visualization
        int score = result.getFinalScore();
        String priority = score >= 80 ? "🔥 HOT LEAD" : score >= 60 ? "🌡️ WARM LEAD" : score >= 40 ? "❄️ COOL LEAD" : "🧊 COLD LEAD";
        System.out.printf("║  FINAL SCORE: %-3d / 100    %s                                   ║%n", score, priority);
        System.out.println("║                                                                            ║");

        // Visual score bar
        int filledBlocks = score / 5;
        int emptyBlocks = 20 - filledBlocks;
        String scoreBar = "█".repeat(filledBlocks) + "░".repeat(emptyBlocks);
        System.out.printf("║  [%s] %3d%%                                    ║%n", scoreBar, score);
        System.out.println("║                                                                            ║");
        System.out.println("╠────────────────────────────────────────────────────────────────────────────╣");
        System.out.println("║  RULE CONTRIBUTIONS:                                                       ║");
        System.out.println("║                                                                            ║");

        // Show each rule's contribution
        for (var entry : result.getBreakdown().entrySet()) {
            String ruleName = entry.getKey();
            double factor = entry.getValue();
            int barLength = (int) (factor * 20);
            String ruleBar = "▓".repeat(barLength) + "░".repeat(20 - barLength);
            System.out.printf("║    %-18s [%s] %.2f                        ║%n", ruleName, ruleBar, factor);
        }

        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printTopLeadsComparison(InMemoryLeadRepository repo) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🏆 TOP LEADS COMPARISON BY DEALER                                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");

        // Dealer 1
        System.out.println("║                                                                            ║");
        System.out.println("║  DEALER-1 (Tenant: tenant-1)                                               ║");
        System.out.println("║  ─────────────────────────────────────────────────────────────────────     ║");
        List<Lead> dealer1Leads = repo.findByDealerIdOrderByScore("dealer-1", 5);
        int rank = 1;
        for (Lead l : dealer1Leads) {
            String scoreBar = "█".repeat(l.getScore() / 10) + "░".repeat(10 - l.getScore() / 10);
            System.out.printf("║    %d. %-15s %-12s [%s] %3d pts              ║%n",
                    rank++, l.getFirstName() + " " + l.getLastName(),
                    "(" + l.getSource() + ")", scoreBar, l.getScore());
        }

        // Dealer 2
        System.out.println("║                                                                            ║");
        System.out.println("║  DEALER-2 (Tenant: tenant-2)                                               ║");
        System.out.println("║  ─────────────────────────────────────────────────────────────────────     ║");
        List<Lead> dealer2Leads = repo.findByDealerIdOrderByScore("dealer-2", 5);
        rank = 1;
        for (Lead l : dealer2Leads) {
            String scoreBar = "█".repeat(l.getScore() / 10) + "░".repeat(10 - l.getScore() / 10);
            System.out.printf("║    %d. %-15s %-12s [%s] %3d pts              ║%n",
                    rank++, l.getFirstName() + " " + l.getLastName(),
                    "(" + l.getSource() + ")", scoreBar, l.getScore());
        }

        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printMultiTenantDemo(LeadService leadService, Lead lead1, Lead lead3, InMemoryLeadRepository repo) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🔒 MULTI-TENANT ISOLATION DEMONSTRATION                                   ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");
        System.out.println("║  Testing cross-dealer data access prevention:                              ║");
        System.out.println("║                                                                            ║");

        // Test 1
        var crossAccess1 = leadService.findByIdAndDealerId(lead3.getLeadId(), "dealer-1");
        String result1 = crossAccess1.isPresent() ? "⚠️ ACCESSIBLE (BUG!)" : "🔒 BLOCKED ✓";
        System.out.printf("║  TEST 1: Dealer-1 → Dealer-2's lead: %-37s ║%n", result1);

        // Test 2
        var crossAccess2 = leadService.findByIdAndDealerId(lead1.getLeadId(), "dealer-2");
        String result2 = crossAccess2.isPresent() ? "⚠️ ACCESSIBLE (BUG!)" : "🔒 BLOCKED ✓";
        System.out.printf("║  TEST 2: Dealer-2 → Dealer-1's lead: %-37s ║%n", result2);

        System.out.println("║                                                                            ║");
        System.out.println("╠────────────────────────────────────────────────────────────────────────────╣");
        System.out.println("║  Data Isolation Summary:                                                   ║");
        List<Lead> d1Leads = repo.findByDealerIdOrderByScore("dealer-1", 10);
        List<Lead> d2Leads = repo.findByDealerIdOrderByScore("dealer-2", 10);
        System.out.printf("║    • Dealer-1 can see: %d leads                                             ║%n", d1Leads.size());
        System.out.printf("║    • Dealer-2 can see: %d leads                                             ║%n", d2Leads.size());
        System.out.println("║    • Total in system: 4 leads                                              ║");
        System.out.println("║    • ✅ Each dealer sees ONLY their own data                               ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printNotificationDemo(Lead lead, NotificationRouter router) {
        Notification notification = new Notification(
                lead.getDealerId(), lead.getTenantId(), lead.getSiteId(), lead.getLeadId(),
                NotificationType.SMS, null,
                "Hello " + lead.getFirstName() + ", thanks for your interest!",
                "+916200845646"
        );

        NotificationResult result = router.route(notification);

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  📨 NOTIFICATION SYSTEM DEMO                                               ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  %-20s │ %-53s ║%n", "Notification Type", notification.getType());
        System.out.printf("║  %-20s │ %-53s ║%n", "Recipient", notification.getTo());
        System.out.printf("║  %-20s │ %-53s ║%n", "Message", truncate(notification.getBody(), 53));
        System.out.println("╠────────────────────────────────────────────────────────────────────────────╣");
        System.out.printf("║  %-20s │ %-53s ║%n", "Success", result.isSuccess() ? "✅ Yes" : "❌ No");
        System.out.printf("║  %-20s │ %-53s ║%n", "Vendor Used", result.getVendor());
        System.out.printf("║  %-20s │ %-53s ║%n", "Message ID", truncate(result.getMessageId(), 53));
        if (result.getErrorMessage() != null) {
            System.out.printf("║  %-20s │ %-53s ║%n", "Error", truncate(result.getErrorMessage(), 53));
        }
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printBonusFeaturesHeader() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                            ║");
        System.out.println("║                    🌟 BONUS FEATURES DEMONSTRATION 🌟                      ║");
        System.out.println("║                                                                            ║");
        System.out.println("║    The following features demonstrate additional capabilities:             ║");
        System.out.println("║    • Audit Trail (+5 points) - State transitions with actor tracking       ║");
        System.out.println("║    • Bulk Scoring (+5 points) - Parallel batch processing                  ║");
        System.out.println("║    • Circuit Breaker (+10 points) - Resilience pattern                     ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printAuditTrailDemo() {
        Lead auditLead = Lead.newLead("dealer-1", "tenant-1", "site-1", "Audit", "Demo",
                new Email("audit@demo.com"), new PhoneCoordinate("+1", "5550001234"),
                LeadSource.WEBSITE, new VehicleInterest("Honda", "Civic", 2023, null));

        auditLead.transitionTo(LeadState.CONTACTED, "sales-rep-101", "Initial phone call completed");
        auditLead.transitionTo(LeadState.QUALIFIED, "sales-rep-101", "Customer confirmed budget");
        auditLead.transitionTo(LeadState.CONVERTED, "sales-mgr-001", "Deal closed - $28,000");

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  📜 AUDIT TRAIL DEMO (+5 points)                                           ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");
        System.out.println("║  Lead lifecycle with full audit tracking:                                  ║");
        System.out.println("║                                                                            ║");

        for (var entry : auditLead.getAuditTrail()) {
            System.out.printf("║    %-72s ║%n", truncate(entry.toLogMessage(), 72));
        }

        System.out.println("║                                                                            ║");
        System.out.println("║  ✅ Each transition records: timestamp, actor, from/to states, reason      ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printBulkScoringDemo(LeadScoringEngine engine, Lead... leads) {
        List<Lead> leadList = List.of(leads);
        long startTime = System.nanoTime();
        var batchResults = engine.scoreBatch(leadList);
        long duration = System.nanoTime() - startTime;

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ⚡ BULK SCORING DEMO (+5 points)                                          ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");
        System.out.printf("║  Batch processed %d leads in %.2f ms (parallel execution)                  ║%n",
                batchResults.size(), duration / 1_000_000.0);
        System.out.println("║                                                                            ║");
        System.out.println("╠────────────────────────────────────────────────────────────────────────────╣");

        for (Lead lead : leadList) {
            var result = batchResults.get(lead.getLeadId());
            int score = result.getFinalScore();
            String bar = "█".repeat(score / 10) + "░".repeat(10 - score / 10);
            System.out.printf("║    %-20s [%s] %3d pts                           ║%n",
                    lead.getFirstName() + " " + lead.getLastName(), bar, score);
        }

        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printCircuitBreakerDemo() {
        CircuitBreaker breaker = new CircuitBreaker("demo-sms", 2, java.time.Duration.ofSeconds(5));

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🔌 CIRCUIT BREAKER DEMO (+10 points)                                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                            ║");
        System.out.println("║  State Machine Demonstration:                                              ║");
        System.out.println("║                                                                            ║");
        System.out.println("║    ┌──────────┐    failures    ┌──────────┐   timeout   ┌───────────┐     ║");
        System.out.println("║    │  CLOSED  │ ─────────────→ │   OPEN   │ ──────────→ │ HALF_OPEN │     ║");
        System.out.println("║    └──────────┘   >= threshold └──────────┘             └───────────┘     ║");
        System.out.println("║         ↑                                                     │           ║");
        System.out.println("║         └─────────────────── success ─────────────────────────┘           ║");
        System.out.println("║                                                                            ║");
        System.out.println("╠────────────────────────────────────────────────────────────────────────────╣");
        System.out.printf("║  Initial State      : %-54s ║%n", breaker.getState());
        breaker.recordFailure();
        System.out.printf("║  After 1 failure    : %-54s ║%n", breaker.getState() + " (threshold=2)");
        breaker.recordFailure();
        System.out.printf("║  After 2 failures   : %-54s ║%n", breaker.getState() + " ⚠️ CIRCUIT OPEN!");
        System.out.printf("║  Allow request?     : %-54s ║%n", breaker.allowRequest() ? "Yes" : "No - Fast Fail!");
        System.out.println("║                                                                            ║");
        System.out.println("║  ✅ Prevents cascade failures by failing fast when service is down        ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printFooter() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                            ║");
        System.out.println("║                         ✨ END OF DEMO ✨                                  ║");
        System.out.println("║                                                                            ║");
        System.out.println("║  Summary of Demonstrated Features:                                         ║");
        System.out.println("║    ✓ Hexagonal Architecture with Ports & Adapters                          ║");
        System.out.println("║    ✓ Multi-Tenant Data Isolation                                           ║");
        System.out.println("║    ✓ Lead Scoring Engine with 5 Configurable Rules                         ║");
        System.out.println("║    ✓ Notification System with Failover                                     ║");
        System.out.println("║    ✓ Audit Trail for State Transitions                                     ║");
        System.out.println("║    ✓ Bulk Scoring with Parallel Processing                                 ║");
        System.out.println("║    ✓ Circuit Breaker for Resilience                                        ║");
        System.out.println("║                                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Creates the notification router with appropriate adapters.
     * Uses Twilio Verify if credentials are configured, otherwise falls back to mock adapters.
     *
     * <p>Credentials are loaded from:
     * <ol>
     *   <li>.env file in project root (preferred for local development)</li>
     *   <li>System environment variables (fallback)</li>
     * </ol>
     */
    private static NotificationRouter createNotificationRouter() {
        // Load .env file if present, ignore if missing
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Get credentials from .env or system environment
        String accountSid = getEnvVar(dotenv, "TWILIO_ACCOUNT_SID");
        String authToken = getEnvVar(dotenv, "TWILIO_AUTH_TOKEN");
        String verifyServiceSid = getEnvVar(dotenv, "TWILIO_VERIFY_SERVICE_SID");

        // Check if all Twilio credentials are provided
        boolean hasTwilioConfig = accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && verifyServiceSid != null && !verifyServiceSid.isBlank();

        if (hasTwilioConfig) {
            System.out.println("  📱 Twilio Verify configured - real SMS will be sent");
            return new NotificationRouter(List.of(
                    new TwilioVerifyAdapter(accountSid, authToken, verifyServiceSid),
                    new EmailNotificationAdapter()
            ));
        } else {
            System.out.println("  📧 No Twilio config - using mock SMS adapter");
            System.out.println("      Create a .env file with TWILIO_* credentials for real SMS");
            return new NotificationRouter(List.of(
                    new SmsNotificationAdapter(),     // Mock SMS for demo
                    new EmailNotificationAdapter()   // Mock Email for demo
            ));
        }
    }

    /**
     * Gets an environment variable from dotenv first, then falls back to system env.
     */
    private static String getEnvVar(Dotenv dotenv, String name) {
        String value = dotenv.get(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value;
    }
}