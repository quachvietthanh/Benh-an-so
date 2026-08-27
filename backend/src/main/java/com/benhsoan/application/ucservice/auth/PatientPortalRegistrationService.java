package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;
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

import com.benhsoan.application.ucservice.patient.PatientChangeDetailBuilder;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.EmailAlreadyExistsException;
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientConsentVersion;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.PatientAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientConsentRequiredException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Patient portal self-registration (NCL-14-CN-001 / QTN-23, NCL-15-CN-001 / QTN-24).
 * Creates a PATIENT {@link User}, links it to an existing {@link Patient} by phone or
 * creates a basic patient record with validated consent (TC-01..04), and writes audit trails.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientPortalRegistrationService implements PatientPortalRegistrationUseCase {

    private static final String PATIENT_ROLE = "PATIENT";
    private static final String REGISTRATION_METHOD = "SELF_PORTAL_REGISTRATION";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String DUPLICATE_PHONE_MESSAGE =
            "Số điện thoại đã được đăng ký tài khoản. Vui lòng đăng nhập.";
    private static final Duration REFRESH_TOKEN_TIMEOUT = Duration.ofDays(7);
    private static final Pattern VIETNAMESE_MOBILE =
            Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final PatientChangeDetailBuilder patientChangeDetailBuilder;
    private final PasswordEncoderPort passwordEncoderPort;
    private final PatientCodeGenerator patientCodeGenerator;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;
    private final JwtTokenPort jwtTokenPort;
    private final RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    private final TokenHashPort tokenHashPort;
    private final UserSessionRepository userSessionRepository;

    @Override
    public PatientPortalRegistrationResult register(PatientPortalRegistrationCommand command) {

        String phone = normalizePhone(command.phone());
        String email = resolveEmail(command, phone);
        String consentVersion = requireValidConsent(command);

        if (userRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
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
                    email,
                    phone,
                    role.getId()
            );
            User savedUser = userRepository.save(user);

            UUID userId = savedUser.getId();

            List<Patient> candidates = patientRepository.findAllByPhone(phone);
            Instant now = clockPort.now();

            boolean isNewPatient = candidates.isEmpty();
            Patient existingPatient = isNewPatient
                    ? null
                    : findCandidateForUpdate(resolveCandidate(candidates, command));
            boolean recordsLegacyConsent = existingPatient != null
                    && !existingPatient.isConsentAgreed();
            Patient patient = isNewPatient
                    ? createPatient(command, phone, userId, consentVersion)
                    : linkCandidate(existingPatient, userId, consentVersion, now, recordsLegacyConsent);

            Patient saved = patientRepository.save(patient);

            if (isNewPatient) {
                String changeDetail = patientChangeDetailBuilder.forCreate(saved);
                PatientChangeLog changeLog = PatientChangeLog.create(
                        saved.getId(),
                        userId,
                        PatientChangeAction.CREATE,
                        changeDetail
                );
                patientChangeLogRepository.save(changeLog);
            } else if (recordsLegacyConsent) {
                PatientChangeLog changeLog = PatientChangeLog.create(
                        saved.getId(),
                        userId,
                        PatientChangeAction.UPDATE,
                        patientChangeDetailBuilder.forPortalConsentRecorded(saved)
                );
                patientChangeLogRepository.save(changeLog);
            }

            String refreshToken = refreshTokenGeneratorPort.generate();
            UserSession session = UserSession.create(
                    savedUser.getId(),
                    tokenHashPort.hash(refreshToken),
                    now.plus(REFRESH_TOKEN_TIMEOUT)
            );
            userSessionRepository.save(session);

            String accessToken = jwtTokenPort.generateToken(
                    savedUser.getId(),
                    session.getId(),
                    savedUser.getUsername(),
                    role.getName(),
                    role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet()),
                    saved.getId()
            );

            auditLogRepository.save(AuditLog.create(
                    savedUser.getId(),
                    ActionType.CREATE,
                    ResourceType.PATIENT_PORTAL,
                    saved.getId(),
                    auditDetail(saved, phone),
                    null,
                    now
            ));

            return new PatientPortalRegistrationResult(
                    savedUser.getId(),
                    saved.getId(),
                    saved.getPatientCode(),
                    phone,
                    saved.getFullName(),
                    accessToken,
                    refreshToken,
                    TOKEN_TYPE
            );

        } catch (DataIntegrityViolationException ex) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }
    }

    private String resolveEmail(PatientPortalRegistrationCommand command, String phone) {
        if (command.email() != null && !command.email().isBlank()) {
            return command.email().trim();
        }
        return phone + "@benhsoan.com";
    }

    private Patient linkCandidate(
            Patient candidate,
            UUID userId,
            String consentVersion,
            Instant now,
            boolean recordsLegacyConsent
    ) {
        candidate.linkUser(userId);
        if (recordsLegacyConsent) {
            candidate.renewConsent(consentVersion, now);
        }
        return candidate;
    }

    private Patient findCandidateForUpdate(Patient candidate) {
        Patient lockedCandidate = patientRepository.findByIdForUpdate(candidate.getId())
                .orElseThrow(() -> new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE));

        if (lockedCandidate.getUserId() != null) {
            throw new PhoneAlreadyExistsException(DUPLICATE_PHONE_MESSAGE);
        }

        return lockedCandidate;
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

        if (command.fullName() != null && command.dateOfBirth() != null) {
            Optional<Patient> byDemographics = unlinked.stream()
                    .filter(candidate -> candidate.getFullName() != null
                            && candidate.getDateOfBirth() != null
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

    private Patient createPatient(
            PatientPortalRegistrationCommand command,
            String phone,
            UUID userId,
            String consentVersion
    ) {
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
                true,
                consentVersion,
                userId
        );
        patient.linkUser(userId);
        return patient;
    }

    private String requireValidConsent(PatientPortalRegistrationCommand command) {
        if (!Boolean.TRUE.equals(command.consentAgreed())) {
            throw new PatientConsentRequiredException(
                    "Phải có phiếu đồng ý trước khi đăng ký tài khoản và xử lý dữ liệu cá nhân (QTN-24)."
            );
        }

        return PatientConsentVersion.resolveForNewConsent(command.consentVersion());
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientId", patient.getId());
        payload.put("patientCode", patient.getPatientCode());
        payload.put("phone", phone);
        payload.put("registrationMethod", REGISTRATION_METHOD);
        payload.put("consentAgreed", patient.isConsentAgreed());
        payload.put("consentVersion", patient.getConsentVersion());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize portal registration audit payload.", ex);
        }
    }
}
