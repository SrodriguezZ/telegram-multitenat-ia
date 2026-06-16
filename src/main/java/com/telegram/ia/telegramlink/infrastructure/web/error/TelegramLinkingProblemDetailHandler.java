package com.telegram.ia.telegramlink.infrastructure.web.error;

import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TelegramLinkingProblemDetailHandler {
    @ExceptionHandler(TelegramLinkingApplicationException.class)
    ResponseEntity<ProblemDetail> handleApplicationException(TelegramLinkingApplicationException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception.errorCode());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Telegram linking request failed");
        problem.setProperty("errorCode", exception.errorCode());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request is invalid");
        problem.setTitle("Telegram linking request failed");
        problem.setProperty("errorCode", "INVALID_REQUEST");
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    private HttpStatus statusFor(String errorCode) {
        return switch (errorCode) {
            case "CLIENT_NOT_ASSIGNED_TO_AGENT", "USER_NOT_ACTIVE", "CURRENT_USER_NOT_AVAILABLE" -> HttpStatus.FORBIDDEN;
            case "CLIENT_NOT_FOUND", "INVITATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CLIENT_ALREADY_LINKED", "INVITATION_ALREADY_PENDING" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
