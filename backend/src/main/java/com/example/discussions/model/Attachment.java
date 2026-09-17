package com.example.discussions.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id public UUID id;
    @Column(nullable = false, length = 255) public String originalName;
    @Column(nullable = false, length = 127) public String contentType;
    @Column(nullable = false) public long size;
    @Column(nullable = false, columnDefinition = "bytea") public byte[] data;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) public User uploader;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
