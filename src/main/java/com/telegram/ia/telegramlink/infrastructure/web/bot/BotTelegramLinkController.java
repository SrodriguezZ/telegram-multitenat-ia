package com.telegram.ia.telegramlink.infrastructure.web.bot;

import com.telegram.ia.telegramlink.application.command.ConfirmTelegramLinkCommand;
import com.telegram.ia.telegramlink.application.command.ValidateTelegramLinkTokenCommand;
import com.telegram.ia.telegramlink.application.port.in.ConfirmTelegramLinkPort;
import com.telegram.ia.telegramlink.application.port.in.ValidateTelegramLinkTokenPort;
import com.telegram.ia.telegramlink.domain.model.TelegramProfileSnapshot;
import com.telegram.ia.telegramlink.infrastructure.web.mapper.TelegramLinkWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bot/telegram-link")
public class BotTelegramLinkController {
    private final ValidateTelegramLinkTokenPort validateTokenPort;
    private final ConfirmTelegramLinkPort confirmLinkPort;

    public BotTelegramLinkController(ValidateTelegramLinkTokenPort validateTokenPort, ConfirmTelegramLinkPort confirmLinkPort) {
        this.validateTokenPort = validateTokenPort;
        this.confirmLinkPort = confirmLinkPort;
    }

    @PostMapping("/validate")
    public ResponseEntity<TelegramLinkTokenValidationHttpResponse> validate(@Valid @RequestBody ValidateTelegramLinkTokenHttpRequest request) {
        var response = validateTokenPort.execute(new ValidateTelegramLinkTokenCommand(request.token(), request.telegramUserId()));
        return ResponseEntity.ok(TelegramLinkWebMapper.toHttp(response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmTelegramLinkHttpResponse> confirm(@Valid @RequestBody ConfirmTelegramLinkHttpRequest request) {
        var response = confirmLinkPort.execute(new ConfirmTelegramLinkCommand(request.token(), new TelegramProfileSnapshot(
                request.telegramUserId(), request.telegramChatId(), request.telegramUsername(), request.telegramFirstName(), request.telegramLastName())));
        return ResponseEntity.ok(TelegramLinkWebMapper.toHttp(response));
    }
}
