package com.benhsoan.application.ucservice.auth;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Patient portal self-registration (NCL-14-CN-001 / QTN-23). Creates a PATIENT {@link User},
 * links it to an existing {@link Patient} by phone or creates a basic patient record, and writes
 * an audit trail (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientPortalRegistrationService implements PatientPortalRegistrationUseCase {

    private static final String PATIENT_ROLE = "PATIENT";
    private static final String REGISTRATION_METHOD = "SELF_PORTAL_REGISTRATION";
    private static final String DUPLICATE_PHONE_MESSAGE =
            "Số điện thoại đã được đăng ký tài khoản. Vui lòng đăng nhập.";
    private static final Pattern VIETNAMESE_MOBILE =
            Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final PatientCodeGenerator patientCodeGenerator;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientPortalRegistrationResult register(PatientPortalRegistrationCommand command) {

        String phone = normalizePhone(command.phone());

        if (userRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }

        if (command.identityNumber() != null
                && !command.identityNumber().isBlank()
                && patientRepository.existsByIdentityNumber(command.identityNumber())) {
            throw new PatientAlreadyExistsException("identity number");
        }

        Role role = roleRepository.findByName(PATIENT_ROLE)
                .orElseThrow(RoleNotFoundException::new);

        try {

            User user = User.create(
                    phone,
                    passwordEncoderPort.encode(command.password()),
                    command.fullName(),
                    phone + "@benhsoan.com",
                    phone,
                    role.getId()
            );
            User savedUser = userRepository.save(user);

            UUID userId = savedUser.getId();

            List<Patient> candidates = patientRepository.findAllByPhone(phone);

            Patient patient = candidates.isEmpty()
                    ? createPatient(command, phone, userId)
                    : linkCandidate(resolveCandidate(candidates, command), userId);

            Patient saved = patientRepository.save(patient);

            auditLogRepository.save(AuditLog.create(
                    savedUser.getId(),
                    ActionType.CREATE,
                    ResourceType.PATIENT_PORTAL,
                    saved.getId(),
                    auditDetail(saved, phone),
                    null,
                    clockPort.now()
            ));

            return new PatientPortalRegistrationResult(
                    savedUser.getId(),
                    saved.getId(),
                    phone,
                    saved.getFullName()
            );

        } catch (DataIntegrityViolationException ex) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }
    }

    private Patient linkCandidate(Patient candidate, UUID userId) {
        candidate.linkUser(userId);
        return candidate;
    }

    private Patient resolveCandidate(
            List<Patient> candidates,
            PatientPortalRegistrationCommand command
    ) {
        List<Patient> unlinked = candidates.stream()
                .filter(candidate -> candidate.getUserId() == null)
                .toList();

        if (unlinked.isEmpty()) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }

        if (unlinked.size() == 1) {
            return unlinked.get(0);
        }

        if (command.identityNumber() != null && !command.identityNumber().isBlank()) {
            Optional<Patient> byIdentity = unlinked.stream()
                    .filter(candidate -> command.identityNumber().equals(candidate.getIdentityNumber()))
                    .findFirst();
            if (byIdentity.isPresent()) {
                return byIdentity.get();
            }
        }

        if (command.dateOfBirth() != null) {
            Optional<Patient> byDemographics = unlinked.stream()
                    .filter(candidate -> command.fullName() != null
                            && command.fullName().equalsIgnoreCase(candidate.getFullName())
                            && command.dateOfBirth().equals(candidate.getDateOfBirth()))
                    .findFirst();
            if (byDemographics.isPresent()) {
                return byDemographics.get();
            }
        }

        return unlinked.stream()
                .max(Comparator
                        .comparing(Patient::isActive)
                        .thenComparing(candidate -> candidate.getUpdatedAt() != null
                                ? candidate.getUpdatedAt()
                                : candidate.getCreatedAt()))
                .orElseThrow(() -> new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE));
    }

    private Patient createPatient(PatientPortalRegistrationCommand command, String phone, UUID userId) {
        if (command.dateOfBirth() == null) {
            throw new ValidationException("Date of birth is required.");
        }
        if (command.gender() == null) {
            throw new ValidationException("Gender is required.");
        }

        Patient patient = Patient.create(
                patientCodeGenerator.generate(),
                command.fullName(),
                command.dateOfBirth(),
                command.gender(),
                phone,
                null,
                null,
                command.identityNumber(),
                null,
                null,
                null,
                null,
                userId
        );
        patient.linkUser(userId);
        return patient;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("Phone number is required.");
        }

        String trimmed = phone.trim();

        if (!VIETNAMESE_MOBILE.matcher(trimmed).matches()) {
            throw new ValidationException("Số điện thoại không hợp lệ.");
        }

        return trimmed.startsWith("+84")
                ? "0" + trimmed.substring(3)
                : trimmed;
    }

    private String auditDetail(Patient patient, String phone) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("method", REGISTRATION_METHOD);
        detail.put("phone", phone);
        detail.put("patientId", patient.getId().toString());
        detail.put("registeredAt", clockPort.now().toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient registration audit detail.", exception);
        }
    }
}
