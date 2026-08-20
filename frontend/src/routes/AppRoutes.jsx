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
      <Route path="/login" element={<Login />} />
      <Route path="/public-lookup" element={<PublicLookupPage />} />
      <Route path="/tra-cuu" element={<Navigate to="/public-lookup" replace />} />

      <Route
        path="/"
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="patients" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'receptionist']}><PatientList /></PrivateRoute>} />
        <Route path="patients/:id" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'receptionist']}><PatientDetail /></PrivateRoute>} />
        <Route path="appointments" element={<PrivateRoute allowedRoles={['admin', 'doctor', 'nurse', 'receptionist']}><AppointmentQueue /></PrivateRoute>} />
        <Route path="after-care" element={<PrivateRoute allowedRoles={['receptionist']}><AfterCarePage /></PrivateRoute>} />
        <Route path="medical-records" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><MedicalEncounter /></PrivateRoute>} />
        <Route path="medical-records/visits/:visitId" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><MedicalEncounter /></PrivateRoute>} />
        <Route path="prescriptions" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><PrescriptionPage /></PrivateRoute>} />
        <Route path="prescriptions/:medicalRecordId" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><PrescriptionPage /></PrivateRoute>} />
        <Route path="clinical-orders" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><Navigate to="/appointments" replace /></PrivateRoute>} />
        <Route path="clinical-results" element={<PrivateRoute allowedRoles={['admin', 'doctor']}><ResultPage /></PrivateRoute>} />
        <Route path="results" element={<Navigate to="/clinical-results" replace />} />
        <Route path="pharmacy" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><PharmacyPage /></PrivateRoute>} />
        <Route path="pharmacy/receipts" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><InventoryReceiptPage /></PrivateRoute>} />
        <Route path="medicines" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><MedicineCatalogPage /></PrivateRoute>} />
        <Route path="medicine-catalog" element={<PrivateRoute allowedRoles={['admin', 'pharmacist']}><MedicineCatalogPage /></PrivateRoute>} />
        <Route path="billing" element={<PrivateRoute allowedRoles={['admin', 'manager', 'receptionist']}><BillingPage /></PrivateRoute>} />
        <Route path="reports" element={<PrivateRoute allowedRoles={['admin', 'manager']}><ReportsPage /></PrivateRoute>} />
        <Route path="system-management" element={<PrivateRoute allowedRoles={['admin', 'manager', 'clinic_manager']}><SystemManagementPage /></PrivateRoute>} />
        <Route path="backup-restore" element={<PrivateRoute allowedRoles={['admin']}><BackupRestorePage /></PrivateRoute>} />
        <Route path="audit-logs" element={<PrivateRoute allowedRoles={['admin']}><MedicalRecordAccessLogsPage /></PrivateRoute>} />
        <Route path="medical-records/access-logs" element={<PrivateRoute allowedRoles={['admin']}><MedicalRecordAccessLogsPage /></PrivateRoute>} />
        <Route path="users" element={<PrivateRoute allowedRoles={['admin']}><UsersPage /></PrivateRoute>} />
        <Route path="services" element={<PrivateRoute allowedRoles={['admin', 'manager', 'clinic_manager']}><ServicesPage /></PrivateRoute>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

export default AppRoutes
