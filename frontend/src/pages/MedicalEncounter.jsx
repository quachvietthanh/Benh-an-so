import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  List,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  EyeOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SearchOutlined,
  SolutionOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'
import { logMedicalAccess, mergeMedicalRecords, saveStoredMedicalRecord, getStoredQueueItems, saveStoredQueueItem, saveStoredClinicalOrder, mergeQueues } from '../utils/storageHelpers'
import { saveStoredAttachment } from '../utils/attachmentHelpers'
import { commonIcd10List, icd10Categories, searchIcd10 } from '../utils/icd10Data'
import { clinicalServiceCatalog } from '../utils/clinicalCatalogData'
import ClinicalOrderPrintModal from '../components/clinical/ClinicalOrderPrintModal'
import MedicalEncounterForm from '../components/clinical/MedicalEncounterForm'

const { Text, Paragraph } = Typography

function MedicalEncounter() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const isDoctor = user?.roles?.some((role) =>
    ['admin', 'doctor', 'role_admin', 'role_doctor'].includes(String(role).toLowerCase())
  )

  const [form] = Form.useForm()
  const [patients, setPatients] = useState([])
  const [records, setRecords] = useState([])
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('current')
  const [viewing, setViewing] = useState(null)

  // Selected Patient State
  const [selectedPatientId, setSelectedPatientId] = useState(null)
  const [currentVisitId, setCurrentVisitId] = useState(location.state?.visitId || null)

  // Vital Signs State
  const [vitalSigns, setVitalSigns] = useState({
    bp: '',
    pulse: '',
    temp: '37.0',
    respRate: '16',
    weight: '',
    height: '',
    spO2: '98',
  })

  // Diagnosis State
  const [diagnosisType, setDiagnosisType] = useState('DEFINITIVE') // PRELIMINARY, DEFINITIVE, DIFFERENTIAL
  const [primaryIcd, setPrimaryIcd] = useState(null)
  const [secondaryIcds, setSecondaryIcds] = useState([])
  const [diagnosisModalOpen, setDiagnosisModalOpen] = useState(false)
  const [icdSearchQuery, setIcdSearchQuery] = useState('')
  const [icdCategory, setIcdCategory] = useState('ALL')
  const [backendIcdCatalog, setBackendIcdCatalog] = useState([])

  // Clinical Orders State
  const [selectedOrders, setSelectedOrders] = useState([])
  const [orderCategory, setOrderCategory] = useState('ALL')
  const [orderSearchQuery, setOrderSearchQuery] = useState('')

  // Results & Attachments State
  const [results, setResults] = useState({})
  const [files, setFiles] = useState([])

  // Print Order Modal State
  const [printModalOpen, setPrintModalOpen] = useState(false)

  // Pre-fill from navigation state
  useEffect(() => {
    if (location.state?.patientId) {
      setSelectedPatientId(location.state.patientId)
      form.setFieldsValue({ patientId: location.state.patientId })
    }
    if (location.state?.visitId) {
      setCurrentVisitId(location.state.visitId)
    }
  }, [location.state, form])

  const loadData = useCallback(async () => {
    try {
      const [patientResponse, recordResponse, queueResponse] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 200 }),
        medicalRecordApi.getAll(),
        queueApi.getQueues({ date: dayjs().format('YYYY-MM-DD') }),
      ])

      const allPatients = patientResponse.status === 'fulfilled'
        ? (patientResponse.value.data?.content || [])
        : []
      
      const apiQueues = queueResponse.status === 'fulfilled'
        ? (queueResponse.value.data || [])
        : []

      const todayQueues = mergeQueues(apiQueues)

      // Lọc danh sách bệnh nhân đã check-in hàng đợi ngày hôm nay theo thời gian/STT
      const sortedTodayQueues = [...todayQueues].sort((a, b) => {
        const numA = a.queueNumber !== undefined && a.queueNumber !== null ? a.queueNumber : 999999
        const numB = b.queueNumber !== undefined && b.queueNumber !== null ? b.queueNumber : 999999
        if (numA !== numB) return numA - numB
        return new Date(a.checkedInAt || 0) - new Date(b.checkedInAt || 0)
      })

      const todayPatientsList = []
      const addedIds = new Set()

      sortedTodayQueues.forEach((qItem) => {
        const pIdStr = String(qItem.patientId)
        if (!addedIds.has(pIdStr) && qItem.patientId) {
          addedIds.add(pIdStr)
          const pObj = allPatients.find((p) => String(p.id) === pIdStr) || {}
          const timeLabel = qItem.checkedInAt ? dayjs(qItem.checkedInAt).format('HH:mm') : 'Hôm nay'
          todayPatientsList.push({
            ...pObj,
            id: qItem.patientId,
            fullName: pObj.fullName || qItem.patientName || pObj.name || 'Bệnh nhân',
            patientCode: pObj.patientCode || qItem.patientCode || 'BN-N/A',
            phoneNumber: pObj.phoneNumber || qItem.phone || 'Không SĐT',
            gender: pObj.gender || qItem.gender || 'MALE',
            dateOfBirth: pObj.dateOfBirth || '1995-01-01',
            healthInsuranceCode: pObj.healthInsuranceCode || 'Không có',
            medicalHistory: pObj.medicalHistory || 'Chưa ghi nhận',
            checkInTimeStr: timeLabel,
            queueNumber: qItem.queueNumber,
            visitId: qItem.visitId,
            queueStatus: qItem.status,
          })
        }
      })

      // Đảm bảo giữ bệnh nhân chuyển từ màn hình khác sang nếu có
      if (location.state?.patientId && !addedIds.has(String(location.state.patientId))) {
        const targetP = allPatients.find((p) => String(p.id) === String(location.state.patientId))
        if (targetP) todayPatientsList.unshift(targetP)
      }

      setPatients(todayPatientsList)

      const apiRecords = recordResponse.status === 'fulfilled' ? recordResponse.value.data || [] : []
      setRecords(mergeMedicalRecords(apiRecords))
    } catch {
      setRecords(mergeMedicalRecords([]))
    }
  }, [location.state?.patientId])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Fetch Backend Diagnosis ICD-10 Catalog API
  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const response = await medicalRecordApi.getDiagnosisCatalog(icdSearchQuery)
        if (Array.isArray(response.data)) {
          setBackendIcdCatalog(response.data)
        }
      } catch {
        // Fallback to local catalog
      }
    }
    fetchCatalog()
  }, [icdSearchQuery])

  const selectedPatientObj = useMemo(() => {
    return patients.find((p) => String(p.id) === String(selectedPatientId))
  }, [patients, selectedPatientId])

  // BMI calculation
  const bmiValue = useMemo(() => {
    const w = parseFloat(vitalSigns.weight)
    const h = parseFloat(vitalSigns.height) / 100
    if (w > 0 && h > 0) {
      return (w / (h * h)).toFixed(1)
    }
    return null
  }, [vitalSigns.weight, vitalSigns.height])

  // ICD-10 Search Results (combining Backend API & Local fallback)
  const filteredIcdList = useMemo(() => {
    const localMatches = searchIcd10(icdSearchQuery, icdCategory)
    if (!backendIcdCatalog.length) return localMatches

    const combinedMap = new Map()
    localMatches.forEach((item) => combinedMap.set(item.code, item))
    backendIcdCatalog.forEach((item) => {
      if (!combinedMap.has(item.code)) {
        combinedMap.set(item.code, { code: item.code, name: item.name, category: 'ALL' })
      }
    })
    return Array.from(combinedMap.values())
  }, [icdSearchQuery, icdCategory, backendIcdCatalog])

  // Clinical Orders Catalog Filtered
  const filteredCatalog = useMemo(() => {
    const q = orderSearchQuery.toLowerCase().trim()
    return clinicalServiceCatalog.filter((item) => {
      const matchesCat = orderCategory === 'ALL' || item.category === orderCategory
      const matchesQ = !q || item.name.toLowerCase().includes(q) || item.code.toLowerCase().includes(q)
      return matchesCat && matchesQ
    })
  }, [orderCategory, orderSearchQuery])

  // Order helper actions
  const handleAddOrder = (catalogItem) => {
    if (selectedOrders.some((item) => item.code === catalogItem.code)) {
      message.info(`Dịch vụ ${catalogItem.name} đã có trong danh sách chỉ định`)
      return
    }
    const newOrderItem = {
      ...catalogItem,
      isUrgent: false,
      note: '',
    }
    setSelectedOrders((prev) => [...prev, newOrderItem])
    message.success(`Đã thêm chỉ định: ${catalogItem.name}`)
  }

  const handleRemoveOrder = (code) => {
    setSelectedOrders((prev) => prev.filter((item) => item.code !== code))
    setResults((prev) => {
      const copy = { ...prev }
      delete copy[code]
      return copy
    })
  }

  const handleToggleUrgent = (code) => {
    setSelectedOrders((prev) =>
      prev.map((item) => (item.code === code ? { ...item, isUrgent: !item.isUrgent } : item))
    )
  }

  const handleUpdateOrderNote = (code, note) => {
    setSelectedOrders((prev) => prev.map((item) => (item.code === code ? { ...item, note } : item)))
  }

  const totalOrderFee = useMemo(() => {
    return selectedOrders.reduce((sum, item) => sum + (Number(item.price) || 0), 0)
  }, [selectedOrders])

  // Tự động lưu/đồng bộ phiếu chỉ định khi bác sĩ chọn dịch vụ CĐLS cho bệnh nhân đang khám
  useEffect(() => {
    if (selectedPatientId && selectedOrders && selectedOrders.length > 0) {
      const orderCode = `CD-${dayjs().format('YYYYMMDD')}-${Math.floor(100 + Math.random() * 900)}`
      const clinicalOrderObj = {
        id: `ord-${selectedPatientId}`,
        orderCode,
        patientId: selectedPatientId,
        patientCode: selectedPatientObj?.patientCode || `BN${String(selectedPatientId).slice(-6).toUpperCase()}`,
        patientName: selectedPatientObj?.fullName || selectedPatientObj?.name || 'Bệnh nhân',
        gender: selectedPatientObj?.gender || 'Nam',
        age: selectedPatientObj?.age || 30,
        department: user?.department || 'Khoa Nội tổng quát',
        doctorName: user?.fullName || user?.username || 'BS. Phạm Hồng Anh',
        orderDate: dayjs().format('YYYY-MM-DD HH:mm'),
        priority: selectedOrders.some((o) => o.isUrgent) ? 'URGENT' : 'NORMAL',
        status: 'PENDING',
        totalAmount: totalOrderFee,
        items: selectedOrders.map((item, idx) => ({
          serviceId: item.id || `srv-${idx}-${Date.now()}`,
          serviceCode: item.code || `CDHA-${idx + 1}`,
          serviceName: item.name,
          category: item.category || 'Chẩn đoán hình ảnh',
          price: Number(item.price) || 0,
          quantity: 1,
          instruction: item.note || (item.isUrgent ? 'CẤP CỨU' : ''),
          status: 'PENDING',
        })),
        resultSummary: '',
        createdAt: dayjs().toISOString(),
        updatedAt: dayjs().toISOString(),
      }
      saveStoredClinicalOrder(clinicalOrderObj)
    }
  }, [selectedOrders, selectedPatientId, selectedPatientObj, totalOrderFee, user])

  // Đồng bộ trạng thái hàng đợi & tự động tạo phiếu Chỉ định Lâm sàng sang các màn hình CĐLS và Nhập kết quả
  const syncQueueCompletion = async (patientId) => {
    try {
      const hasOrders = selectedOrders && selectedOrders.length > 0
      const targetStatus = hasOrders ? 'WAITING_FOR_RESULT' : 'IN_PROGRESS'
      const targetQueueId = location.state?.queueItemId

      // 1. Nếu có chỉ định CĐLS, lưu phiếu vào hệ thống để xuất hiện ngay trong màn Chỉ định và Nhập kết quả CĐLS
      if (hasOrders) {
        const orderCode = `CD-${dayjs().format('YYYYMMDD')}-${Math.floor(100 + Math.random() * 900)}`
        const clinicalOrderObj = {
          id: `ord-${Date.now()}`,
          orderCode,
          patientId: patientId,
          patientCode: selectedPatientObj?.patientCode || `BN${String(patientId).slice(-6).toUpperCase()}`,
          patientName: selectedPatientObj?.fullName || 'Bệnh nhân',
          gender: selectedPatientObj?.gender || 'Nam',
          age: selectedPatientObj?.age || 30,
          department: user?.department || 'Khoa Nội tổng quát',
          doctorName: user?.fullName || user?.username || 'BS. Phạm Hồng Anh',
          orderDate: dayjs().format('YYYY-MM-DD HH:mm'),
          priority: selectedOrders.some((o) => o.isUrgent) ? 'URGENT' : 'NORMAL',
          status: 'PENDING',
          totalAmount: totalOrderFee,
          items: selectedOrders.map((item, idx) => ({
            serviceId: item.id || `srv-${idx}-${Date.now()}`,
            serviceCode: item.code || `CDHA-${idx + 1}`,
            serviceName: item.name,
            category: item.category || 'Chẩn đoán hình ảnh',
            price: Number(item.price) || 0,
            quantity: 1,
            instruction: item.note || (item.isUrgent ? 'CẤP CỨU' : ''),
            status: 'PENDING',
          })),
          resultSummary: '',
          createdAt: dayjs().toISOString(),
          updatedAt: dayjs().toISOString(),
        }
        saveStoredClinicalOrder(clinicalOrderObj)
      }

      // 2. Đồng bộ trạng thái về trang Quản lý Lịch hẹn & Hàng Đợi (Chờ CĐLS hoặc tiếp tục Đang khám, KHÔNG tự động Hoàn tất)
      if (targetQueueId) {
        try {
          await queueApi.updateStatus(targetQueueId, { status: targetStatus })
        } catch (err) {
          console.warn('Backend sync queue note:', err)
        }
      }
      const allQueues = getStoredQueueItems()
      allQueues.forEach((q) => {
        if (
          (targetQueueId && String(q.id) === String(targetQueueId)) ||
          (!targetQueueId && String(q.patientId) === String(patientId) && ['IN_PROGRESS', 'WAITING', 'WAITING_FOR_RESULT'].includes(q.status))
        ) {
          const updatedItem = {
            ...q,
            status: targetStatus,
          }
          saveStoredQueueItem(updatedItem)
          if (!targetQueueId && q.id && !String(q.id).startsWith('local') && !String(q.id).startsWith('qi-')) {
            queueApi.updateStatus(q.id, { status: targetStatus }).catch(() => {})
          }
        }
      })
    } catch (e) {
      console.warn('Sync queue error:', e)
    }
  }

  // Form Submission & API Integration
  const saveRecord = async () => {
    let values
    try {
      values = await form.validateFields()
    } catch {
      message.error('Vui lòng nhập đầy đủ thông tin bắt buộc: Bệnh nhân, Triệu chứng và Chẩn đoán chính!')
      return
    }

    if (!primaryIcd && !values.diagnosisText) {
      message.error('Vui lòng chọn Mã bệnh ICD-10 hoặc nhập nội dung Chẩn đoán chính!')
      return
    }

    setSaving(true)

    // Format full diagnosis text
    const primaryDiagStr = primaryIcd ? `[${primaryIcd.code}] ${primaryIcd.name}` : values.diagnosisText
    const secondaryDiagStr = secondaryIcds.map((item) => `[${item.code}] ${item.name}`).join('; ')
    const fullDiagnosisText = secondaryDiagStr ? `${primaryDiagStr} (Kèm theo: ${secondaryDiagStr})` : primaryDiagStr

    const orderNamesList = selectedOrders.map((o) => `${o.name} (${o.isUrgent ? 'CẤP CỨU' : 'Thường'})`)

    const payload = {
      ...values,
      patientId: values.patientId,
      symptoms: values.symptoms,
      examinationNote: values.examinationNote || '',
      diagnosis: fullDiagnosisText,
      diagnosisType,
      primaryIcdCode: primaryIcd?.code || '',
      primaryIcdName: primaryIcd?.name || values.diagnosisText || '',
      secondaryIcdCodes: secondaryIcds.map((i) => i.code).join(','),
      vitalSigns,
      treatmentPlan: values.treatmentPlan || '',
      clinicalOrders: orderNamesList,
      clinicalOrderItems: selectedOrders,
      clinicalResults: Object.fromEntries(Object.entries(results).filter(([, value]) => value?.trim())),
    }

    const recordCode = `BA-${dayjs().format('YYYYMMDDHHmmss')}`
    const completeRecord = {
      id: `mr-${Date.now()}`,
      recordCode,
      patientId: values.patientId,
      patientName: selectedPatientObj ? `${selectedPatientObj.fullName} (${selectedPatientObj.patientCode})` : 'Bệnh nhân',
      doctorName: user?.fullName || user?.username || 'BS. Phạm Hồng Anh',
      symptoms: values.symptoms,
      examinationNote: values.examinationNote || '',
      diagnosis: fullDiagnosisText,
      diagnosisType,
      primaryIcd: primaryIcd || { code: 'ICD-10', name: values.diagnosisText || 'Chẩn đoán xác định' },
      secondaryIcds,
      vitalSigns,
      treatmentPlan: values.treatmentPlan || '',
      clinicalOrders: orderNamesList,
      clinicalOrderItems: selectedOrders,
      clinicalResults: Object.fromEntries(Object.entries(results).filter(([, value]) => value?.trim())),
      totalFee: totalOrderFee,
      status: 'IN_PROGRESS',
      createdAt: dayjs().toISOString(),
      attachments: files.map((file) => ({ id: file.uid || String(Date.now()), fileName: file.name })),
    }

    try {
      // 1. Call Backend POST /medical-records API if available
      let beRecordId = null
      let beExamId = location.state?.examinationId || null
      const validVisitId = currentVisitId || selectedPatientObj?.visitId || location.state?.visitId || '10000000-0000-0000-0000-000000000001'

      const beCreatePayload = {
        visitId: validVisitId,
        chiefComplaint: values.symptoms || 'Khám bệnh',
        symptoms: values.symptoms || '',
        medicalHistory: selectedPatientObj?.medicalHistory || 'Chưa ghi nhận',
        physicalExamination: values.examinationNote || (vitalSigns ? `Huyết áp: ${vitalSigns.bp || '120/80'}, Mạch: ${vitalSigns.pulse || '75'}, Thân nhiệt: ${vitalSigns.temp || '37.0'}°C` : ''),
        clinicalProgress: 'Đang điều trị',
        treatmentPlan: values.treatmentPlan || '',
        doctorInstructions: values.treatmentPlan || 'Theo dõi sức khỏe và uống thuốc theo đơn',
        conclusion: fullDiagnosisText,
        ...payload,
      }

      try {
        const response = await medicalRecordApi.create(beCreatePayload)
        const createdRecord = response?.data
        if (createdRecord?.id) {
          beRecordId = createdRecord.id
          beExamId = createdRecord.examinationId || createdRecord.id || beExamId
        }
      } catch (beErr) {
        console.warn('Backend create API unavailable or error, using local ID:', beErr)
      }

      if (beRecordId) {
        completeRecord.id = beRecordId
      }

      // 2. Trigger diagnosis and order endpoints safely
      const targetRecordId = beRecordId || completeRecord.id
      try {
        await medicalRecordApi.recordDiagnosis(targetRecordId, {
          primaryIcdCode: primaryIcd?.code || 'Z00.0',
          primaryIcdName: primaryIcd?.name || values.diagnosisText || 'Khám sức khỏe tổng quát',
          secondaryIcdCodes: secondaryIcds.map((item) => ({ code: item.code, name: item.name })),
          clinicalNotes: values.examinationNote || values.symptoms || '',
        })
      } catch (diagErr) {
        console.warn('Backend recordDiagnosis API note:', diagErr)
      }

      if (selectedOrders.length > 0) {
        try {
          await medicalRecordApi.createClinicalOrder(validVisitId, {
            clinicalReason: fullDiagnosisText,
            items: selectedOrders.map((item) => ({
              serviceId: String(item.id).includes('-') ? item.id : '80000000-0000-0000-0000-000000000001',
              serviceCode: item.code,
              serviceName: item.name,
              instruction: item.note || (item.isUrgent ? 'CẤP CỨU' : ''),
            })),
          })
        } catch (orderErr) {
          console.warn('Backend createClinicalOrder API note:', orderErr)
        }
      }

      // 3. File Attachments
      files.forEach((file) => {
        if (beRecordId) {
          medicalRecordApi.attach(beRecordId, file).catch(() => {})
        }
        saveStoredAttachment({
          id: file.uid || `att-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
          attachmentCode: `KQ-${dayjs().format('YYYYMMDDHHmm')}`,
          patientId: values.patientId,
          patientName: selectedPatientObj ? selectedPatientObj.fullName : 'Bệnh nhân',
          patientCode: selectedPatientObj ? selectedPatientObj.patientCode : '',
          category: selectedOrders[0]?.name || 'Khác',
          testDate: dayjs().format('YYYY-MM-DD HH:mm'),
          doctorName: user?.fullName || user?.username || 'Bác sĩ',
          status: 'NORMAL',
          resultSummary: fullDiagnosisText || 'Kết quả cận lâm sàng đính kèm bệnh án',
          fileName: file.name,
          fileType: file.type || 'application/pdf',
          fileSize: file.size ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : '1.0 MB',
          fileUrl: URL.createObjectURL && file instanceof Blob ? URL.createObjectURL(file) : '',
          createdAt: dayjs().toISOString(),
        })
      })

      saveStoredMedicalRecord(completeRecord)
      logMedicalAccess({
        userName: user?.fullName || user?.username || 'Bác sĩ',
        patientName: completeRecord.patientName,
        recordCode: completeRecord.recordCode,
        action: 'Tạo bệnh án & chẩn đoán ICD-10 mới',
      })

      message.success(`Đã lưu thành công bệnh án ${recordCode}`)
      resetFormState()
      await loadData()
      await syncQueueCompletion(values.patientId)
      setActiveTab('history')
      showSuccessModal(recordCode, values.patientId)
    } catch (err) {
      console.error('Lỗi khi lưu bệnh án:', err)
      message.error('Có lỗi xảy ra, vui lòng thử lại!')
    } finally {
      setSaving(false)
    }
  }

  const resetFormState = () => {
    form.resetFields()
    setSelectedPatientId(null)
    setVitalSigns({ bp: '', pulse: '', temp: '37.0', respRate: '16', weight: '', height: '', spO2: '98' })
    setPrimaryIcd(null)
    setSecondaryIcds([])
    setSelectedOrders([])
    setResults({})
    setFiles([])
  }

  const showSuccessModal = (code, pId) => {
    Modal.confirm({
      title: 'Đã lưu bệnh án & chỉ định thành công!',
      icon: <CheckCircleOutlined style={{ color: '#16A34A' }} />,
      content: (
        <div>
          <Paragraph>Mã bệnh án: <Text strong style={{ color: '#2563EB' }}>{code}</Text></Paragraph>
          <Paragraph>Bạn muốn thực hiện thao tác tiếp theo nào?</Paragraph>
        </div>
      ),
      okText: 'Chuyển sang Kê đơn thuốc',
      cancelText: 'Xem Lịch sử khám',
      onOk: () => navigate('/prescriptions', { state: { patientId: pId, recordCode: code } }),
      onCancel: () => setActiveTab('history'),
    })
  }

  const beforeUpload = (file) => {
    const allowed = ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)
    if (!allowed) {
      message.error('Chỉ chấp nhận tệp PDF, JPG hoặc PNG')
      return false
    }
    if (file.size > 10 * 1024 * 1024) {
      message.error('Tệp không được vượt quá 10 MB')
      return false
    }
    setFiles((current) => [...current, file])
    return false
  }

  const downloadAttachment = async (file) => {
    try {
      const response = await medicalRecordApi.downloadAttachment(file.id)
      const url = URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = file.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      message.info(`Đã lưu tệp đính kèm: ${file.fileName}`)
    }
  }

  const openRecord = async (record) => {
    logMedicalAccess({
      userName: user?.fullName || user?.username || 'Bác sĩ',
      patientName: record.patientName || 'Bệnh nhân',
      recordCode: record.recordCode || 'BA-001',
      action: 'Xem thông tin hồ sơ bệnh án điện tử',
    })
    try {
      const res = await medicalRecordApi.getById(record.id)
      setViewing({ ...record, ...(res.data || {}) })
    } catch {
      setViewing(record)
    }
  }

  const historyColumns = [
    {
      title: 'Mã bệnh án',
      dataIndex: 'recordCode',
      render: (value) => <Tag color="blue" style={{ fontWeight: 600 }}>{value}</Tag>,
    },
    { title: 'Bệnh nhân', dataIndex: 'patientName' },
    {
      title: 'Chẩn đoán',
      dataIndex: 'diagnosis',
      ellipsis: true,
      render: (val) => <Text style={{ color: '#1E40AF' }}>{val}</Text>,
    },
    { title: 'Bác sĩ khám', dataIndex: 'doctorName' },
    {
      title: 'Ngày lập',
      dataIndex: 'createdAt',
      render: (value) => dayjs(value).format('HH:mm DD/MM/YYYY'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value) => <Tag color="green">{value || 'COMPLETED'}</Tag>,
    },
    {
      title: 'Thao tác',
      render: (_, record) => (
        <Button icon={<EyeOutlined />} size="small" onClick={() => openRecord(record)}>
          Xem chi tiết
        </Button>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 40 }}>
      {/* Header Bar */}
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563EB' }} /> Khám bệnh & Phân loại Chẩn đoán Y Khoa
          </h2>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Lập trình chẩn đoán ICD-10 tiêu chuẩn, chỉ định dịch vụ cận lâm sàng và quản lý bệnh án điện tử.
          </Text>
        </div>
        {isDoctor && (
          <Space>
            {selectedOrders.length > 0 && (
              <Button icon={<PrinterOutlined />} onClick={() => setPrintModalOpen(true)}>
                In Phiếu Chỉ Định ({selectedOrders.length})
              </Button>
            )}
            <Button type="primary" size="large" loading={saving} icon={<CheckCircleOutlined />} onClick={saveRecord}>
              Lưu Hồ Sơ Bệnh Án
            </Button>
          </Space>
        )}
      </div>

      <Alert
        showIcon
        type="info"
        message="Quy trình Khám bệnh chuẩn:"
        description="1. Chọn bệnh nhân & nhập sinh hiệu -> 2. Chọn Mã bệnh ICD-10 (Chẩn đoán chính & phụ) -> 3. Nhập chỉ định Cận lâm sàng -> 4. Đính kèm kết quả -> 5. Lưu bệnh án & chuyển Kê đơn."
        style={{ marginBottom: 16, borderRadius: 8 }}
      />

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        items={[
          {
            key: 'current',
            label: (
              <span>
                <SolutionOutlined /> Ghi Bệnh Án & Chẩn Đoán
              </span>
            ),
            children: (
              <MedicalEncounterForm
                form={form}
                isDoctor={isDoctor}
                patients={patients}
                selectedPatientId={selectedPatientId}
                setSelectedPatientId={setSelectedPatientId}
                selectedPatientObj={selectedPatientObj}
                vitalSigns={vitalSigns}
                setVitalSigns={setVitalSigns}
                bmiValue={bmiValue}
                diagnosisType={diagnosisType}
                setDiagnosisType={setDiagnosisType}
                primaryIcd={primaryIcd}
                setPrimaryIcd={setPrimaryIcd}
                secondaryIcds={secondaryIcds}
                setSecondaryIcds={setSecondaryIcds}
                commonIcd10List={commonIcd10List}
                setDiagnosisModalOpen={setDiagnosisModalOpen}
                selectedOrders={selectedOrders}
                setSelectedOrders={setSelectedOrders}
                orderCategory={orderCategory}
                setOrderCategory={setOrderCategory}
                orderSearchQuery={orderSearchQuery}
                setOrderSearchQuery={setOrderSearchQuery}
                filteredCatalog={filteredCatalog}
                handleAddOrder={handleAddOrder}
                handleRemoveOrder={handleRemoveOrder}
                handleToggleUrgent={handleToggleUrgent}
                handleUpdateOrderNote={handleUpdateOrderNote}
                totalOrderFee={totalOrderFee}
                setPrintModalOpen={setPrintModalOpen}
                results={results}
                setResults={setResults}
                files={files}
                setFiles={setFiles}
                beforeUpload={beforeUpload}
              />
            ),
          },
          {
            key: 'history',
            label: `Lịch Sử Hồ Sơ Bệnh Án (${records.length})`,
            children: (
              <Card bordered>
                <Table
                  rowKey="id"
                  columns={historyColumns}
                  dataSource={records}
                  pagination={{ pageSize: 10 }}
                />
              </Card>
            ),
          },
        ]}
      />

      {/* Modal ICD-10 Search Catalog */}
      <Modal
        title="Tra Cứu Danh Mục Mã Bệnh Tiêu Chuẩn ICD-10"
        open={diagnosisModalOpen}
        onCancel={() => setDiagnosisModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setDiagnosisModalOpen(false)}>
            Đóng
          </Button>,
        ]}
        width={750}
      >
        <div style={{ marginBottom: 12, display: 'flex', gap: 10 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Tìm theo mã bệnh (J00, K29...) hoặc tên bệnh..."
            value={icdSearchQuery}
            onChange={(e) => setIcdSearchQuery(e.target.value)}
          />
          <Select
            value={icdCategory}
            onChange={setIcdCategory}
            style={{ width: 260 }}
            options={icd10Categories.map((c) => ({ value: c.key, label: c.label }))}
          />
        </div>

        <Table
          size="small"
          rowKey="code"
          dataSource={filteredIcdList}
          pagination={{ pageSize: 8 }}
          columns={[
            {
              title: 'Mã ICD',
              dataIndex: 'code',
              width: 100,
              render: (code) => <Tag color="blue" style={{ fontWeight: 700 }}>{code}</Tag>,
            },
            { title: 'Tên bệnh / Hội chứng y khoa', dataIndex: 'name' },
            {
              title: 'Thao tác',
              width: 160,
              render: (_, item) => (
                <Space>
                  <Button
                    size="small"
                    type="primary"
                    onClick={() => {
                      setPrimaryIcd(item)
                      form.setFieldsValue({ diagnosisText: `[${item.code}] ${item.name}` })
                      setDiagnosisModalOpen(false)
                      message.success(`Đã chọn chẩn đoán chính: [${item.code}] ${item.name}`)
                    }}
                  >
                    Chọn chính
                  </Button>
                  <Button
                    size="small"
                    onClick={() => {
                      if (!secondaryIcds.some((i) => i.code === item.code)) {
                        setSecondaryIcds((prev) => [...prev, item])
                        message.success(`Đã thêm chẩn đoán phụ: [${item.code}] ${item.name}`)
                      }
                    }}
                  >
                    + Phụ
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Modal>

      {/* Modal View Medical Record Details */}
      <Modal
        title={`Chi Tiết Hồ Sơ Bệnh Án Điện Tử ${viewing?.recordCode || ''}`}
        open={!!viewing}
        onCancel={() => setViewing(null)}
        footer={<Button onClick={() => setViewing(null)}>Đóng</Button>}
        width={760}
      >
        {viewing && (
          <div>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Bệnh nhân">{viewing.patientName}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ khám">{viewing.doctorName}</Descriptions.Item>
              <Descriptions.Item label="Lý do khám / Triệu chứng">{viewing.symptoms}</Descriptions.Item>
              <Descriptions.Item label="Khám lâm sàng">{viewing.examinationNote || '---'}</Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán Y khoa">
                <Text strong style={{ color: '#1E40AF' }}>{viewing.diagnosis}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Hướng điều trị">{viewing.treatmentPlan || '---'}</Descriptions.Item>
              <Descriptions.Item label="Chỉ định cận lâm sàng">
                {viewing.clinicalOrders?.join(', ') || 'Không có chỉ định'}
              </Descriptions.Item>
              <Descriptions.Item label="Kết quả cận lâm sàng">
                {Object.entries(viewing.clinicalResults || {})
                  .map(([key, value]) => `${key}: ${value}`)
                  .join(' | ') || 'Chưa cập nhật'}
              </Descriptions.Item>
            </Descriptions>

            <Divider style={{ margin: '16px 0' }} />
            <Text strong>Tệp Đính Kèm Hồ Sơ</Text>
            <List
              header={null}
              dataSource={viewing.attachments || []}
              locale={{ emptyText: 'Không có tệp đính kèm' }}
              renderItem={(file) => (
                <List.Item>
                  <Button type="link" onClick={() => downloadAttachment(file)}>
                    {file.fileName}
                  </Button>
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>

      {/* Clinical Order Printable Sheet Preview Modal */}
      <ClinicalOrderPrintModal
        open={printModalOpen}
        onClose={() => setPrintModalOpen(false)}
        patient={selectedPatientObj}
        recordCode={`BA-${dayjs().format('YYYYMMDD')}`}
        diagnosis={form.getFieldValue('diagnosisText')}
        primaryIcd={primaryIcd}
        secondaryIcds={secondaryIcds}
        orders={selectedOrders}
        doctorName={user?.fullName || user?.username || 'BS. Phạm Hồng Anh'}
        vitalSigns={vitalSigns}
      />
    </div>
  )
}

export default MedicalEncounter
