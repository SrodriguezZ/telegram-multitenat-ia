# Design: Implement Telegram Linking MVP

## Technical Approach

Build a new bounded context under `com.telegram.ia.telegramlink` using strict Clean/Hexagonal Architecture: Domain is pure Java, Application owns use-case orchestration and ports, and Infrastructure owns Spring MVC, JPA, configuration, hashing, simulated auth, OpenAPI, and transaction adapters. `TelegramIaApplication` already lives in `com.telegram.ia`, so Spring component/entity scanning can discover this package without moving the entrypoint.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Package boundary | `com.telegram.ia.telegramlink` | Older `com.fuqi.telegramlink` examples | Matches the real project base and keeps the feature isolated. |
| Layering | Domain -> Application -> Infrastructure dependency rule | Controller/repository-first CRUD | Prevents Spring/JPA/HTTP leakage into business rules. |
| Persistence | Separate Domain models from `*JpaEntity` classes and MapStruct/manual mappers | Annotating domain with JPA | Preserves pure Domain and explicit boundary mapping. |
| Bot HTTP semantics | Validate/confirm always return `200 OK` with `status`/`errorCode` | Semantic HTTP errors for bot calls | Bot integration needs stable payload-driven outcomes. |
| Admin HTTP semantics | Use semantic HTTP statuses + ProblemDetail-style error body | Always-200 everywhere | Admin API clients should get standard 400/403/404/409 behavior. |
| Schema MVP | YAML config with Hibernate `ddl-auto: update` | Flyway now | User requested startup-created dev tables; production migrations remain a risk. |

## Package Structure and Layer Placement

```text
com.telegram.ia.telegramlink
  domain.model|valueobject|exception|policy
  application.port.in|port.out|usecase|command|query|response|error
  infrastructure.web.admin|web.bot|web.mapper|web.error
  infrastructure.persistence.jpa.entity|repository|mapper|adapter
  infrastructure.security|token|clock|config|openapi
```

Domain components: `Company`, `CompanyUser`, `Client`, `TelegramInvitation`, `ClientTelegramAccount`, `TelegramLinkEvent`, statuses, value objects, and invitation/authorization policies. Application components: `CreateTelegramInvitationUseCase`, `ValidateTelegramLinkTokenUseCase`, `ConfirmTelegramLinkUseCase`, `RevokeTelegramInvitationUseCase`, `GetTelegramLinkStatusUseCase`, plus repository, token, hash, clock, event, and `CurrentUserProviderPort` ports. Infrastructure components: REST controllers, DTO mappers, JPA adapters, `@ConfigurationProperties`, SHA-256 hashing adapter, secure token generator, system clock, simulated current user provider, OpenAPI config, and global exception handler.

## Data Flow

```text
Admin create/revoke/status:
HTTP DTO -> AdminController -> UseCase -> Ports -> JPA Adapter -> DB
                         \-> CurrentUserProviderPort

Bot validate:
Bot DTO -> BotController -> ValidateUseCase -> hash token -> read invitation -> audit -> 200 body

Bot confirm:
Bot DTO -> BotController -> ConfirmUseCase -> lock invitation -> create account
                                           -> mark USED -> audit -> commit -> 200 body
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/telegram/ia/telegramlink/domain/**` | Create | Pure business model, value objects, policies, exceptions. |
| `src/main/java/com/telegram/ia/telegramlink/application/**` | Create | Use cases, ports, commands/queries/responses, transaction boundary classes. |
| `src/main/java/com/telegram/ia/telegramlink/infrastructure/**` | Create | Web, JPA, mappers, config, OpenAPI, hashing, clock, simulated auth. |
| `src/main/resources/application.yaml` | Modify | Datasource, `spring.jpa.hibernate.ddl-auto: update`, `telegram-link.*`, springdoc. |
| `pom.xml` | Modify | Add `springdoc-openapi-starter-webmvc-ui`; add MapStruct only if mapper generation is used. |
| `src/test/java/com/telegram/ia/telegramlink/**` | Create | Domain/application unit tests and JPA/web integration tests. |

## Interfaces / Contracts

Inbound ports: create invitation, validate token, confirm link, revoke invitation, get link status. Outbound ports: company, company user, client, invitation, Telegram account, audit event repositories; token generator; token hashing; clock; current user provider.

`CurrentUserProviderPort` returns a simulated `AuthenticatedUser(companyUserId, companyId, role, status)` from headers/config for MVP, but keeps the contract ready for JWT/JWKS later.

Token hashing stores `SHA-256(pepper + rawToken)` and `tokenPrefix`; pepper comes from YAML/env (`telegram-link.token.pepper`) and is never persisted, logged, or audited.

Tables: `companies`, `company_users`, `clients`, `company_user_client_assignments`, `telegram_invitation_tokens`, `client_telegram_accounts`, `telegram_link_events`.

## Transaction / Locking Design

Create and revoke run in application-level write transactions. Validate may read and always audit failure/success safely. Confirm is one write transaction and MUST revalidate with a pessimistic write lock / `SELECT ... FOR UPDATE` on the invitation row before creating `client_telegram_accounts`, marking the invitation `USED`, and writing audit.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Policies, status transitions, use-case authorization, bot result mapping | JUnit without Spring. |
| Integration | JPA mappings, token hash persistence, locking, audit writes | Spring Boot + PostgreSQL/Testcontainers or configured test DB. |
| Web | Admin semantic HTTP and bot always-200 contracts; OpenAPI exposure | MockMvc/WebMvc tests. |

## Migration / Rollout

No production migration required for MVP. Development uses YAML `ddl-auto: update`; final delivery must list created/required table names and state that production needs managed migrations later.

## Risks

- Springdoc compatibility with Spring Boot 4.1.0 must be verified during implementation.
- Current context-load test fails until datasource/test DB config exists.
- `ddl-auto: update` is development-only and can drift from production needs.
- Review size will exceed 400 lines unless implementation is split into chained vertical slices.
- Layering can degrade if JPA entities, DTOs, or Spring annotations cross into Domain/Application.

## Open Questions

- [ ] None blocking.
