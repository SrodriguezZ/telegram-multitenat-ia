# Telegram Backend Relational Data Model

This document defines the initial relational database model for a Java + PostgreSQL backend that manages Telegram invitation links, client linking, and multi-company authorization.

The goal is to support a scalable multi-tenant system where many companies can invite their own clients to use a Telegram bot, while the backend remains the source of truth for identity, permissions, and access rules.

---

## 1. Functional Objective

The backend must allow a company user, usually an owner, admin, supervisor, or authorized agent, to generate a short invitation link for a client.

The client receives a Telegram link like:

```text
https://t.me/<bot_username>?start=<short_token>
```

When the client opens the bot, Telegram sends the token to the backend. The backend validates the token and, if valid, links the client's Telegram user ID to the correct client record.

The token must be:

- short enough to share easily
- random
- opaque
- unique
- temporary
- single-use
- validated only by the backend

The token must not contain secrets, client data, company data, JWTs, permissions, or signed payloads.

---

## 2. Core Design Principles

### 2.1 Multi-company from day one

Every business entity must belong to a company. This prevents data from different companies from mixing and allows the system to grow safely.

### 2.2 Invitation is temporary, Telegram link is permanent

The invitation token and the Telegram account link are different concepts.

- `telegram_invitation_tokens` stores temporary invitations.
- `client_telegram_accounts` stores the permanent link between a client and a Telegram account.

Do not store the permanent Telegram relationship only inside the invitation table.

### 2.3 Backend owns authorization

Telegram identifies the user, but the backend decides what that user can do.

Hermes or the Telegram bot should never invent permissions. They should ask the backend.

### 2.4 Store token hashes, not raw tokens

For better security, the database should store a hash of the invitation token instead of the plain token.

Recommended approach:

- Generate raw token: `K7P9Q2`
- Send raw token in Telegram link.
- Store only `token_hash` in database.
- Optionally store `token_prefix` for support/debugging.

---

## 3. Entity Relationship Summary

```text
companies 1 ---- N company_users
companies 1 ---- N clients
companies 1 ---- N telegram_invitation_tokens
companies 1 ---- N client_telegram_accounts

company_users 1 ---- N telegram_invitation_tokens
clients 1 ---- N telegram_invitation_tokens
clients 1 ---- 0..N client_telegram_accounts
telegram_invitation_tokens 0..1 ---- 1 client_telegram_accounts
```

High-level flow:

```text
company_user creates invitation
        ↓
telegram_invitation_tokens row is created
        ↓
client opens Telegram bot link
        ↓
backend validates token
        ↓
client_telegram_accounts row is created
        ↓
invitation token becomes USED
```

---

# 4. Tables

## 4.1 `companies`

Stores each company or tenant using the system.

### Purpose

A company is the root boundary for tenant data. Users, clients, invitations, and Telegram links must all belong to one company.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `name` | `varchar(150)` | yes | Company display name. |
| `legal_name` | `varchar(200)` | no | Legal company name, if needed. |
| `status` | `varchar(30)` | yes | Company status: `ACTIVE`, `SUSPENDED`, `DELETED`. |
| `created_at` | `timestamptz` | yes | Creation timestamp. |
| `updated_at` | `timestamptz` | yes | Last update timestamp. |

### Relationships

- One company has many `company_users`.
- One company has many `clients`.
- One company has many `telegram_invitation_tokens`.
- One company has many `client_telegram_accounts`.

### Important constraints

- `id` is the primary key.
- `status` should be constrained to allowed values.

---

## 4.2 `company_users`

Stores internal users who belong to a company, such as owners, admins, supervisors, or agents.

### Purpose

These users can generate invitations, manage clients, or perform company-level operations depending on their role.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `company_id` | `uuid` | yes | Foreign key to `companies.id`. |
| `email` | `varchar(255)` | yes | User email. |
| `full_name` | `varchar(150)` | yes | User full name. |
| `role` | `varchar(40)` | yes | Role inside the company: `OWNER`, `ADMIN`, `SUPERVISOR`, `AGENT`. |
| `status` | `varchar(30)` | yes | User status: `ACTIVE`, `INVITED`, `SUSPENDED`, `DELETED`. |
| `created_at` | `timestamptz` | yes | Creation timestamp. |
| `updated_at` | `timestamptz` | yes | Last update timestamp. |

### Relationships

- Belongs to one `company`.
- Can create many `telegram_invitation_tokens`.

### Important constraints

- `company_id` references `companies.id`.
- Unique email per company: `(company_id, email)`.
- `role` should be constrained to allowed values.
- `status` should be constrained to allowed values.

### Notes

If the current system already has a user/auth table, this table can map to the existing one instead of replacing it.

---

## 4.3 `clients`

Stores the clients/customers of each company.

### Purpose

A client is the business entity that will be linked to a Telegram user. The client belongs to a specific company.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `company_id` | `uuid` | yes | Foreign key to `companies.id`. |
| `external_reference` | `varchar(100)` | no | Optional ID from another system. |
| `document_type` | `varchar(30)` | no | Example: `CEDULA`, `RUC`, `PASSPORT`. |
| `document_number` | `varchar(50)` | no | Client document number. |
| `full_name` | `varchar(180)` | yes | Client name. |
| `phone_number` | `varchar(40)` | no | Client phone number. |
| `email` | `varchar(255)` | no | Client email. |
| `status` | `varchar(30)` | yes | Client status: `ACTIVE`, `INACTIVE`, `BLOCKED`, `DELETED`. |
| `created_at` | `timestamptz` | yes | Creation timestamp. |
| `updated_at` | `timestamptz` | yes | Last update timestamp. |

### Relationships

- Belongs to one `company`.
- Can have many `telegram_invitation_tokens`.
- Can have zero or many `client_telegram_accounts`.

### Important constraints

- `company_id` references `companies.id`.
- Optional unique document per company: `(company_id, document_type, document_number)` where document fields are not null.
- Optional unique external reference per company: `(company_id, external_reference)` where `external_reference` is not null.

### Notes

If FuqiCallCenterIA already has a client table, this table should be adapted to match the existing domain instead of duplicated.

---

## 4.4 `telegram_invitation_tokens`

Stores temporary invitation tokens used to link a client to Telegram.

### Purpose

This table tracks every generated invitation link. It is used to validate whether a Telegram start token is valid, expired, already used, revoked, or invalid.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `company_id` | `uuid` | yes | Foreign key to `companies.id`. |
| `client_id` | `uuid` | yes | Foreign key to `clients.id`. |
| `created_by_user_id` | `uuid` | yes | Foreign key to `company_users.id`. |
| `token_hash` | `varchar(255)` | yes | Secure hash of the raw token. |
| `token_prefix` | `varchar(12)` | no | Short visible prefix for support/debugging. |
| `status` | `varchar(30)` | yes | Token status: `PENDING`, `USED`, `EXPIRED`, `REVOKED`. |
| `expires_at` | `timestamptz` | yes | Expiration timestamp. |
| `used_at` | `timestamptz` | no | Timestamp when token was used. |
| `revoked_at` | `timestamptz` | no | Timestamp when token was revoked. |
| `revoked_by_user_id` | `uuid` | no | User who revoked the token. |
| `used_by_telegram_user_id` | `bigint` | no | Telegram user ID that used this token. |
| `created_at` | `timestamptz` | yes | Creation timestamp. |
| `updated_at` | `timestamptz` | yes | Last update timestamp. |

### Relationships

- Belongs to one `company`.
- Belongs to one `client`.
- Created by one `company_user`.
- Can be linked to one `client_telegram_accounts` row after successful use.

### Important constraints

- `company_id` references `companies.id`.
- `client_id` references `clients.id`.
- `created_by_user_id` references `company_users.id`.
- `revoked_by_user_id` references `company_users.id` and can be null.
- Unique `token_hash`.
- `expires_at` must be greater than `created_at`.
- `used_at` should be null unless status is `USED`.
- `revoked_at` should be null unless status is `REVOKED`.

### Recommended indexes

- Unique index on `token_hash`.
- Index on `(company_id, client_id)`.
- Index on `(company_id, status)`.
- Index on `expires_at` for cleanup jobs.

### Business rules

A token is valid only if:

- `status = 'PENDING'`
- `expires_at > now()`
- no existing active Telegram account already blocks this client, unless the business allows multiple Telegram accounts per client
- the client belongs to the same company as the token
- the creator belongs to the same company as the token

When a token is successfully used:

- create a `client_telegram_accounts` row
- set token status to `USED`
- set `used_at`
- set `used_by_telegram_user_id`

---

## 4.5 `client_telegram_accounts`

Stores the permanent relationship between a client and a Telegram account.

### Purpose

This table represents the successful link between a client and a Telegram user. This is what the backend should use later to identify the client when messages arrive from Telegram.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `company_id` | `uuid` | yes | Foreign key to `companies.id`. |
| `client_id` | `uuid` | yes | Foreign key to `clients.id`. |
| `telegram_user_id` | `bigint` | yes | Stable Telegram user ID. |
| `telegram_chat_id` | `bigint` | no | Telegram chat ID where the bot talks to the user. |
| `telegram_username` | `varchar(100)` | no | Telegram username at linking time. Can change later. |
| `telegram_first_name` | `varchar(100)` | no | Telegram first name at linking time. |
| `telegram_last_name` | `varchar(100)` | no | Telegram last name at linking time. |
| `linked_by_invitation_token_id` | `uuid` | no | Foreign key to `telegram_invitation_tokens.id`. |
| `status` | `varchar(30)` | yes | Link status: `ACTIVE`, `DISABLED`, `UNLINKED`. |
| `linked_at` | `timestamptz` | yes | Timestamp when account was linked. |
| `unlinked_at` | `timestamptz` | no | Timestamp when account was unlinked. |
| `created_at` | `timestamptz` | yes | Creation timestamp. |
| `updated_at` | `timestamptz` | yes | Last update timestamp. |

### Relationships

- Belongs to one `company`.
- Belongs to one `client`.
- Optionally linked from one `telegram_invitation_tokens` row.

### Important constraints

- `company_id` references `companies.id`.
- `client_id` references `clients.id`.
- `linked_by_invitation_token_id` references `telegram_invitation_tokens.id`.
- Unique active Telegram user per company: `(company_id, telegram_user_id)` where `status = 'ACTIVE'`.
- Optional unique active client Telegram link: `(company_id, client_id)` where `status = 'ACTIVE'`, if each client can only have one active Telegram account.

### Notes

Do not rely on `telegram_username` for identity. Telegram usernames can change. Use `telegram_user_id` as the stable identifier.

---

## 4.6 `telegram_link_events`

Stores audit events for invitation creation, validation attempts, successful linking, failures, revocations, and unlinking.

### Purpose

This table gives visibility into what happened during the Telegram linking flow. It is useful for debugging, security review, customer support, and compliance.

### Columns

| Column | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `uuid` | yes | Primary key. |
| `company_id` | `uuid` | no | Company related to the event, if known. |
| `client_id` | `uuid` | no | Client related to the event, if known. |
| `invitation_token_id` | `uuid` | no | Invitation related to the event, if known. |
| `company_user_id` | `uuid` | no | Internal user who caused the event, if applicable. |
| `telegram_user_id` | `bigint` | no | Telegram user involved in the event, if known. |
| `telegram_chat_id` | `bigint` | no | Telegram chat involved in the event, if known. |
| `event_type` | `varchar(50)` | yes | Event type. |
| `result` | `varchar(30)` | yes | `SUCCESS`, `FAILURE`, or `INFO`. |
| `reason_code` | `varchar(80)` | no | Machine-readable reason code. |
| `message` | `text` | no | Human-readable explanation. |
| `metadata` | `jsonb` | no | Extra non-sensitive context. |
| `created_at` | `timestamptz` | yes | Event timestamp. |

### Relationships

- Optionally belongs to one `company`.
- Optionally belongs to one `client`.
- Optionally belongs to one `telegram_invitation_tokens` row.
- Optionally belongs to one `company_user`.

### Suggested event types

- `INVITATION_CREATED`
- `INVITATION_REVOKED`
- `INVITATION_EXPIRED`
- `TOKEN_VALIDATION_STARTED`
- `TOKEN_VALIDATION_FAILED`
- `CLIENT_TELEGRAM_LINKED`
- `CLIENT_TELEGRAM_UNLINKED`
- `DUPLICATE_TELEGRAM_ACCOUNT_DETECTED`

### Suggested reason codes

- `TOKEN_NOT_FOUND`
- `TOKEN_EXPIRED`
- `TOKEN_ALREADY_USED`
- `TOKEN_REVOKED`
- `CLIENT_NOT_ACTIVE`
- `COMPANY_NOT_ACTIVE`
- `TELEGRAM_ACCOUNT_ALREADY_LINKED`
- `UNEXPECTED_ERROR`

### Important constraints

- `event_type` should be constrained to known values or managed as an application enum.
- `result` should be constrained to `SUCCESS`, `FAILURE`, `INFO`.

---

# 5. Recommended PostgreSQL Types

For MVP speed, status fields can be `varchar` with `CHECK` constraints.

For stricter database-level modeling, PostgreSQL enums can be introduced later.

Recommended MVP approach:

```sql
status varchar(30) not null check (status in ('PENDING', 'USED', 'EXPIRED', 'REVOKED'))
```

This keeps migrations simpler while still protecting data integrity.

---

# 6. Validation Flow

## 6.1 Generate invitation

Input:

- `company_id`
- `client_id`
- authenticated `created_by_user_id`

Backend validates:

- company exists and is active
- creator belongs to company
- creator has permission to invite clients
- client exists
- client belongs to same company
- client is active

Backend then:

1. Generates random raw token.
2. Hashes token.
3. Stores `telegram_invitation_tokens` with `PENDING` status.
4. Writes `INVITATION_CREATED` event.
5. Returns Telegram link with raw token.

## 6.2 Validate Telegram start token

Input from Telegram:

- raw token from `/start <token>`
- `telegram_user_id`
- `telegram_chat_id`
- optional Telegram profile fields

Backend validates:

- token hash exists
- token status is `PENDING`
- token has not expired
- company is active
- client is active
- Telegram user is not already active for another client in the same company, unless allowed by business rules

Backend then:

1. Creates `client_telegram_accounts` row.
2. Marks token as `USED`.
3. Saves `used_at` and `used_by_telegram_user_id`.
4. Writes `CLIENT_TELEGRAM_LINKED` event.
5. Returns success message for the bot.

---

# 7. Initial API Endpoints

These endpoints are not the full backend, but they define the minimum needed for the Telegram invitation flow.

## 7.1 Create invitation

```http
POST /api/v1/companies/{companyId}/clients/{clientId}/telegram-invitations
```

### Response example

```json
{
  "invitationId": "uuid",
  "telegramLink": "https://t.me/my_bot?start=K7P9Q2",
  "expiresAt": "2026-06-15T10:00:00Z"
}
```

## 7.2 Validate Telegram token

```http
POST /api/v1/telegram/link/validate
```

### Request example

```json
{
  "token": "K7P9Q2",
  "telegramUserId": 123456789,
  "telegramChatId": 123456789,
  "telegramUsername": "client_user",
  "telegramFirstName": "Juan",
  "telegramLastName": "Perez"
}
```

### Success response example

```json
{
  "status": "LINKED",
  "message": "Telegram account linked successfully."
}
```

### Failure response example

```json
{
  "status": "INVALID_TOKEN",
  "message": "This invitation link is invalid or expired."
}
```

---

# 8. Open Business Decisions

Before implementation, confirm these decisions:

1. Can one client have multiple active Telegram accounts?
2. Can one Telegram account be linked to clients from different companies?
3. How long should invitation tokens live? Suggested MVP: 24 or 48 hours.
4. Who can generate invitations: owner only, admin, supervisor, or agent?
5. Should the client be linked immediately after opening the bot, or should the bot ask for an extra confirmation?
6. Should expired tokens be updated by a scheduled job, or treated as expired dynamically by checking `expires_at`?

---

# 9. Recommended MVP Decisions

For the first version, use these defaults:

- One client has only one active Telegram account per company.
- One Telegram user can only be linked once per company.
- Token expiration: 48 hours.
- Token status starts as `PENDING`.
- Expiration is checked dynamically with `expires_at`.
- A cleanup job can later mark old pending tokens as `EXPIRED`.
- Store `token_hash`, not raw token.
- Keep audit events from day one.

---

# 10. Development Notes for Java + PostgreSQL

Recommended backend stack:

- Java 21
- Spring Boot
- PostgreSQL
- Flyway or Liquibase for migrations
- JPA/Hibernate or jOOQ for persistence

Recommended implementation order:

1. Create database migration for core tables.
2. Create entities/models.
3. Create repositories.
4. Create invitation token generation service.
5. Create invitation creation endpoint.
6. Create Telegram token validation endpoint.
7. Add audit event writing.
8. Add integration tests for valid, expired, used, revoked, and invalid tokens.

---

# 11. Summary

The relational model should separate:

- companies and users
- clients
- temporary invitation tokens
- permanent Telegram account links
- audit events

This separation keeps the system scalable, secure, and easier to maintain as more companies and clients are added.
