package com.mindpulse.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReminderWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    // Handle reminder notifications
    @MessageMapping("/remind")
    public void sendReminder(@Payload Map<String, Object> reminderData) {
        messagingTemplate.convertAndSend("/topic/reminders", reminderData);
    }

    // TODO: Extract JWT authentication from STOMP session headers during the WebSocket
    //  handshake instead of trusting the username sent by the client. The current approach
    //  allows any connected client to impersonate any user. Proper implementation requires:
    //  1. Extract the "Authorization" header from the STOMP CONNECT frame in WebSocketConfig
    //  2. Validate the JWT token and resolve the authenticated user
    //  3. Store the authenticated Principal in the WebSocket session
    //  4. Use the Principal here instead of reading "username" from the message payload

    @MessageMapping("/remind-user")
    public void sendUserReminder(@Payload Map<String, Object> reminderData) {
        String username = (String) reminderData.get("username");
        messagingTemplate.convertAndSendToUser(username, "/queue/reminders", reminderData);
    }
}
