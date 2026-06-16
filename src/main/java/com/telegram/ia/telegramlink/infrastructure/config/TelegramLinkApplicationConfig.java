package com.telegram.ia.telegramlink.infrastructure.config;

import com.telegram.ia.telegramlink.application.port.out.ClientAssignmentRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.CurrentUserProviderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramAccountIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationLinkBuilderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenHashingPort;
import com.telegram.ia.telegramlink.application.port.out.TransactionRunnerPort;
import com.telegram.ia.telegramlink.application.usecase.ConfirmTelegramLinkUseCase;
import com.telegram.ia.telegramlink.application.usecase.CreateTelegramInvitationUseCase;
import com.telegram.ia.telegramlink.application.usecase.GetTelegramLinkStatusUseCase;
import com.telegram.ia.telegramlink.application.usecase.RevokeTelegramInvitationUseCase;
import com.telegram.ia.telegramlink.application.usecase.ValidateTelegramLinkTokenUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramLinkApplicationConfig {
    @Bean
    CreateTelegramInvitationUseCase createTelegramInvitationUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenGeneratorPort tokenGenerator,
            TokenHashingPort tokenHashing,
            TelegramInvitationIdGeneratorPort invitationIdGenerator,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationLinkBuilderPort linkBuilder,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        return new CreateTelegramInvitationUseCase(currentUserProvider, clientRepository, assignmentRepository, invitationRepository,
                accountRepository, tokenGenerator, tokenHashing, invitationIdGenerator, eventIdGenerator, linkBuilder, auditPort, clock, transactionRunner);
    }

    @Bean
    RevokeTelegramInvitationUseCase revokeTelegramInvitationUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        return new RevokeTelegramInvitationUseCase(currentUserProvider, clientRepository, assignmentRepository, invitationRepository,
                eventIdGenerator, auditPort, clock, transactionRunner);
    }

    @Bean
    GetTelegramLinkStatusUseCase getTelegramLinkStatusUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            ClockPort clock) {
        return new GetTelegramLinkStatusUseCase(currentUserProvider, clientRepository, assignmentRepository, invitationRepository, accountRepository, clock);
    }

    @Bean
    ValidateTelegramLinkTokenUseCase validateTelegramLinkTokenUseCase(
            TelegramInvitationRepositoryPort invitationRepository,
            ClientRepositoryPort clientRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenHashingPort tokenHashing,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock) {
        return new ValidateTelegramLinkTokenUseCase(invitationRepository, clientRepository, accountRepository, tokenHashing, eventIdGenerator, auditPort, clock);
    }

    @Bean
    ConfirmTelegramLinkUseCase confirmTelegramLinkUseCase(
            TelegramInvitationRepositoryPort invitationRepository,
            ClientRepositoryPort clientRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenHashingPort tokenHashing,
            TelegramAccountIdGeneratorPort accountIdGenerator,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        return new ConfirmTelegramLinkUseCase(invitationRepository, clientRepository, accountRepository, tokenHashing,
                accountIdGenerator, eventIdGenerator, auditPort, clock, transactionRunner);
    }
}
