package com.telegram.ia.telegramlink.infrastructure.web.admin;

import com.telegram.ia.telegramlink.application.command.CreateTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.command.RevokeTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.port.in.CreateTelegramInvitationPort;
import com.telegram.ia.telegramlink.application.port.in.GetTelegramLinkStatusPort;
import com.telegram.ia.telegramlink.application.port.in.RevokeTelegramInvitationPort;
import com.telegram.ia.telegramlink.application.query.GetTelegramLinkStatusQuery;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.infrastructure.web.mapper.TelegramLinkWebMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/telegram-link")
public class AdminTelegramLinkController {
    private final CreateTelegramInvitationPort createInvitationPort;
    private final RevokeTelegramInvitationPort revokeInvitationPort;
    private final GetTelegramLinkStatusPort getStatusPort;

    public AdminTelegramLinkController(
            CreateTelegramInvitationPort createInvitationPort,
            RevokeTelegramInvitationPort revokeInvitationPort,
            GetTelegramLinkStatusPort getStatusPort) {
        this.createInvitationPort = createInvitationPort;
        this.revokeInvitationPort = revokeInvitationPort;
        this.getStatusPort = getStatusPort;
    }

    @PostMapping("/invitations")
    public ResponseEntity<TelegramInvitationCreatedHttpResponse> createInvitation(@Valid @RequestBody CreateTelegramInvitationHttpRequest request) {
        var response = createInvitationPort.execute(new CreateTelegramInvitationCommand(new ClientId(request.clientId())));
        return ResponseEntity.created(URI.create("/api/v1/admin/telegram-link/invitations/" + response.invitationId().value()))
                .body(TelegramLinkWebMapper.toHttp(response));
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<TelegramInvitationRevokedHttpResponse> revokeInvitation(@PathVariable UUID invitationId) {
        var response = revokeInvitationPort.execute(new RevokeTelegramInvitationCommand(new InvitationTokenId(invitationId)));
        return ResponseEntity.ok(TelegramLinkWebMapper.toHttp(response));
    }

    @GetMapping("/clients/{clientId}/status")
    public ResponseEntity<TelegramLinkStatusHttpResponse> getStatus(@PathVariable UUID clientId) {
        var response = getStatusPort.execute(new GetTelegramLinkStatusQuery(new ClientId(clientId)));
        return ResponseEntity.ok(TelegramLinkWebMapper.toHttp(response));
    }
}
