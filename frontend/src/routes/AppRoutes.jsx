import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Alert } from 'antd'
import { useAuthContext } from '../context/AuthContext'
import MainLayout from '../components/layout/MainLayout'
import Loading from '../components/common/Loading'

const Login = React.lazy(() => import('../pages/Login'))
const Dashboard = React.lazy(() => import('../pages/Dashboard'))
const PatientList = React.lazy(() => import('../pages/PatientList'))
const PatientDetail = React.lazy(() => import('../pages/PatientDetail'))
const AppointmentQueue = React.lazy(() => import('../pages/AppointmentQueue'))
const AfterCarePage = React.lazy(() => import('../pages/AfterCarePage'))
const MedicalEncounter = React.lazy(() => import('../pages/MedicalEncounter'))
const PrescriptionPage = React.lazy(() => import('../pages/PrescriptionPage'))
const PharmacyPage = React.lazy(() => import('../pages/PharmacyPage'))
const InventoryReceiptPage = React.lazy(() => import('../pages/InventoryReceiptPage'))
const MedicineCatalogPage = React.lazy(() => import('../pages/MedicineCatalogPage'))
const BillingPage = React.lazy(() => import('../pages/BillingPage'))
const ReportsPage = React.lazy(() => import('../pages/ReportsPage'))
const UsersPage = React.lazy(() => import('../pages/UsersPage'))
const ServicesPage = React.lazy(() => import('../pages/ServicesPage'))
const ResultPage = React.lazy(() => import('../pages/ResultPage'))
const PublicLookupPage = React.lazy(() => import('../pages/PublicLookupPage'))
const SystemManagementPage = React.lazy(() => import('../pages/SystemManagementPage'))
const BackupRestorePage = React.lazy(() => import('../pages/BackupRestorePage'))
const MedicalRecordAccessLogsPage = React.lazy(() => import('../pages/MedicalRecordAccessLogsPage'))
const NotFound = React.lazy(() => import('../pages/NotFound'))

const LazyPage = ({ children }) => (
  <React.Suspense fallback={<Loading />}>
    {children}
  </React.Suspense>
)

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
      <Route path="/login" element={<LazyPage><Login /></LazyPage>} />
      <Route path="/public-lookup" element={<LazyPage><PublicLookupPage /></LazyPage>} />
      <Route path="/portal" element={<LazyPage><PublicLookupPage /></LazyPage>} />
      <Route path="/tra-cuu-ket-qua" element={<LazyPage><PublicLookupPage /></LazyPage>} />
      <Route path="/tra-cuu" element={<Navigate to="/portal" replace />} />

      <Route
        path="/"
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<LazyPage><Dashboard /></LazyPage>} />
        <Route path="patients" element={<PrivateRoute allowedPermissions={['PATIENT_READ', 'PATIENT_CREATE', 'PATIENT_UPDATE']} allowedRoles={['admin', 'doctor', 'receptionist']}><LazyPage><PatientList /></LazyPage></PrivateRoute>} />
        <Route path="patients/:id" element={<PrivateRoute allowedPermissions={['PATIENT_READ', 'PATIENT_CREATE', 'PATIENT_UPDATE']} allowedRoles={['admin', 'doctor', 'receptionist']}><LazyPage><PatientDetail /></LazyPage></PrivateRoute>} />
        <Route path="appointments" element={<PrivateRoute allowedPermissions={['APPOINTMENT_READ', 'APPOINTMENT_CREATE', 'APPOINTMENT_UPDATE', 'QUEUE_VIEW', 'QUEUE_CREATE']} allowedRoles={['admin', 'doctor', 'nurse', 'receptionist']}><LazyPage><AppointmentQueue /></LazyPage></PrivateRoute>} />
        <Route path="after-care" element={<PrivateRoute allowedPermissions={['FOLLOW_UP_REMINDER_READ', 'FOLLOW_UP_REMINDER_CREATE', 'CARE_LOG_READ', 'CARE_LOG_CREATE']} allowedRoles={['receptionist', 'admin']}><LazyPage><AfterCarePage /></LazyPage></PrivateRoute>} />
        <Route path="medical-records" element={<PrivateRoute allowedPermissions={['MEDICAL_RECORD_READ', 'MEDICAL_RECORD_CREATE', 'MEDICAL_RECORD_UPDATE']} allowedRoles={['admin', 'doctor']}><LazyPage><MedicalEncounter /></LazyPage></PrivateRoute>} />
        <Route path="medical-records/visits/:visitId" element={<PrivateRoute allowedPermissions={['MEDICAL_RECORD_READ', 'MEDICAL_RECORD_CREATE', 'MEDICAL_RECORD_UPDATE']} allowedRoles={['admin', 'doctor']}><LazyPage><MedicalEncounter /></LazyPage></PrivateRoute>} />
        <Route path="prescriptions" element={<PrivateRoute allowedPermissions={['PRESCRIPTION_READ', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_PRINT']} allowedRoles={['admin', 'doctor']}><LazyPage><PrescriptionPage /></LazyPage></PrivateRoute>} />
        <Route path="prescriptions/:medicalRecordId" element={<PrivateRoute allowedPermissions={['PRESCRIPTION_READ', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_PRINT']} allowedRoles={['admin', 'doctor']}><LazyPage><PrescriptionPage /></LazyPage></PrivateRoute>} />
        <Route path="clinical-orders" element={<PrivateRoute allowedPermissions={['CLINICAL_RESULT_READ', 'CLINICAL_RESULT_CREATE']} allowedRoles={['admin', 'doctor']}><Navigate to="/appointments" replace /></PrivateRoute>} />
        <Route path="clinical-results" element={<PrivateRoute allowedPermissions={['CLINICAL_RESULT_READ', 'CLINICAL_RESULT_CREATE', 'CLINICAL_RESULT_UPDATE']} allowedRoles={['admin', 'doctor']}><LazyPage><ResultPage /></LazyPage></PrivateRoute>} />
        <Route path="results" element={<Navigate to="/clinical-results" replace />} />
        <Route path="pharmacy" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PRESCRIPTION_READ']} allowedRoles={['admin', 'pharmacist']}><LazyPage><PharmacyPage /></LazyPage></PrivateRoute>} />
        <Route path="pharmacy/receipts" element={<PrivateRoute allowedPermissions={['PHARMACY_CREATE', 'PHARMACY_READ']} allowedRoles={['admin', 'pharmacist']}><LazyPage><InventoryReceiptPage /></LazyPage></PrivateRoute>} />
        <Route path="medicines" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PHARMACY_CREATE', 'PHARMACY_UPDATE']} allowedRoles={['admin', 'pharmacist']}><LazyPage><MedicineCatalogPage /></LazyPage></PrivateRoute>} />
        <Route path="medicine-catalog" element={<PrivateRoute allowedPermissions={['PHARMACY_READ', 'PHARMACY_CREATE', 'PHARMACY_UPDATE']} allowedRoles={['admin', 'pharmacist']}><LazyPage><MedicineCatalogPage /></LazyPage></PrivateRoute>} />
        <Route path="billing" element={<PrivateRoute allowedPermissions={['INVOICE_READ', 'INVOICE_CREATE', 'INVOICE_UPDATE']} allowedRoles={['admin', 'manager', 'receptionist']}><LazyPage><BillingPage /></LazyPage></PrivateRoute>} />
        <Route path="reports" element={<PrivateRoute allowedPermissions={['REPORT_VIEW', 'REPORT_EXPORT']} allowedRoles={['admin', 'manager']}><LazyPage><ReportsPage /></LazyPage></PrivateRoute>} />
        <Route path="system-management" element={<PrivateRoute allowedPermissions={['ROLE_READ', 'ROLE_UPDATE', 'CLINIC_CONFIGURATION_READ', 'USER_READ', 'AUDIT_READ', 'BACKUP_READ']} allowedRoles={['admin', 'manager', 'clinic_manager']}><LazyPage><SystemManagementPage /></LazyPage></PrivateRoute>} />
        <Route path="backup-restore" element={<PrivateRoute allowedPermissions={['BACKUP_READ', 'BACKUP_CREATE', 'BACKUP_RESTORE']} allowedRoles={['admin']}><LazyPage><BackupRestorePage /></LazyPage></PrivateRoute>} />
        <Route path="audit-logs" element={<PrivateRoute allowedPermissions={['AUDIT_READ']} allowedRoles={['admin']}><LazyPage><MedicalRecordAccessLogsPage /></LazyPage></PrivateRoute>} />
        <Route path="medical-records/access-logs" element={<PrivateRoute allowedPermissions={['AUDIT_READ']} allowedRoles={['admin']}><LazyPage><MedicalRecordAccessLogsPage /></LazyPage></PrivateRoute>} />
        <Route path="users" element={<PrivateRoute allowedPermissions={['USER_READ', 'USER_CREATE', 'USER_UPDATE']} allowedRoles={['admin']}><LazyPage><UsersPage /></LazyPage></PrivateRoute>} />
        <Route path="services" element={<PrivateRoute allowedPermissions={['SERVICE_CATALOG_READ', 'SERVICE_CATALOG_CREATE', 'SERVICE_CATALOG_UPDATE', 'SERVICE_PRICE_MANAGE']} allowedRoles={['admin', 'manager', 'clinic_manager']}><LazyPage><ServicesPage /></LazyPage></PrivateRoute>} />
      </Route>

      <Route path="*" element={<LazyPage><NotFound /></LazyPage>} />
    </Routes>
  )
}

export default AppRoutes
