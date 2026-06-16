# Proposal: Implement Telegram Linking MVP

## Intent

Implement the first Telegram invitation and client-linking backend slice so company users can create short-lived invitation links, Telegram users can validate and confirm them through the bot, and the backend remains the source of truth for tenant isolation, authorization, persistence, and auditability.

## Scope

### In Scope
- New `com.telegram.ia.telegramlink` bounded context with strict Domain → Application → Infrastructure boundaries.
- Invitation creation, token validation, confirmation, revocation, and link-status use cases.
- JPA persistence for companies, users, clients, assignments, invitations, Telegram accounts, and audit events.
- YAML-only configuration, Swagger/OpenAPI, and development schema creation with Hibernate `ddl-auto: update`.
- Simulated `CurrentUserProviderPort` shaped around `companyUserId`, `companyId`, `role`, and `status`.

### Out of Scope
- Production JWT/JWKS authentication and principal backend profile lookup.
- Telegram webhook handling inside this backend.
- Unlinking, multi-account clients, shared global person identity, and production schema governance.

## Capabilities

### New Capabilities
- `telegram-linking`: Telegram invitation, validation, confirmation, authorization, persistence, API, audit, and development schema behavior.

### Modified Capabilities
- None.

## Approach

Use vertical implementation slices under one OpenSpec change: domain rules first, then application ports/use cases, then infrastructure adapters. Bot-facing endpoints must return `200 OK` bodies with domain `status`/`errorCode`; admin endpoints use semantic HTTP statuses. AGENT users may invite only active assigned clients through `company_user_client_assignments`; OWNER, ADMIN, and SUPERVISOR may invite company clients.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/telegram/ia/telegramlink/domain/**` | New | Pure models, value objects, policies, statuses, exceptions. |
| `src/main/java/com/telegram/ia/telegramlink/application/**` | New | Use cases, ports, commands, responses, transaction boundaries. |
| `src/main/java/com/telegram/ia/telegramlink/infrastructure/**` | New | REST, JPA, mappers, config, simulated auth, token/hash/clock adapters. |
| `src/main/resources/application.yaml` | Modified | Datasource, JPA update mode, Telegram link, token pepper, springdoc config. |
| `pom.xml` | Modified | Add Swagger/OpenAPI dependency; Flyway only if migrations are introduced. |
| `src/test/java/com/telegram/ia/**` | Modified | Unit/integration coverage and datasource test setup. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Feature exceeds 400-line review budget | High | Use chained/reviewable vertical slices. |
| Double token use during confirmation | Medium | Lock invitation row in confirm transaction. |
| Layering leakage from JPA/Web into core | Medium | Keep DTO/entity mapping in Infrastructure only. |
| `ddl-auto: update` mistaken for production migration | Medium | Document table names and production migration gap at delivery. |

## Rollback Plan

Revert the change set, remove added configuration/dependencies, and drop development-created Telegram linking tables manually if needed. No production migration rollback is implied for MVP `ddl-auto` schema creation.

## Dependencies

- PostgreSQL datasource configuration for tests/dev startup.
- `springdoc-openapi-starter-webmvc-ui` compatible with Spring Boot 4.1.0.

## Success Criteria

- [ ] Authorized users can create, revoke, validate, confirm, and query Telegram links.
- [ ] Bot-facing failures return `200 OK` with domain status/error bodies.
- [ ] Raw tokens are never stored; audit excludes secrets.
- [ ] Final delivery reports actual table names for manual creation.
