package com.telegram.ia.telegramlink.infrastructure.web.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalDocumentationGuardController {
    @GetMapping({"/telegram-backend-technical-design.md", "/telegram-backend-data-model.md"})
    public ResponseEntity<Void> blockInternalDocumentation() {
        return ResponseEntity.notFound().build();
    }
}
