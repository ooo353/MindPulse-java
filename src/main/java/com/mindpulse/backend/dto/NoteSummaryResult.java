package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Note summary processing result pushed via WebSocket")
public class NoteSummaryResult {

    @Schema(description = "Note ID")
    private Long noteId;

    @Schema(description = "Note title")
    private String title;

    @Schema(description = "AI-generated summary")
    private String summary;

    @Schema(description = "AI-recommended tags")
    private String tags;

    @Schema(description = "AI-recommended category")
    private String category;

    @Schema(description = "Processing status: completed/failed")
    private String status;

    @Schema(description = "Owner username")
    private String author;

    @Schema(description = "Processing time in milliseconds")
    private long processingTimeMs;

    public NoteSummaryResult() {}

    public NoteSummaryResult(Long noteId, String title, String summary, String tags,
                             String category, String status, String author, long processingTimeMs) {
        this.noteId = noteId;
        this.title = title;
        this.summary = summary;
        this.tags = tags;
        this.category = category;
        this.status = status;
        this.author = author;
        this.processingTimeMs = processingTimeMs;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}
