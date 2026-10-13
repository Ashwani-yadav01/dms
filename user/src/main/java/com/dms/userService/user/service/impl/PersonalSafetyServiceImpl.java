package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.SafetyCheckInRequest;
import com.dms.userService.user.dto.request.SosAlertRequest;
import com.dms.userService.user.dto.request.TrustedContactRequest;
import com.dms.userService.user.dto.response.SafetyCheckInResponse;
import com.dms.userService.user.dto.response.SosAlertResponse;
import com.dms.userService.user.dto.response.TrustedContactResponse;
import com.dms.userService.user.entity.SafetyCheckIn;
import com.dms.userService.user.entity.SosAlert;
import com.dms.userService.user.entity.TrustedContact;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.exception.ResourceNotFoundException;
import com.dms.userService.user.repository.SafetyCheckInRepository;
import com.dms.userService.user.repository.SosAlertRepository;
import com.dms.userService.user.repository.TrustedContactRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.PersonalSafetyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalSafetyServiceImpl implements PersonalSafetyService {

    private final TrustedContactRepository trustedContactRepository;
    private final SafetyCheckInRepository safetyCheckInRepository;
    private final SosAlertRepository sosAlertRepository;
    private final UserRepository userRepository;
    private final NotificationServiceClientHttp notificationClient;

    private static final int MAX_TRUSTED_CONTACTS = 10;

    // ==========================================
    // TRUSTED CONTACTS
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<TrustedContactResponse> getTrustedContacts(UUID userId) {
        return trustedContactRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::mapToContactResponse)
                .toList();
    }

    @Override
    @Transactional
    public TrustedContactResponse addTrustedContact(UUID userId, TrustedContactRequest request) {
        User user = getUserOrThrow(userId);

        long currentCount = trustedContactRepository.countByUserId(userId);
        if (currentCount >= MAX_TRUSTED_CONTACTS) {
            throw new BadRequestException("Maximum limit of " + MAX_TRUSTED_CONTACTS + " trusted contacts reached.");
        }

        TrustedContact contact = TrustedContact.builder()
                .user(user)
                .name(request.getName().trim())
                .relationship(request.getRelationship() != null ? request.getRelationship().trim() : "Other")
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .isEmergencyContact(request.isEmergencyContact())
                .build();

        TrustedContact saved = trustedContactRepository.save(contact);
        log.info("Added trusted contact {} for user {}", saved.getId(), userId);
        return mapToContactResponse(saved);
    }

    @Override
    @Transactional
    public TrustedContactResponse updateTrustedContact(UUID userId, UUID contactId, TrustedContactRequest request) {
        TrustedContact contact = trustedContactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found or access denied."));

        contact.setName(request.getName().trim());
        contact.setRelationship(request.getRelationship() != null ? request.getRelationship().trim() : "Other");
        contact.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        contact.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        contact.setEmergencyContact(request.isEmergencyContact());

        TrustedContact saved = trustedContactRepository.save(contact);
        return mapToContactResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTrustedContact(UUID userId, UUID contactId) {
        TrustedContact contact = trustedContactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found or access denied."));
        trustedContactRepository.delete(contact);
        log.info("Deleted trusted contact {} for user {}", contactId, userId);
    }

    // ==========================================
    // "I'M SAFE" CHECK-IN
    // ==========================================

    @Override
    @Transactional
    public SafetyCheckInResponse recordCheckIn(UUID userId, SafetyCheckInRequest request) {
        // Idempotency: If clientReferenceId was already processed, return existing record
        Optional<SafetyCheckIn> existing = safetyCheckInRepository.findByClientReferenceId(request.getClientReferenceId());
        if (existing.isPresent()) {
            SafetyCheckIn record = existing.get();
            if (record.getUser().getId().equals(userId)) {
                log.info("Returning existing safety check-in for clientReferenceId: {}", request.getClientReferenceId());
                return mapToCheckInResponse(record);
            } else {
                throw new BadRequestException("Client reference ID collision detected.");
            }
        }

        User user = getUserOrThrow(userId);

        int notifiedCount = 0;
        String deliveryStatus = "NONE";

        if (request.isNotifyContacts()) {
            List<TrustedContact> contacts = trustedContactRepository.findByUserIdOrderByCreatedAtAsc(userId);
            List<String> emails = contacts.stream()
                    .map(TrustedContact::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .toList();

            if (!emails.isEmpty()) {
                String senderName = resolveUserName(user);
                notifiedCount = notificationClient.dispatchSafetyCheckIn(
                        emails,
                        senderName,
                        request.getMessage(),
                        request.getLatitude(),
                        request.getLongitude(),
                        Instant.now().toString()
                );
                deliveryStatus = notifiedCount > 0 ? (notifiedCount == emails.size() ? "SENT" : "PARTIAL") : "FAILED";
            }
        }

        SafetyCheckIn checkIn = SafetyCheckIn.builder()
                .user(user)
                .clientReferenceId(request.getClientReferenceId())
                .status("SAFE")
                .message(request.getMessage() != null ? request.getMessage().trim() : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .notifyContacts(request.isNotifyContacts())
                .contactsNotifiedCount(notifiedCount)
                .deliveryStatus(deliveryStatus)
                .createdAt(Instant.now())
                .build();

        SafetyCheckIn saved = safetyCheckInRepository.save(checkIn);
        log.info("Recorded 'I am Safe' check-in {} for user {}", saved.getId(), userId);
        return mapToCheckInResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SafetyCheckInResponse getLatestCheckIn(UUID userId) {
        return safetyCheckInRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::mapToCheckInResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SafetyCheckInResponse> getCheckInHistory(UUID userId) {
        return safetyCheckInRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToCheckInResponse)
                .toList();
    }

    // ==========================================
    // SOS QUICK ALERT
    // ==========================================

    @Override
    @Transactional
    public SosAlertResponse triggerSosAlert(UUID userId, SosAlertRequest request) {
        // Idempotency: Return existing alert if already submitted
        Optional<SosAlert> existing = sosAlertRepository.findByClientReferenceId(request.getClientReferenceId());
        if (existing.isPresent()) {
            SosAlert record = existing.get();
            if (record.getUser().getId().equals(userId)) {
                log.info("Returning existing SOS alert for clientReferenceId: {}", request.getClientReferenceId());
                return mapToSosResponse(record);
            } else {
                throw new BadRequestException("Client reference ID collision detected.");
            }
        }

        User user = getUserOrThrow(userId);

        List<TrustedContact> contacts = trustedContactRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<String> emails = contacts.stream()
                .map(TrustedContact::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .toList();

        int notifiedCount = 0;
        String alertStatus = "SENT";

        if (!emails.isEmpty()) {
            String senderName = resolveUserName(user);
            notifiedCount = notificationClient.dispatchSosAlert(
                    emails,
                    senderName,
                    request.getMessage(),
                    request.getLatitude(),
                    request.getLongitude(),
                    Instant.now().toString()
            );
            alertStatus = notifiedCount > 0 ? (notifiedCount == emails.size() ? "SENT" : "PARTIAL") : "FAILED";
        } else {
            // Alert is successfully stored even if no email contacts were configured
            alertStatus = "SENT";
        }

        SosAlert alert = SosAlert.builder()
                .user(user)
                .clientReferenceId(request.getClientReferenceId())
                .message(request.getMessage() != null ? request.getMessage().trim() : "Emergency assistance requested!")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(alertStatus)
                .contactsNotifiedCount(notifiedCount)
                .createdAt(Instant.now())
                .build();

        SosAlert saved = sosAlertRepository.save(alert);
        log.warn("🚨 Priority SOS Alert triggered by user {}: ID={}, notifiedContacts={}", userId, saved.getId(), notifiedCount);
        return mapToSosResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SosAlertResponse getLatestSosAlert(UUID userId) {
        return sosAlertRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::mapToSosResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SosAlertResponse> getSosAlertHistory(UUID userId) {
        return sosAlertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToSosResponse)
                .toList();
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private String resolveUserName(User user) {
        if (user.getUserProfile() != null && user.getUserProfile().getName() != null && !user.getUserProfile().getName().isBlank()) {
            return user.getUserProfile().getName();
        }
        return user.getEmail();
    }

    private TrustedContactResponse mapToContactResponse(TrustedContact c) {
        return TrustedContactResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .relationship(c.getRelationship())
                .phone(c.getPhone())
                .email(c.getEmail())
                .isEmergencyContact(c.isEmergencyContact())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private SafetyCheckInResponse mapToCheckInResponse(SafetyCheckIn s) {
        return SafetyCheckInResponse.builder()
                .id(s.getId())
                .clientReferenceId(s.getClientReferenceId())
                .status(s.getStatus())
                .message(s.getMessage())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .notifyContacts(s.isNotifyContacts())
                .contactsNotifiedCount(s.getContactsNotifiedCount())
                .deliveryStatus(s.getDeliveryStatus())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SosAlertResponse mapToSosResponse(SosAlert a) {
        return SosAlertResponse.builder()
                .id(a.getId())
                .clientReferenceId(a.getClientReferenceId())
                .status(a.getStatus())
                .message(a.getMessage())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .contactsNotifiedCount(a.getContactsNotifiedCount())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
