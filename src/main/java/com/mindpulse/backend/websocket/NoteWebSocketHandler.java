package com.mindpulse.backend.websocket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@Tag(name = "Note Summary WebSocket", description = "Real-time push for note summary processing results")
@RequiredArgsConstructor
public class NoteWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client subscribes to note summary results via /app/note-subscribe.
     * Server pushes summary results to /user/queue/note-summary.
     */
    @Operation(summary = "Subscribe to note summary results", description = "Client receives real-time notifications when note summary processing completes")
    @MessageMapping("/note-subscribe")
    public void subscribeNoteSummary(@Payload Map<String, Object> subscribeData) {
        String username = (String) subscribeData.get("username");
        log.info("User [{}] subscribed to note summary push", username);

        messagingTemplate.convertAndSendToUser(username, "/queue/note-summary",
                Map.of("type", "subscribed", "message", "Note summary push channel established", "username", username));
    }

    /**
     * Push note summary result to a specific user
     */
    public void pushNoteSummary(String username, Object result) {
        messagingTemplate.convertAndSendToUser(username, "/queue/note-summary", result);
        log.info("Note summary pushed to user: {}", username);
    }
}
