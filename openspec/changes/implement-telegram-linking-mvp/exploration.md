## Exploration: Implement Telegram Linking MVP

### Current State
The repository is a minimal Spring Boot 4.1.0 / Java 17 Maven application. The only production Java class is `com.telegram.ia.TelegramIaApplication`, and there are no existing domain, application, or infrastructure packages yet.

The project already includes Spring Web MVC, Spring Data JPA, Validation, Actuator, PostgreSQL runtime driver, and Lombok. `application.yaml` currently only defines the application name, so datasource, JPA schema generation, token/security settings, and OpenAPI configuration still need to be added.

OpenSpec is initialized for project `telegram-ia` and explicitly requires strict Hexagonal Architecture for Spring features. The existing Spring context smoke test is known to fail until datasource/test database configuration is provided.

Two static design documents already define most of the target business model and flow: invitation tokens are opaque, hashed, temporary, single-use records; Telegram links are permanent account records; confirmation must be transactional; audit events should capture both success and failure paths. The user-confirmed constraints override older package examples and require the package base `com.telegram.ia.telegramlink`.

Important alignment gap: the technical design currently describes semantic HTTP statuses for bot-facing validate/confirm failures, but the confirmed MVP constraint says bot-facing endpoints must always return `200 OK` with domain-level `status` / `errorCode`; only administrative endpoints should use semantic HTTP status codes.

### Affected Areas
- `src/main/java/com/telegram/ia/TelegramIaApplication.java` — root Spring Boot entrypoint already sits above the target package base, so component and entity scanning can discover `com.telegram.ia.telegramlink` without moving it.
- `src/main/java/com/telegram/ia/telegramlink/domain/**` — new pure Java domain model, value objects, enums, policies, and domain exceptions for companies, users, clients, invitations, Telegram accounts, assignments, and audit events.
- `src/main/java/com/telegram/ia/telegramlink/application/**` — new use cases, inbound ports, outbound ports, command/query DTOs, application errors, and transaction boundaries.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/**` — new REST controllers, JPA entities/repositories, persistence adapters/mappers, simulated current-user provider, clock/token/hash adapters, configuration properties, and exception translation.
- `pom.xml` — add OpenAPI/Swagger dependency and optionally Flyway if migrations are introduced; current dependencies already cover Web MVC, JPA, Validation, PostgreSQL, and tests.
- `src/main/resources/application.yaml` — add YAML-only datasource, Hibernate `ddl-auto: update` for development, Telegram link configuration, token pepper, bot username/link template, and springdoc settings.
- `src/test/java/com/telegram/ia/**` — add unit tests for domain/application rules and integration tests or test datasource setup; current context-load test needs a database configuration before it can pass.
- `src/main/resources/static/telegram-backend-data-model.md` — reference model for table names and constraints; needs one MVP extension for `company_user_client_assignments`.
- `src/main/resources/static/telegram-backend-technical-design.md` — reference flow and ports; needs alignment with package base and bot-facing `200 OK` response rule.
- `openspec/config.yaml` — confirms SDD execution mode, strict TDD support, review budget, and Hexagonal Architecture rules.

### Approaches
1. **Full Hexagonal MVP in one change** — Implement domain/application/infrastructure slices for all required MVP use cases, persistence, endpoints, OpenAPI, development schema update, and tests in one OpenSpec change.
   - Pros: Delivers the complete user goal coherently; keeps business rules centralized; enables end-to-end verification of invitation creation through confirmation.
   - Cons: Likely exceeds the 400-line review budget; touches many files; harder to review without chained PRs.
   - Effort: High

2. **Vertical slice with chained implementation phases** — Keep one OpenSpec change but plan implementation as reviewable slices: foundation/config/persistence, create invitation, bot validate/confirm, admin revoke/status, then tests/docs.
   - Pros: Preserves architectural consistency while respecting review budget; each slice can be reviewed and tested around a business capability; reduces merge risk.
   - Cons: Requires careful task sequencing and possibly user confirmation for chained PR strategy before implementation.
   - Effort: Medium-High

3. **Schema-first CRUD-style MVP** — Start from JPA entities and controllers, then backfill application/domain abstractions.
   - Pros: Fastest short-term endpoint scaffolding.
   - Cons: Violates the confirmed strict Clean/Hexagonal constraint; business rules would leak into controllers/repositories; harder to fix later.
   - Effort: Medium initially, High cleanup later

### Recommendation
Use **Approach 2: Vertical slice with chained implementation phases**.

The implementation should still model the complete MVP in the proposal/spec/design, but actual coding should be split into small slices because the feature adds a new bounded context, multiple tables, authorization rules, persistence adapters, endpoint contracts, and tests. This respects the configured `ask-always` chained PR strategy and the 400-line review budget without sacrificing Clean/Hexagonal Architecture.

Recommended technical direction:
- Keep the package base as `com.telegram.ia.telegramlink`.
- Use pure domain/application types for business rules and ports.
- Keep Spring MVC controllers, JPA entities, Spring Data repositories, configuration properties, simulated auth, and exception handling inside infrastructure.
- Add `company_user_client_assignments` to support AGENT authorization.
- Use JPA/Hibernate `ddl-auto: update` for development table creation as requested; introduce Flyway only if the next SDD phases decide to formalize migrations now.
- Add `springdoc-openapi-starter-webmvc-ui` for Swagger UI/OpenAPI because the project uses Spring Web MVC.
- Return uniform `200 OK` response bodies from bot-facing validation/confirmation endpoints; use semantic HTTP statuses for administrative endpoints.
- Simulate `CurrentUserProviderPort` for MVP using configured/header-provided claims, but keep the port shaped around the future production claims: `companyUserId`, `companyId`, `role`, and `status`.

### Risks
- The existing context-load test will keep failing until datasource or embedded/test database configuration is addressed.
- Spring Boot 4.1.0 is newer than the common Spring Boot 3.x ecosystem baseline; verify springdoc compatibility during implementation.
- `ddl-auto: update` is acceptable for development but is not a production migration strategy; final delivery must report table names and should not imply production schema governance is solved.
- Bot-facing endpoint semantics must be corrected from older design notes to avoid leaking token validation failures through HTTP status codes.
- Concurrency on confirm requires row locking or equivalent pessimistic locking to prevent double token use.
- Token hash pepper must come from configuration/environment and must never be logged or stored in audit metadata.
- Strict Hexagonal layering can be accidentally broken if JPA entities or web DTOs cross into domain/application.

### Ready for Proposal
Yes — proceed to the proposal phase. The orchestrator should tell the user that exploration found enough repo context and prior design material to define the OpenSpec change. The next phase should formalize scope, reconcile the bot-facing `200 OK` constraint with existing design docs, and plan chained/reviewable implementation slices before code is written.
