package com.codabli.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = MediaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MediaUploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaximumUploadSize(
            MaxUploadSizeExceededException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.CONTENT_TOO_LARGE.value());
        body.put("error", "Content Too Large");
        body.put("error-code", "error.media.too-large");
        body.put("message", "Le fichier dépasse la taille maximale autorisée.");
        body.put("path", "/api/media");
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(body);
    }
}
