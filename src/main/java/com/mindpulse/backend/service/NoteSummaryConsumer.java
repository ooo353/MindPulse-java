package com.mindpulse.backend.service;

import com.mindpulse.backend.config.RabbitMQConfig;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import com.mindpulse.backend.dto.NoteSummaryResult;
import com.mindpulse.backend.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteSummaryConsumer {

    private final AiAgentClient aiAgentClient;
    private final NoteMapper noteMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTE_SUMMARY_QUEUE)
    public void handleSummaryTask(NoteSummaryMessage message) {
        long startTime = System.currentTimeMillis();
        log.info("Received summary task: noteId={}, title={}", message.getNoteId(), message.getTitle());

        try {
            String contentForSummary = message.getContent();
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                String fileContent = readFileContent(message.getFileUrl());
                if (fileContent != null && !fileContent.isEmpty()) {
                    contentForSummary = (contentForSummary != null ? contentForSummary : "")
                            + "\n\n[File Content]\n" + fileContent;
                }
            }
            Map<String, Object> aiResult = aiAgentClient.generateSummary(contentForSummary);

            String summary = (String) aiResult.getOrDefault("summary", "");
            String tags = (String) aiResult.getOrDefault("tags", "");
            String category = inferCategory(tags, (String) aiResult.get("title"));

            String aiTitle = (String) aiResult.get("title");
            String finalTitle = (aiTitle != null && !aiTitle.isEmpty() && !"Auto-generated Title".equals(aiTitle))
                    ? aiTitle : message.getTitle();

            noteMapper.updateSummaryAndTags(message.getNoteId(), finalTitle, summary, tags, category, "completed");
            log.info("Note summary saved to database: noteId={}, category={}", message.getNoteId(), category);

            long elapsed = System.currentTimeMillis() - startTime;

            NoteSummaryResult result = new NoteSummaryResult(
                    message.getNoteId(), finalTitle, summary, tags, category,
                    "completed", message.getAuthor(), elapsed
            );
            pushResult(message.getAuthor(), result);

        } catch (Exception e) {
            log.error("Summary processing failed: noteId={}, error={}", message.getNoteId(), e.getMessage());

            noteMapper.updateSummaryAndTags(message.getNoteId(), message.getTitle(),
                    null, null, null, "failed");

            long elapsed = System.currentTimeMillis() - startTime;
            NoteSummaryResult result = new NoteSummaryResult(
                    message.getNoteId(), message.getTitle(), null, null, null,
                    "failed", message.getAuthor(), elapsed
            );
            pushResult(message.getAuthor(), result);
        }
    }

    private String inferCategory(String tags, String title) {
        if (tags != null) {
            String lower = tags.toLowerCase();
            if (lower.contains("exam") || lower.contains("review") || lower.contains("test")) return "Exam Review";
            if (lower.contains("class") || lower.contains("lecture") || lower.contains("course")) return "Class Notes";
            if (lower.contains("reading") || lower.contains("book")) return "Reading Notes";
            if (lower.contains("project") || lower.contains("lab") || lower.contains("paper")) return "Project Research";
            if (lower.contains("life") || lower.contains("daily") || lower.contains("memo")) return "Life Record";
        }
        if (title != null) {
            String lower = title.toLowerCase();
            if (lower.contains("exam") || lower.contains("review")) return "Exam Review";
            if (lower.contains("class") || lower.contains("lecture")) return "Class Notes";
            if (lower.contains("reading") || lower.contains("book")) return "Reading Notes";
        }
        return "General Notes";
    }

    private String readFileContent(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("File does not exist: {}", filePath);
                return null;
            }
            if (file.length() > 10 * 1024 * 1024) {
                log.warn("File too large to read for summary: {} ({} bytes)", filePath, file.length());
                return null;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (content.length() > 50000) {
                content = content.substring(0, 50000);
                log.info("File content truncated to 50000 chars for summary: {}", filePath);
            }
            return content;
        } catch (IOException e) {
            log.warn("Failed to read file content for summary: {}, error: {}", filePath, e.getMessage());
            return null;
        }
    }

    private void pushResult(String username, NoteSummaryResult result) {
        try {
            messagingTemplate.convertAndSendToUser(
                    username, "/queue/note-summary", result
            );
            log.info("WebSocket push successful: noteId={}, user={}, status={}",
                    result.getNoteId(), username, result.getStatus());
        } catch (Exception e) {
            log.error("WebSocket push failed: noteId={}, error={}", result.getNoteId(), e.getMessage());
        }
    }
}
