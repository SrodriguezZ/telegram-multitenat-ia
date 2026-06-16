package com.telegram.ia.telegramlink.domain.policy;

import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.CompanyUser;
import com.telegram.ia.telegramlink.domain.model.CompanyUserRole;

public final class InvitationAuthorizationPolicy {
    private InvitationAuthorizationPolicy() {}

    public static InvitationAuthorizationResult canInvite(CompanyUser user, Client client, boolean clientAssignedToAgent) {
        if (!user.active()) return InvitationAuthorizationResult.denied(InvitationDenialReason.USER_NOT_ACTIVE);
        if (!client.active()) return InvitationAuthorizationResult.denied(InvitationDenialReason.CLIENT_NOT_ACTIVE);
        if (!user.companyId().equals(client.companyId())) return InvitationAuthorizationResult.denied(InvitationDenialReason.CROSS_COMPANY_ACCESS);
        if (user.managementRole()) return InvitationAuthorizationResult.permit();
        if (user.role() == CompanyUserRole.AGENT && clientAssignedToAgent) return InvitationAuthorizationResult.permit();
        if (user.role() == CompanyUserRole.AGENT) return InvitationAuthorizationResult.denied(InvitationDenialReason.CLIENT_NOT_ASSIGNED_TO_AGENT);
        return InvitationAuthorizationResult.denied(InvitationDenialReason.ROLE_NOT_ALLOWED);
    }
}
