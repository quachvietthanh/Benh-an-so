/**
 * Data Mapper layer to transform Spring Boot DTO responses into Frontend UI models.
 */

// Helper to extract data array whether response is array or Page<T>
export const extractPageData = (response) => {
  if (!response) return { items: [], total: 0, page: 0, size: 20, totalPages: 0 }

  const data = response.data !== undefined ? response.data : response

  if (Array.isArray(data)) {
    return {
      items: data,
      total: data.length,
      page: 0,
      size: data.length,
      totalPages: 1,
    }
  }

  if (data && Array.isArray(data.content)) {
    return {
      items: data.content,
      total: data.totalElements || data.content.length,
      page: data.number || 0,
      size: data.size || 20,
      totalPages: data.totalPages || 1,
    }
  }

  return { items: [], total: 0, page: 0, size: 20, totalPages: 0 }
}

// Map PatientResponse from Backend to UI model
export const mapPatientResponse = (patient) => {
  if (!patient) return null

  return {
    id: patient.id,
    patientCode: patient.patientCode || 'BN' + String(patient.id || '').substring(0, 6).toUpperCase(),
    fullName: patient.fullName || '',
    name: patient.fullName || '',
    dateOfBirth: patient.dateOfBirth || '',
    dob: patient.dateOfBirth || '',
    gender: patient.gender === 'MALE' ? 'Nam' : patient.gender === 'FEMALE' ? 'Nữ' : 'Khác',
    genderRaw: patient.gender || 'OTHER',
    phone: patient.phone || '',
    email: patient.email || '',
    address: patient.address || '',
    identityNumber: patient.identityNumber || '',
    insuranceNumber: patient.insuranceNumber || '',
    bloodType: patient.bloodType || '',
    emergencyContact: patient.emergencyContact || '',
    emergencyPhone: patient.emergencyPhone || '',
    active: patient.active !== undefined ? patient.active : true,
    createdAt: patient.createdAt || '',
    updatedAt: patient.updatedAt || '',
  }
}

// Map UserResponse from Backend to UI model
export const mapUserResponse = (user) => {
  if (!user) return null

  const roleRaw = String(user.role || '').replace('ROLE_', '')
  const roleNameMap = {
    ADMIN: 'Quản trị viên',
    DOCTOR: 'Bác sĩ',
    NURSE: 'Điều dưỡng / KTV',
    RECEPTIONIST: 'Lễ tân',
    PHARMACIST: 'Dược sĩ',
  }

  return {
    id: user.id,
    username: user.username || '',
    fullName: user.fullName || '',
    name: user.fullName || '',
    email: user.email || '',
    phone: user.phone || '',
    role: roleRaw,
    roleName: roleNameMap[roleRaw] || roleRaw,
    department: user.department || 'Khoa Nội',
    active: user.active !== undefined ? user.active : true,
  }
}

// Map AppointmentResponse from Backend to UI model
export const mapAppointmentResponse = (appointment) => {
  if (!appointment) return null

  return {
    id: appointment.id,
    appointmentCode: appointment.appointmentCode || 'LH' + String(appointment.id || '').substring(0, 6).toUpperCase(),
    patientId: appointment.patientId,
    doctorId: appointment.doctorId,
    startTime: appointment.startTime,
    endTime: appointment.endTime,
    status: appointment.status || 'SCHEDULED',
    reason: appointment.reason || '',
    cancelReason: appointment.cancelReason || '',
    checkedInAt: appointment.checkedInAt || null,
    completedAt: appointment.completedAt || null,
    createdAt: appointment.createdAt || '',
  }
}
