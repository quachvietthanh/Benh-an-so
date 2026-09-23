package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalNotificationRestMapper;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalNotificationResponse;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationsUseCase;
import com.benhsoan.port.inbound.portal.MarkPatientPortalNotificationReadUseCase;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-008: patient-portal notifications. Requires ROLE_PATIENT (enforced by
 * SecurityConfig for {@code /patient-portal/**}) and returns only the caller's own
 * notifications (QTN-23).
 */
@RestController
@RequestMapping("/patient-portal/notifications")
@RequiredArgsConstructor
public class PatientPortalNotificationController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final GetPatientPortalNotificationsUseCase getPatientPortalNotificationsUseCase;
    private final GetPatientPortalNotificationDetailUseCase getPatientPortalNotificationDetailUseCase;
    private final MarkPatientPortalNotificationReadUseCase markPatientPortalNotificationReadUseCase;
    private final PatientPortalNotificationRestMapper mapper;

    @GetMapping
    public List<PatientPortalNotificationResponse> getNotifications(
            @RequestParam(required = false) Integer limit
    ) {
        return getPatientPortalNotificationsUseCase.getNotifications(resolveLimit(limit)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PatientPortalNotificationResponse getNotification(@PathVariable UUID id) {
        return mapper.toResponse(getPatientPortalNotificationDetailUseCase.getNotification(id));
    }

    @PatchMapping("/{id}/read")
    public PatientPortalNotificationResponse markRead(@PathVariable UUID id) {
        return mapper.toResponse(markPatientPortalNotificationReadUseCase.markRead(id));
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
