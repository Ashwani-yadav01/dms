package com.dms.hospitalService.hospital.config;

import com.dms.hospitalService.hospital.kafka.event.IncidentCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;


    // ==========================================
    // COMMON CONSUMER PROPERTIES
    // ==========================================

    private Map<String, Object> consumerProperties() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return props;
    }


    // ==========================================
    // OBJECT MAPPER
    // ==========================================

    private ObjectMapper objectMapper() {

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        return objectMapper;
    }


    // ==========================================
    // INCIDENT CREATED CONSUMER
    // ==========================================

    @Bean
    public ConsumerFactory<String, IncidentCreatedEvent>
    incidentCreatedConsumerFactory() {

        JsonDeserializer<IncidentCreatedEvent> jsonDeserializer =
                new JsonDeserializer<>(
                        IncidentCreatedEvent.class,
                        objectMapper()
                );

        jsonDeserializer.addTrustedPackages(
                "com.dms.hospitalService.hospital.kafka.event"
        );

        // Producer does NOT send Java type headers
        jsonDeserializer.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<IncidentCreatedEvent>
                errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }


    // ==========================================
    // INCIDENT CREATED LISTENER FACTORY
    // ==========================================

    @Bean(name = "incidentCreatedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<
            String,
            IncidentCreatedEvent
            > incidentCreatedKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<
                String,
                IncidentCreatedEvent
                > factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                incidentCreatedConsumerFactory()
        );

        return factory;
    }
}