import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Alert } from 'antd'
import { useAuthContext } from '../context/AuthContext'
import MainLayout from '../components/layout/MainLayout'
import Login from '../pages/Login'
import Dashboard from '../pages/Dashboard'
import PatientList from '../pages/PatientList'
import PatientDetail from '../pages/PatientDetail'
import AppointmentQueue from '../pages/AppointmentQueue'
import AfterCarePage from '../pages/AfterCarePage'
import MedicalEncounter from '../pages/MedicalEncounter'
import PrescriptionPage from '../pages/PrescriptionPage'
import PharmacyPage from '../pages/PharmacyPage'
import InventoryReceiptPage from '../pages/InventoryReceiptPage'
import MedicineCatalogPage from '../pages/MedicineCatalogPage'
import BillingPage from '../pages/BillingPage'
import ReportsPage from '../pages/ReportsPage'
import UsersPage from '../pages/UsersPage'
import ServicesPage from '../pages/ServicesPage'
import ResultPage from '../pages/ResultPage'
import PublicLookupPage from '../pages/PublicLookupPage'
import SystemManagementPage from '../pages/SystemManagementPage'
import BackupRestorePage from '../pages/BackupRestorePage'
import MedicalRecordAccessLogsPage from '../pages/MedicalRecordAccessLogsPage'
import NotFound from '../pages/NotFound'

const PrivateRoute = ({ children, allowedRoles = [], allowedPermissions = [] }) => {
  const { isAuthenticated, loading, user } = useAuthContext()

  if (loading) {
    return <div style={{ padding: 24 }}>Đang tải...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const userPerms = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  // Admin has access to all routes by default
  const isAdmin = userRoles.includes('admin')

  // Check role match
  const hasRoleMatch = allowedRoles.length > 0 && allowedRoles.some((role) =>
    userRoles.includes(String(role).toLowerCase().replace(/^role_/, ''))
  )

  // Check permission match
  const hasPermMatch = allowedPermissions.length > 0 && allowedPermissions.some((perm) =>
    userPerms.includes(String(perm).toUpperCase().replace(/^PERMISSION_/, ''))
  )

  if (allowedRoles.length === 0 && allowedPermissions.length === 0) {
    return children
  }

  if (isAdmin || hasPermMatch || hasRoleMatch) {
    return children
  }

  return (
    <div style={{ padding: 24 }}>
      <Alert type="error" showIcon message="Bạn không có quyền truy cập chức năng này." />
    </div>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/public-lookup" element={<PublicLookupPage />} />
      <Route path="/portal" element={<PublicLookupPage />} />
      <Route path="/tra-cuu-ket-qua" element={<PublicLookupPage />} />
      <Route path="/tra-cuu" element={<Navigate to="/portal" replace />} />

      <Route
        path="/"
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="patients" element={<PrivateRoute allowedPermissions={['PATIENT_READ', 'PATIENT_CREATE', 'PATIENT_UPDATE']} allowedRoles={['admin', 'doctor', 'receptionist']}><PatientList /></PrivateRoute>} />
        <Route path="patients/:id" element={<PrivateRoute allowedPermissions={['PATIENT_READ', 'PATIENT_CREATE', 'PATIENT_UPDATE']} allowedRoles={['admin', 'doctor', 'receptionist']}><PatientDetail /></PrivateRoute>} />
        <Route path="appointments" element={<PrivateRoute allowedPermissions={['APPOINTMENT_READ', 'APPOINTMENT_CREATE', 'APPOINTMENT_UPDATE', 'QUEUE_VIEW', 'QUEUE_CREATE']} allowedRoles={['admin', 'doctor', 'nurse', 'receptionist']}><AppointmentQueue /></PrivateRoute>} />
        <Route path="after-care" element={<PrivateRoute allowedPermissions={['FOLLOW_UP_REMINDER_READ', 'FOLLOW_UP_REMINDER_CREATE', 'CARE_LOG_READ', 'CARE_LOG_CREATE']} allowedRoles={['receptionist', 'admin']}><AfterCarePage /></PrivateRoute>} />
        <Route path="medical-records" element={<PrivateRoute allowedPermissions={['MEDICAL_RECORD_READ', 'MEDICAL_RECORD_CREATE', 'MEDICAL_RECORD_UPDATE']} allowedRoles={['admin', 'doctor']}><MedicalEncounter /></PrivateRoute>} />
        <Route path="medical-records/visits/:visitId" element={<PrivateRoute allowedPermissions={['MEDICAL_RECORD_READ', 'MEDICAL_RECORD_CREATE', 'MEDICAL_RECORD_UPDATE']} allowedRoles={['admin', 'doctor']}><MedicalEncounter /></PrivateRoute>} />
        <Route path="prescriptions" element={<PrivateRoute allowedPermissions={['PRESCRIPTION_READ', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_PRINT']} allowedRoles={['admin', 'doctor']}><PrescriptionPage /></PrivateRoute>} />
        <Route path="prescriptions/:medicalRecordId" element={<PrivateRoute allowedPermissions={['PRESCRIPTION_READ', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_PRINT']} allowedRoles={['admin', 'doctor']}><PrescriptionPage /></PrivateRoute>} />
        <Route path="clinical-orders" element={<PrivateRoute allowedPermissions={['CLINICAL_RESULT_READ', 'CLINICAL_RESULT_CREATE']} allowedRoles={['admin', 'doctor']}><Navigate to="/appointments" replace /></PrivateRoute>} />
        <Route path="clinical-results" element={<PrivateRoute allowedPermissions={['CLINICAL_RESULT_READ', 'CLINICAL_RESULT_CREATE', 'CLINICAL_RESULT_UPDATE']} allowedRoles={['admin', 'doctor']}><ResultPage /></PrivateRoute>} />
        <Route path="results" element={<Navigate to="/clinical-results" replace />} />
        <Route path="pharmacy" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PRESCRIPTION_READ']} allowedRoles={['admin', 'pharmacist']}><PharmacyPage /></PrivateRoute>} />
        <Route path="pharmacy/receipts" element={<PrivateRoute allowedPermissions={['PHARMACY_CREATE', 'PHARMACY_READ']} allowedRoles={['admin', 'pharmacist']}><InventoryReceiptPage /></PrivateRoute>} />
        <Route path="medicines" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PHARMACY_CREATE', 'PHARMACY_UPDATE']} allowedRoles={['admin', 'pharmacist']}><MedicineCatalogPage /></PrivateRoute>} />
        <Route path="medicine-catalog" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PHARMACY_CREATE', 'PHARMACY_UPDATE']} allowedRoles={['admin', 'pharmacist']}><MedicineCatalogPage /></PrivateRoute>} />
        <Route path="billing" element={<PrivateRoute allowedPermissions={['INVOICE_READ', 'INVOICE_CREATE', 'INVOICE_UPDATE']} allowedRoles={['admin', 'manager', 'receptionist']}><BillingPage /></PrivateRoute>} />
        <Route path="reports" element={<PrivateRoute allowedPermissions={['REPORT_VIEW', 'REPORT_EXPORT']} allowedRoles={['admin', 'manager']}><ReportsPage /></PrivateRoute>} />
        <Route path="system-management" element={<PrivateRoute allowedPermissions={['ROLE_READ', 'ROLE_UPDATE', 'CLINIC_CONFIGURATION_READ', 'USER_READ', 'AUDIT_READ', 'BACKUP_READ']} allowedRoles={['admin', 'manager', 'clinic_manager']}><SystemManagementPage /></PrivateRoute>} />
        <Route path="backup-restore" element={<PrivateRoute allowedPermissions={['BACKUP_READ', 'BACKUP_CREATE', 'BACKUP_RESTORE']} allowedRoles={['admin']}><BackupRestorePage /></PrivateRoute>} />
        <Route path="audit-logs" element={<PrivateRoute allowedPermissions={['AUDIT_READ']} allowedRoles={['admin']}><MedicalRecordAccessLogsPage /></PrivateRoute>} />
        <Route path="medical-records/access-logs" element={<PrivateRoute allowedPermissions={['AUDIT_READ']} allowedRoles={['admin']}><MedicalRecordAccessLogsPage /></PrivateRoute>} />
        <Route path="users" element={<PrivateRoute allowedPermissions={['USER_READ', 'USER_CREATE', 'USER_UPDATE']} allowedRoles={['admin']}><UsersPage /></PrivateRoute>} />
        <Route path="services" element={<PrivateRoute allowedPermissions={['SERVICE_CATALOG_READ', 'SERVICE_CATALOG_CREATE', 'SERVICE_CATALOG_UPDATE', 'SERVICE_PRICE_MANAGE']} allowedRoles={['admin', 'manager', 'clinic_manager']}><ServicesPage /></PrivateRoute>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

export default AppRoutes
