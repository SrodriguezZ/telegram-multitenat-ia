# Telegram Backend Technical Design - Clean Architecture

This document defines the technical design for implementing the Telegram invitation and client-linking backend using Java, PostgreSQL, and Clean Architecture.

It complements the functional data model document:

```text
docs/telegram-backend-data-model.md
```

The functional document explains the business model and relational database. This technical document explains how to implement the backend without mixing business rules with frameworks, controllers, database code, or Telegram-specific infrastructure.

---

## 1. Final MVP Decisions

These decisions are mandatory for the first implementation.

- The system is multi-tenant.
- `companies` is the tenant boundary.
- `clients` belongs to exactly one company through `company_id`.
- If the same person exists in two companies, create two different `clients` records.
- A client can have only one active Telegram account per company.
- A Telegram user can be linked once per company.
- Invitation tokens are opaque, temporary, single-use, and expire after 48 hours.
- Store only `token_hash`, never the raw token.
- Store `token_prefix` for support/debugging.
- The bot must ask confirmation before creating the permanent Telegram link.
- Failed validations must be audited.
- Confirming the Telegram link must be transactional.

---

## 2. Architecture Style

Use Clean Architecture / Hexagonal Architecture.

The dependency rule is strict:

```text
Infrastructure -> Application -> Domain
```

Domain must not depend on Spring, JPA, HTTP, Telegram SDKs, PostgreSQL, or framework annotations.

### 2.1 Layers

```text
src/main/java/com/fuqi/telegramlink/
  domain/
  application/
  infrastructure/
```

### 2.2 Domain Layer

Contains pure business concepts and rules.

Recommended packages:

```text
com.fuqi.telegramlink.domain.model
com.fuqi.telegramlink.domain.valueobject
com.fuqi.telegramlink.domain.exception
com.fuqi.telegramlink.domain.policy
```

Domain examples:

- `Company`
- `CompanyUser`
- `Client`
- `TelegramInvitationToken`
- `ClientTelegramAccount`
- `TelegramLinkEvent`
- `InvitationStatus`
- `ClientStatus`
- `CompanyStatus`
- `TelegramAccountStatus`
- `TokenHash`
- `TelegramUserId`
- `CompanyId`
- `ClientId`

### 2.3 Application Layer

Contains use cases and ports.

Recommended packages:

```text
com.fuqi.telegramlink.application.usecase
com.fuqi.telegramlink.application.port.in
com.fuqi.telegramlink.application.port.out
com.fuqi.telegramlink.application.dto
com.fuqi.telegramlink.application.error
```

Application examples:

- `CreateTelegramInvitationUseCase`
- `ValidateTelegramLinkTokenUseCase`
- `ConfirmTelegramLinkUseCase`
- `RevokeTelegramInvitationUseCase`
- `GetTelegramLinkStatusUseCase`

### 2.4 Infrastructure Layer

Contains adapters for HTTP, database, Telegram, security, hashing, and time.

Recommended packages:

```text
com.fuqi.telegramlink.infrastructure.web
com.fuqi.telegramlink.infrastructure.persistence.jpa
com.fuqi.telegramlink.infrastructure.persistence.mapper
com.fuqi.telegramlink.infrastructure.security
com.fuqi.telegramlink.infrastructure.clock
com.fuqi.telegramlink.infrastructure.telegram
```

Infrastructure examples:

- REST controllers
- JPA entities
- Spring Data repositories
- database mappers
- transaction configuration
- token generator implementation
- token hashing implementation
- authenticated user provider

---

## 3. Use Cases

## 3.1 CreateTelegramInvitation

Creates a short Telegram invitation link for a client.

### Input

```json
{
  "companyId": "uuid",
  "clientId": "uuid",
  "expiresInHours": 48
}
```

`expiresInHours` is optional. If missing, use `48`.

### Output

```json
{
  "invitationId": "uuid",
  "clientId": "uuid",
  "companyId": "uuid",
  "telegramLink": "https://t.me/my_bot?start=K7P9Q2X8",
  "tokenPrefix": "K7P9",
  "status": "PENDING",
  "expiresAt": "2026-06-17T10:00:00Z",
  "createdAt": "2026-06-15T10:00:00Z"
}
```

### Business rules

- Authenticated user must belong to the company.
- Company must be `ACTIVE`.
- Client must belong to the same company.
- Client must be `ACTIVE`.
- User role must be allowed to invite:
  - `OWNER`: allowed
  - `ADMIN`: allowed
  - `SUPERVISOR`: allowed
  - `AGENT`: allowed only for assigned clients
- Suspended users cannot generate invitations.
- If client already has an active Telegram account, reject with conflict.
- If there is an existing pending non-expired invitation, either:
  - return the existing active invitation status, or
  - revoke the old one and create a new one.

### MVP decision

Reject creation if there is already a pending non-expired invitation for the same client.

---

## 3.2 ValidateTelegramLinkToken

Validates a Telegram `/start <token>` before linking.

This use case does not create the final link. It only validates the token and returns a preview for confirmation.

### Input

```json
{
  "token": "K7P9Q2X8",
  "telegramUserId": 123456789,
  "telegramChatId": 123456789,
  "telegramUsername": "client_user",
  "telegramFirstName": "Juan",
  "telegramLastName": "Perez",
  "telegramLanguageCode": "es"
}
```

### Output when valid

```json
{
  "status": "VALID",
  "invitationId": "uuid",
  "companyId": "uuid",
  "clientId": "uuid",
  "clientDisplayName": "Juan Perez",
  "companyDisplayName": "Empresa Demo",
  "confirmationRequired": true,
  "message": "Invitation is valid. Confirmation is required before linking."
}
```

### Output when invalid

```json
{
  "status": "INVALID",
  "errorCode": "TOKEN_EXPIRED",
  "message": "This invitation link is invalid or expired."
}
```

### Business rules

- Hash the raw token and search by `token_hash`.
- Token must exist.
- Token must be `PENDING`.
- Token must not be expired.
- Company must be `ACTIVE`.
- Client must be `ACTIVE`.
- Telegram user must not already be active in the same company.
- Validation failures must create `telegram_link_events` records.

---

## 3.3 ConfirmTelegramLink

Creates the permanent `client_telegram_accounts` row after the user confirms linking in the bot.

### Input

```json
{
  "invitationId": "uuid",
  "token": "K7P9Q2X8",
  "telegramUserId": 123456789,
  "telegramChatId": 123456789,
  "telegramUsername": "client_user",
  "telegramFirstName": "Juan",
  "telegramLastName": "Perez",
  "telegramLanguageCode": "es"
}
```

### Output

```json
{
  "status": "LINKED",
  "clientTelegramAccountId": "uuid",
  "companyId": "uuid",
  "clientId": "uuid",
  "telegramUserId": 123456789,
  "linkedAt": "2026-06-15T10:05:00Z",
  "message": "Telegram account linked successfully."
}
```

### Business rules

- Token must be revalidated inside the same transaction.
- Token must still be `PENDING`.
- Token must not be expired.
- Company must be `ACTIVE`.
- Client must be `ACTIVE`.
- Telegram user must not already be active in the same company.
- Client must not already have an active Telegram account in the same company.
- Create `client_telegram_accounts`.
- Mark token as `USED`.
- Set `used_at` and `used_by_telegram_user_id`.
- Create audit event `CLIENT_TELEGRAM_LINKED`.

### Transaction requirement

This use case must be atomic:

```text
create client_telegram_accounts
mark invitation USED
write audit event
```

If any step fails, all changes must rollback.

---

## 3.4 RevokeTelegramInvitation

Revokes a pending invitation before it is used.

### Input

```json
{
  "companyId": "uuid",
  "invitationId": "uuid",
  "reason": "Sent to the wrong client"
}
```

### Output

```json
{
  "status": "REVOKED",
  "invitationId": "uuid",
  "revokedAt": "2026-06-15T10:10:00Z"
}
```

### Business rules

- Authenticated user must belong to the company.
- User must have permission to revoke invitations.
- Invitation must belong to the company.
- Only `PENDING` invitations can be revoked.
- Revocation must write audit event `INVITATION_REVOKED`.

---

## 3.5 GetTelegramLinkStatus

Returns the current Telegram linking status for a client.

### Input

```text
companyId
clientId
```

### Output example: no link

```json
{
  "clientId": "uuid",
  "companyId": "uuid",
  "linkStatus": "NOT_LINKED",
  "activeTelegramAccount": null,
  "pendingInvitation": null
}
```

### Output example: pending invitation

```json
{
  "clientId": "uuid",
  "companyId": "uuid",
  "linkStatus": "INVITATION_PENDING",
  "activeTelegramAccount": null,
  "pendingInvitation": {
    "invitationId": "uuid",
    "tokenPrefix": "K7P9",
    "expiresAt": "2026-06-17T10:00:00Z",
    "createdAt": "2026-06-15T10:00:00Z"
  }
}
```

### Output example: linked

```json
{
  "clientId": "uuid",
  "companyId": "uuid",
  "linkStatus": "LINKED",
  "activeTelegramAccount": {
    "id": "uuid",
    "telegramUserId": 123456789,
    "telegramUsername": "client_user",
    "linkedAt": "2026-06-15T10:05:00Z",
    "status": "ACTIVE"
  },
  "pendingInvitation": null
}
```

---

## 4. HTTP API Design

Base path:

```text
/api/v1
```

## 4.1 Create invitation

```http
POST /api/v1/companies/{companyId}/clients/{clientId}/telegram-invitations
```

### Request

```json
{
  "expiresInHours": 48
}
```

### Success

```text
201 Created
```

### Error status codes

- `400 Bad Request`: invalid request body.
- `401 Unauthorized`: missing/invalid authentication.
- `403 Forbidden`: user does not belong to company or lacks permission.
- `404 Not Found`: company or client not found.
- `409 Conflict`: client already linked or already has pending invitation.

---

## 4.2 Validate Telegram token

```http
POST /api/v1/telegram/link/validate
```

### Success

```text
200 OK
```

### Error status codes

- `400 Bad Request`: invalid request body.
- `404 Not Found`: token not found.
- `409 Conflict`: token expired, used, revoked, or Telegram already linked.

Note: For public bot responses, avoid exposing too much detail. The internal error code can be specific, but the user-facing message should be generic.

---

## 4.3 Confirm Telegram link

```http
POST /api/v1/telegram/link/confirm
```

### Success

```text
201 Created
```

### Error status codes

- `400 Bad Request`: invalid request body.
- `404 Not Found`: invitation not found.
- `409 Conflict`: token expired, used, revoked, client already linked, or Telegram already linked.

---

## 4.4 Revoke invitation

```http
POST /api/v1/companies/{companyId}/telegram-invitations/{invitationId}/revoke
```

### Success

```text
200 OK
```

### Error status codes

- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`: invitation cannot be revoked because it is already used, expired, or revoked.

---

## 4.5 Get Telegram link status

```http
GET /api/v1/companies/{companyId}/clients/{clientId}/telegram-link-status
```

### Success

```text
200 OK
```

### Error status codes

- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 5. Standard Error Response

All errors should use a consistent response shape.

```json
{
  "timestamp": "2026-06-15T10:00:00Z",
  "status": 409,
  "errorCode": "TELEGRAM_ALREADY_LINKED",
  "message": "This Telegram account is already linked.",
  "path": "/api/v1/telegram/link/confirm",
  "correlationId": "0f4bdc1a-2a8b-45df-9127-861be327c8d9"
}
```

## 5.1 Error codes

Recommended initial error codes:

```text
COMPANY_NOT_FOUND
COMPANY_NOT_ACTIVE
CLIENT_NOT_FOUND
CLIENT_NOT_ACTIVE
USER_NOT_ALLOWED
USER_SUSPENDED
CLIENT_NOT_ASSIGNED_TO_AGENT
INVITATION_NOT_FOUND
INVITATION_ALREADY_PENDING
TOKEN_NOT_FOUND
TOKEN_EXPIRED
TOKEN_ALREADY_USED
TOKEN_REVOKED
CLIENT_ALREADY_LINKED
TELEGRAM_ALREADY_LINKED
INVALID_REQUEST
UNEXPECTED_ERROR
```

---

## 6. Application Ports

## 6.1 Inbound ports

```java
public interface CreateTelegramInvitationUseCase {
    CreateTelegramInvitationResponse execute(CreateTelegramInvitationCommand command);
}

public interface ValidateTelegramLinkTokenUseCase {
    ValidateTelegramLinkTokenResponse execute(ValidateTelegramLinkTokenCommand command);
}

public interface ConfirmTelegramLinkUseCase {
    ConfirmTelegramLinkResponse execute(ConfirmTelegramLinkCommand command);
}

public interface RevokeTelegramInvitationUseCase {
    RevokeTelegramInvitationResponse execute(RevokeTelegramInvitationCommand command);
}

public interface GetTelegramLinkStatusUseCase {
    GetTelegramLinkStatusResponse execute(GetTelegramLinkStatusQuery query);
}
```

## 6.2 Outbound ports

```java
public interface CompanyRepositoryPort {
    Optional<Company> findById(CompanyId companyId);
}

public interface CompanyUserRepositoryPort {
    Optional<CompanyUser> findById(CompanyUserId userId);
    boolean isClientAssignedToAgent(CompanyUserId userId, ClientId clientId);
}

public interface ClientRepositoryPort {
    Optional<Client> findById(ClientId clientId);
}

public interface TelegramInvitationRepositoryPort {
    Optional<TelegramInvitationToken> findByTokenHash(TokenHash tokenHash);
    Optional<TelegramInvitationToken> findById(InvitationId invitationId);
    boolean existsPendingNonExpiredByClient(ClientId clientId, Instant now);
    TelegramInvitationToken save(TelegramInvitationToken invitation);
}

public interface ClientTelegramAccountRepositoryPort {
    boolean existsActiveByCompanyAndTelegramUserId(CompanyId companyId, TelegramUserId telegramUserId);
    boolean existsActiveByCompanyAndClientId(CompanyId companyId, ClientId clientId);
    Optional<ClientTelegramAccount> findActiveByCompanyAndClientId(CompanyId companyId, ClientId clientId);
    ClientTelegramAccount save(ClientTelegramAccount account);
}

public interface TelegramLinkEventRepositoryPort {
    void save(TelegramLinkEvent event);
}

public interface InvitationTokenGeneratorPort {
    RawInvitationToken generate();
}

public interface TokenHashingPort {
    TokenHash hash(RawInvitationToken rawToken);
}

public interface CurrentUserProviderPort {
    AuthenticatedUser currentUser();
}

public interface ClockPort {
    Instant now();
}
```

---

## 7. Domain Rules

## 7.1 Invitation status transitions

Allowed transitions:

```text
PENDING -> USED
PENDING -> REVOKED
PENDING -> EXPIRED
```

Not allowed:

```text
USED -> PENDING
USED -> REVOKED
REVOKED -> USED
EXPIRED -> USED
```

## 7.2 Client Telegram account status transitions

Allowed transitions:

```text
ACTIVE -> DISABLED
ACTIVE -> UNLINKED
DISABLED -> ACTIVE
```

MVP only needs:

```text
ACTIVE
```

`DISABLED` and `UNLINKED` can be implemented when unlinking is added.

---

## 8. Transaction Design

## 8.1 Create invitation

Single transaction:

```text
validate company/user/client
create invitation
write audit event
commit
```

## 8.2 Validate token

Can run read-only transaction plus audit write.

If validation fails, still write audit event.

## 8.3 Confirm link

Mandatory write transaction:

```text
revalidate token with lock
verify token is PENDING
verify not expired
verify client not linked
verify telegram user not linked in company
create client_telegram_accounts
mark invitation USED
write audit event
commit
```

### Concurrency requirement

When confirming, lock the invitation row to prevent double-use.

Recommended PostgreSQL/JPA behavior:

```text
SELECT ... FOR UPDATE
```

or JPA pessimistic write lock.

---

## 9. Persistence Design Notes

JPA entities belong in infrastructure, not domain.

Recommended split:

```text
Domain model: TelegramInvitationToken
JPA entity: TelegramInvitationTokenJpaEntity
Mapper: TelegramInvitationTokenPersistenceMapper
```

Do not put JPA annotations on domain objects if the project is following Clean Architecture strictly.

---

## 10. Token Design

## 10.1 Raw token

Recommended raw token format:

```text
8 to 12 characters
uppercase letters + digits
no confusing characters
```

Avoid:

```text
0 O I L 1
```

Example alphabet:

```text
ABCDEFGHJKLMNPQRSTUVWXYZ23456789
```

Example token:

```text
K7P9Q2X8
```

## 10.2 Hashing

Store:

```text
token_hash
token_prefix
```

Never store:

```text
raw token
```

Use a server-side hashing strategy. For MVP, SHA-256 with a server-side pepper is acceptable.

Recommended:

```text
token_hash = SHA-256(server_pepper + raw_token)
```

The pepper must come from environment/configuration, not from the database.

---

## 11. Audit Events

Audit event writing should happen for:

- invitation created
- invitation revoked
- validation started
- validation failed
- token confirmed
- Telegram linked
- duplicate Telegram detected
- client already linked
- unexpected error during linking

Never store raw tokens in audit metadata.

Safe metadata examples:

```json
{
  "tokenPrefix": "K7P9",
  "reason": "TOKEN_EXPIRED",
  "telegramUserId": 123456789
}
```

Unsafe metadata examples:

```json
{
  "token": "K7P9Q2X8",
  "jwt": "...",
  "password": "..."
}
```

---

## 12. Security Requirements

- Every company-scoped endpoint must validate `company_id` against authenticated user permissions.
- Never trust `companyId` from the URL alone.
- Never trust Telegram username as identity.
- Use `telegram_user_id` as the stable Telegram identity.
- Never store raw invitation tokens.
- Avoid detailed token failure reasons in public bot messages.
- Keep detailed reason codes internally for audit/debugging.
- Add rate limiting to Telegram validation endpoints later if abuse appears.

---

## 13. Testing Strategy

## 13.1 Unit tests

Test domain/application rules without Spring.

Required cases:

- owner can create invitation
- suspended user cannot create invitation
- agent cannot invite unassigned client
- inactive company blocks invitation
- inactive client blocks invitation
- existing active Telegram account blocks new invitation
- pending invitation blocks duplicate invitation
- expired token fails validation
- used token fails validation
- revoked token fails validation
- active Telegram user in same company blocks linking
- confirming link marks token as used

## 13.2 Integration tests

Use PostgreSQL test container or equivalent.

Required cases:

- create invitation persists token hash, not raw token
- confirm link creates account and marks invitation used atomically
- concurrent confirm attempts result in only one success
- audit event is written on success
- audit event is written on validation failure

---

## 14. Suggested Implementation Order

1. Create database migrations.
2. Create domain value objects and enums.
3. Create domain models.
4. Create application commands/responses.
5. Create outbound ports.
6. Implement `CreateTelegramInvitationUseCase`.
7. Implement `ValidateTelegramLinkTokenUseCase`.
8. Implement `ConfirmTelegramLinkUseCase`.
9. Implement `RevokeTelegramInvitationUseCase`.
10. Implement `GetTelegramLinkStatusUseCase`.
11. Add JPA entities and repositories.
12. Add REST controllers.
13. Add global error handler.
14. Add integration tests.
15. Connect Telegram bot webhook adapter.

---

## 15. MVP Endpoint Checklist

Required for MVP:

- `POST /api/v1/companies/{companyId}/clients/{clientId}/telegram-invitations`
- `POST /api/v1/telegram/link/validate`
- `POST /api/v1/telegram/link/confirm`
- `POST /api/v1/companies/{companyId}/telegram-invitations/{invitationId}/revoke`
- `GET /api/v1/companies/{companyId}/clients/{clientId}/telegram-link-status`

Not required for first MVP:

- unlink Telegram account
- multi-account Telegram per client
- global person identity shared across companies
- advanced rate limiting
- external CRM synchronization

---

## 16. Developer Notes

The implementation should not start from controllers.

Correct order:

```text
Domain rules -> Application use cases -> Ports -> Infrastructure adapters -> Controllers
```

If implementation starts from controllers, the system will likely become framework-driven instead of business-driven.

The important business rules belong in application/domain code, not scattered across REST controllers or JPA repositories.

---

## 17. Definition of Done

The MVP backend design is implemented when:

- Invitation can be created by authorized internal users.
- Invitation stores only `token_hash` and `token_prefix`.
- Telegram token can be validated.
- Bot can request confirmation before linking.
- Confirming link creates `client_telegram_accounts` and marks token `USED` atomically.
- Revocation works for pending invitations.
- Link status can be queried.
- Every success/failure path writes useful audit events.
- Company isolation is enforced in every company-scoped operation.
- Automated tests cover main success and failure cases.
