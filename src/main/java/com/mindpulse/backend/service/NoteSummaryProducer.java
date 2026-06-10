package com.mindpulse.backend.service;

import com.mindpulse.backend.config.RabbitMQConfig;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSummaryProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSummaryTask(NoteSummaryMessage message) {
        log.info("Submitting summary task to queue: noteId={}, title={}", message.getNoteId(), message.getTitle());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTE_SUMMARY_EXCHANGE,
                RabbitMQConfig.NOTE_SUMMARY_ROUTING_KEY,
                message
        );
        log.info("Summary task submitted: noteId={}", message.getNoteId());
    }
}
