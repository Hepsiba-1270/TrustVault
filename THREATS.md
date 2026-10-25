# TrustVault | Security Threat Model

**Project:** TrustVault — Secure Document Sharing with a Real Audit Trail  
**Technology:** Java 21, Spring Boot, Spring Security, MySQL 8  
**Document Type:** Threat Modeling and Security Assessment  
**Assessment Status:** Initial assessment; implementation verification and security testing required

---

## 1. Executive Summary

TrustVault is a secure document-sharing application designed to support controlled access to private documents and improve accountability through audit logging.

Because documents may contain confidential information, the application's security depends on more than authentication alone. Document ownership, sharing permissions, administrative privileges, password recovery, file handling, and audit-record integrity must also be protected.

This threat model identifies key risks across these areas, describes the security controls that should address them, and establishes a practical testing plan for evaluating the application's behavior.

**Security objective:** Ensure that only authorized users can access documents, sensitive account operations are protected, and document-related activity can be reviewed reliably.

## 2. Scope and Assets

The assessment focuses on the application's web interface, backend endpoints, database, private file storage, and account-management workflows.

### Assets requiring protection

| Asset | Security objective |
|---|---|
| User credentials | Confidentiality and protection against account compromise |
| Uploaded documents | Confidentiality and controlled access |
| Sharing permissions | Integrity and correct enforcement |
| User and document records | Confidentiality and integrity |
| Audit logs | Integrity, traceability, and appropriate access |
| Password-recovery data | Confidentiality and account security |
| Database credentials | Confidentiality and restricted use |
| Administrative functions | Restricted access and accountability |

## 3. Trust Boundaries

TrustVault has several boundaries where security checks are essential:

1. **Browser to application:** Requests and submitted input must be handled safely.
2. **User to document:** Every document operation must enforce ownership or an explicitly granted permission.
3. **Application to database:** Database access must use appropriate query practices and restricted credentials.
4. **Application to file storage:** File paths and storage identifiers must not permit unauthorized access.
5. **Password-recovery workflow:** Reset requests must resist guessing, replay, and account enumeration.
6. **User to administrator functions:** Privileged operations must require appropriate server-side authorization.

All security-sensitive decisions must be enforced on the server rather than relying only on interface restrictions.

## 4. Threat Analysis

### T01 — Broken Access Control

**Threat:** A user manipulates a URL, document identifier, or request to access another user's document.

**Potential impact:** Unauthorized disclosure of confidential files.

**Required controls:**
- Verify document ownership or explicit sharing permissions on every relevant request.
- Enforce authorization for both viewing and downloading.
- Deny access when authorization cannot be established.
- Test direct requests that bypass the normal user interface.

**Verification:** Attempt to access another user's document while authenticated as a different user, both with and without a valid sharing permission.

**Priority:** High

### T02 — Privilege Escalation

**Threat:** An ordinary user attempts to invoke administrator-only functionality.

**Potential impact:** Unauthorized management of users, documents, or application data.

**Required controls:**
- Enforce server-side role checks on administrative routes and operations.
- Validate authorization in service methods where appropriate.
- Test access using anonymous, ordinary-user, and administrator accounts.

**Verification:** Send direct requests to restricted endpoints using an ordinary user session.

**Priority:** High

### T03 — Account Takeover Through Password Recovery

**Threat:** An attacker guesses recovery answers, repeatedly submits reset requests, or reuses a password-reset token.

**Potential impact:** Unauthorized account access and loss of control over private documents.

**Required controls:**
- Apply rate limiting and abuse detection to recovery endpoints.
- Use unpredictable, short-lived, single-use reset tokens.
- Store reset tokens securely and invalidate them after successful use.
- Avoid responses that unnecessarily reveal whether an account exists.
- Validate recovery workflows against brute-force and replay attempts.

**Verification:** Test invalid, expired, reused, and malformed recovery tokens; assess rate limiting and account-enumeration behavior.

**Priority:** High

### T04 — Credential and Secret Exposure

**Threat:** Passwords, database credentials, or recovery tokens are exposed through source code, repository history, logs, or error messages.

**Potential impact:** Account compromise, unauthorized database access, or wider system compromise.

**Required controls:**
- Use password hashing rather than plaintext password storage.
- Keep secrets outside tracked source files.
- Use environment variables or a secrets manager for deployment credentials.
- Avoid logging passwords, recovery tokens, or sensitive request data.
- Review repository history if a secret may have been committed.

**Verification:** Review tracked files and relevant repository history for exposed credentials.

**Priority:** High

### T05 — Unsafe File Uploads

**Threat:** An attacker submits an oversized file, a disguised file type, or content that violates the application's file-handling assumptions.

**Potential impact:** Resource exhaustion, unsafe content handling, or exposure of other files.

**Required controls:**
- Validate permitted file types and enforce upload-size limits.
- Use server-generated storage names rather than trusting user-supplied paths.
- Keep uploaded files outside publicly served directories.
- Prevent path traversal and unauthorized file retrieval.
- Consider malware scanning based on the deployment's risk profile.

**Verification:** Test oversized uploads, disallowed extensions, misleading content types, malformed files, and unauthorized retrieval attempts.

**Priority:** High

### T06 — Cross-Site Request Forgery (CSRF)

**Threat:** A malicious website attempts to trigger state-changing actions through a user's authenticated browser session.

**Potential impact:** Unauthorized changes to account or document-sharing settings.

**Required controls:**
- Retain appropriate Spring Security CSRF protection.
- Include valid CSRF tokens in applicable state-changing requests.
- Review cookie and session settings.
- Avoid disabling CSRF globally without a justified alternative design.

**Verification:** Confirm that protected state-changing requests without valid CSRF tokens are rejected where applicable.

**Priority:** Medium–High

### T07 — Audit-Log Integrity and Completeness

**Threat:** Security-relevant activity is not recorded consistently, or audit records can be altered by unauthorized users.

**Potential impact:** Reduced traceability and difficulty investigating suspicious activity.

**Required controls:**
- Record relevant document access and permission changes.
- Restrict audit-record modification to trusted server-side components.
- Protect audit views using appropriate authorization.
- Avoid including credentials, tokens, or document contents in logs.
- Consider tamper-evident or append-only storage for stronger assurance.

**Verification:** Confirm expected audit events are generated and ordinary users cannot modify or delete audit records through application endpoints.

**Priority:** High

### T08 — SQL Injection

**Threat:** Malicious input influences database queries in an unintended way.

**Potential impact:** Unauthorized access to or modification of database records.

**Required controls:**
- Prefer parameterized queries and safe Spring Data JPA operations.
- Avoid concatenating untrusted input into SQL statements.
- Validate input and restrict database privileges.
- Review custom queries and data-access code.

**Verification:** Review query construction and test relevant input-handling paths using safe security test cases.

**Priority:** High

### T09 — Sensitive Information Disclosure

**Threat:** Error pages, logs, responses, or publicly accessible paths reveal internal configuration or private information.

**Potential impact:** Exposure of credentials, document metadata, or implementation details that assist further attacks.

**Required controls:**
- Use appropriate production error handling.
- Keep private files outside public static resources.
- Avoid exposing stack traces and sensitive internal details to users.
- Use HTTPS for deployed environments.
- Review logs and responses for unnecessary sensitive data.

**Verification:** Inspect error responses, logs, and document URLs for unintended information disclosure.

**Priority:** Medium

## 5. Risk Prioritization

The following is a preliminary assessment based on potential impact, not the result of a completed penetration test.

| Risk area                    | Potential impact                     | Initial priority |
| Unauthorized document access | Confidentiality breach               | High             |
| Password-recovery abuse      | Account takeover                     | High             |
| Privilege escalation         | Unauthorized administration          | High             |
| Secret exposure              | Account or infrastructure compromise | High             |
| Unsafe file handling         | Resource exhaustion or unsafe access | High             |
| Audit-log manipulation       | Loss of accountability               | High             |
| SQL injection                | Database compromise                  | High             |
| CSRF                         | Unauthorized state changes           | Medium–High      |
| Information disclosure       | Exposure of sensitive details        | Medium           |

Actual severity should be reassessed after implementation review and testing.

## 6. Security Verification Checklist

Use this checklist to track evidence rather than assuming a control is effective because it is described in documentation.

- [ ] Anonymous users are denied protected document operations.
- [ ] Users cannot access documents without ownership or valid sharing permission.
- [ ] VIEW and DOWNLOAD permissions are enforced independently.
- [ ] Ordinary users cannot perform administrator-only operations.
- [ ] Password recovery rejects invalid, expired, and reused reset tokens.
- [ ] Recovery endpoints have appropriate abuse protections.
- [ ] Database credentials and other secrets are excluded from tracked files.
- [ ] Upload limits and file-type validation are enforced server-side.
- [ ] Private files cannot be retrieved through unauthorized paths.
- [ ] Applicable CSRF protections are verified.
- [ ] Audit records are protected against unauthorized modification.
- [ ] Error responses do not disclose unnecessary sensitive information.
- [ ] Automated tests cover important authentication and authorization cases.

## 7. Recommended Security Improvements

Before production deployment, prioritize:

1. **Authorization testing:** Verify every document endpoint against ownership and sharing permissions.
2. **Recovery hardening:** Add and test rate limiting, expiry, replay prevention, and appropriate recovery responses.
3. **File-handling review:** Verify content validation, storage isolation, and protection against path traversal.
4. **Audit assurance:** Check event coverage, access restrictions, and record integrity.
5. **Secret management:** Use deployment-specific secrets and least-privilege database credentials.
6. **Automated verification:** Add regression tests for critical security boundaries and run dependency vulnerability checks.
7. **Production hardening:** Configure HTTPS, secure session settings, monitoring, backups, and safe error handling.

## 8. Limitations and Assumptions

This document is an initial threat model. It identifies relevant risks and proposed controls but does not establish that every control has been implemented or independently verified.

Security assurance requires code review, automated tests, configuration review, and appropriate security testing. The risk priorities in this document are preliminary and should change as evidence becomes available.

## 9. Conclusion

TrustVault's security depends on consistent enforcement of document permissions, protection of account-recovery workflows, secure file handling, and reliable audit records.

This threat model provides a structured basis for reviewing those controls, prioritizing security work, and documenting verification results as the project evolves.

**TrustVault — Controlled document access, privacy-focused storage, and accountable activity.**