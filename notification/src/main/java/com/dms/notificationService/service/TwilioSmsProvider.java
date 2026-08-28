package com.dms.notificationService.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsProvider implements SmsProvider {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio SDK Initialized for Production SMS");
    }

    @Override
    public void sendSms(String toPhoneNumber, String text) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    text
            ).create();
            log.info("Twilio SMS sent successfully. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Twilio delivery failed: {}", e.getMessage());
        }
    }
}