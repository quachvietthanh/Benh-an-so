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

const PrivateRoute = ({ children, allowedRoles = [] }) => {
  const { isAuthenticated, loading, user } = useAuthContext()

  if (loading) {
    return <div style={{ padding: 24 }}>Đang tải...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles.length > 0) {
    const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    const isAllowed = allowedRoles.some((role) =>
      userRoles.includes(String(role).toLowerCase().replace(/^role_/, ''))
    )
    if (!isAllowed) {
      return (
        <div style={{ padding: 24 }}>
          <Alert type="error" showIcon message="Bạn không có quyền truy cập chức năng này." />
        </div>
      )
    }
  }

  return children
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
        <Route path="patients" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'receptionist']}><LazyPage><PatientList /></LazyPage></PrivateRoute>} />
        <Route path="patients/:id" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'receptionist']}><LazyPage><PatientDetail /></LazyPage></PrivateRoute>} />
        <Route path="appointments" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'nurse', 'receptionist']}><LazyPage><AppointmentQueue /></LazyPage></PrivateRoute>} />
        <Route path="after-care" element={<PrivateRoute allowedRoles={['receptionist', 'admin']}><LazyPage><AfterCarePage /></LazyPage></PrivateRoute>} />
        <Route path="medical-records" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><LazyPage><MedicalEncounter /></LazyPage></PrivateRoute>} />
        <Route path="medical-records/visits/:visitId" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><LazyPage><MedicalEncounter /></LazyPage></PrivateRoute>} />
        <Route path="prescriptions" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><LazyPage><PrescriptionPage /></LazyPage></PrivateRoute>} />
        <Route path="prescriptions/:medicalRecordId" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><LazyPage><PrescriptionPage /></LazyPage></PrivateRoute>} />
        <Route path="clinical-orders" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><Navigate to="/appointments" replace /></PrivateRoute>} />
        <Route path="clinical-results" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><LazyPage><ResultPage /></LazyPage></PrivateRoute>} />
        <Route path="results" element={<Navigate to="/clinical-results" replace />} />
        <Route path="pharmacy" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><LazyPage><PharmacyPage /></LazyPage></PrivateRoute>} />
        <Route path="pharmacy/receipts" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><LazyPage><InventoryReceiptPage /></LazyPage></PrivateRoute>} />
        <Route path="medicines" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><LazyPage><MedicineCatalogPage /></LazyPage></PrivateRoute>} />
        <Route path="medicine-catalog" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><LazyPage><MedicineCatalogPage /></LazyPage></PrivateRoute>} />
        <Route path="billing" element={<PrivateRoute allowedRoles={['admin', 'manager', 'receptionist']}><LazyPage><BillingPage /></LazyPage></PrivateRoute>} />
        <Route path="reports" element={<PrivateRoute allowedRoles={['admin', 'manager']}><LazyPage><ReportsPage /></LazyPage></PrivateRoute>} />
        <Route path="system-management" element={<PrivateRoute allowedRoles={['admin', 'manager', 'clinic_manager']}><LazyPage><SystemManagementPage /></LazyPage></PrivateRoute>} />
        <Route path="backup-restore" element={<PrivateRoute allowedRoles={['admin']}><LazyPage><BackupRestorePage /></LazyPage></PrivateRoute>} />
        <Route path="audit-logs" element={<PrivateRoute allowedRoles={['admin']}><LazyPage><MedicalRecordAccessLogsPage /></LazyPage></PrivateRoute>} />
        <Route path="medical-records/access-logs" element={<PrivateRoute allowedRoles={['admin']}><LazyPage><MedicalRecordAccessLogsPage /></LazyPage></PrivateRoute>} />
        <Route path="users" element={<PrivateRoute allowedRoles={['admin']}><LazyPage><UsersPage /></LazyPage></PrivateRoute>} />
        <Route path="services" element={<PrivateRoute allowedRoles={['admin', 'manager', 'clinic_manager']}><LazyPage><ServicesPage /></LazyPage></PrivateRoute>} />
      </Route>

      <Route path="*" element={<LazyPage><NotFound /></LazyPage>} />
    </Routes>
  )
}

export default AppRoutes
