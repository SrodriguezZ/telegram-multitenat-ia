package com.telegram.ia.telegramlink.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientStatus;
import com.telegram.ia.telegramlink.domain.model.CompanyUser;
import com.telegram.ia.telegramlink.domain.model.CompanyUserRole;
import com.telegram.ia.telegramlink.domain.model.CompanyUserStatus;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationPolicy;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationResult;
import com.telegram.ia.telegramlink.domain.policy.InvitationDenialReason;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationAuthorizationPolicyTest {

    private final CompanyId companyId = new CompanyId(UUID.randomUUID());
    private final Client activeClient = new Client(new ClientId(UUID.randomUUID()), companyId, "Jane Doe", ClientStatus.ACTIVE);

    @Test
    void ownerAdminAndSupervisorCanInviteActiveCompanyClientWithoutAssignment() {
        assertThat(InvitationAuthorizationPolicy.canInvite(user(CompanyUserRole.OWNER), activeClient, false).allowed()).isTrue();
        assertThat(InvitationAuthorizationPolicy.canInvite(user(CompanyUserRole.ADMIN), activeClient, false).allowed()).isTrue();
        assertThat(InvitationAuthorizationPolicy.canInvite(user(CompanyUserRole.SUPERVISOR), activeClient, false).allowed()).isTrue();
    }

    @Test
    void agentCanInviteOnlyWhenAssignedToActiveCompanyClient() {
        CompanyUser agent = user(CompanyUserRole.AGENT);

        InvitationAuthorizationResult assigned = InvitationAuthorizationPolicy.canInvite(agent, activeClient, true);
        InvitationAuthorizationResult unassigned = InvitationAuthorizationPolicy.canInvite(agent, activeClient, false);

        assertThat(assigned.allowed()).isTrue();
        assertThat(unassigned.allowed()).isFalse();
        assertThat(unassigned.denialReason()).contains(InvitationDenialReason.CLIENT_NOT_ASSIGNED_TO_AGENT);
    }

    @Test
    void inactiveUserOrClientCannotCreateInvitation() {
        CompanyUser inactiveUser = new CompanyUser(
                new CompanyUserId(UUID.randomUUID()), companyId, CompanyUserRole.ADMIN, CompanyUserStatus.SUSPENDED);
        Client inactiveClient = new Client(activeClient.id(), companyId, activeClient.fullName(), ClientStatus.BLOCKED);

        assertThat(InvitationAuthorizationPolicy.canInvite(inactiveUser, activeClient, false).denialReason())
                .contains(InvitationDenialReason.USER_NOT_ACTIVE);
        assertThat(InvitationAuthorizationPolicy.canInvite(user(CompanyUserRole.ADMIN), inactiveClient, false).denialReason())
                .contains(InvitationDenialReason.CLIENT_NOT_ACTIVE);
    }

    private CompanyUser user(CompanyUserRole role) {
        return new CompanyUser(new CompanyUserId(UUID.randomUUID()), companyId, role, CompanyUserStatus.ACTIVE);
    }
}
