package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * RabbitMQ 笔记摘要处理消息体
 */
@Schema(description = "笔记摘要异步处理消息")
public class NoteSummaryMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "笔记内容")
    private String content;

    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记作者")
    private String author;

    public NoteSummaryMessage() {}

    public NoteSummaryMessage(Long noteId, String content, String title, String author) {
        this.noteId = noteId;
        this.content = content;
        this.title = title;
        this.author = author;
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

    @Override
    public String toString() {
        return "NoteSummaryMessage{noteId=" + noteId + ", title='" + title + "', author='" + author + "'}";
    }
}
