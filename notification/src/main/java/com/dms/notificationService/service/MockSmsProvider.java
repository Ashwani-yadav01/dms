package com.dms.notificationService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {
    @Override
    public void sendSms(String toPhoneNumber, String text) {
        log.info("========================================");
        log.info("📱 MOCK SMS DISPATCHED");
        log.info("To: {}", toPhoneNumber);
        log.info("Message: {}", text);
        log.info("========================================");
    }
}