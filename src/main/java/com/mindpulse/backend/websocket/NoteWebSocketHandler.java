package com.mindpulse.backend.websocket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * 笔记摘要 WebSocket 处理器，负责前端订阅和结果推送
 */
@Controller
@Tag(name = "笔记摘要 WebSocket", description = "笔记摘要处理结果实时推送")
public class NoteWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NoteWebSocketHandler.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 前端订阅笔记摘要结果，发起到用户队列的连接
     * 客户端发送消息到 /app/note-subscribe 后，
     * 服务端会向 /user/queue/note-summary 推送摘要结果
     */
    @Operation(summary = "订阅笔记摘要结果", description = "客户端订阅后将实时接收笔记摘要处理完成的通知")
    @MessageMapping("/note-subscribe")
    public void subscribeNoteSummary(@Payload Map<String, Object> subscribeData) {
        String username = (String) subscribeData.get("username");
        log.info("用户 [{}] 已订阅笔记摘要推送", username);

        // 发送确认消息
        messagingTemplate.convertAndSendToUser(username, "/queue/note-summary",
                Map.of("type", "subscribed", "message", "笔记摘要推送通道已建立", "username", username));
    }

    /**
     * 主动向指定用户推送笔记摘要结果
     */
    public void pushNoteSummary(String username, Object result) {
        messagingTemplate.convertAndSendToUser(username, "/queue/note-summary", result);
        log.info("笔记摘要已推送至用户: {}", username);
    }
}
