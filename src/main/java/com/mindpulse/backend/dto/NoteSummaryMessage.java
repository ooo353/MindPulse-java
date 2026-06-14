package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Note summary async processing message")
public class NoteSummaryMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Note ID")
    private Long noteId;

    @Schema(description = "Note content")
    private String content;

    @Schema(description = "Note title")
    private String title;

    @Schema(description = "Note author")
    private String author;

    @Schema(description = "Note file URL")
    private String fileUrl;

    public NoteSummaryMessage() {}

    public NoteSummaryMessage(Long noteId, String content, String title, String author, String fileUrl) {
        this.noteId = noteId;
        this.content = content;
        this.title = title;
        this.author = author;
        this.fileUrl = fileUrl;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    public String toString() {
        return "NoteSummaryMessage{noteId=" + noteId + ", title='" + title + "', author='" + author + "'}";
    }
}
