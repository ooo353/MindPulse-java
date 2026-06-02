package com.mindpulse.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTE_SUMMARY_QUEUE = "note.summary.queue";
    public static final String NOTE_SUMMARY_EXCHANGE = "note.summary.exchange";
    public static final String NOTE_SUMMARY_ROUTING_KEY = "note.summary";

    @Bean
    public Queue noteSummaryQueue() {
        return QueueBuilder.durable(NOTE_SUMMARY_QUEUE).build();
    }

    @Bean
    public DirectExchange noteSummaryExchange() {
        return new DirectExchange(NOTE_SUMMARY_EXCHANGE);
    }

    @Bean
    public Binding noteSummaryBinding() {
        return BindingBuilder.bind(noteSummaryQueue())
                .to(noteSummaryExchange())
                .with(NOTE_SUMMARY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
