package com.dms.incident.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Value("${application.kafka.topic.incident-created}")
    private String incidentCreatedTopic;

    @Bean
    public NewTopic incidentCreatedTopic() {
        return TopicBuilder.name(incidentCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}