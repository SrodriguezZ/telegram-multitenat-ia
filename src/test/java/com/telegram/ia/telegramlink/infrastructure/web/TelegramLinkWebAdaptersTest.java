package com.telegram.ia.telegramlink.infrastructure.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.ClientJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyUserJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramInvitationTokenJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.ClientJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.CompanyUserJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.TelegramInvitationTokenJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelegramLinkWebAdaptersTest {
    private static final UUID COMPANY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID CLIENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID OTHER_CLIENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000005");
    private static final UUID INVITATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000006");
    private static final UUID OTHER_INVITATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000007");
    private static final Instant NOW = Instant.parse("2026-06-14T12:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired CompanyJpaRepository companyRepository;
    @Autowired CompanyUserJpaRepository companyUserRepository;
    @Autowired ClientJpaRepository clientRepository;
    @Autowired TelegramInvitationTokenJpaRepository invitationRepository;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        clientRepository.deleteAll();
        companyUserRepository.deleteAll();
        companyRepository.deleteAll();
        companyRepository.save(new CompanyJpaEntity(COMPANY_ID, "Main Company", null, "ACTIVE", NOW, NOW));
        companyRepository.save(new CompanyJpaEntity(OTHER_COMPANY_ID, "Other Company", null, "ACTIVE", NOW, NOW));
        companyUserRepository.save(new CompanyUserJpaEntity(ADMIN_ID, COMPANY_ID, "admin@example.com", "Admin", "ADMIN", "ACTIVE", NOW, NOW));
        clientRepository.save(new ClientJpaEntity(CLIENT_ID, COMPANY_ID, null, null, null, "Jane Doe", null, null, "ACTIVE", NOW, NOW));
        clientRepository.save(new ClientJpaEntity(OTHER_CLIENT_ID, OTHER_COMPANY_ID, null, null, null, "Other Client", null, null, "ACTIVE", NOW, NOW));
    }

    @Test
    void adminDuplicateInvitationReturnsHttpConflictProblemDetail() throws Exception {
        invitationRepository.save(new TelegramInvitationTokenJpaEntity(
                INVITATION_ID, COMPANY_ID, CLIENT_ID, ADMIN_ID, "existing-hash", "exis", "PENDING",
                NOW.plusSeconds(172_800), null, null, null, null, NOW, NOW));

        mockMvc.perform(post("/api/v1/admin/telegram-link/invitations")
                        .headers(currentUserHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("INVITATION_ALREADY_PENDING")))
                .andExpect(jsonPath("$.title", is("Telegram linking request failed")));
    }

    @Test
    void botValidationInvalidTokenAlwaysReturnsOkPayload() throws Exception {
        mockMvc.perform(post("/api/v1/bot/telegram-link/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"missing-token\",\"telegramUserId\":777001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INVALID")))
                .andExpect(jsonPath("$.errorCode", is("INVALID_TOKEN")))
                .andExpect(jsonPath("$.confirmationRequired", is(false)));
    }

    @Test
    void crossCompanyRevokeIsRejectedWithoutChangingOtherCompanyInvitation() throws Exception {
        invitationRepository.save(new TelegramInvitationTokenJpaEntity(
                OTHER_INVITATION_ID, OTHER_COMPANY_ID, OTHER_CLIENT_ID, ADMIN_ID, "other-hash", "othe", "PENDING",
                NOW.plusSeconds(172_800), null, null, null, null, NOW, NOW));

        mockMvc.perform(delete("/api/v1/admin/telegram-link/invitations/{invitationId}", OTHER_INVITATION_ID)
                        .headers(currentUserHeaders()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("INVITATION_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/admin/telegram-link/clients/{clientId}/status", CLIENT_ID)
                        .headers(currentUserHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOT_LINKED")));
    }

    @Test
    void crossCompanyCreateReturnsNotFoundWithoutLeakingOtherCompanyClient() throws Exception {
        mockMvc.perform(post("/api/v1/admin/telegram-link/invitations")
                        .headers(currentUserHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + OTHER_CLIENT_ID + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("CLIENT_NOT_FOUND")));
    }

    @Test
    void clientHeadersCannotSpoofConfiguredCurrentUserIdentity() throws Exception {
        org.springframework.http.HttpHeaders spoofedHeaders = new org.springframework.http.HttpHeaders();
        spoofedHeaders.add("X-Company-User-Id", UUID.fromString("20000000-0000-0000-0000-000000000099").toString());
        spoofedHeaders.add("X-Company-Id", OTHER_COMPANY_ID.toString());
        spoofedHeaders.add("X-Company-User-Role", "AGENT");
        spoofedHeaders.add("X-Company-User-Status", "SUSPENDED");

        mockMvc.perform(post("/api/v1/admin/telegram-link/invitations")
                        .headers(spoofedHeaders)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void swaggerOpenApiExposesTelegramLinkingEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Telegram IA API")))
                .andExpect(jsonPath("$.paths['/api/v1/bot/telegram-link/validate']").exists());
    }

    @Test
    void internalTechnicalDesignDocumentsAreNotServedAsStaticResources() throws Exception {
        mockMvc.perform(get("/telegram-backend-technical-design.md"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.http.HttpHeaders currentUserHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Company-User-Id", ADMIN_ID.toString());
        headers.add("X-Company-Id", COMPANY_ID.toString());
        headers.add("X-Company-User-Role", "ADMIN");
        headers.add("X-Company-User-Status", "ACTIVE");
        return headers;
    }
}
