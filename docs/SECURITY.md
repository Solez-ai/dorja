# DORJA Security Model

## Threat Model

The local backend protects against:
- Accidental data leakage
- Database backups being copied
- Ordinary developers browsing raw records
- Public API overexposure

**Limitation:** The system cannot protect against a fully compromised host operating system or a malicious privileged administrator.

## Data Protection

### Envelope Encryption
- AES-256-GCM envelope encryption for sensitive fields
- Each field uses a unique Data Encryption Key (DEK)
- Root Key (KEK) encrypts each DEK
- Additional Authenticated Data (AAD) binds ciphertext to field identity: `dorja:v1:{subjectType}:{subjectId}:{fieldName}`

### Encrypted Fields
| Entity | Fields | Reason |
|--------|--------|--------|
| Listing | exact address, lat, lng | Public discovery must not reveal entry-level location |
| IdentityVerification | provider reference, name, result | Identity provider result is sensitive |
| Message | full text | Limited safe preview without public plaintext |
| SafetyReport | description, evidence, resolution | Prevent retaliation and data exposure |

### Unencrypted Fields
- Numeric public price
- Listing category
- Approximate area
- Verification status
- Timestamps
- Workflow states

## Authentication
- OTP-based phone verification (console transport in dev)
- JWT access tokens (1h expiry)
- Refresh token rotation
- Argon2id for passwords if added later
- HMAC-hashed OTP storage (never plain)
- SHA-256 hashed viewing-pass tokens

## Audit Trail
- Append-only audit log with HMAC hash chain
- Tamper detection via SHA-256 chain
- Separate HMAC key from encryption root key
- Non-sensitive metadata only in audit payloads

## Data Minimisation
- Raw NID values never stored in User model
- Identity document images never exposed to counterparties
- Exact address never in listing search API responses
- Raw viewing-pass values never logged
- Sensitive values excluded from WebSocket events
- Safety reports hidden from reported user during investigation

## Access Control
- All permission checks server-side
- Single least-privilege database role (`dorja_app`)
- Separate migration role (`dorja_migrator`)
- Rate limiting on all sensitive endpoints
- No raw documents in public API responses

## Retention Schedule
| Data | Default Retention | Action |
|------|-------------------|--------|
| OTP | 10 minutes | Delete |
| Rejected identity evidence | 30 days | Delete |
| Accepted identity evidence | 180 days after expiry | Delete |
| Exact address blob | Active + 90 days | Purge |
| Safety report | 365 days after closure | Purge payload |
| Audit log | 24 months minimum | Retain |
