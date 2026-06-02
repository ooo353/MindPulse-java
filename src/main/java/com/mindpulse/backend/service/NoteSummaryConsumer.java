package com.mindpulse.backend.service;

import com.mindpulse.backend.config.RabbitMQConfig;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import com.mindpulse.backend.dto.NoteSummaryResult;
import com.mindpulse.backend.mapper.NoteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 笔记摘要消息消费者，异步调用 AI 生成摘要并推送结果
 */
@Component
public class NoteSummaryConsumer {

    private static final Logger log = LoggerFactory.getLogger(NoteSummaryConsumer.class);

    @Autowired
    private AiAgentClient aiAgentClient;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTE_SUMMARY_QUEUE)
    public void handleSummaryTask(NoteSummaryMessage message) {
        long startTime = System.currentTimeMillis();
        log.info("收到摘要任务: noteId={}, title={}", message.getNoteId(), message.getTitle());

        try {
            // 调用 AI 服务生成摘要
            Map<String, Object> aiResult = aiAgentClient.generateSummary(message.getContent());

            String summary = (String) aiResult.getOrDefault("summary", "");
            String tags = (String) aiResult.getOrDefault("tags", "");
            String category = inferCategory(tags, (String) aiResult.get("title"));

            // 确定最佳标题
            String aiTitle = (String) aiResult.get("title");
            String finalTitle = (aiTitle != null && !aiTitle.isEmpty() && !"Auto-generated Title".equals(aiTitle))
                    ? aiTitle : message.getTitle();

            // 更新数据库
            noteMapper.updateSummaryAndTags(message.getNoteId(), finalTitle, summary, tags, category, "completed");
            log.info("笔记摘要已入库: noteId={}, category={}", message.getNoteId(), category);

            long elapsed = System.currentTimeMillis() - startTime;

            // WebSocket 推送成功结果
            NoteSummaryResult result = new NoteSummaryResult(
                    message.getNoteId(), finalTitle, summary, tags, category,
                    "completed", message.getAuthor(), elapsed
            );
            pushResult(message.getAuthor(), result);

        } catch (Exception e) {
            log.error("摘要处理失败: noteId={}, error={}", message.getNoteId(), e.getMessage());

            // 标记失败状态
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

    /**
     * 根据标签推断分类
     */
    private String inferCategory(String tags, String title) {
        // 基于标签的关键词匹配
        if (tags != null) {
            String lower = tags.toLowerCase();
            if (lower.contains("考试") || lower.contains("复习") || lower.contains("考点")) return "考试复习";
            if (lower.contains("课堂") || lower.contains("上课") || lower.contains("课程")) return "课堂笔记";
            if (lower.contains("读书") || lower.contains("阅读") || lower.contains("书籍")) return "读书笔记";
            if (lower.contains("项目") || lower.contains("实验") || lower.contains("论文")) return "项目研究";
            if (lower.contains("生活") || lower.contains("日常") || lower.contains("备忘")) return "生活记录";
        }
        if (title != null) {
            String lower = title.toLowerCase();
            if (lower.contains("考试") || lower.contains("复习")) return "考试复习";
            if (lower.contains("课堂") || lower.contains("课程")) return "课堂笔记";
            if (lower.contains("读书") || lower.contains("阅读")) return "读书笔记";
        }
        return "通用笔记";
    }

    /**
     * 通过 WebSocket 推送结果到指定用户
     */
    private void pushResult(String username, NoteSummaryResult result) {
        try {
            messagingTemplate.convertAndSendToUser(
                    username, "/queue/note-summary", result
            );
            log.info("WebSocket 推送成功: noteId={}, user={}, status={}",
                    result.getNoteId(), username, result.getStatus());
        } catch (Exception e) {
            log.error("WebSocket 推送失败: noteId={}, error={}", result.getNoteId(), e.getMessage());
        }
    }
}
