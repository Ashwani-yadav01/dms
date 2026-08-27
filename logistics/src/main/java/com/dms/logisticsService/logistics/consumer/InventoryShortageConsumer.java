package com.dms.logisticsService.logistics.consumer;

import com.dms.logisticsService.logistics.dto.event.InventoryShortageAlertEvent;
import com.dms.logisticsService.logistics.service.SupplyDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryShortageConsumer {

    private final SupplyDispatchService supplyDispatchService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "inventory-shortage-topic",
            groupId = "logistics-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShortageAlert(@Payload InventoryShortageAlertEvent event) {
        log.info("Received InventoryShortageAlertEvent from Kafka: {}", event);
        supplyDispatchService.processShortageAlert(event);
    }

    @DltHandler
    public void handleDlt(
            @Payload InventoryShortageAlertEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("CRITICAL ERROR: Message routed to Dead Letter Topic [{}] at offset {}. Payload: {}",
                topic, offset, event);
        // Alert ops or persist into a quarantined_events table for investigation
    }
}