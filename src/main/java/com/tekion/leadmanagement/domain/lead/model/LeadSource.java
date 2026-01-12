package com.tekion.leadmanagement.domain.lead.model;

/**
 * Represents the acquisition channel through which a lead was obtained.
 *
 * <p>Lead source is a key factor in lead scoring as different channels
 * have different conversion rates:
 *
 * <h2>Source Quality Ranking (for scoring)</h2>
 * <table border="1">
 *   <tr><th>Source</th><th>Score Factor</th><th>Rationale</th></tr>
 *   <tr><td>REFERRAL</td><td>1.0 (highest)</td><td>Personal recommendations have highest trust</td></tr>
 *   <tr><td>WEBSITE</td><td>0.7</td><td>Active research indicates intent</td></tr>
 *   <tr><td>PHONE</td><td>0.5</td><td>Inbound calls show moderate interest</td></tr>
 *   <tr><td>WALKIN</td><td>0.3 (lowest)</td><td>Physical visit but may be casual browsing</td></tr>
 * </table>
 *
 * @see com.tekion.leadmanagement.domain.scoring.rule.SourceQualityRule
 */
public enum LeadSource {

    /** Lead came through the dealership website (form submission, chat, etc.). */
    WEBSITE,

    /** Lead came via phone call (inbound or outbound). */
    PHONE,

    /** Customer walked into the dealership in person. */
    WALKIN,

    /** Lead was referred by an existing customer or partner. */
    REFERRAL
}