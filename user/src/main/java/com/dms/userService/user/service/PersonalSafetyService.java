package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.SafetyCheckInRequest;
import com.dms.userService.user.dto.request.SosAlertRequest;
import com.dms.userService.user.dto.request.TrustedContactRequest;
import com.dms.userService.user.dto.response.SafetyCheckInResponse;
import com.dms.userService.user.dto.response.SosAlertResponse;
import com.dms.userService.user.dto.response.TrustedContactResponse;

import java.util.List;
import java.util.UUID;

public interface PersonalSafetyService {

    // Trusted Contacts
    List<TrustedContactResponse> getTrustedContacts(UUID userId);
    TrustedContactResponse addTrustedContact(UUID userId, TrustedContactRequest request);
    TrustedContactResponse updateTrustedContact(UUID userId, UUID contactId, TrustedContactRequest request);
    void deleteTrustedContact(UUID userId, UUID contactId);

    // "I'm Safe" Check-In
    SafetyCheckInResponse recordCheckIn(UUID userId, SafetyCheckInRequest request);
    SafetyCheckInResponse getLatestCheckIn(UUID userId);
    List<SafetyCheckInResponse> getCheckInHistory(UUID userId);

    // SOS Quick Alert
    SosAlertResponse triggerSosAlert(UUID userId, SosAlertRequest request);
    SosAlertResponse getLatestSosAlert(UUID userId);
    List<SosAlertResponse> getSosAlertHistory(UUID userId);
}
