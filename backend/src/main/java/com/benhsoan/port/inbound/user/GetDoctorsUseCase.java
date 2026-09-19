package com.benhsoan.port.inbound.user;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.UserResult;

public interface GetDoctorsUseCase {

    List<UserResult> getAllActiveDoctors();

    List<UserResult> getActiveDoctorsBySpecialty(UUID specialtyId);
}
