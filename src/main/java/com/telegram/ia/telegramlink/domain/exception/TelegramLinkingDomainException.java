package com.telegram.ia.telegramlink.domain.exception;

public class TelegramLinkingDomainException extends RuntimeException {
    private final String errorCode;

    public TelegramLinkingDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}
