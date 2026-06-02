package com.mindpulse.backend.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class ReminderWebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Handle reminder notifications
    @MessageMapping("/remind")
    public void sendReminder(@Payload Map<String, Object> reminderData) {
        // Send reminder to all subscribers of the /topic/reminders topic
        messagingTemplate.convertAndSend("/topic/reminders", reminderData);
    }

    // Handle user-specific reminders
    @MessageMapping("/remind-user")
    public void sendUserReminder(@Payload Map<String, Object> reminderData) {
        String username = (String) reminderData.get("username");
        // Send reminder to a specific user
        messagingTemplate.convertAndSendToUser(username, "/queue/reminders", reminderData);
    }
}