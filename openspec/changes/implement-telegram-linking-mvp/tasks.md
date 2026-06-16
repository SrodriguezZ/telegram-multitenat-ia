# Tasks: Implement Telegram Linking MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 900-1,400 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | WU1 foundation/config/domain/persistence skeleton -> WU2 admin application -> WU3 bot confirmation -> WU4 web/docs hardening |
| Delivery strategy | auto first safe slice; ask-always before remaining chain strategy |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

First autonomous slice allowed now: WU1 only. `feature-branch-chain` was selected before WU2 and continues for WU3/WU4.

## Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| WU1 | Foundation, YAML, pure Domain, persistence skeleton | PR 1 | Safe autonomous slice; no endpoint behavior yet. |
| WU2 | Admin create/revoke/status use cases | PR 2 | Depends on WU1; includes authorization and conflicts. |
| WU3 | Bot validate/confirm and locking | PR 3 | Depends on WU2; includes single-use confirmation. |
| WU4 | Web adapters, OpenAPI, final verification | PR 4 | Depends on WU2/WU3; API contract polish. |

## WU1: Foundation / Config / Persistence Skeleton

- [x] 1.1 RED: add domain tests in `src/test/java/com/telegram/ia/telegramlink/domain/**` for invitation states, token secrecy, and role assignment policy.
- [x] 1.2 Create pure Domain models, value objects, statuses, exceptions, and policies in `src/main/java/com/telegram/ia/telegramlink/domain/**`.
- [x] 1.3 Update `pom.xml` and `src/main/resources/application.yaml` for springdoc, datasource/JPA update mode, and `telegram-link.*` YAML properties.
- [x] 1.4 Create JPA entities/repositories/mappers/adapters under `src/main/java/com/telegram/ia/telegramlink/infrastructure/persistence/**` for the seven MVP tables, including invitation lock query.
- [x] 1.5 GREEN: add persistence mapping/hash-prefix tests in `src/test/java/com/telegram/ia/telegramlink/infrastructure/persistence/**`.

## WU2: Admin Application Use Cases

- [x] 2.1 RED: add application tests for create, duplicate conflict, unassigned agent denial, revoke, and status scenarios.
- [x] 2.2 Create commands/responses and inbound/outbound ports under `src/main/java/com/telegram/ia/telegramlink/application/**`.
- [x] 2.3 Implement `CreateTelegramInvitationUseCase`, `RevokeTelegramInvitationUseCase`, and `GetTelegramLinkStatusUseCase` with transaction boundaries and audit ports.

## WU3: Bot Validation / Confirmation

- [x] 3.1 RED: add tests for invalid-token always-200 payloads, valid previews, confirmation success, and concurrent single-use conflict.
- [x] 3.2 Implement token generator/hash/clock adapters in `src/main/java/com/telegram/ia/telegramlink/infrastructure/token/**` and `.../clock/**`.
- [x] 3.3 Implement `ValidateTelegramLinkTokenUseCase` and `ConfirmTelegramLinkUseCase` with lock, account creation, USED transition, and safe audit metadata.

## WU4: Web, OpenAPI, Verification

- [x] 4.1 RED: add MockMvc tests for admin semantic HTTP, bot always-200 contracts, and Swagger exposure.
- [x] 4.2 Create admin/bot controllers, DTOs, mappers, simulated current-user provider, OpenAPI config, and `ProblemDetail` handler under `src/main/java/com/telegram/ia/telegramlink/infrastructure/**`.
- [x] 4.3 Run `./mvnw test` and `./mvnw package`; document MVP table names and production migration gap in final delivery notes.
