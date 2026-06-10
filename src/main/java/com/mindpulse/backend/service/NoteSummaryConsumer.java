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
            Map<String, Object> aiResult = aiAgentClient.generateSummary(message.getContent());

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
