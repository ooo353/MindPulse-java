package com.mindpulse.backend.dto;

import java.time.LocalDateTime;

public record NoteDto(
    Long id,
    String title,
    String content,
    String type, // pdf/image/text
    String fileUrl,
    String tags, // Comma-separated tags
    String summary, // AI-generated summary
    String category, // AI-recommended category
    String status, // processing/completed/failed
    String author
) {

    // Constructor with essential fields
    public NoteDto(String title, String content, String author) {
        this(null, title, content, "text", null, null, null, null, "processing", author);
    }
}