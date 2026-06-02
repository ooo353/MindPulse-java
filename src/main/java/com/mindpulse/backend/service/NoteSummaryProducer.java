package com.mindpulse.backend.service;

import com.mindpulse.backend.config.RabbitMQConfig;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 笔记摘要消息生产者，将处理任务投递至 RabbitMQ
 */
@Service
public class NoteSummaryProducer {

    private static final Logger log = LoggerFactory.getLogger(NoteSummaryProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 投递笔记摘要处理任务到消息队列
     */
    public void sendSummaryTask(NoteSummaryMessage message) {
        log.info("投递摘要任务到队列: noteId={}, title={}", message.getNoteId(), message.getTitle());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTE_SUMMARY_EXCHANGE,
                RabbitMQConfig.NOTE_SUMMARY_ROUTING_KEY,
                message
        );
        log.info("摘要任务已投递: noteId={}", message.getNoteId());
    }
}
