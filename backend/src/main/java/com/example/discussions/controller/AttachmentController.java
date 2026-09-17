package com.example.discussions.controller;

import com.example.discussions.exception.ApiException;
import com.example.discussions.model.Attachment;
import com.example.discussions.repository.AttachmentRepository;
import com.example.discussions.service.CurrentUser;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {
    private static final long MAX_SIZE = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private final AttachmentRepository attachments;
    private final CurrentUser current;

    public AttachmentController(AttachmentRepository attachments, CurrentUser current) {
        this.attachments = attachments;
        this.current = current;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (file.isEmpty() || contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Можно загружать только изображения PNG, JPEG, GIF или WebP");
        }
        if (file.getSize() > MAX_SIZE) throw ApiException.badRequest("Размер изображения не должен превышать 8 МБ");
        var attachment = new Attachment();
        attachment.id = UUID.randomUUID();
        attachment.originalName = safeName(file.getOriginalFilename());
        attachment.contentType = contentType;
        attachment.size = file.getSize();
        attachment.data = file.getBytes();
        attachment.uploader = current.required();
        attachments.save(attachment);
        return Map.of("url", "/api/attachments/" + attachment.id, "name", attachment.originalName);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var attachment = attachments.findById(id).orElseThrow(() -> ApiException.notFound("Изображение не найдено"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(attachment.contentType))
                .contentLength(attachment.size)
                .body(attachment.data);
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "image";
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
    }
}
