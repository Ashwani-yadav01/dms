package com.dms.notificationService.service;

public interface SmsProvider {
    void sendSms(String toPhoneNumber, String text);
}