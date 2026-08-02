import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  message,
  Modal,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
} from 'antd'
import {
  AlertOutlined,
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  EyeOutlined,
  FileDoneOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  HeartOutlined,
  HistoryOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  LockOutlined,
  PlusOutlined,
  PrinterOutlined,
  SaveOutlined,
  SearchOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import {
  getStoredAppointments,
  getStoredQueue,
  logMedicalAccess,
  mergeAppointments,
  mergeMedicalRecords,
  saveStoredMedicalRecord,
} from '../utils/storageHelpers'

const { Title, Text, Paragraph } = Typography

// Danh mục mã bệnh ICD-10 tiêu chuẩn (Mục IV)
const ICD10_CATALOG = [
  { code: 'J00', name: 'Viêm mũi họng cấp (Cảm lạnh thông thường)' },
  { code: 'J02', name: 'Viêm họng cấp' },
  { code: 'J03', name: 'Viêm amidan cấp' },
  { code: 'J06.9', name: 'Nhiễm trùng đường hô hấp trên cấp tính' },
  { code: 'J18.9', name: 'Viêm phổi, không đặc hiệu' },
  { code: 'J20', name: 'Viêm phế quản cấp' },
  { code: 'J45', name: 'Hen phế quản' },
  { code: 'I10', name: 'Bệnh tăng huyết áp vô căn (nguyên phát)' },
  { code: 'I11', name: 'Bệnh tim do tăng huyết áp' },
  { code: 'I20', name: 'Cơn đau thắt ngực' },
  { code: 'I25', name: 'Bệnh tim do thiếu máu cục bộ mãn tính' },
  { code: 'E11', name: 'Bệnh đái tháo đường týp 2' },
  { code: 'E14', name: 'Bệnh đái tháo đường không đặc hiệu' },
  { code: 'E78.5', name: 'Tăng lipid máu, không đặc hiệu' },
  { code: 'K29.7', name: 'Viêm dạ dày, không đặc hiệu' },
  { code: 'K21', name: 'Bệnh trào ngược dạ dày - thực quản' },
  { code: 'K35', name: 'Viêm ruột thừa cấp' },
  { code: 'M17', name: 'Thoái hóa khớp gối' },
  { code: 'M54.5', name: 'Đau thắt lưng thấp' },
  { code: 'N39.0', name: 'Nhiễm trùng đường tiết niệu' },
  { code: 'A09', name: 'Viêm dạ dày ruột và viêm đại tràng do nhiễm trùng' },
  { code: 'B05', name: 'Bệnh sởi' },
  { code: 'B01', name: 'Bệnh thủy đậu' },
  { code: 'R50.9', name: 'Sốt không rõ nguyên nhân' },
]

// Danh mục dịch vụ Cận lâm sàng theo Nhóm chuyên khoa (Mục V)
const CLINICAL_SERVICES_CATALOG = [
  { id: 'LAB_01', category: 'Xét nghiệm', name: 'Tổng phân tích tế bào máu ngoại vi (24 thông số)', dept: 'Khoa Xét nghiệm' },
  { id: 'LAB_02', category: 'Xét nghiệm', name: 'Định lượng Glucose máu', dept: 'Khoa Xét nghiệm' },
  { id: 'LAB_03', category: 'Xét nghiệm', name: 'Định lượng Urea & Creatinine máu', dept: 'Khoa Xét nghiệm' },
  { id: 'LAB_04', category: 'Xét nghiệm', name: 'Đo hoạt độ AST (SGOT) / ALT (SGPT)', dept: 'Khoa Xét nghiệm' },
  { id: 'LAB_05', category: 'Xét nghiệm', name: 'Định lượng Lipid máu toàn phần (Cholesterol, Triglyceride, HDL, LDL)', dept: 'Khoa Xét nghiệm' },
  { id: 'LAB_06', category: 'Xét nghiệm', name: 'Tổng phân tích nước tiểu (10 thông số)', dept: 'Khoa Xét nghiệm' },

  { id: 'IMG_01', category: 'Chẩn đoán hình ảnh', name: 'Chụp X-quang Ngực thẳng', dept: 'Khoa CĐHA' },
  { id: 'IMG_02', category: 'Chẩn đoán hình ảnh', name: 'Chụp X-quang Cột sống thắt lưng', dept: 'Khoa CĐHA' },
  { id: 'IMG_03', category: 'Chẩn đoán hình ảnh', name: 'Siêu âm ổ bụng tổng quát', dept: 'Khoa CĐHA' },
  { id: 'IMG_04', category: 'Chẩn đoán hình ảnh', name: 'Siêu âm Tim Doppler màu', dept: 'Khoa CĐHA' },
  { id: 'IMG_05', category: 'Chẩn đoán hình ảnh', name: 'Chụp CT Scanner Sọ não', dept: 'Khoa CĐHA' },
  { id: 'IMG_06', category: 'Chẩn đoán hình ảnh', name: 'Chụp MRI Cột sống cổ / thắt lưng', dept: 'Khoa CĐHA' },

  { id: 'FUNC_01', category: 'Thăm dò chức năng', name: 'Điện tim thường (ECG 12 chuyển đạo)', dept: 'Khoa TDCN' },
  { id: 'FUNC_02', category: 'Thăm dò chức năng', name: 'Nội soi dạ dày - tá tràng ống mềm', dept: 'Khoa TDCN' },
  { id: 'FUNC_03', category: 'Thăm dò chức năng', name: 'Đo chức năng hô hấp (Phế định ký)', dept: 'Khoa TDCN' },
  { id: 'FUNC_04', category: 'Thăm dò chức năng', name: 'Đo mật độ xương (DEXA scan)', dept: 'Khoa TDCN' },
]

function MedicalEncounter() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // Phân quyền chi tiết (Mục XI)
  const isDoctor = useMemo(() => {
    return user?.roles?.some((role) =>
      ['admin', 'doctor', 'role_admin', 'role_doctor'].includes(String(role).toLowerCase())
    )
  }, [user])

  const isNurse = useMemo(() => {
    return user?.roles?.some((role) =>
      ['nurse', 'role_nurse'].includes(String(role).toLowerCase())
    )
  }, [user])

  const isReceptionistOrPharmacist = useMemo(() => {
    return user?.roles?.some((role) =>
      ['receptionist', 'pharmacist', 'role_receptionist', 'role_pharmacist'].includes(String(role).toLowerCase())
    )
  }, [user])

  const [form] = Form.useForm()
  const [activeTabKey, setActiveTabKey] = useState('1')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  // Data states
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [encounters, setEncounters] = useState([])
  const [medicalRecordsHistory, setMedicalRecordsHistory] = useState([])

  // State chẩn đoán
  const [secondaryDiagnoses, setSecondaryDiagnoses] = useState([])
  const [newSecondaryDiag, setNewSecondaryDiag] = useState('')

  // State sinh tồn & BMI
  const [vitals, setVitals] = useState({
    pulse: '',
    bpSystolic: '',
    bpDiastolic: '',
    temperature: '',
    respRate: '',
    weight: '',
    height: '',
    spO2: '',
  })

  // State chỉ định CLS
  const [ordersList, setOrdersList] = useState([])
  const [selectedServiceId, setSelectedServiceId] = useState(null)
  const [orderPriority, setOrderPriority] = useState('REGULAR')
  const [orderNote, setOrderNote] = useState('')

  // State nhập kết quả & file đính kèm
  const [resultModalOpen, setResultModalOpen] = useState(false)
  const [activeOrderForResult, setActiveOrderForResult] = useState(null)
  const [resultText, setResultText] = useState('')
  const [conclusionText, setConclusionText] = useState('')
  const [refRangeText, setRefRangeText] = useState('')
  const [resultNoteText, setResultNoteText] = useState('')
  const [attachments, setAttachments] = useState([])

  // Lock status
  const [recordStatus, setRecordStatus] = useState('DRAFT') // DRAFT | IN_PROGRESS | COMPLETED | LOCKED
  const [confirmLockModalOpen, setConfirmLockModalOpen] = useState(false)
  const [viewHistoryModalRecord, setViewHistoryModalRecord] = useState(null)

  // Nạp danh sách Bệnh nhân & Lượt khám ban đầu
  const loadInitialData = useCallback(async () => {
    setLoading(true)
    try {
      const [patientRes, recordRes] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 300 }),
        medicalRecordApi.getAll(),
      ])

      // Safe normalization (Mục XII)
      let patientData = []
      if (patientRes.status === 'fulfilled') {
        const rawP = patientRes.value?.data?.content ?? patientRes.value?.data?.data ?? patientRes.value?.data?.items ?? patientRes.value?.data ?? []
        patientData = Array.isArray(rawP) ? rawP : []
      }
      setPatients(patientData)

      let recordData = []
      if (recordRes.status === 'fulfilled') {
        const rawR = recordRes.value?.data?.content ?? recordRes.value?.data?.data ?? recordRes.value?.data?.items ?? recordRes.value?.data ?? []
        recordData = Array.isArray(rawR) ? rawR : []
      }
      const safeHistory = mergeMedicalRecords(recordData)
      setMedicalRecordsHistory(Array.isArray(safeHistory) ? safeHistory : [])
    } catch (err) {
      console.warn('Lỗi nạp dữ liệu bệnh án:', err)
      setMedicalRecordsHistory(mergeMedicalRecords([]))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadInitialData()
  }, [loadInitialData])

  const getPatientAppointmentInfo = useCallback((pId, pName, stateObj) => {
    if (stateObj?.doctorName || stateObj?.appointment?.doctorName) {
      return {
        doctorName: stateObj.doctorName || stateObj.appointment.doctorName,
        department: stateObj.department || stateObj.appointment.department || 'Khoa Khám Bệnh',
      }
    }

    const allApps = mergeAppointments(getStoredAppointments())
    const allQueue = getStoredQueue()

    const foundApp = allApps.find((a) => String(a.patientId) === String(pId) || (pName && a.patientName === pName))
    const foundQueue = allQueue.find((q) => String(q.patientId) === String(pId) || (pName && q.patientName === pName))

    const doctorName = foundApp?.doctorName || foundQueue?.doctorName
    const department = foundApp?.department || foundQueue?.department

    return { doctorName, department }
  }, [])

  // Lắng nghe patientId từ Location State nếu được điều hướng từ hàng đợi
  useEffect(() => {
    if (location.state?.patientId && patients.length > 0) {
      const targetP = patients.find((p) => String(p.id) === String(location.state.patientId))
      if (targetP) {
        setSelectedPatient(targetP)
        const appInfo = getPatientAppointmentInfo(targetP.id, targetP.fullName, location.state)
        const doctorDisplayName = appInfo.doctorName
          || ((user?.fullName && user.fullName !== 'admin' && user.fullName !== user.username) ? user.fullName : 'BS. Phạm Hồng Anh')
        form.setFieldsValue({
          patientId: targetP.id,
          encounterCode: location.state?.encounterCode || `LK-${dayjs().format('YYYYMMDDHHmm')}`,
          attendingDoctor: doctorDisplayName,
          department: appInfo.department || 'Khoa Khám Bệnh',
        })
      }
    }
  }, [location.state, patients, form, user, getPatientAppointmentInfo])

  // Tính BMI tự động (Mục II)
  const bmi = useMemo(() => {
    const w = parseFloat(vitals.weight)
    const h = parseFloat(vitals.height) / 100
    if (w > 0 && h > 0) {
      return (w / (h * h)).toFixed(1)
    }
    return null
  }, [vitals.weight, vitals.height])

  // Xử lý khi chọn Bệnh nhân từ Select
  const handleSelectPatient = (patientId) => {
    const p = patients.find((item) => String(item.id) === String(patientId))
    setSelectedPatient(p || null)
    if (p) {
      const appInfo = getPatientAppointmentInfo(p.id, p.fullName, location.state)
      const doctorDisplayName = appInfo.doctorName
        || ((user?.fullName && user.fullName !== 'admin' && user.fullName !== user.username) ? user.fullName : 'BS. Phạm Hồng Anh')
      form.setFieldsValue({
        encounterCode: `LK-${dayjs().format('YYYYMMDDHHmm')}`,
        attendingDoctor: doctorDisplayName,
        department: appInfo.department || 'Khoa Khám Bệnh',
        allergies: p.allergies || 'Khám phát hiện chưa dị ứng thuốc',
        medicalHistory: p.medicalHistory || 'Chưa ghi nhận tiền sử bệnh mãn tính',
      })
    }
  }

  // Thêm chẩn đoán phụ (Mục IV)
  const handleAddSecondaryDiag = () => {
    const val = newSecondaryDiag.trim()
    if (!val) {
      message.warning('Vui lòng nhập nội dung chẩn đoán phụ')
      return
    }
    if (secondaryDiagnoses.includes(val)) {
      message.error('Chẩn đoán phụ này đã tồn tại trong danh sách')
      return
    }
    setSecondaryDiagnoses((prev) => [...prev, val])
    setNewSecondaryDiag('')
  }

  const handleRemoveSecondaryDiag = (tag) => {
    setSecondaryDiagnoses((prev) => prev.filter((item) => item !== tag))
  }

  // Thêm Chỉ định Cận lâm sàng (Mục V)
  const handleAddClinicalOrder = () => {
    if (!selectedServiceId) {
      message.error('Vui lòng chọn dịch vụ cận lâm sàng')
      return
    }
    const service = CLINICAL_SERVICES_CATALOG.find((s) => s.id === selectedServiceId)
    if (!service) return

    if (ordersList.some((o) => o.serviceId === service.id)) {
      message.error('Dịch vụ này đã được thêm vào danh sách chỉ định')
      return
    }

    const newOrder = {
      id: `ord-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
      orderCode: `CDLS-${Date.now().toString().slice(-6)}`,
      serviceId: service.id,
      serviceName: service.name,
      category: service.category,
      department: service.dept,
      priority: orderPriority,
      note: orderNote.trim(),
      createdAt: new Date().toISOString(),
      status: 'WAITING', // WAITING | IN_PROGRESS | RESULTED | CONFIRMED
      result: '',
      conclusion: '',
      referenceRange: '',
    }

    setOrdersList((prev) => [...prev, newOrder])
    setSelectedServiceId(null)
    setOrderNote('')
    message.success(`Đã thêm chỉ định: ${service.name}`)
  }

  const handleRemoveClinicalOrder = (orderId) => {
    setOrdersList((prev) => prev.filter((item) => item.id !== orderId))
  }

  // Xử lý Upload tệp đính kèm kết quả (Mục VI & VII)
  const beforeFileUpload = (file) => {
    const isAllowedType = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'].includes(file.type)
    if (!isAllowedType) {
      message.error('Định dạng tệp không hợp lệ! Chỉ nhận file PDF, JPG hoặc PNG.')
      return Upload.LIST_IGNORE
    }
    const isUnder10MB = file.size / 1024 / 1024 <= 10
    if (!isUnder10MB) {
      message.error('Kích thước tệp vượt quá giới hạn 10MB!')
      return Upload.LIST_IGNORE
    }

    const isDuplicate = attachments.some((f) => f.name === file.name && f.size === file.size)
    if (isDuplicate) {
      message.warning('Tệp này đã được đính kèm trước đó')
      return Upload.LIST_IGNORE
    }

    const newAttachment = {
      uid: file.uid || `att-${Date.now()}`,
      name: file.name,
      size: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
      type: file.type.includes('pdf') ? 'PDF' : 'IMAGE',
      uploader: user?.fullName || user?.username || 'Bác sĩ',
      uploadedAt: new Date().toISOString(),
      originFileObj: file,
      previewUrl: file.type.includes('image') ? URL.createObjectURL(file) : null,
    }

    setAttachments((prev) => [...prev, newAttachment])
    message.success(`Đã đính kèm tệp: ${file.name}`)
    return false
  }

  const handleRemoveAttachment = (uid) => {
    setAttachments((prev) => prev.filter((item) => item.uid !== uid))
  }

  // Mở Modal Nhập Kết Quả CLS (Mục VI)
  const handleOpenResultModal = (order) => {
    setActiveOrderForResult(order)
    setResultText(order.result || '')
    setConclusionText(order.conclusion || '')
    setRefRangeText(order.referenceRange || '')
    setResultNoteText(order.note || '')
    setResultModalOpen(true)
  }

  const handleSaveOrderResult = () => {
    if (!resultText.trim() && !conclusionText.trim()) {
      message.error('Vui lòng nhập chỉ số kết quả hoặc kết luận chuyên môn')
      return
    }

    setOrdersList((prev) =>
      prev.map((o) => {
        if (o.id === activeOrderForResult.id) {
          return {
            ...o,
            result: resultText.trim(),
            conclusion: conclusionText.trim(),
            referenceRange: refRangeText.trim(),
            note: resultNoteText.trim(),
            status: 'RESULTED',
          }
        }
        return o
      })
    )

    message.success(`Đã lưu kết quả cho chỉ định #${activeOrderForResult.orderCode}`)
    setResultModalOpen(false)
  }

  // Validate form nghiêm ngặt (Mục III)
  const validateFormStrictly = async () => {
    try {
      const values = await form.validateFields()

      // Bắt buộc chặn chuỗi chỉ toàn khoảng trắng
      if (!values.symptoms || !values.symptoms.trim()) {
        message.error('Triệu chứng / Lý do khám không được để trống hoặc chỉ chứa khoảng trắng!')
        setActiveTabKey('3')
        return null
      }
      if (!values.mainDiagnosis || !values.mainDiagnosis.trim()) {
        message.error('Chẩn đoán chính không được để trống hoặc chỉ chứa khoảng trắng!')
        setActiveTabKey('4')
        return null
      }

      return values
    } catch (err) {
      message.error('Vui lòng điền đầy đủ các thông tin bắt buộc (*)!')
      return null
    }
  }

  // Nút LƯU NHÁP (Mục II & X)
  const handleSaveDraft = async () => {
    const values = await validateFormStrictly()
    if (!values) return

    setSaving(true)
    try {
      const currPatient = patients.find((p) => String(p.id) === String(values.patientId)) || selectedPatient
      const patientName = currPatient ? `${currPatient.fullName} (${currPatient.patientCode || 'BN001'})` : 'Bệnh nhân'
      const patientId = currPatient?.id || values.patientId

      const recordPayload = {
        id: `mr-${Date.now()}`,
        recordCode: `BA-${dayjs().format('YYYYMMDDHHmmss')}`,
        patientId,
        patientName,
        doctorName: values.attendingDoctor || user?.fullName || 'BS. Phạm Hồng Anh',
        symptoms: values.symptoms.trim(),
        examinationNote: values.examinationNote?.trim() || '',
        diagnosis: values.mainDiagnosis.trim(),
        icd10Code: values.icd10Code || '',
        secondaryDiagnoses,
        differentialDiagnosis: values.differentialDiagnosis?.trim() || '',
        clinicalNotes: values.clinicalNotes?.trim() || '',
        treatmentPlan: values.treatmentPlan?.trim() || '',
        doctorAdvice: values.doctorAdvice?.trim() || '',
        reExaminationDate: values.reExaminationDate ? values.reExaminationDate.format('YYYY-MM-DD') : '',
        vitals,
        clinicalOrders: ordersList,
        attachments: attachments.map((a) => ({ name: a.name, size: a.size, type: a.type })),
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
      }

      saveStoredMedicalRecord(recordPayload)
      logMedicalAccess({
        userName: user?.fullName || user?.username || 'Bác sĩ',
        patientName: recordPayload.patientName,
        recordCode: recordPayload.recordCode,
        action: 'Lưu nháp bệnh án điện tử',
      })

      setRecordStatus('DRAFT')
      message.success(`Đã lưu nháp bệnh án điện tử #${recordPayload.recordCode}`)
      loadInitialData()
    } catch (err) {
      message.error('Lỗi khi lưu nháp bệnh án. Vui lòng thử lại.')
    } finally {
      setSaving(false)
    }
  }

  // Nút HOÀN TẤT & KHÓA BỆNH ÁN (Mục X)
  const handleConfirmLockAndComplete = async () => {
    const values = await validateFormStrictly()
    if (!values) {
      setConfirmLockModalOpen(false)
      return
    }

    setSaving(true)
    try {
      const currPatient = patients.find((p) => String(p.id) === String(values.patientId)) || selectedPatient
      const patientName = currPatient ? `${currPatient.fullName} (${currPatient.patientCode || 'BN001'})` : 'Bệnh nhân'
      const patientId = currPatient?.id || values.patientId

      const lockedRecordPayload = {
        id: `mr-${Date.now()}`,
        recordCode: `BA-${dayjs().format('YYYYMMDDHHmmss')}`,
        patientId,
        patientName,
        doctorName: values.attendingDoctor || user?.fullName || 'BS. Phạm Hồng Anh',
        symptoms: values.symptoms.trim(),
        examinationNote: values.examinationNote?.trim() || '',
        diagnosis: values.mainDiagnosis.trim(),
        icd10Code: values.icd10Code || '',
        secondaryDiagnoses,
        differentialDiagnosis: values.differentialDiagnosis?.trim() || '',
        clinicalNotes: values.clinicalNotes?.trim() || '',
        treatmentPlan: values.treatmentPlan?.trim() || '',
        doctorAdvice: values.doctorAdvice?.trim() || '',
        reExaminationDate: values.reExaminationDate ? values.reExaminationDate.format('YYYY-MM-DD') : '',
        vitals,
        clinicalOrders: ordersList,
        attachments: attachments.map((a) => ({ name: a.name, size: a.size, type: a.type })),
        status: 'LOCKED',
        createdAt: new Date().toISOString(),
      }

      // 1. Thử gọi REST API
      try {
        await medicalRecordApi.create(lockedRecordPayload)
      } catch (apiErr) {
        console.warn('API error, using local storage fallback:', apiErr)
      }

      saveStoredMedicalRecord(lockedRecordPayload)
      logMedicalAccess({
        userName: user?.fullName || user?.username || 'Bác sĩ',
        patientName: lockedRecordPayload.patientName,
        recordCode: lockedRecordPayload.recordCode,
        action: 'Hoàn tất & Khóa lưu trữ hồ sơ Bệnh án',
      })

      setRecordStatus('LOCKED')
      setConfirmLockModalOpen(false)
      message.success(`Hồ sơ bệnh án #${lockedRecordPayload.recordCode} đã được HOÀN TẤT và KHÓA LƯU TRỮ thành công!`)

      await loadInitialData()

      Modal.confirm({
        title: 'Bệnh án đã được Khóa thành công!',
        content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Kê đơn thuốc) cho bệnh nhân này không?',
        okText: 'Chuyển sang Kê đơn thuốc',
        cancelText: 'Về danh sách Lịch sử',
        onOk: () =>
          navigate('/prescriptions', {
            state: { patientId: values.patientId, recordCode: lockedRecordPayload.recordCode },
          }),
        onCancel: () => setActiveTabKey('9'),
      })
    } catch (err) {
      message.error('Có lỗi xảy ra khi khóa bệnh án.')
    } finally {
      setSaving(false)
    }
  }

  // Nút HỦY / TẠO MỚI (Mục II)
  const handleResetForm = () => {
    form.resetFields()
    setSelectedPatient(null)
    setSecondaryDiagnoses([])
    setVitals({ pulse: '', bpSystolic: '', bpDiastolic: '', temperature: '', respRate: '', weight: '', height: '', spO2: '' })
    setOrdersList([])
    setAttachments([])
    setRecordStatus('DRAFT')
    message.info('Đã xóa dữ liệu trên form và làm mới trạng thái khám')
  }

  // Nếu người dùng là Lễ tân hoặc Dược sĩ -> Hiển thị Alert 403 (Mục XI)
  if (isReceptionistOrPharmacist) {
    return (
      <div style={{ maxWidth: 900, margin: '40px auto', padding: 24 }}>
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined style={{ fontSize: 24 }} />}
          message="403 - TRUY CẬP BỊ TỪ CHỐI"
          description="Bạn đang đăng nhập với vai trò Lễ tân / Dược sĩ. Chức năng Ghi và Quản lý Bệnh án Điện tử chỉ dành riêng cho Bác sĩ điều trị và Quản trị viên hệ thống."
          action={
            <Button type="primary" onClick={() => navigate('/appointments')}>
              Quay về Hàng đợi khám
            </Button>
          }
        />
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', paddingBottom: 40 }}>
      {/* Top Header Bar & Action Buttons (Mục II) */}
      <Card
        style={{
          marginBottom: 16,
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
          background: 'linear-gradient(to right, #ffffff, #f8fafc)',
        }}
      >
        <Row align="middle" justify="space-between" gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Space align="center" size="middle">
              <Avatar size={48} icon={<FileDoneOutlined />} style={{ backgroundColor: '#2563eb' }} />
              <div>
                <Title level={4} style={{ margin: 0, color: '#0f172a' }}>
                  Khám Bệnh & Bệnh Án Điện Tử
                </Title>
                <Text type="secondary">
                  Hệ thống ghi nhận thông tin bệnh án, chẩn đoán ICD-10 và chỉ định Cận lâm sàng
                </Text>
              </div>
            </Space>
          </Col>

          {/* Thanh 5 Nút Bấm Thao Tác (Mục II) */}
          <Col xs={24} md={12} style={{ textAlign: 'right' }}>
            <Space wrap size="small">
              <Button icon={<SaveOutlined />} loading={saving} onClick={handleSaveDraft} disabled={recordStatus === 'LOCKED'}>
                Lưu nháp
              </Button>
              <Button
                type="primary"
                icon={<LockOutlined />}
                loading={saving}
                onClick={() => setConfirmLockModalOpen(true)}
                disabled={recordStatus === 'LOCKED' || isNurse}
                style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
              >
                Hoàn tất bệnh án
              </Button>
              <Popconfirm title="Xóa dữ liệu đang nhập trên form?" onConfirm={handleResetForm} okText="Đồng ý" cancelText="Hủy">
                <Button danger disabled={recordStatus === 'LOCKED'}>
                  Hủy
                </Button>
              </Popconfirm>
              <Button icon={<PrinterOutlined />} onClick={() => window.print()}>
                In bệnh án
              </Button>
              <Button icon={<HistoryOutlined />} onClick={() => setActiveTabKey('9')}>
                Xem lịch sử
              </Button>
            </Space>
          </Col>
        </Row>

        {recordStatus === 'LOCKED' && (
          <Alert
            type="success"
            showIcon
            message="Hồ sơ bệnh án đã ở trạng thái ĐÃ KHÓA (LOCKED)"
            description="Bệnh án đã hoàn tất và đóng lưu trữ. Không thể chỉnh sửa trực tiếp."
            style={{ marginTop: 12, borderRadius: 8 }}
          />
        )}
      </Card>

      {/* Main Container - 9 Tab chuyên biệt (Mục XV) */}
      <Spin spinning={loading}>
        <Form form={form} layout="vertical" disabled={recordStatus === 'LOCKED'}>
          <Tabs
            activeKey={activeTabKey}
            onChange={setActiveTabKey}
            type="card"
            style={{ background: '#fff', padding: 16, borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}
            items={[
              // TAB 1: THÔNG TIN LƯỢT KHÁM (Mục II.1 & II.2)
              {
                key: '1',
                label: (
                  <span>
                    <UserOutlined /> 1. Thông tin lượt khám
                  </span>
                ),
                children: (
                  <Card title="Hành chính Bệnh nhân & Lượt khám" size="small" style={{ borderRadius: 8 }}>
                    <Row gutter={16}>
                      <Col xs={24} md={12}>
                        <Form.Item
                          name="patientId"
                          label="Bệnh nhân (*)"
                          rules={[{ required: true, message: 'Vui lòng chọn Bệnh nhân' }]}
                        >
                          <Select
                            showSearch
                            placeholder="Tìm bệnh nhân theo Mã hoặc Họ tên..."
                            optionFilterProp="label"
                            onChange={handleSelectPatient}
                            options={patients.map((p) => ({
                              value: p.id,
                              label: `${p.fullName} - ${p.patientCode} (${p.phone || 'N/A'})`,
                            }))}
                          />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={12}>
                        <Form.Item name="encounterCode" label="Mã lượt khám (*)">
                          <Input readOnly placeholder="Mã tự động LK-YYYYMMDDHHmm" />
                        </Form.Item>
                      </Col>
                    </Row>

                    {selectedPatient && (
                      <Descriptions bordered column={{ xs: 1, sm: 2, md: 4 }} size="small" style={{ marginTop: 8, marginBottom: 16 }}>
                        <Descriptions.Item label="Mã BN">{selectedPatient.patientCode}</Descriptions.Item>
                        <Descriptions.Item label="Họ tên">{selectedPatient.fullName}</Descriptions.Item>
                        <Descriptions.Item label="Ngày sinh">
                          {selectedPatient.dob ? dayjs(selectedPatient.dob).format('DD/MM/YYYY') : '---'}
                        </Descriptions.Item>
                        <Descriptions.Item label="Giới tính">{selectedPatient.gender === 'MALE' ? 'Nam' : 'Nữ'}</Descriptions.Item>
                        <Descriptions.Item label="Điện thoại">{selectedPatient.phone || '---'}</Descriptions.Item>
                        <Descriptions.Item label="Địa chỉ">{selectedPatient.address || '---'}</Descriptions.Item>
                        <Descriptions.Item label="Dị ứng">
                          <Tag color="red">{selectedPatient.allergies || 'Chưa ghi nhận'}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Bệnh nền">{selectedPatient.medicalHistory || 'Không'}</Descriptions.Item>
                      </Descriptions>
                    )}

                    <Row gutter={16}>
                      <Col xs={24} md={8}>
                        <Form.Item name="attendingDoctor" label="Bác sĩ phụ trách (*)">
                          <Input readOnly />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="department" label="Khoa/Phòng khám">
                          <Input placeholder="Ví dụ: Khoa Khám Bệnh Nội Tổng hợp" />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item label="Trạng thái lượt khám">
                          <Tag color={recordStatus === 'LOCKED' ? 'red' : 'processing'} style={{ fontSize: 13, padding: '4px 12px' }}>
                            {recordStatus === 'LOCKED' ? 'Đã khóa (LOCKED)' : 'Đang khám (IN_PROGRESS)'}
                          </Tag>
                        </Form.Item>
                      </Col>
                    </Row>
                  </Card>
                ),
              },

              // TAB 2: DẤU HIỆU SINH TỒN (Mục II.3)
              {
                key: '2',
                label: (
                  <span>
                    <HeartOutlined /> 2. Dấu hiệu sinh tồn
                  </span>
                ),
                children: (
                  <Card title="Chỉ số Sinh tồn (Vitals)" size="small" style={{ borderRadius: 8 }}>
                    <Row gutter={[16, 16]}>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Mạch (lần/phút):</Text>
                        <Input
                          value={vitals.pulse}
                          onChange={(e) => setVitals({ ...vitals, pulse: e.target.value })}
                          placeholder="VD: 75"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Huyết áp Tâm thu (mmHg):</Text>
                        <Input
                          value={vitals.bpSystolic}
                          onChange={(e) => setVitals({ ...vitals, bpSystolic: e.target.value })}
                          placeholder="VD: 120"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Huyết áp Tâm trương (mmHg):</Text>
                        <Input
                          value={vitals.bpDiastolic}
                          onChange={(e) => setVitals({ ...vitals, bpDiastolic: e.target.value })}
                          placeholder="VD: 80"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Nhiệt độ (°C):</Text>
                        <Input
                          value={vitals.temperature}
                          onChange={(e) => setVitals({ ...vitals, temperature: e.target.value })}
                          placeholder="VD: 36.8"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Nhịp thở (lần/phút):</Text>
                        <Input
                          value={vitals.respRate}
                          onChange={(e) => setVitals({ ...vitals, respRate: e.target.value })}
                          placeholder="VD: 18"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>SpO2 (%):</Text>
                        <Input
                          value={vitals.spO2}
                          onChange={(e) => setVitals({ ...vitals, spO2: e.target.value })}
                          placeholder="VD: 98"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Cân nặng (kg):</Text>
                        <Input
                          value={vitals.weight}
                          onChange={(e) => setVitals({ ...vitals, weight: e.target.value })}
                          placeholder="VD: 62"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                      <Col xs={12} sm={8} md={6}>
                        <Text strong>Chiều cao (cm):</Text>
                        <Input
                          value={vitals.height}
                          onChange={(e) => setVitals({ ...vitals, height: e.target.value })}
                          placeholder="VD: 168"
                          style={{ marginTop: 4 }}
                        />
                      </Col>
                    </Row>

                    <Divider style={{ margin: '16px 0' }} />
                    <Alert
                      type={bmi && (bmi < 18.5 || bmi > 25) ? 'warning' : 'info'}
                      showIcon
                      message={`Chỉ số BMI tự động: ${bmi || '---'} kg/m²`}
                      description={
                        bmi
                          ? bmi < 18.5
                            ? 'Thể trạng gầy (Nhẹ cân)'
                            : bmi <= 22.9
                            ? 'Thể trạng bình thường'
                            : bmi <= 24.9
                            ? 'Thể trạng tiền béo phì (Thừa cân)'
                            : 'Thể trạng béo phì'
                          : 'Vui lòng nhập chiều cao và cân nặng để hệ thống tự động tính chỉ số BMI'
                      }
                    />
                  </Card>
                ),
              },

              // TAB 3: KHÁM LÂM SÀNG (Mục II.3 & III)
              {
                key: '3',
                label: (
                  <span>
                    <FileTextOutlined /> 3. Khám lâm sàng
                  </span>
                ),
                children: (
                  <Card title="Nội dung Khám lâm sàng & Diễn biến" size="small" style={{ borderRadius: 8 }}>
                    <Form.Item
                      name="symptoms"
                      label="Triệu chứng chính / Lý do vào khám (*)"
                      rules={[{ required: true, message: 'Triệu chứng không được để trống' }]}
                    >
                      <Input.TextArea rows={3} placeholder="Ví dụ: Đau ngực trái lan ra sau lưng, ho kéo dài 3 ngày..." />
                    </Form.Item>

                    <Row gutter={16}>
                      <Col xs={24} md={12}>
                        <Form.Item name="medicalHistory" label="Tiền sử bệnh bản thân & Gia đình">
                          <Input.TextArea rows={3} placeholder="Tiền sử dị ứng thuốc, tiểu đường, cao huyết áp..." />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={12}>
                        <Form.Item name="allergies" label="Dị ứng cần lưu ý">
                          <Input.TextArea rows={3} placeholder="Dị ứng Penicillin, hải sản, thời tiết..." />
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item name="examinationNote" label="Khám cơ quan & Diễn biến lâm sàng">
                      <Input.TextArea
                        rows={4}
                        placeholder="Khám tim mạch: Tiếng T1 T2 đều; Khám hô hấp: Phổi thông khí rõ, không rale..."
                      />
                    </Form.Item>
                  </Card>
                ),
              },

              // TAB 4: CHẨN ĐOÁN (Mục IV)
              {
                key: '4',
                label: (
                  <span>
                    <ExperimentOutlined /> 4. Chẩn đoán (ICD-10)
                  </span>
                ),
                children: (
                  <Card title="Phân tách Chẩn đoán chuyên môn & Danh mục ICD-10" size="small" style={{ borderRadius: 8 }}>
                    <Row gutter={16}>
                      <Col xs={24} md={16}>
                        <Form.Item
                          name="mainDiagnosis"
                          label="Chẩn đoán chính (*)"
                          rules={[{ required: true, message: 'Chẩn đoán chính không được để trống' }]}
                        >
                          <Input placeholder="Ví dụ: Viêm phế quản cấp / Tăng huyết áp vô căn..." />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="icd10Code" label="Tra cứu Mã ICD-10 tiêu chuẩn">
                          <Select
                            showSearch
                            allowClear
                            placeholder="Chọn từ danh mục ICD-10..."
                            optionFilterProp="label"
                            onChange={(val) => {
                              const match = ICD10_CATALOG.find((item) => item.code === val)
                              if (match) {
                                form.setFieldsValue({ mainDiagnosis: match.name })
                              }
                            }}
                            options={ICD10_CATALOG.map((item) => ({
                              value: item.code,
                              label: `${item.code} - ${item.name}`,
                            }))}
                          />
                        </Form.Item>
                      </Col>
                    </Row>

                    {/* Chẩn đoán phụ tags (Mục IV) */}
                    <div style={{ marginBottom: 16 }}>
                      <Text strong style={{ display: 'block', marginBottom: 8 }}>
                        Chẩn đoán phụ kèm theo (Thêm nhiều tags, chống chọn trùng):
                      </Text>
                      <Space wrap style={{ marginBottom: 8 }}>
                        {secondaryDiagnoses.map((tag) => (
                          <Tag key={tag} color="blue" closable onClose={() => handleRemoveSecondaryDiag(tag)}>
                            {tag}
                          </Tag>
                        ))}
                      </Space>
                      {recordStatus !== 'LOCKED' && (
                        <Space.Compact style={{ width: '100%', maxWidth: 500 }}>
                          <Input
                            placeholder="Nhập tên chẩn đoán phụ..."
                            value={newSecondaryDiag}
                            onChange={(e) => setNewSecondaryDiag(e.target.value)}
                            onPressEnter={handleAddSecondaryDiag}
                          />
                          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddSecondaryDiag}>
                            Thêm
                          </Button>
                        </Space.Compact>
                      )}
                    </div>

                    <Row gutter={16}>
                      <Col xs={24} md={12}>
                        <Form.Item name="differentialDiagnosis" label="Chẩn đoán phân biệt">
                          <Input.TextArea rows={3} placeholder="Phân biệt với Viêm dạ dày ruột cấp, Cơn đau thắt ngực..." />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={12}>
                        <Form.Item name="clinicalNotes" label="Ghi chú chuyên môn Bác sĩ">
                          <Input.TextArea rows={3} placeholder="Ghi chú theo dõi đặc biệt, đánh giá nguy cơ..." />
                        </Form.Item>
                      </Col>
                    </Row>
                  </Card>
                ),
              },

              // TAB 5: CHỈ ĐỊNH CẬN LÂM SÀNG (Mục V)
              {
                key: '5',
                label: (
                  <span>
                    <InboxOutlined /> 5. Chỉ định Cận lâm sàng ({ordersList.length})
                  </span>
                ),
                children: (
                  <Card title="Chỉ định Dịch vụ Cận lâm sàng (Xét nghiệm, CĐHA, TDCN)" size="small" style={{ borderRadius: 8 }}>
                    {recordStatus !== 'LOCKED' && (
                      <div style={{ background: '#f8fafc', padding: 16, borderRadius: 8, marginBottom: 16 }}>
                        <Row gutter={[12, 12]} align="middle">
                          <Col xs={24} md={10}>
                            <Text strong style={{ display: 'block', marginBottom: 4 }}>
                              Chọn Dịch vụ Cận lâm sàng (Phân nhóm):
                            </Text>
                            <Select
                              showSearch
                              style={{ width: '100%' }}
                              placeholder="Tìm theo tên dịch vụ hoặc loại dịch vụ..."
                              value={selectedServiceId}
                              onChange={setSelectedServiceId}
                              optionFilterProp="label"
                              options={CLINICAL_SERVICES_CATALOG.map((s) => ({
                                value: s.id,
                                label: `[${s.category}] ${s.name} (${s.dept})`,
                              }))}
                            />
                          </Col>
                          <Col xs={12} md={4}>
                            <Text strong style={{ display: 'block', marginBottom: 4 }}>
                              Mức ưu tiên:
                            </Text>
                            <Select value={orderPriority} onChange={setOrderPriority} style={{ width: '100%' }}>
                              <Select.Option value="REGULAR">Thường</Select.Option>
                              <Select.Option value="EMERGENCY">
                                <Text type="danger" strong>
                                  Khẩn cấp
                                </Text>
                              </Select.Option>
                            </Select>
                          </Col>
                          <Col xs={12} md={6}>
                            <Text strong style={{ display: 'block', marginBottom: 4 }}>
                              Ghi chú chỉ định:
                            </Text>
                            <Input
                              placeholder="Ghi chú lâm sàng..."
                              value={orderNote}
                              onChange={(e) => setOrderNote(e.target.value)}
                            />
                          </Col>
                          <Col xs={24} md={4} style={{ marginTop: 20 }}>
                            <Button type="primary" block icon={<PlusOutlined />} onClick={handleAddClinicalOrder}>
                              Thêm chỉ định
                            </Button>
                          </Col>
                        </Row>
                      </div>
                    )}

                    {/* Bảng danh sách chỉ định (Mục V) */}
                    <Table
                      rowKey="id"
                      size="small"
                      dataSource={ordersList}
                      pagination={false}
                      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có chỉ định Cận lâm sàng" /> }}
                      columns={[
                        { title: 'Mã CĐ', dataIndex: 'orderCode', key: 'orderCode', render: (v) => <Tag color="blue">{v}</Tag> },
                        { title: 'Tên Dịch vụ', dataIndex: 'serviceName', key: 'serviceName' },
                        { title: 'Loại Dịch vụ', dataIndex: 'category', key: 'category', render: (c) => <Tag color="cyan">{c}</Tag> },
                        { title: 'Khoa thực hiện', dataIndex: 'department', key: 'department' },
                        {
                          title: 'Ưu tiên',
                          dataIndex: 'priority',
                          key: 'priority',
                          render: (p) => (p === 'EMERGENCY' ? <Tag color="red">KHẨN</Tag> : <Tag color="default">Thường</Tag>),
                        },
                        {
                          title: 'Trạng thái',
                          dataIndex: 'status',
                          key: 'status',
                          render: (st) => {
                            if (st === 'RESULTED') return <Tag color="green">Đã có kết quả</Tag>
                            if (st === 'CONFIRMED') return <Tag color="purple">Đã xác nhận</Tag>
                            return <Tag color="gold">Chờ thực hiện</Tag>
                          },
                        },
                        {
                          title: 'Thao tác',
                          key: 'action',
                          render: (_, record) => (
                            <Space size="small">
                              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleOpenResultModal(record)}>
                                Nhập KQ
                              </Button>
                              {recordStatus !== 'LOCKED' && (
                                <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleRemoveClinicalOrder(record.id)}>
                                  Xóa
                                </Button>
                              )}
                            </Space>
                          ),
                        },
                      ]}
                    />
                  </Card>
                ),
              },

              // TAB 6: KẾT QUẢ & FILE ĐÍNH KÈM (Mục VI & VII)
              {
                key: '6',
                label: (
                  <span>
                    <CheckCircleOutlined /> 6. Kết quả & File ({attachments.length})
                  </span>
                ),
                children: (
                  <Card title="Kết quả Cận lâm sàng & Tệp đính kèm (PDF/JPG/PNG <= 10MB)" size="small" style={{ borderRadius: 8 }}>
                    <List
                      header={<Text strong>Kết quả Cận lâm sàng chi tiết:</Text>}
                      dataSource={ordersList.filter((o) => o.status === 'RESULTED' || o.result)}
                      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có kết quả Cận lâm sàng" /> }}
                      renderItem={(item) => (
                        <List.Item>
                          <List.Item.Meta
                            title={
                              <Space>
                                <Tag color="blue">{item.orderCode}</Tag>
                                <Text strong>{item.serviceName}</Text>
                              </Space>
                            }
                            description={
                              <div>
                                <div>
                                  <Text type="secondary">Kết quả / Chỉ số:</Text> <Text strong>{item.result || '---'}</Text>
                                </div>
                                <div>
                                  <Text type="secondary">Kết luận:</Text> {item.conclusion || '---'}
                                </div>
                              </div>
                            }
                          />
                        </List.Item>
                      )}
                    />

                    <Divider style={{ margin: '16px 0' }} />

                    <div style={{ marginBottom: 16 }}>
                      <Text strong style={{ display: 'block', marginBottom: 8 }}>
                        {'Tệp đính kèm (PDF, JPG, PNG <= 10MB):'}
                      </Text>
                      {recordStatus !== 'LOCKED' && (
                        <Upload beforeUpload={beforeFileUpload} showUploadList={false}>
                          <Button icon={<UploadOutlined />}>Đính kèm tệp PDF / Ảnh kết quả</Button>
                        </Upload>
                      )}

                      <Table
                        style={{ marginTop: 12 }}
                        rowKey="uid"
                        size="small"
                        dataSource={attachments}
                        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tệp đính kèm" /> }}
                        columns={[
                          { title: 'Tên file', dataIndex: 'name', key: 'name' },
                          { title: 'Dung lượng', dataIndex: 'size', key: 'size', width: 120 },
                          { title: 'Loại file', dataIndex: 'type', key: 'type', width: 100, render: (t) => <Tag color="purple">{t}</Tag> },
                          { title: 'Người tải', dataIndex: 'uploader', key: 'uploader', width: 150 },
                          {
                            title: 'Thao tác',
                            key: 'act',
                            width: 120,
                            render: (_, rec) => (
                              <Space size="small">
                                {rec.previewUrl && (
                                  <Tooltip title="Xem ảnh">
                                    <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => window.open(rec.previewUrl, '_blank')} />
                                  </Tooltip>
                                )}
                                {recordStatus !== 'LOCKED' && (
                                  <Button size="small" type="link" danger icon={<DeleteOutlined />} onClick={() => handleRemoveAttachment(rec.uid)} />
                                )}
                              </Space>
                            ),
                          },
                        ]}
                      />
                    </div>
                  </Card>
                ),
              },

              // TAB 7: ĐƠN THUỐC (Mục II.3 & XV)
              {
                key: '7',
                label: (
                  <span>
                    <FilePdfOutlined /> 7. Đơn thuốc
                  </span>
                ),
                children: (
                  <Card title="Hướng điều trị & Chỉ dẫn Kê đơn thuốc" size="small" style={{ borderRadius: 8 }}>
                    <Form.Item name="treatmentPlan" label="Hướng điều trị & Phác đồ">
                      <Input.TextArea rows={4} placeholder="Ví dụ: Điều trị nội khoa ngoại trú, uống thuốc theo đơn 7 ngày..." />
                    </Form.Item>

                    <Alert
                      type="info"
                      showIcon
                      message="Chuyển sang module Kê đơn thuốc chuyên sâu"
                      description="Sau khi lưu hoặc hoàn tất bệnh án, Bác sĩ có thể bấm nút chuyển nhanh sang module Quản lý Kê đơn thuốc để chọn thuốc từ kho Dược."
                    />
                  </Card>
                ),
              },

              // TAB 8: LỜI DẶN BÁC SĨ (Mục II.3 & XV)
              {
                key: '8',
                label: (
                  <span>
                    <InfoCircleOutlined /> 8. Lời dặn Bác sĩ
                  </span>
                ),
                children: (
                  <Card title="Lời dặn & Hẹn tái khám" size="small" style={{ borderRadius: 8 }}>
                    <Form.Item name="doctorAdvice" label="Lời dặn Bác sĩ dành cho bệnh nhân">
                      <Input.TextArea rows={4} placeholder="Uống nhiều nước, ăn đồ mềm, kiêng rượu bia, nghỉ ngơi hợp lý..." />
                    </Form.Item>

                    <Form.Item name="reExaminationDate" label="Ngày hẹn tái khám (nếu có)">
                      <DatePicker format="DD/MM/YYYY" style={{ width: 260 }} placeholder="Chọn ngày tái khám" />
                    </Form.Item>
                  </Card>
                ),
              },

              // TAB 9: LỊCH SỬ BỆNH ÁN (Mục IX & XV)
              {
                key: '9',
                label: (
                  <span>
                    <HistoryOutlined /> 9. Lịch sử bệnh án ({medicalRecordsHistory.length})
                  </span>
                ),
                children: (
                  <Card title="Lịch sử các lượt khám bệnh án điện tử" size="small" style={{ borderRadius: 8 }}>
                    <Table
                      rowKey="id"
                      size="small"
                      dataSource={medicalRecordsHistory}
                      pagination={{ pageSize: 8 }}
                      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có lịch sử khám" /> }}
                      columns={[
                        { title: 'Mã Bệnh Án', dataIndex: 'recordCode', key: 'recordCode', render: (v) => <Tag color="green">{v}</Tag> },
                        { title: 'Bệnh nhân', dataIndex: 'patientName', key: 'patientName' },
                        { title: 'Chẩn đoán chính', dataIndex: 'diagnosis', key: 'diagnosis' },
                        { title: 'Bác sĩ phụ trách', dataIndex: 'doctorName', key: 'doctorName', render: (v) => (v === 'admin' || !v ? 'BS. Phạm Hồng Anh' : v) },
                        { title: 'Ngày khám', dataIndex: 'createdAt', key: 'createdAt', render: (v) => dayjs(v).format('HH:mm DD/MM/YYYY') },
                        {
                          title: 'Trạng thái',
                          dataIndex: 'status',
                          key: 'status',
                          render: (s) => (s === 'LOCKED' ? <Tag color="purple">Đã khóa</Tag> : <Tag color="blue">Hoàn thành</Tag>),
                        },
                        {
                          title: 'Thao tác',
                          key: 'act',
                          render: (_, record) => (
                            <Button size="small" icon={<EyeOutlined />} onClick={() => setViewHistoryModalRecord(record)}>
                              Xem chi tiết
                            </Button>
                          ),
                        },
                      ]}
                    />
                  </Card>
                ),
              },
            ]}
          />
        </Form>
      </Spin>

      {/* Modal Nhập Kết Quả Cận Lâm Sàng (Mục VI) */}
      <Modal
        title={`Nhập kết quả Cận lâm sàng #${activeOrderForResult?.orderCode || ''}`}
        open={resultModalOpen}
        onCancel={() => setResultModalOpen(false)}
        onOk={handleSaveOrderResult}
        okText="Lưu kết quả"
        cancelText="Hủy bỏ"
      >
        {activeOrderForResult && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={1}>
              <Descriptions.Item label="Dịch vụ">{activeOrderForResult.serviceName}</Descriptions.Item>
              <Descriptions.Item label="Loại CLS">{activeOrderForResult.category}</Descriptions.Item>
              <Descriptions.Item label="Khoa thực hiện">{activeOrderForResult.department}</Descriptions.Item>
            </Descriptions>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 4 }}>
                Chỉ số kết quả (*):
              </Text>
              <Input.TextArea
                rows={3}
                value={resultText}
                onChange={(e) => setResultText(e.target.value)}
                placeholder="Ví dụ: RBC: 4.8 T/L, Hb: 142 g/L, WBC: 7.2 G/L..."
              />
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 4 }}>
                Chỉ số tham chiếu tiêu chuẩn:
              </Text>
              <Input
                value={refRangeText}
                onChange={(e) => setRefRangeText(e.target.value)}
                placeholder="Ví dụ: Nam: 130-160 g/L; Nữ: 120-150 g/L"
              />
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 4 }}>
                Kết luận chuyên môn Bác sĩ/KTV (*):
              </Text>
              <Input.TextArea
                rows={2}
                value={conclusionText}
                onChange={(e) => setConclusionText(e.target.value)}
                placeholder="Ví dụ: Công thức máu trong giới hạn bình thường..."
              />
            </div>
          </Space>
        )}
      </Modal>

      {/* Modal Xác nhận Hoàn tất & Khóa Bệnh án (Mục X) */}
      <Modal
        title="Xác nhận Hoàn tất & Khóa Bệnh án"
        open={confirmLockModalOpen}
        onCancel={() => setConfirmLockModalOpen(false)}
        onOk={handleConfirmLockAndComplete}
        okText="Khóa bệnh án"
        okButtonProps={{ danger: true, loading: saving }}
        cancelText="Hủy bỏ"
      >
        <Alert
          type="warning"
          showIcon
          message="Lưu ý pháp lý quan trọng khi khóa bệnh án!"
          description="Sau khi bấm xác nhận 'Khóa bệnh án', hồ sơ bệnh án sẽ chuyển sang trạng thái ĐÃ KHÓA (LOCKED). Bạn sẽ không thể trực tiếp sửa đổi thông tin khám ban đầu. Mọi bổ sung sau đó phải ghi kèm Lý do bổ sung."
          style={{ marginBottom: 16 }}
        />
        <Paragraph>Bạn có chắc chắn muốn khóa lưu trữ bệnh án này không?</Paragraph>
      </Modal>

      {/* Modal Xem chi tiết Bệnh án từ Lịch sử (Mục IX) */}
      <Modal
        title={`Chi tiết Hồ sơ Bệnh án ${viewHistoryModalRecord?.recordCode || ''}`}
        open={!!viewHistoryModalRecord}
        onCancel={() => setViewHistoryModalRecord(null)}
        footer={<Button onClick={() => setViewHistoryModalRecord(null)}>Đóng</Button>}
        width={800}
      >
        {viewHistoryModalRecord && (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Bệnh nhân">{viewHistoryModalRecord.patientName}</Descriptions.Item>
            <Descriptions.Item label="Bác sĩ phụ trách">{viewHistoryModalRecord.doctorName}</Descriptions.Item>
            <Descriptions.Item label="Triệu chứng">{viewHistoryModalRecord.symptoms}</Descriptions.Item>
            <Descriptions.Item label="Khám lâm sàng">{viewHistoryModalRecord.examinationNote || '---'}</Descriptions.Item>
            <Descriptions.Item label="Chẩn đoán chính">{viewHistoryModalRecord.diagnosis}</Descriptions.Item>

            <Descriptions.Item label="Chẩn đoán phụ">
              {viewHistoryModalRecord.secondaryDiagnoses?.length
                ? viewHistoryModalRecord.secondaryDiagnoses.join(', ')
                : 'Không có'}
            </Descriptions.Item>

            <Descriptions.Item label="Hướng điều trị">{viewHistoryModalRecord.treatmentPlan || '---'}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color="purple">{viewHistoryModalRecord.status}</Tag>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MedicalEncounter
