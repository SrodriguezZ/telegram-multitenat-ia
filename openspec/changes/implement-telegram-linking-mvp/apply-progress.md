# Apply Progress: Implement Telegram Linking MVP

## Current Slice

- Work unit: WU4 follow-up — Final critical blocker fix
- Mode: Strict TDD
- Delivery boundary: WU4 critical fixes only. WU1/WU2/WU3/WU4 task completion is preserved below.
- Chain strategy: `feature-branch-chain` selected before WU2 and continued for WU4.
- Review budget posture: Full MVP exceeds 400 changed lines. This follow-up is limited to security/compliance blockers found by verification.

## Completed Tasks

- [x] 1.1 RED: add domain tests in `src/test/java/com/telegram/ia/telegramlink/domain/**` for invitation states, token secrecy, and role assignment policy.
- [x] 1.2 Create pure Domain models, value objects, statuses, exceptions, and policies in `src/main/java/com/telegram/ia/telegramlink/domain/**`.
- [x] 1.3 Update `pom.xml` and `src/main/resources/application.yaml` for springdoc, datasource/JPA update mode, and `telegram-link.*` YAML properties.
- [x] 1.4 Create JPA entities/repositories/mappers/adapters under `src/main/java/com/telegram/ia/telegramlink/infrastructure/persistence/**` for the seven MVP tables, including invitation lock query.
- [x] 1.5 GREEN: add persistence mapping/hash-prefix tests in `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/**`.
- [x] 2.1 RED: add application tests for create, duplicate conflict, unassigned agent denial, revoke, and status scenarios.
- [x] 2.2 Create commands/responses and inbound/outbound ports under `src/main/java/com/telegram/ia/telegramlink/application/**`.
- [x] 2.3 Implement `CreateTelegramInvitationUseCase`, `RevokeTelegramInvitationUseCase`, and `GetTelegramLinkStatusUseCase` with transaction boundaries and audit ports.
- [x] 3.1 RED: add tests for invalid-token always-200 payloads, valid previews, confirmation success, and concurrent single-use conflict.
- [x] 3.2 Implement token generator/hash/clock adapters in `src/main/java/com/telegram/ia/telegramlink/infrastructure/token/**` and `.../clock/**`.
- [x] 3.3 Implement `ValidateTelegramLinkTokenUseCase` and `ConfirmTelegramLinkUseCase` with lock, account creation, USED transition, and safe audit metadata.
- [x] 4.1 RED: add MockMvc tests for admin semantic HTTP, bot always-200 contracts, and Swagger exposure.
- [x] 4.2 Create admin/bot controllers, DTOs, mappers, simulated current-user provider, OpenAPI config, and `ProblemDetail` handler under `src/main/java/com/telegram/ia/telegramlink/infrastructure/**`.
- [x] 4.3 Run `./mvnw test` and `./mvnw package`; document MVP table names and production migration gap in final delivery notes.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/telegram/ia/telegramlink/domain/TelegramInvitationTest.java`, `src/test/java/com/telegram/ia/telegramlink/domain/InvitationAuthorizationPolicyTest.java` | Unit | ⚠️ Pre-existing `./mvnw test` failed before edits due missing datasource | ✅ Written first; failed compilation against missing domain classes | ✅ Domain tests passed | ✅ Invitation state/token secrecy + role/agent/inactive edge cases | ✅ Domain kept framework-free |
| 1.2 | Same as 1.1 | Unit | N/A (new files) | ✅ Covered by 1.1 tests | ✅ Domain tests passed | ✅ Multiple role/status/state paths covered | ✅ No Spring/JPA annotations in Domain |
| 1.3 | `src/test/java/com/telegram/ia/telegramlink/config/ApplicationYamlConfigurationTest.java` | Unit/config | ⚠️ Pre-existing datasource failure captured before config fix | ✅ Written first; failed on missing YAML keys and Springdoc class | ✅ Config tests passed | ➖ Structural/config assertions only | ✅ Test YAML datasource added |
| 1.4 | `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/TelegramInvitationPersistenceTest.java` | Integration | N/A (new package) | ✅ Written first; failed compilation against missing JPA classes | ✅ Persistence tests passed | ✅ Table names, hash/prefix mapping, lock query | ✅ JPA stayed Infrastructure-only |
| 1.5 | `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/TelegramInvitationPersistenceTest.java` | Integration | N/A (new package) | ✅ Mapping/hash-prefix expectations before implementation | ✅ Persistence test passed | ✅ Mapper round-trip and locked repository lookup | ✅ Adapter wraps repository/mapper |
| 2.1 | `src/test/java/com/telegram/ia/telegramlink/application/AdminTelegramLinkUseCasesTest.java` | Unit/application | N/A (new application package) | ✅ Failed compilation against missing commands, ports, responses, use cases | ✅ Admin use-case tests passed | ✅ Create, duplicate, unassigned/assigned agent, revoke, status states | ✅ Fakes isolate use cases from Spring/JPA |
| 2.2 | `src/test/java/com/telegram/ia/telegramlink/application/AdminTelegramLinkUseCasesTest.java` | Unit/application contracts | N/A (new files) | ✅ Tests referenced missing contracts first | ✅ Targeted application test passed | ✅ Ports cover current user, repositories, generators, hashing, audit, clock, transactions | ✅ Contracts remain framework-free |
| 2.3 | `src/test/java/com/telegram/ia/telegramlink/application/AdminTelegramLinkUseCasesTest.java` | Unit/application behavior | N/A (new files) | ✅ Tests referenced missing admin use cases first | ✅ Final WU2 `./mvnw test` passed 18 tests; later WU2 hardening passed 23 | ✅ Authorization, duplicate, assignment, revocation, audit, and status logic | ✅ `TransactionRunnerPort` preserved application boundary |
| 3.1 | `src/test/java/com/telegram/ia/telegramlink/application/BotTelegramLinkUseCasesTest.java` | Unit/application | ✅ WU3 safety net passed 14 existing tests | ✅ Failed compilation against missing bot contracts/use cases | ✅ Bot use-case tests passed 5 tests | ✅ Invalid token, valid preview, duplicate Telegram account, confirmation success, second confirmation | ✅ Bot failures return payload status/errorCode |
| 3.2 | `src/test/java/com/telegram/ia/telegramlink/infrastructure/token/TokenInfrastructureAdaptersTest.java` | Unit/infrastructure | N/A (new adapters) | ✅ Failed compilation against missing token/clock adapters | ✅ Token adapter tests passed 3 tests | ✅ URL-safe uniqueness, pepper-sensitive SHA-256, system clock | ✅ Adapters implement ports in Infrastructure |
| 3.3 | `src/test/java/com/telegram/ia/telegramlink/application/BotTelegramLinkUseCasesTest.java` | Unit/application behavior | ✅ WU3 safety-net baseline passed | ✅ Tests referenced missing validate/confirm use cases | ✅ WU3 final `./mvnw test` passed 31 tests | ✅ Lock path exercised twice; second confirm returns `INVITATION_ALREADY_USED` | ✅ No raw token in audit metadata |
| 4.1 | `src/test/java/com/telegram/ia/telegramlink/infrastructure/web/TelegramLinkWebAdaptersTest.java`, `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/TelegramInvitationPersistenceTest.java` | Integration/Web + Persistence | ✅ `./mvnw -Dtest='com.telegram.ia.telegramlink.application.AdminTelegramLinkUseCasesTest,com.telegram.ia.telegramlink.application.BotTelegramLinkUseCasesTest,com.telegram.ia.telegramlink.infrastructure.persistence.TelegramInvitationPersistenceTest' test` passed 19 tests before WU4 production edits | ✅ WU4 RED failed: admin/bot endpoints returned 404, OpenAPI title/path missing, static docs were served, and active account uniqueness was not enforced | ✅ Targeted WU4 command passed 9 tests after implementation | ✅ Covered admin 409 ProblemDetail, bot always-200 invalid payload, cross-company revoke 404, OpenAPI path, static docs 404, and two uniqueness conflicts | ✅ Tests assert externally visible HTTP/database behavior |
| 4.2 | Same as 4.1 | Infrastructure adapters | ✅ Same WU4 safety net | ✅ Tests referenced missing web/config/adapter behavior first | ✅ Controllers, DTOs, mappers, simulated current-user provider, OpenAPI config, transaction/id/link/audit/persistence adapters passed targeted tests | ✅ Both admin and bot endpoints plus OpenAPI and documentation guard paths exercised | ✅ HTTP/JPA concerns kept in Infrastructure; Application ports remain framework-free |
| 4.3 | Full suite/build | Verification | ✅ Targeted WU4 tests green before full verification | ✅ Final verification required by task before marking done | ✅ `./mvnw test` passed 37 tests; `./mvnw package` passed | ✅ Package reran all test layers and built jar | ✅ Final notes include table names and migration gap |
| 4.4 | `src/test/java/com/telegram/ia/telegramlink/infrastructure/web/TelegramLinkWebAdaptersTest.java`, `src/test/java/com/telegram/ia/telegramlink/config/ApplicationYamlConfigurationTest.java` | Integration/Web + Config | ✅ Targeted web/config safety net passed 7 tests before edits | ✅ New tests failed as expected: cross-company create returned 400, spoofed headers changed identity, and main YAML lacked current-user keys | ✅ Targeted web/config command passed 9 tests after implementation | ✅ Covers non-leaking cross-company create 404 and spoofed header rejection by using configured server-side identity | ✅ Replaced header-backed provider with property-backed simulated provider in Infrastructure |

## Test Summary

- Total tests passing: 39.
- WU4 tests added: 8 — 7 web/OpenAPI/static-doc/security integration tests and 1 persistence uniqueness test. Config assertions were updated to require server-side simulated current-user properties.
- Layers used across the change: Unit/domain/application/config/token, Spring Boot integration/persistence/web, Spring context smoke.
- Approval tests: None — WU4 adds adapters and regression coverage rather than refactoring existing behavior.
- Pure functions/models created in WU4: HTTP DTO mappers and framework-bound adapters; core business logic remains in existing Application/Domain layers.

## Verification

- WU4 safety net: `./mvnw -Dtest='com.telegram.ia.telegramlink.application.AdminTelegramLinkUseCasesTest,com.telegram.ia.telegramlink.application.BotTelegramLinkUseCasesTest,com.telegram.ia.telegramlink.infrastructure.persistence.TelegramInvitationPersistenceTest' test` passed 19 tests before production edits.
- WU4 RED: `./mvnw -Dtest='com.telegram.ia.telegramlink.infrastructure.web.TelegramLinkWebAdaptersTest,com.telegram.ia.telegramlink.infrastructure.persistence.TelegramInvitationPersistenceTest' test` failed as expected on missing endpoint/OpenAPI/doc-guard behavior and missing account uniqueness.
- WU4 GREEN: same targeted command passed 9 tests.
- Final blocker RED: `./mvnw -Dtest='com.telegram.ia.telegramlink.infrastructure.web.TelegramLinkWebAdaptersTest,com.telegram.ia.telegramlink.config.ApplicationYamlConfigurationTest' test` failed as expected on cross-company create returning 400, spoofed headers affecting identity, and missing YAML current-user keys.
- Final blocker GREEN: same targeted command passed 9 tests.
- Final test verification: `./mvnw test` passed 39 tests.
- Final package verification: `./mvnw package` passed and produced `target/Telegram-IA-0.0.1-SNAPSHOT.jar`.

## Files Changed in WU4

- `src/test/java/com/telegram/ia/telegramlink/infrastructure/web/TelegramLinkWebAdaptersTest.java` — added MockMvc coverage for admin semantic HTTP, bot always-200 payloads, cross-company revoke regression, OpenAPI exposure, and static documentation guard.
- `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/TelegramInvitationPersistenceTest.java` — added DB-backed uniqueness regression for active company/Telegram-user and company/client links.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/web/**` — added admin/bot controllers, DTOs, mapper, ProblemDetail handler, and static documentation guard controller.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/config/**` — added use-case wiring, id generators, and Telegram invitation link builder.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/security/SimulatedCurrentUserProviderAdapter.java` and `SimulatedCurrentUserProperties.java` — replaced spoofable header-backed identity with server-side YAML/env-backed simulated current user.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/openapi/TelegramLinkOpenApiConfig.java` — added OpenAPI metadata.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/transaction/SpringTransactionRunnerAdapter.java` — added Spring transaction runner adapter for application write transactions.
- `src/main/java/com/telegram/ia/telegramlink/infrastructure/persistence/jpa/**` — added missing persistence adapters/mappers/repository methods and uniqueness constraints for active account lookups.
- `docs/telegram-backend-technical-design.md`, `docs/telegram-backend-data-model.md` — moved internal technical docs out of `src/main/resources/static` so they are not packaged as public static resources.
- `src/main/resources/application.yaml`, `src/test/resources/application.yaml` — added `telegram-link.current-user.*` server-side simulated identity properties.
- `openspec/changes/implement-telegram-linking-mvp/tasks.md` — marked WU4 complete.
- `openspec/changes/implement-telegram-linking-mvp/apply-progress.md` — merged WU1/WU2/WU3 progress with WU4 progress.

## Deviations / Notes

- Simulated current user is now supplied by server-side `telegram-link.current-user.*` YAML/env-backed properties. Client-controlled identity headers are ignored for MVP; production JWT/JWKS remains out of scope.
- Cross-company invitation create now returns `404 CLIENT_NOT_FOUND` to avoid leaking whether another company's client exists.
- Active uniqueness is implemented as `(company_id, telegram_user_id, status)` and `(company_id, client_id, status)` unique constraints. This is compatible with the MVP because only `ACTIVE` accounts are created; a future unlinking feature may need partial PostgreSQL indexes for `status = 'ACTIVE'`.
- Internal docs were moved out of static resources and a guard controller returns 404 for the previous public filenames to protect existing stale build outputs.
- `ddl-auto: update` remains development-only; production requires managed migrations for every table, index, and uniqueness constraint.

## Remaining Tasks

- [x] None for this OpenSpec task list. Proceed to SDD verify/archive as the next workflow phase.

## Actual MVP Table Names

- `companies`
- `company_users`
- `clients`
- `company_user_client_assignments`
- `telegram_invitation_tokens`
- `client_telegram_accounts`
- `telegram_link_events`

## Production Migration Gap

- No Flyway/Liquibase migration was added in this MVP. Production rollout must create managed migrations for the seven tables, existing indexes, invitation token hash uniqueness, and active account uniqueness constraints before using this outside development.
