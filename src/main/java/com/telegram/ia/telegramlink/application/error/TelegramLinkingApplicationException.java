package com.telegram.ia.telegramlink.application.error;

public class TelegramLinkingApplicationException extends RuntimeException {
    private final String errorCode;

    public TelegramLinkingApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
