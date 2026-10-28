package com.codabli.controller;

import com.codabli.dto.MediaUploadResponse;
import com.codabli.service.MediaLifecycleService;
import com.codabli.service.MediaUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaUploadService mediaUploadService;
    private final MediaLifecycleService mediaLifecycleService;

    public MediaController(
            MediaUploadService mediaUploadService,
            MediaLifecycleService mediaLifecycleService) {
        this.mediaUploadService = mediaUploadService;
        this.mediaLifecycleService = mediaLifecycleService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('admin', 'super_admin', 'comite_lecture')")
    public ResponseEntity<MediaUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaUploadService.upload(file));
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> delete(@PathVariable UUID mediaId) {
        mediaLifecycleService.requestDeletion(mediaId);
        return ResponseEntity.accepted().build();
    }
}
