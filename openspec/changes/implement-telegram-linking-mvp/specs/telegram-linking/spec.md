# Telegram Linking Specification

## Requirements

### Requirement: Company isolation

Records MUST be scoped by `companyId`. Clients MUST belong to one company. OWNER, ADMIN, and SUPERVISOR MAY manage invitations; AGENT users MUST invite only assigned clients.

#### Scenario: Cross-company blocked
- GIVEN a company A user and company B resource
- WHEN the user creates, revokes, or queries a link
- THEN the operation MUST be forbidden or not found

#### Scenario: Unassigned agent is denied
- GIVEN an active AGENT and unassigned company client
- WHEN the AGENT creates an invitation
- THEN the request MUST fail with `CLIENT_NOT_ASSIGNED_TO_AGENT`

### Requirement: Invitation creation

The system MUST create 48-hour opaque, single-use invitations for active unlinked clients. Raw tokens MUST NOT be stored; `token_hash` MUST be stored; `token_prefix` MAY be stored. Duplicates MUST fail.

#### Scenario: Invitation is created
- GIVEN an authorized user and unlinked company client
- WHEN the user creates an invitation
- THEN a `PENDING` invitation MUST be stored
- AND response MUST include link, expiration, and prefix

#### Scenario: Duplicate pending invitation conflicts
- GIVEN a client has a pending invitation
- WHEN another invitation is requested
- THEN the system MUST return a conflict

### Requirement: Token preview

Bot validation MUST hash the token, check state, company/client status, Telegram uniqueness, and preview without linking.

#### Scenario: Valid token previews link
- GIVEN a pending invitation for active company/client
- WHEN the bot validates the token
- THEN the body MUST contain `status=VALID` and `confirmationRequired=true`

#### Scenario: Invalid token stays bot-safe
- GIVEN an invalid, expired, used, revoked, or conflicting token
- WHEN the bot validates it
- THEN HTTP status MUST be `200 OK`
- AND the body MUST contain `status=INVALID` and `errorCode`

### Requirement: Link confirmation

Confirmation MUST revalidate and lock the invitation, create one active `client_telegram_accounts` row, mark it `USED`, and audit atomically.

#### Scenario: Confirmation creates permanent link
- GIVEN a valid invitation and Telegram confirmation
- WHEN confirmation is submitted
- THEN the active link MUST be created
- AND the invitation MUST become `USED`

#### Scenario: Concurrent confirmation remains single-use
- GIVEN two confirmation attempts for one invitation
- WHEN both run concurrently
- THEN exactly one MUST succeed
- AND the other MUST return a domain conflict

### Requirement: Revocation and status

Authorized users MUST revoke only company pending invitations. Link status MUST report `NOT_LINKED`, `INVITATION_PENDING`, or `LINKED` within company scope.

#### Scenario: Pending invitation is revoked
- GIVEN a pending invitation in the user's company
- WHEN the user revokes it
- THEN it MUST become `REVOKED`
- AND user, time, and audit event MUST be recorded

#### Scenario: Status reflects current state
- GIVEN a client with no link, pending invitation, or account
- WHEN link status is requested
- THEN only the matching state details MUST be returned

### Requirement: Audit and HTTP

The system MUST audit creation, validation start/failure, confirmation success/failure, revocation, duplicate Telegram, and already linked cases. Audit metadata MUST NOT include raw tokens, JWTs, passwords, or secrets. Bot validate/confirm endpoints MUST always return `200 OK` with domain `status` and optional `errorCode`; admin endpoints MUST use semantic HTTP errors.

#### Scenario: Failure is audited safely
- GIVEN a token validation failure
- WHEN the failure is handled
- THEN `telegram_link_events` MUST store reason and safe metadata only

#### Scenario: Admin conflict uses HTTP conflict
- GIVEN an admin invites an already linked client
- WHEN the request is processed
- THEN the response MUST be `409 Conflict` with an error code

### Requirement: Configuration and reporting

Configuration MUST be YAML-based. OpenAPI/Swagger MUST expose MVP endpoints. Development schema creation MUST use Hibernate `ddl-auto: update`. Final delivery SHOULD report table names.

#### Scenario: Schema creation
- GIVEN the app starts in the development profile
- WHEN persistence initializes
- THEN MVP tables SHOULD be created by Hibernate update
- AND final notes SHOULD list tables
