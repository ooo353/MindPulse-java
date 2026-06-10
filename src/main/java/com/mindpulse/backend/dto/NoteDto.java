package com.mindpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteDto(
    Long id,

    @NotBlank(message = "Note title is required")
    String title,

    @NotBlank(message = "Note content is required")
    String content,

    String type,
    String fileUrl,
    String tags,
    String summary,
    String category,
    String status,

    @NotBlank(message = "Note author is required")
    String author
) {

    // Constructor with essential fields
    public NoteDto(String title, String content, String author) {
        this(null, title, content, "text", null, null, null, null, "processing", author);
    }
}
