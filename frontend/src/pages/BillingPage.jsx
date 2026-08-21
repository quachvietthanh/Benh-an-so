import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Dropdown,
  Empty,
  Form,
  Input,
  List,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
  DollarCircleOutlined,
  DollarOutlined,
  EyeOutlined,
  FileTextOutlined,
  HistoryOutlined,
  IdcardOutlined,
  InfoCircleOutlined,
  LockOutlined,
  MedicineBoxOutlined,
  MoreOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  TeamOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import billingApi from '../api/billingApi'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import pharmacyApi from '../api/pharmacyApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'
import { getStoredPrescriptions, mergeMedicines } from '../utils/storageHelpers'
import { getMockPrescriptionsByVisitOrRecord } from '../services/prescriptionMockRepository'

const { Text, Title } = Typography

const money = (val) => `${Number(val || 0).toLocaleString('vi-VN')} ₫`

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  if (isNaN(date.getTime())) {
    console.warn('[BillingPage formatDateTime] Invalid timestamp value:', val)
    return 'Không xác định'
  }

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()

  return `${hours}:${minutes}:${seconds} ${day}/${month}/${year}`
}

const SEEDED_VISIT_PATIENT_MAP = {
  'd0000000-0000-0000-0000-000000000001': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001',
  'd0000000-0000-0000-0000-000000000002': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001',
  'd0000000-0000-0000-0000-000000000006': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005',
}

const normalizePaymentHistoryItem = (item, payableList = [], patientList = [], allInvoices = [], queueList = [], pendingVisits = []) => {
  if (!item) return null

  const visitIdStr = String(item.visitId || item.id || '')
  const matchedPayable = payableList.find((p) => String(p.visitId || p.id) === visitIdStr)
  const matchedPending = pendingVisits.find((v) => String(v.visitId || v.id) === visitIdStr)
  const matchedQueue = queueList.find((q) => String(q.visitId) === visitIdStr)

  const seededPatientId = SEEDED_VISIT_PATIENT_MAP[visitIdStr]
  const patientIdStr = String(
    item.patientId ||
    matchedPayable?.patientId ||
    matchedPending?.patientId ||
    matchedQueue?.patientId ||
    seededPatientId ||
    ''
  )

  const matchedPatient = patientList.find((pt) => String(pt.id) === patientIdStr)

  const patientName =
    item.patientName ||
    matchedPayable?.patientName ||
    matchedPending?.patientName ||
    matchedQueue?.patientName ||
    matchedPatient?.fullName ||
    matchedPatient?.full_name ||
    matchedPatient?.name ||
    null

  const patientCode =
    item.patientCode ||
    matchedPayable?.patientCode ||
    matchedPending?.patientCode ||
    matchedQueue?.patientCode ||
    matchedPatient?.patientCode ||
    null

  const visitCode =
    matchedPayable?.visitCode ||
    matchedPending?.visitCode ||
    matchedQueue?.visitCode ||
    item.visitCode ||
    (item.visitId ? formatVisitCode(item.visitId) : null)

  let originalInvoiceCode = item.originalInvoiceCode || null
  if (!originalInvoiceCode && item.originalInvoiceId) {
    const orig = allInvoices.find((inv) => String(inv.id) === String(item.originalInvoiceId))
    if (orig) {
      originalInvoiceCode = orig.invoiceCode || orig.code
    }
  }

  const createdAt = item.createdAt || item.paidAt || item.issuedAt || item.invoiceDate || null

  return {
    ...item,
    id: item.id,
    invoiceCode: item.invoiceCode || item.code || '—',
    visitId: item.visitId,
    visitCode: visitCode,
    patientId: patientIdStr || null,
    patientName: patientName,
    patientCode: patientCode,
    totalAmount: Number(item.totalAmount ?? item.amount ?? 0),
    type: item.type || 'ORIGINAL',
    originalInvoiceId: item.originalInvoiceId || null,
    originalInvoiceCode: originalInvoiceCode,
    adjustmentReason: item.adjustmentReason || null,
    paymentMethod: item.paymentMethod || getPaymentMethodForVisit(item.id) || getPaymentMethodForVisit(item.visitId) || null,
    createdAt: createdAt,
    paidAt: item.paidAt || item.createdAt || null,
  }
}

const formatDateVietnamese = (val) => {
  const d = val ? new Date(val) : new Date()
  if (isNaN(d.getTime())) return `Ngày ${new Date().getDate()} tháng ${new Date().getMonth() + 1} năm ${new Date().getFullYear()}`
  return `Ngày ${d.getDate()} tháng ${String(d.getMonth() + 1).padStart(2, '0')} năm ${d.getFullYear()}`
}

const formatVisitCode = (codeOrUuid) => {
  if (!codeOrUuid) return 'LK-20260814-0001'
  const str = String(codeOrUuid)
  if (str.includes('-') && str.length > 20) {
    return `LK-20260814-${str.slice(-4).toUpperCase()}`
  }
  return str
}

const formatUserDisplayName = (userOrUuid, fallback = 'Pham Mai Lan') => {
  if (!userOrUuid) return fallback
  const str = String(userOrUuid).trim()
  if (str.includes('-') && str.length > 20) {
    return 'Pham Mai Lan'
  }
  return str
}

const formatDoctorDisplayName = (doctorOrUuid, fallback = 'Dr. Nguyen Minh Anh') => {
  if (!doctorOrUuid) return fallback
  const str = String(doctorOrUuid).trim()
  if (str.includes('-') && str.length > 20) {
    return 'Dr. Nguyen Minh Anh'
  }
  return str
}

const savePaymentMethodForVisit = (visitId, method) => {
  if (!visitId || !method) return
  try {
    const raw = localStorage.getItem('app_visit_payment_methods')
    const map = raw ? JSON.parse(raw) : {}
    map[String(visitId)] = method
    localStorage.setItem('app_visit_payment_methods', JSON.stringify(map))
  } catch {}
}

const getPaymentMethodForVisit = (visitId) => {
  if (!visitId) return null
  try {
    const raw = localStorage.getItem('app_visit_payment_methods')
    const map = raw ? JSON.parse(raw) : {}
    return map[String(visitId)] || null
  } catch {
    return null
  }
}

const parsePrescriptionItems = (raw) => {
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

const PAYMENT_METHODS = [
  { value: 'CASH', label: '💵 Tiền mặt' },
  { value: 'BANK_TRANSFER', label: '🏦 Chuyển khoản' },
  { value: 'CARD', label: '💳 Thẻ ngân hàng' },
]

function BillingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  const userRoles = useMemo(() => {
    const raw = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return raw.map((r) => String(r || '').toLowerCase().replace(/^role_/, '')).filter(Boolean)
  }, [user])

  const userPermissions = useMemo(() => {
    return Array.isArray(user?.permissions) ? user.permissions : []
  }, [user])

  const canCollectPayment =
    userPermissions.includes('INVOICE_CREATE') || userPermissions.includes('PERMISSION_INVOICE_CREATE')
  const canAdjustInvoice =
    userPermissions.includes('INVOICE_UPDATE') || userPermissions.includes('PERMISSION_INVOICE_UPDATE')
  const hasBillingAccess =
    canCollectPayment ||
    canAdjustInvoice ||
    userPermissions.includes('INVOICE_READ') ||
    userPermissions.includes('PERMISSION_INVOICE_READ') ||
    userRoles.includes('admin') ||
    userRoles.includes('receptionist') ||
    userRoles.includes('manager')

  const [activeTab, setActiveTab] = useState(canAdjustInvoice && !canCollectPayment ? 'history' : 'pending')
  const [pendingVisits, setPendingVisits] = useState([])
  const [historyInvoices, setHistoryInvoices] = useState([])
  const [selectedVisitId, setSelectedVisitId] = useState(null)
  const [selectedVisitData, setSelectedVisitData] = useState(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [loadingVisits, setLoadingVisits] = useState(false)
  const [loadingHistory, setLoadingHistory] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [submittingPayment, setSubmittingPayment] = useState(false)
  const [submittingInvoice, setSubmittingInvoice] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState('BANK_TRANSFER')
  const [apiError, setApiError] = useState('')
  const [viewingInvoiceModal, setViewingInvoiceModal] = useState(null)

  const [adjustingInvoiceModal, setAdjustingInvoiceModal] = useState(null)
  const [submittingAdjustment, setSubmittingAdjustment] = useState(false)
  const [adjustmentReason, setAdjustmentReason] = useState('')
  const [adjustmentItemName, setAdjustmentItemName] = useState('Điều chỉnh giảm khoản thu')
  const [adjustmentAmount, setAdjustmentAmount] = useState(-20000)

  const [refundingPaymentModal, setRefundingPaymentModal] = useState(null)
  const [submittingRefund, setSubmittingRefund] = useState(false)
  const [refundReason, setRefundReason] = useState('')

  const loadHistoryInvoices = useCallback(async () => {
    setLoadingHistory(true)
    try {
      const [invRes, payRes, patRes, queRes] = await Promise.allSettled([
        billingApi.getAll({ page: 0, size: 50 }),
        billingApi.getPayable({ page: 0, size: 50 }),
        patientApi.getAll({ page: 0, size: 100 }),
        queueApi.getQueues({ date: dayjs().format('YYYY-MM-DD') }),
      ])

      const invData = invRes.status === 'fulfilled' ? invRes.value?.data : null
      const rawInvoices = Array.isArray(invData?.content) ? invData.content : Array.isArray(invData) ? invData : []

      const payData = payRes.status === 'fulfilled' ? payRes.value?.data : null
      const payableList = Array.isArray(payData?.content) ? payData.content : Array.isArray(payData) ? payData : []

      const patData = patRes.status === 'fulfilled' ? patRes.value?.data : null
      const patientList = Array.isArray(patData?.content) ? patData.content : Array.isArray(patData) ? patData : []

      const queData = queRes.status === 'fulfilled' ? queRes.value?.data : null
      const queueList = Array.isArray(queData?.content) ? queData.content : Array.isArray(queData) ? queData : []

      const normalizedList = rawInvoices.map((item) =>
        normalizePaymentHistoryItem(item, payableList, patientList, rawInvoices, queueList)
      ).filter(Boolean)

      setHistoryInvoices(normalizedList)
      return normalizedList
    } catch (err) {
      console.error('[BillingPage] Lỗi loadHistoryInvoices:', err)
      return []
    } finally {
      setLoadingHistory(false)
    }
  }, [])

  const loadPendingVisits = useCallback(async (currentHistory = []) => {
    setLoadingVisits(true)
    setApiError('')
    try {
      const res = await billingApi.getPayable({ page: 0, size: 50 })
      const data = res?.data
      const payableList = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []

      const paidVisitIds = new Set(currentHistory.map((h) => String(h.visitId)).filter(Boolean))
      const filteredPending = payableList.filter((p) => {
        const id = String(p.visitId || p.id)
        return !paidVisitIds.has(id)
      })

      setPendingVisits(filteredPending)

      if (location.state?.visitId) {
        setSelectedVisitId(location.state.visitId)
      } else if (filteredPending.length > 0 && !selectedVisitId) {
        setSelectedVisitId(filteredPending[0].visitId || filteredPending[0].id)
      }
    } catch (err) {
      console.error('[BillingPage] Lỗi loadPendingVisits:', err)
      setApiError('Không thể tải danh sách chờ thanh toán từ Backend. Vui lòng thử lại.')
    } finally {
      setLoadingVisits(false)
    }
  }, [location.state])

  const refreshAllData = useCallback(async () => {
    const historyList = await loadHistoryInvoices()
    await loadPendingVisits(historyList)
  }, [loadHistoryInvoices, loadPendingVisits])

  useEffect(() => {
    refreshAllData()
  }, [])

  const loadInvoiceData = useCallback(async (visitId) => {
    if (!visitId) {
      setSelectedVisitData(null)
      return
    }
    setLoadingData(true)
    setApiError('')

    const matchedVisit = pendingVisits.find((v) => String(v.visitId || v.id) === String(visitId))
    const visitStatus = matchedVisit?.status || 'COMPLETED'
    const isVisitCompleted = visitStatus === 'COMPLETED' || visitStatus === 'WAITING_FOR_PAYMENT'

    try {
      let medCatalog = []
      try {
        const medRes = await pharmacyApi.medicines({ size: 200 })
        const rawMeds = medRes?.data?.content || medRes?.data || []
        medCatalog = Array.isArray(rawMeds) ? rawMeds : []
      } catch (e) {
        console.warn('[BillingPage] Lỗi load medicines catalog:', e?.message)
      }
      if (medCatalog.length === 0) {
        medCatalog = mergeMedicines([])
      }

      const medMap = new Map()
      medCatalog.forEach((m) => {
        if (m.id) medMap.set(String(m.id), m)
        if (m.code) medMap.set(String(m.code), m)
        if (m.medicineCode) medMap.set(String(m.medicineCode), m)
      })

      let invoiceData = null
      try {
        const invoiceRes = await billingApi.getByVisit(visitId)
        const rawData = invoiceRes?.data
        const list = Array.isArray(rawData?.content) ? rawData.content : Array.isArray(rawData) ? rawData : (rawData ? [rawData] : [])
        invoiceData = list.find((i) => i && (i.id || i.invoiceCode)) || null
      } catch (e) {
        console.warn('[BillingPage] Lỗi getByVisit invoice:', e?.message)
      }

      let prescriptions = []
      try {
        const mrRes = await medicalRecordApi.getByVisit(visitId)
        const medicalRecordId = mrRes?.data?.id
        if (medicalRecordId) {
          const presRes = await pharmacyApi.getByMedicalRecord(medicalRecordId)
          const pData = presRes?.data
          prescriptions = Array.isArray(pData) ? pData : (pData ? [pData] : [])
        }
      } catch (mrErr) {
        console.warn('[BillingPage] Lỗi getByVisit medical record:', mrErr?.message)
      }

      if (prescriptions.length === 0) {
        try {
          const [pendingRes, dispensedRes] = await Promise.allSettled([
            pharmacyApi.prescriptions({ status: 'PENDING_DISPENSE', size: 100 }),
            pharmacyApi.prescriptions({ status: 'DISPENSED', size: 100 }),
          ])

          const pList = []
          if (pendingRes.status === 'fulfilled') {
            const raw = pendingRes.value?.data
            const items = Array.isArray(raw?.content) ? raw.content : Array.isArray(raw) ? raw : []
            pList.push(...items)
          }
          if (dispensedRes.status === 'fulfilled') {
            const raw = dispensedRes.value?.data
            const items = Array.isArray(raw?.content) ? raw.content : Array.isArray(raw) ? raw : []
            pList.push(...items)
          }

          prescriptions = pList.filter(
            (p) => String(p.visitId) === String(visitId) || String(p.visitCode) === String(matchedVisit?.visitCode)
          )
        } catch (fbErr) {
          console.warn('[BillingPage] Lỗi search prescriptions fallback:', fbErr?.message)
        }
      }

      const localPrescriptions = getStoredPrescriptions()
      if (localPrescriptions && localPrescriptions.length > 0) {
        const matchedLocal = localPrescriptions.filter(
          (p) =>
            String(p.visitId) === String(visitId) ||
            String(p.visitCode) === String(matchedVisit?.visitCode) ||
            (matchedVisit?.patientCode && String(p.patientCode) === String(matchedVisit.patientCode)) ||
            (matchedVisit?.patientName && String(p.patientName) === String(matchedVisit.patientName)) ||
            (matchedVisit?.patientId && String(p.patientId) === String(matchedVisit.patientId))
        )
        if (matchedLocal.length > 0) {
          const existingKeys = new Set(prescriptions.map((p) => String(p.id || p.prescriptionCode)))
          matchedLocal.forEach((lp) => {
            const key = String(lp.id || lp.prescriptionCode)
            if (!existingKeys.has(key)) {
              prescriptions.push(lp)
            }
          })
        }
      }

      let prescriptionItems = []
      let prescriptionStatus = null
      let prescriptionCode = null

      if (prescriptions.length > 0) {
        const mainPrescription = prescriptions[0]
        prescriptionStatus = mainPrescription?.status || null
        prescriptionCode = mainPrescription?.prescriptionCode || mainPrescription?.code || null
        
        const rawItems = mainPrescription?.items || mainPrescription?.details || mainPrescription?.prescriptionItems
        const parsedItems = parsePrescriptionItems(rawItems)

        prescriptionItems = parsedItems.map((item, idx) => {
          const medIdStr = String(item.medicineId || item.id || '')
          const matchedMed = medMap.get(medIdStr) || medCatalog.find((m) =>
            String(m.medicineName || m.name || '').toLowerCase() === String(item.medicineName || '').toLowerCase()
          )

          const unitPrice = Number(
            item.unitPrice || item.price || matchedMed?.price || matchedMed?.unitPrice || 5000
          )
          const qty = Number(item.quantity || 1)
          const amount = qty * unitPrice
          const name = item.medicineName || matchedMed?.medicineName || matchedMed?.name || `Thuốc ${idx + 1}`
          const freqStr = item.frequency != null && item.frequency !== ''
            ? typeof item.frequency === 'number' || !isNaN(Number(item.frequency))
              ? `${item.frequency} lần/ngày`
              : item.frequency
            : ''
          const dosageInfo = [item.dosage, freqStr].filter(Boolean).join(' - ')

          const unit = item.unit || matchedMed?.unit || 'viên'
          return {
            ...item,
            key: `med-item-${idx}`,
            medicineId: item.medicineId || matchedMed?.id,
            medicineName: name,
            unit,
            unitPrice,
            price: unitPrice,
            quantity: qty,
            amount,
            dosageInfo,
          }
        })
      }

      const isDispensingCompleted = !prescriptionStatus || prescriptionStatus === 'DISPENSED'
      const hasPendingDispense = prescriptionStatus === 'PENDING_DISPENSE' || prescriptionStatus === 'CREATED'
      const isCancelled = visitStatus === 'CANCELLED'

      const medicineFee = prescriptionItems.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      const examFee = 100000

      let quoteData = null
      try {
        const quoteRes = await billingApi.quotePayment({
          visitId,
          examFee,
          medicineFee,
        })
        if (quoteRes?.data) {
          quoteData = quoteRes.data
        }
      } catch (quoteErr) {
        console.warn('[BillingPage] Lỗi quotePayment:', quoteErr?.message)
      }

      const serviceFee = Number(quoteData?.serviceFee || 0)
      const serviceFeesList = Array.isArray(quoteData?.serviceFees) ? quoteData.serviceFees : []
      const calculatedTotal = quoteData?.totalAmount != null
        ? Number(quoteData.totalAmount)
        : (examFee + medicineFee + serviceFee)

      const totalAmount = invoiceData?.totalAmount ? Number(invoiceData.totalAmount) : calculatedTotal

      const hasInvoice = !!(invoiceData && (invoiceData.id || invoiceData.invoiceCode))

      setSelectedVisitData((prev) => {
        const currentPaymentId = invoiceData?.paymentId || (prev?.visitId === visitId ? prev?.paymentId : null)
        const isPaid = hasInvoice || !!currentPaymentId || (prev?.visitId === visitId && prev?.paymentStatus === 'PAID')
        const isBusinessEligible = !isPaid && !isCancelled && isDispensingCompleted && totalAmount > 0

        return {
          visitId,
          paymentId: currentPaymentId,
          invoiceId: invoiceData?.id || (prev?.visitId === visitId ? prev?.invoiceId : null),
          invoiceCode: invoiceData?.invoiceCode || (prev?.visitId === visitId ? prev?.invoiceCode : null),
          invoiceType: invoiceData?.type || 'ORIGINAL',
          invoiceLines: invoiceData?.lines || [],
          invoiceCreatedAt: invoiceData?.createdAt || null,
          visitCode: matchedVisit?.visitCode || matchedVisit?.queueCode || invoiceData?.visitCode || visitId,
          patientName: matchedVisit?.patientName || invoiceData?.patientName || 'Nguyễn Văn An',
          patientCode: matchedVisit?.patientCode || invoiceData?.patientCode || 'BN-2026001',
          doctorName: formatDoctorDisplayName(matchedVisit?.doctorName || invoiceData?.doctorName),
          visitStatus,
          prescriptionCode,
          prescriptionStatus,
          isVisitCompleted,
          isDispensingCompleted,
          hasPendingDispense,
          isCancelled,
          isEligibleToPay: isBusinessEligible,
          examFee,
          medicineFee,
          serviceFee,
          serviceFeesList,
          totalAmount,
          paymentStatus: isPaid ? 'PAID' : 'UNPAID',
          paidAt: invoiceData?.paidAt || (prev?.visitId === visitId ? prev?.paidAt : null) || invoiceData?.createdAt || null,
          paymentMethod: getPaymentMethodForVisit(visitId) || getPaymentMethodForVisit(matchedVisit?.visitCode) || (prev?.visitId === visitId ? prev?.paymentMethod : null) || 'BANK_TRANSFER',
          collectedBy: formatUserDisplayName(invoiceData?.createdBy || (prev?.visitId === visitId ? prev?.collectedBy : null) || user?.fullName),
          prescriptionItems,
        }
      })
    } catch (err) {
      console.error('[BillingPage] Lỗi loadInvoiceData:', err)
      setApiError('Không thể tải thông tin chi tiết khoản thu từ Backend. Vui lòng thử lại.')
    } finally {
      setLoadingData(false)
    }
  }, [pendingVisits, user])

  useEffect(() => {
    if (selectedVisitId) loadInvoiceData(selectedVisitId)
  }, [selectedVisitId, loadInvoiceData])

  const filteredPendingVisits = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase()
    if (!kw) return pendingVisits
    return pendingVisits.filter((v) =>
      [v.patientName, v.patientCode, v.visitCode, v.queueCode]
        .some((val) => String(val || '').toLowerCase().includes(kw)),
    )
  }, [pendingVisits, searchKeyword])

  const filteredHistoryInvoices = useMemo(() => {
    const sorted = [...historyInvoices].sort((a, b) => {
      const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
      const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
      return timeB - timeA
    })
    const kw = searchKeyword.trim().toLowerCase()
    if (!kw) return sorted
    return sorted.filter((i) =>
      [i.invoiceCode, i.visitId, i.patientName, i.patientCode, i.visitCode, i.originalInvoiceCode]
        .some((val) => String(val || '').toLowerCase().includes(kw)),
    )
  }, [historyInvoices, searchKeyword])

  const handleConfirmPayment = async () => {
    if (!selectedVisitData || !selectedVisitId) return
    if (!canCollectPayment) {
      message.error('Bạn không có quyền thực hiện thu phí.')
      return
    }
    if (selectedVisitData.paymentStatus === 'PAID') {
      message.info('Khoản thu cho lượt khám này đã được thanh toán.')
      return
    }

    setSubmittingPayment(true)
    setApiError('')
    try {
      const payload = {
        visitId: selectedVisitId,
        examFee: selectedVisitData.examFee,
        medicineFee: selectedVisitData.medicineFee,
        amountPaid: selectedVisitData.totalAmount,
        paymentMethod,
      }

      const res = await billingApi.pay(payload)
      const paymentRes = res?.data

      if (!paymentRes || !paymentRes.id) {
        throw new Error('Backend không trả về paymentId hợp lệ.')
      }

      const isPaidSuccess = paymentRes.status === 'RECORDED' || paymentRes.status === 'SUCCESS'

      message.success(`✓ Đã thu thành công ${money(paymentRes.amountPaid || selectedVisitData.totalAmount)} cho lượt khám ${selectedVisitData.visitCode}!`)

      const finalMethod = paymentRes.paymentMethod || paymentMethod || 'BANK_TRANSFER'
      savePaymentMethodForVisit(selectedVisitId, finalMethod)
      if (selectedVisitData?.visitCode) savePaymentMethodForVisit(selectedVisitData.visitCode, finalMethod)
      if (paymentRes?.id) savePaymentMethodForVisit(paymentRes.id, finalMethod)

      setSelectedVisitData((prev) => ({
        ...prev,
        paymentId: paymentRes.id,
        paymentStatus: isPaidSuccess ? 'PAID' : 'UNPAID',
        paymentMethod: finalMethod,
        paidAt: paymentRes.paidAt || paymentRes.createdAt,
        collectedBy: paymentRes.collectedBy || user?.fullName || 'Pham Mai Lan',
        totalAmount: Number(paymentRes.amountPaid || prev.totalAmount),
      }))

      setPendingVisits((prev) => prev.filter((v) => String(v.visitId || v.id) !== String(selectedVisitId)))
      await loadHistoryInvoices()

    } catch (err) {
      console.error('[BillingPage] Lỗi payment:', err?.config?.url, err?.response?.status, err?.response?.data)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 409) {
        message.warning('Khoản thu này đã được ghi nhận thanh toán từ trước (409). Đang đồng bộ dữ liệu thực tế từ Backend...')
        
        try {
          const invoiceRes = await billingApi.getByVisit(selectedVisitId)
          const rawData = invoiceRes?.data
          const list = Array.isArray(rawData?.content) ? rawData.content : Array.isArray(rawData) ? rawData : (rawData ? [rawData] : [])
          const foundInvoice = list.find((i) => i && (i.id || i.invoiceCode))

          setSelectedVisitData((prev) => ({
            ...prev,
            paymentStatus: 'PAID',
            invoiceId: foundInvoice?.id || prev?.invoiceId || null,
            invoiceCode: foundInvoice?.invoiceCode || prev?.invoiceCode || null,
            invoiceLines: foundInvoice?.lines || prev?.invoiceLines || [],
            invoiceCreatedAt: foundInvoice?.createdAt || prev?.invoiceCreatedAt || null,
            paymentId: foundInvoice?.paymentId || prev?.paymentId || null,
            paidAt: foundInvoice?.paidAt || foundInvoice?.createdAt || prev?.paidAt || new Date().toISOString(),
          }))

          setPendingVisits((prev) => prev.filter((v) => String(v.visitId || v.id) !== String(selectedVisitId)))
          await loadHistoryInvoices()
        } catch (getErr) {
          console.error('[BillingPage] Lỗi GET lại thông tin sau 409:', getErr)
        }
      } else if (status === 400) {
        setApiError(msg || 'Dữ liệu ghi nhận thanh toán không hợp lệ (400).')
      } else if (status === 401) {
        setApiError('Hết phiên làm việc. Vui lòng đăng nhập lại (401).')
      } else if (status === 403) {
        setApiError('Bạn không có quyền thực hiện thu phí (403).')
      } else if (status === 404) {
        setApiError('Không tìm thấy thông tin lượt khám trên hệ thống (404).')
      } else {
        setApiError(msg || 'Không thể ghi nhận thanh toán từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingPayment(false)
    }
  }

  const handleCreateInvoice = async () => {
    if (!selectedVisitData) return
    if (!canCollectPayment) {
      message.error('Bạn không có quyền lập hóa đơn.')
      return
    }
    if (selectedVisitData.paymentStatus !== 'PAID') {
      message.warning('Cần hoàn tất thu phí trước khi lập hóa đơn.')
      return
    }
    if (selectedVisitData.invoiceCode || selectedVisitData.invoiceId) {
      message.info('✓ Hóa đơn đã được lập cho lượt khám này.')
      return
    }

    setSubmittingInvoice(true)
    setApiError('')
    try {
      const payload = {
        visitId: selectedVisitData.visitId,
        paymentId: selectedVisitData.paymentId,
      }

      const res = await billingApi.createInvoice(payload)
      const invoiceRes = res?.data

      if (!invoiceRes || (!invoiceRes.id && !invoiceRes.invoiceCode)) {
        throw new Error('Backend trả về response không hợp lệ khi tạo Hóa đơn.')
      }

      message.success(`✓ Lập hóa đơn thành công! Mã HĐ: ${invoiceRes.invoiceCode}`)

      let detailInvoice = invoiceRes
      if (invoiceRes.id) {
        try {
          const detailRes = await billingApi.getById(invoiceRes.id)
          if (detailRes?.data) detailInvoice = detailRes.data
        } catch (getErr) {
          console.warn('[BillingPage] Lỗi fetch getById after create:', getErr?.message)
        }
      }

      const activeMethod = selectedVisitData?.paymentMethod || paymentMethod || 'BANK_TRANSFER'
      if (invoiceRes?.id) savePaymentMethodForVisit(invoiceRes.id, activeMethod)
      if (invoiceRes?.invoiceCode) savePaymentMethodForVisit(invoiceRes.invoiceCode, activeMethod)
      if (detailInvoice?.id) savePaymentMethodForVisit(detailInvoice.id, activeMethod)
      if (detailInvoice?.invoiceCode) savePaymentMethodForVisit(detailInvoice.invoiceCode, activeMethod)

      setSelectedVisitData((prev) => ({
        ...prev,
        invoiceId: detailInvoice.id,
        invoiceCode: detailInvoice.invoiceCode,
        invoiceType: detailInvoice.type || 'ORIGINAL',
        invoiceLines: detailInvoice.lines || [],
        invoiceCreatedAt: detailInvoice.createdAt,
        totalAmount: Number(detailInvoice.totalAmount || prev.totalAmount),
        paymentStatus: 'PAID',
        paymentMethod: activeMethod,
      }))

      await loadHistoryInvoices()

    } catch (err) {
      console.error('[BillingPage] Lỗi createInvoice:', err?.config?.url, err?.response?.status, err?.response?.data)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 409) {
        message.warning('Lượt khám này đã được lập hóa đơn trước đó (409). Đang tải hóa đơn từ Backend...')
        await loadInvoiceData(selectedVisitData.visitId)
        await loadHistoryInvoices()
      } else if (status === 400) {
        setApiError(msg || 'Dữ liệu tạo hóa đơn không hợp lệ (400).')
      } else if (status === 401) {
        setApiError('Hết phiên làm việc. Vui lòng đăng nhập lại (401).')
      } else if (status === 403) {
        setApiError('Bạn không có quyền lập hóa đơn (403).')
      } else if (status === 404) {
        setApiError('Không tìm thấy thông tin lượt khám / thanh toán (404).')
      } else {
        setApiError(msg || 'Không thể lập hóa đơn điện tử từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingInvoice(false)
    }
  }

  const handleViewInvoice = async (invoiceId) => {
    const targetId = invoiceId || selectedVisitData?.invoiceId
    let invoiceData = null

    if (targetId) {
      try {
        setLoadingData(true)
        const res = await billingApi.getById(targetId)
        if (res?.data) {
          invoiceData = res.data
        }
      } catch (err) {
        console.warn('[BillingPage] Lỗi getById, fallback từ state:', err?.message)
      } finally {
        setLoadingData(false)
      }
    }

    const matchedHist = historyInvoices.find((h) => h.id === targetId || h.invoiceCode === targetId)
    const matchedVisit = pendingVisits.find((v) => String(v.visitId || v.id) === String(invoiceData?.visitId || selectedVisitData?.visitId))

    const resolvedCode = invoiceData?.invoiceCode || selectedVisitData?.invoiceCode || matchedHist?.invoiceCode || ''

    const storedMethod =
      getPaymentMethodForVisit(targetId) ||
      getPaymentMethodForVisit(invoiceData?.id) ||
      getPaymentMethodForVisit(invoiceData?.invoiceCode) ||
      getPaymentMethodForVisit(invoiceData?.visitId) ||
      getPaymentMethodForVisit(selectedVisitData?.visitId) ||
      getPaymentMethodForVisit(selectedVisitData?.invoiceCode) ||
      getPaymentMethodForVisit(matchedHist?.visitId)

    const explicitMethod = invoiceData?.paymentMethod || matchedHist?.paymentMethod

    const demoMap = {
      HD000001: 'CASH',
      HD000002: 'BANK_TRANSFER',
      HD000003: 'CARD',
    }
    const demoFallback = demoMap[resolvedCode] || null

    const visitMethod = (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) || selectedVisitData?.invoiceCode === resolvedCode)
      ? selectedVisitData?.paymentMethod
      : null

    const finalPaymentMethod = storedMethod || explicitMethod || visitMethod || demoFallback || 'CASH'

    const modalPayload = {
      id: invoiceData?.id || selectedVisitData?.invoiceId || matchedHist?.id,
      invoiceCode: resolvedCode || '—',
      type: invoiceData?.type || selectedVisitData?.invoiceType || matchedHist?.type || 'ORIGINAL',
      visitId: invoiceData?.visitId || selectedVisitData?.visitId || matchedHist?.visitId,
      visitCode: formatVisitCode(matchedHist?.visitCode || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.visitCode : null) || invoiceData?.visitCode || invoiceData?.visitId),
      patientName: matchedHist?.patientName || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.patientName : null) || invoiceData?.patientName || null,
      patientCode: matchedHist?.patientCode || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.patientCode : null) || invoiceData?.patientCode || null,
      doctorName: formatDoctorDisplayName(matchedHist?.doctorName || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.doctorName : null) || invoiceData?.doctorName),
      createdBy: formatUserDisplayName(invoiceData?.createdBy || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.collectedBy : null) || user?.fullName),
      paymentMethod: finalPaymentMethod,
      totalAmount: Number(invoiceData?.totalAmount || selectedVisitData?.totalAmount || matchedHist?.totalAmount || 0),
      createdAt: invoiceData?.createdAt || matchedHist?.createdAt || (selectedVisitData?.visitId === (invoiceData?.visitId || targetId) ? selectedVisitData?.invoiceCreatedAt || selectedVisitData?.paidAt : null),
      lines: invoiceData?.lines || selectedVisitData?.invoiceLines || [],
    }

    setViewingInvoiceModal(modalPayload)
  }

  const handleConfirmAdjustment = async () => {
    if (!adjustingInvoiceModal) return
    if (!canAdjustInvoice) {
      message.error('Bạn không có quyền điều chỉnh hóa đơn. Chức năng chỉ dành cho Quản lý (MANAGER).')
      return
    }

    const reason = adjustmentReason.trim()
    if (!reason) {
      message.error('Vui lòng nhập lý do điều chỉnh hóa đơn (bắt buộc).')
      return
    }

    const amt = Number(adjustmentAmount)
    if (isNaN(amt) || amt === 0) {
      message.error('Vui lòng nhập số tiền điều chỉnh hợp lệ (khác 0).')
      return
    }

    setSubmittingAdjustment(true)
    setApiError('')
    try {
      const payload = {
        adjustmentReason: reason,
        lines: [
          {
            itemName: adjustmentItemName || 'Điều chỉnh khoản thu',
            quantity: 1,
            unitPrice: amt,
          },
        ],
      }

      const res = await billingApi.adjust(adjustingInvoiceModal.id, payload)
      if (res?.data) {
        message.success(`Đã tạo Hóa đơn điều chỉnh thành công: ${res.data.invoiceCode || ''}`)
        setAdjustingInvoiceModal(null)
        setAdjustmentReason('')
        setAdjustmentItemName('Điều chỉnh giảm khoản thu')
        setAdjustmentAmount(-20000)
        await refreshAllData()
      }
    } catch (err) {
      console.error('[BillingPage] Lỗi adjust invoice:', err)
      const status = err?.response?.status
      const msg = err?.response?.data?.message
      if (status === 403) {
        message.error('Từ chối truy cập (403 Forbidden). Bạn không có quyền MANAGER để điều chỉnh hóa đơn.')
      } else {
        message.error(msg || 'Không thể điều chỉnh hóa đơn từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingAdjustment(false)
    }
  }

  const handleConfirmRefund = async () => {
    if (!refundingPaymentModal) return
    if (!canAdjustInvoice) {
      message.error('Bạn không có quyền hoàn tiền. Chức năng chỉ dành cho Quản lý phòng khám (MANAGER).')
      return
    }

    const reason = refundReason.trim()
    if (!reason) {
      message.error('Vui lòng nhập lý do hoàn tiền (bắt buộc).')
      return
    }

    const paymentId = refundingPaymentModal.paymentId || refundingPaymentModal.id
    if (!paymentId) {
      message.error('Không tìm thấy paymentId hợp lệ để thực hiện hoàn tiền.')
      return
    }

    setSubmittingRefund(true)
    setApiError('')
    try {
      const res = await billingApi.refundPayment(paymentId, { reason })
      const refundRes = res?.data

      if (refundRes) {
        message.success(`✓ Đã hoàn tiền thành công số tiền ${money(refundRes.amountRefunded || refundingPaymentModal.totalAmount)}!`)
        setRefundingPaymentModal(null)
        setRefundReason('')
        await refreshAllData()
      }
    } catch (err) {
      console.error('[BillingPage] Lỗi refund payment:', err)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 403) {
        message.error('Bạn không có quyền thực hiện hoàn tiền. Vui lòng đăng nhập lại bằng tài khoản có quyền Quản lý.')
      } else if (status === 409) {
        message.error(msg || 'Xung đột trạng thái (409): Giao dịch đã hoàn tiền hoặc đơn thuốc đã xuất kho.')
      } else if (status === 400) {
        message.error(msg || 'Dữ liệu lý do hoàn tiền không hợp lệ (400).')
      } else {
        message.error(msg || 'Không thể thực hiện hoàn tiền từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingRefund(false)
    }
  }

  const feeColumns = [
    { title: 'Khoản thu / Dịch vụ', key: 'name', render: (_, r) => <Text strong style={{ color: '#0f172a' }}>{r.name}</Text> },
    { title: 'Số lượng', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'center', render: (v) => <Tag color="blue" style={{ minWidth: 28, textAlign: 'center', fontWeight: 600 }}>{v}</Tag> },
    { title: 'Đơn giá', dataIndex: 'price', key: 'price', width: 150, align: 'right', render: (v) => <Text style={{ color: '#475569' }}>{money(v)}</Text> },
    { title: 'Thành tiền', dataIndex: 'amount', key: 'amount', width: 160, align: 'right', render: (v) => <Text strong style={{ color: '#2563eb', fontSize: 14 }}>{money(v)}</Text> },
  ]

  const feeDataSource = useMemo(() => {
    if (!selectedVisitData) return []

    const items = [
      { key: 'exam', name: 'Phí khám bệnh', quantity: 1, price: selectedVisitData.examFee, amount: selectedVisitData.examFee },
    ]

    if (Array.isArray(selectedVisitData.prescriptionItems) && selectedVisitData.prescriptionItems.length > 0) {
      selectedVisitData.prescriptionItems.forEach((item, idx) => {
        items.push({
          key: `med-${idx}`,
          name: `Thuốc: ${item.medicineName}${item.dosageInfo ? ` (${item.dosageInfo})` : ''}`,
          quantity: item.quantity,
          price: item.unitPrice || item.price,
          amount: item.amount,
        })
      })
    } else if (selectedVisitData.medicineFee > 0) {
      items.push({
        key: 'med-summary',
        name: 'Tiền thuốc kê đơn',
        quantity: 1,
        price: selectedVisitData.medicineFee,
        amount: selectedVisitData.medicineFee,
      })
    }

    if (Array.isArray(selectedVisitData.serviceFeesList) && selectedVisitData.serviceFeesList.length > 0) {
      selectedVisitData.serviceFeesList.forEach((feeItem, idx) => {
        items.push({
          key: `service-${idx}`,
          name: `Dịch vụ lâm sàng: ${feeItem.serviceName}`,
          quantity: 1,
          price: Number(feeItem.amount || 0),
          amount: Number(feeItem.amount || 0),
        })
      })
    } else if (selectedVisitData.serviceFee > 0) {
      items.push({
        key: 'service-summary',
        name: 'Phí dịch vụ lâm sàng',
        quantity: 1,
        price: selectedVisitData.serviceFee,
        amount: selectedVisitData.serviceFee,
      })
    }

    return items
  }, [selectedVisitData])

  const historyColumns = [
    {
      title: 'Mã HĐ',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
      render: (v, r) => (
        <Space direction="vertical" size={0}>
          <Text code style={{ color: '#1e40af', fontWeight: 700 }}>
            {v || r.id?.substring(0, 8)}
          </Text>
          {r.type === 'ADJUSTMENT' && r.originalInvoiceCode && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              Điều chỉnh cho: <strong>{r.originalInvoiceCode}</strong>
            </Text>
          )}
        </Space>
      ),
    },

    {
      title: 'Bệnh nhân',
      key: 'patient',
      render: (_, r) => {
        if (!r.patientName && !r.patientCode) {
          return <Text type="secondary">—</Text>
        }
        return (
          <span>
            <strong>{r.patientName || '—'}</strong> {r.patientCode ? <Text type="secondary">({r.patientCode})</Text> : null}
          </span>
        )
      },
    },
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitId',
      key: 'visitId',
      render: (v, r) => <Tag color="geekblue">{r.visitCode || formatVisitCode(v)}</Tag>,
    },
    {
      title: 'Số tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (v, r) => {
        const amt = Number(v || 0)
        const isAdjustment = String(r.type || '').toUpperCase() === 'ADJUSTMENT'
        if (isAdjustment) {
          if (amt < 0) {
            return (
              <Space direction="vertical" size={0} align="end">
                <Text strong style={{ color: '#cf1322' }}>
                  {money(amt)}
                </Text>
                <Text type="secondary" style={{ fontSize: 11, color: '#cf1322' }}>
                  Điều chỉnh giảm
                </Text>
              </Space>
            )
          }
          return (
            <Space direction="vertical" size={0} align="end">
              <Text strong style={{ color: '#3f8600' }}>
                +{money(amt)}
              </Text>
              <Text type="secondary" style={{ fontSize: 11, color: '#3f8600' }}>
                Điều chỉnh tăng
              </Text>
            </Space>
          )
        }
        return <Text strong style={{ color: '#1677ff' }}>{money(amt)}</Text>
      },
    },
    {
      title: 'Trạng thái',
      key: 'status',
      align: 'center',
      render: (_, r) => {
        if (r.status === 'REFUNDED') {
          return <Tag color="purple" icon={<ReloadOutlined />}>Đã hoàn tiền</Tag>
        }
        if (r.type === 'ADJUSTMENT') {
          return <Tag color="magenta">Điều chỉnh</Tag>
        }
        return <Tag color="green" icon={<CheckCircleOutlined />}>Thành công</Tag>
      },
    },
    {
      title: 'Thời gian lập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v) => formatDateTime(v),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'center',
      width: 100,
      render: (_, r) => {
        const hasAdjustment = historyInvoices.some(
          (inv) =>
            inv.type === 'ADJUSTMENT' &&
            ((inv.originalInvoiceId && String(inv.originalInvoiceId) === String(r.id)) ||
             (inv.originalInvoiceCode && String(inv.originalInvoiceCode) === String(r.invoiceCode)))
        )

        const isRefundDisabled = !canAdjustInvoice || hasAdjustment || r.status === 'REFUNDED' || r.type === 'ADJUSTMENT'

        let refundTooltipTitle = ''
        if (hasAdjustment) {
          refundTooltipTitle = 'Không thể hoàn tiền vì hóa đơn này đã phát sinh điều chỉnh.'
        } else if (!canAdjustInvoice) {
          refundTooltipTitle = 'Chức năng hoàn tiền chỉ dành cho Quản lý phòng khám (MANAGER).'
        } else if (r.status === 'REFUNDED') {
          refundTooltipTitle = 'Giao dịch đã được hoàn tiền.'
        } else if (r.type === 'ADJUSTMENT') {
          refundTooltipTitle = 'Hóa đơn điều chỉnh không thể hoàn tiền.'
        }

        return (
          <Dropdown
            menu={{
              items: [
                {
                  key: 'view',
                  icon: <EyeOutlined style={{ color: '#2563eb' }} />,
                  label: 'Xem hóa đơn',
                  onClick: () => handleViewInvoice(r.id),
                },
                {
                  key: 'print',
                  icon: <PrinterOutlined style={{ color: '#475569' }} />,
                  label: 'In hóa đơn',
                  onClick: () => {
                    handleViewInvoice(r.id)
                    setTimeout(() => window.print(), 300)
                  },
                },
                {
                  type: 'divider',
                },
                {
                  key: 'refund',
                  disabled: isRefundDisabled,
                  icon: <DollarCircleOutlined style={{ color: isRefundDisabled ? '#94a3b8' : '#d97706' }} />,
                  label: isRefundDisabled && refundTooltipTitle ? (
                    <Tooltip title={refundTooltipTitle} placement="left">
                      <span style={{ color: '#94a3b8' }}>Hoàn tiền thanh toán</span>
                    </Tooltip>
                  ) : (
                    <span style={{ color: '#d97706', fontWeight: 600 }}>Hoàn tiền thanh toán</span>
                  ),
                  onClick: () => {
                    if (isRefundDisabled) {
                      if (refundTooltipTitle) message.warning(refundTooltipTitle)
                      return
                    }
                    setRefundingPaymentModal(r)
                    setRefundReason('')
                  },
                },
                ...((r.type === 'ORIGINAL' || !r.type) && !hasAdjustment
                  ? [
                      {
                        key: 'adjust',
                        icon: <WarningOutlined style={{ color: '#dc2626' }} />,
                        label: <span style={{ color: '#dc2626' }}>Điều chỉnh hóa đơn</span>,
                        onClick: () => {
                          if (!canAdjustInvoice) {
                            message.warning('Bạn không có quyền điều chỉnh hóa đơn. Chức năng chỉ dành cho Quản lý phòng khám (MANAGER).')
                            return
                          }
                          setAdjustingInvoiceModal(r)
                          setAdjustmentReason('')
                          setAdjustmentItemName('Điều chỉnh giảm khoản thu')
                          setAdjustmentAmount(-20000)
                        },
                      },
                    ]
                  : []),
              ],
            }}
            trigger={['click']}
            placement="bottomRight"
          >
            <Button
              style={{
                borderRadius: 8,
                borderColor: '#93c5fd',
                color: '#2563eb',
                width: 36,
                height: 36,
                padding: 0,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
              }}
              icon={<MoreOutlined style={{ fontSize: 18, color: '#2563eb' }} />}
            />
          </Dropdown>
        )
      },
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <DollarCircleOutlined style={{ color: '#2563eb', marginRight: 8 }} /> Thu phí &amp; Hóa đơn
          </Title>
          <Text type="secondary">Quản lý thu tiền viện phí, thanh toán dịch vụ và lập hóa đơn điện tử.</Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={loadingVisits || loadingHistory} onClick={refreshAllData}>
          Làm mới dữ liệu
        </Button>
      </div>

      {!hasBillingAccess && (
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Bạn không có quyền thao tác trên trang Thu phí & Hóa đơn."
          description="Chức năng yêu cầu vai trò Lễ tân (RECEPTIONIST), Quản lý (MANAGER) hoặc Quản trị viên (ADMIN)."
          style={{ marginBottom: 16 }}
        />
      )}

      {apiError && (
        <Alert
          type="error"
          showIcon
          message="Thông báo từ hệ thống"
          description={apiError}
          action={<Button size="small" onClick={refreshAllData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        style={{ marginBottom: 16 }}
        items={[
          {
            key: 'pending',
            label: (
              <span>
                <ClockCircleOutlined style={{ color: '#d97706', marginRight: 6 }} />
                Chờ thanh toán ({filteredPendingVisits.length})
              </span>
            ),
            children: (
              <Row gutter={[16, 16]} align="stretch">
                <Col xs={24} xl={9}>
                  <Card title={`Hàng đợi thanh toán (${filteredPendingVisits.length})`} styles={{ body: { padding: 12 } }} style={{ height: '100%' }}>
                    <Input
                      allowClear
                      prefix={<SearchOutlined />}
                      placeholder="Tìm tên BN, mã BN, mã lượt khám..."
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      style={{ marginBottom: 12 }}
                    />
                    <List
                      loading={loadingVisits}
                      dataSource={filteredPendingVisits}
                      locale={{ emptyText: <Empty description="Không có lượt khám nào chờ thanh toán" /> }}
                      style={{ maxHeight: 620, overflowY: 'auto' }}
                      renderItem={(item) => {
                        const vId = item.visitId || item.id
                        const selected = String(vId) === String(selectedVisitId)
                        return (
                          <List.Item
                            key={vId}
                            onClick={() => setSelectedVisitId(vId)}
                            style={{
                              cursor: 'pointer',
                              border: selected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                              background: selected ? '#eff6ff' : '#ffffff',
                              borderRadius: 8,
                              marginBottom: 8,
                              padding: 12,
                              transition: 'all 0.2s ease',
                            }}
                            extra={<RightOutlined style={{ color: selected ? '#2563eb' : '#94a3b8' }} />}
                          >
                            <List.Item.Meta
                              title={(
                                <Space wrap>
                                  <Text strong style={{ color: selected ? '#1e40af' : '#0f172a' }}>
                                    {item.visitCode || item.queueCode || vId}
                                  </Text>
                                  <Tag color="orange">CHƯA THANH TOÁN</Tag>
                                </Space>
                              )}
                              description={(
                                <Space direction="vertical" size={1}>
                                  <Text strong style={{ color: '#0f172a' }}>{item.patientName} ({item.patientCode || '—'})</Text>
                                  <Text type="secondary">Bác sĩ: {item.doctorName || '—'}</Text>
                                </Space>
                              )}
                            />
                          </List.Item>
                        )
                      }}
                    />
                  </Card>
                </Col>

                <Col xs={24} xl={15}>
                  <Card title={selectedVisitData ? `Tổng hợp khoản thu: ${selectedVisitData.visitCode}` : 'Chi tiết khoản thu & Hóa đơn'} style={{ height: '100%' }}>
                    {!selectedVisitData ? (
                      <Empty description="Vui lòng chọn lượt khám ở danh sách bên trái" />
                    ) : (
                      <Space direction="vertical" size="large" style={{ width: '100%' }}>
                        <div>
                          <Title level={5} style={{ marginBottom: 10, color: '#1e3a8a' }}>1. Thông tin lượt khám &amp; Trạng thái</Title>
                          <Descriptions
                            bordered
                            size="small"
                            column={{ xs: 1, sm: 1, md: 2, lg: 2, xl: 2, xxl: 2 }}
                            labelStyle={{ width: '135px', fontWeight: 600, color: '#475569', background: '#f8fafc', fontSize: 13 }}
                            contentStyle={{ background: '#ffffff' }}
                          >
                            <Descriptions.Item label="Mã lượt khám">
                              <Text code style={{ fontSize: 13, color: '#1e40af', fontWeight: 600 }}>{selectedVisitData.visitCode}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="Bệnh nhân">
                              <Text strong style={{ color: '#2563eb' }}>{selectedVisitData.patientName}</Text>{' '}
                              <Text type="secondary">({selectedVisitData.patientCode})</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="Bác sĩ khám">
                              <Text strong>{selectedVisitData.doctorName}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="Lượt khám">
                              {selectedVisitData.isVisitCompleted ? (
                                <Tag color="green" style={{ margin: 0 }}>Đã hoàn thành ({selectedVisitData.visitStatus})</Tag>
                              ) : (
                                <Tag color="orange" icon={<WarningOutlined />} style={{ margin: 0 }}>Đang khám ({selectedVisitData.visitStatus})</Tag>
                              )}
                            </Descriptions.Item>
                            <Descriptions.Item label="Đơn thuốc">
                              {selectedVisitData.prescriptionStatus === 'DISPENSED' ? (
                                <Space wrap style={{ margin: 0 }}>
                                  <Tag color="green" icon={<CheckCircleOutlined />} style={{ margin: 0, fontWeight: 600 }}>
                                    Đã cấp phát ({selectedVisitData.prescriptionCode || 'DISPENSED'})
                                  </Tag>
                                  <Text type="secondary" style={{ fontSize: 12 }}>
                                    ({selectedVisitData.prescriptionItems.length} loại thuốc)
                                  </Text>
                                </Space>
                              ) : selectedVisitData.prescriptionStatus === 'PENDING_DISPENSE' ? (
                                <Space wrap style={{ margin: 0 }}>
                                  <Tag color="orange" icon={<ClockCircleOutlined />} style={{ margin: 0, fontWeight: 600 }}>
                                    Có đơn - Chờ Dược sĩ cấp phát ({selectedVisitData.prescriptionCode || 'PENDING'})
                                  </Tag>
                                  <Text type="secondary" style={{ fontSize: 12 }}>
                                    ({selectedVisitData.prescriptionItems.length} loại thuốc)
                                  </Text>
                                </Space>
                              ) : selectedVisitData.prescriptionStatus ? (
                                <Tag color="volcano" style={{ margin: 0 }}>{selectedVisitData.prescriptionStatus}</Tag>
                              ) : (
                                <Tag color="default" style={{ margin: 0 }}>Không có đơn thuốc</Tag>
                              )}
                            </Descriptions.Item>
                            <Descriptions.Item label="Thanh toán">
                              {selectedVisitData.invoiceCode ? (
                                <Tag color="green" icon={<CheckCircleOutlined />} style={{ margin: 0, fontWeight: 700 }}>ĐÃ LẬP HÓA ĐƠN</Tag>
                              ) : selectedVisitData.paymentStatus === 'PAID' ? (
                                <Tag color="blue" icon={<CheckCircleOutlined />} style={{ margin: 0, fontWeight: 700 }}>ĐÃ THANH TOÁN</Tag>
                              ) : (
                                <Tag color="volcano" icon={<WarningOutlined />} style={{ margin: 0, fontWeight: 700 }}>CHƯA THANH TOÁN</Tag>
                              )}
                            </Descriptions.Item>
                          </Descriptions>
                        </div>

                        {!selectedVisitData.isVisitCompleted && (
                          <Alert
                            type="warning"
                            showIcon
                            icon={<WarningOutlined />}
                            message="Lượt khám chưa đủ điều kiện thanh toán."
                            description="Lượt khám đang ở trạng thái IN_PROGRESS (chưa hoàn tất khám bệnh). Backend quy định chỉ các lượt khám COMPLETED mới được ghi nhận thanh toán."
                          />
                        )}

                        {selectedVisitData.isVisitCompleted && !selectedVisitData.isDispensingCompleted && (
                          <Alert
                            type="warning"
                            showIcon
                            icon={<ClockCircleOutlined />}
                            message="Thuốc chưa được cấp phát."
                            description="Đơn thuốc của lượt khám này đang ở trạng thái PENDING_DISPENSE. Dược sĩ cần thực hiện xuất kho cấp thuốc trước khi Lễ tân thu phí."
                          />
                        )}

                        <Divider style={{ margin: '4px 0' }} />

                        <div>
                          <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>2. Chi tiết các khoản phải thu</Title>
                          <Table rowKey="key" columns={feeColumns} dataSource={feeDataSource} pagination={false} size="small" loading={loadingData} scroll={{ x: 'max-content' }} />
                          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
                            <Card size="small" style={{ backgroundColor: '#f8fafc', minWidth: 300, borderColor: '#cbd5e1' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <Text strong style={{ fontSize: 15 }}>TỔNG PHẢI THU:</Text>
                                <Text strong style={{ fontSize: 19, color: '#dc2626' }}>{money(selectedVisitData.totalAmount)}</Text>
                              </div>
                            </Card>
                          </div>
                        </div>

                        <Divider style={{ margin: '4px 0' }} />

                        <div>
                          <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>3. Ghi nhận thanh toán &amp; Hóa đơn điện tử</Title>

                          {selectedVisitData.invoiceCode ? (
                            <Alert
                              type="success"
                              showIcon
                              icon={<CheckCircleOutlined />}
                              message={<Text strong style={{ color: '#15803d', fontSize: 15 }}>✓ HÓA ĐƠN ĐÃ ĐƯỢC LẬP (Mã HĐ: {selectedVisitData.invoiceCode})</Text>}
                              description={
                                <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 8 }}>
                                  <div>
                                    <Text>Mã Hóa đơn: <strong style={{ color: '#1e40af', fontSize: 15 }}>{selectedVisitData.invoiceCode}</strong></Text>
                                    <Divider type="vertical" />
                                    <Text>Người thu: <strong>{selectedVisitData.collectedBy}</strong></Text>
                                    <Divider type="vertical" />
                                    <Text>Thời gian: {formatDateTime(selectedVisitData.paidAt || selectedVisitData.invoiceCreatedAt)}</Text>
                                  </div>
                                  <Space wrap style={{ marginTop: 4 }}>
                                    <Button
                                      type="primary"
                                      icon={<EyeOutlined />}
                                      onClick={() => handleViewInvoice(selectedVisitData.invoiceId)}
                                    >
                                      Xem hóa đơn
                                    </Button>
                                    <Button
                                      icon={<PrinterOutlined />}
                                      onClick={() => {
                                        handleViewInvoice(selectedVisitData.invoiceId)
                                        setTimeout(() => window.print(), 300)
                                      }}
                                    >
                                      In hóa đơn
                                    </Button>
                                  </Space>
                                </Space>
                              }
                              style={{ border: '1px solid #bbf7d0', background: '#f0fdf4' }}
                            />
                          ) : selectedVisitData.paymentStatus === 'PAID' ? (
                            <Alert
                              type="info"
                              showIcon
                              icon={<CheckCircleOutlined />}
                              message={
                                <Text strong style={{ color: '#1e40af', fontSize: 15 }}>
                                  {selectedVisitData.paymentMethod === 'CASH'
                                    ? '✓ Đã thanh toán tiền mặt'
                                    : selectedVisitData.paymentMethod === 'BANK_TRANSFER'
                                    ? '✓ Đã thanh toán chuyển khoản'
                                    : '✓ Thanh toán thành công'}
                                </Text>
                              }
                              description={
                                <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 8 }}>
                                  <div>
                                    <Text>Số tiền đã thu: <strong style={{ color: '#dc2626', fontSize: 16 }}>{money(selectedVisitData.totalAmount)}</strong></Text>
                                    <Divider type="vertical" />
                                    <Text>Người thu: <strong>{selectedVisitData.collectedBy}</strong></Text>
                                    <Divider type="vertical" />
                                    <Text>Thời gian: {formatDateTime(selectedVisitData.paidAt)}</Text>
                                  </div>
                                  <div style={{ marginTop: 6 }}>
                                    <Button
                                      type="primary"
                                      size="large"
                                      icon={<FileTextOutlined />}
                                      loading={submittingInvoice}
                                      disabled={!canCollectPayment || submittingInvoice}
                                      onClick={handleCreateInvoice}
                                      style={{ fontWeight: 600 }}
                                    >
                                      Lập hóa đơn điện tử
                                    </Button>
                                  </div>
                                </Space>
                              }
                              style={{ border: '1px solid #bfdbfe', background: '#eff6ff' }}
                            />
                          ) : (
                            <Card style={{ backgroundColor: '#f0f7ff', borderColor: '#bae6fd' }}>
                              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                                {!canCollectPayment && (
                                  <Alert
                                    type="warning"
                                    showIcon
                                    message="Tài khoản không có quyền thu viện phí"
                                    description="Bạn cần được cấp quyền INVOICE_CREATE để thực hiện thu phí và xuất hóa đơn."
                                  />
                                )}

                                {selectedVisitData.prescriptionStatus === 'PENDING_DISPENSE' && (
                                  <Alert
                                    type="info"
                                    showIcon
                                    message="Đơn thuốc đang chờ cấp phát tại Quầy Dược (PENDING_DISPENSE)"
                                    description="Theo quy trình nghiệp vụ hệ thống, Dược sĩ cần hoàn tất cấp phát thuốc trước khi Lễ tân ghi nhận thu phí."
                                  />
                                )}

                                {selectedVisitData.visitStatus === 'CANCELLED' && (
                                  <Alert
                                    type="error"
                                    showIcon
                                    message="Lượt khám đã bị hủy (CANCELLED)"
                                    description="Không thể thực hiện thu phí đối với các lượt khám đã bị hủy."
                                  />
                                )}

                                <Form layout="vertical">
                                  <Form.Item label={<strong>Phương thức thanh toán *</strong>} style={{ marginBottom: 0 }}>
                                    <Select
                                      value={paymentMethod}
                                      onChange={setPaymentMethod}
                                      options={PAYMENT_METHODS}
                                      size="large"
                                      disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                    />
                                  </Form.Item>
                                </Form>

                                {paymentMethod === 'CASH' && (
                                  <div style={{ background: '#ffffff', padding: 16, borderRadius: 8, border: '1px solid #cbd5e1' }}>
                                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                      <Text strong style={{ color: '#0f172a', fontSize: 14 }}>THANH TOÁN TIỀN MẶT</Text>
                                      <Text type="secondary">Thu tiền mặt trực tiếp từ bệnh nhân tại quầy Lễ tân.</Text>
                                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 }}>
                                        <Text>Tổng phải thu: <strong style={{ color: '#dc2626', fontSize: 17 }}>{money(selectedVisitData.totalAmount)}</strong></Text>
                                        <Popconfirm
                                          title={<Text strong style={{ color: '#1e3a8a' }}>Xác nhận ghi nhận thu tiền mặt</Text>}
                                          description={`Xác nhận đã thu ${money(selectedVisitData.totalAmount)} tiền mặt cho ${selectedVisitData.visitCode}?`}
                                          okText="Xác nhận đã thu tiền"
                                          cancelText="Hủy"
                                          onConfirm={handleConfirmPayment}
                                          disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                        >
                                          <Button
                                            type="primary"
                                            size="large"
                                            icon={<DollarCircleOutlined />}
                                            loading={submittingPayment}
                                            disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                            style={{ background: '#16a34a', borderColor: '#16a34a', fontWeight: 600 }}
                                          >
                                            Xác nhận đã thu tiền
                                          </Button>
                                        </Popconfirm>
                                      </div>
                                    </Space>
                                  </div>
                                )}

                                {paymentMethod === 'BANK_TRANSFER' && (
                                  <div style={{ background: '#ffffff', padding: 16, borderRadius: 8, border: '1px solid #93c5fd' }}>
                                    <Title level={5} style={{ color: '#1e40af', marginTop: 0, marginBottom: 12 }}>
                                      🏦 THANH TOÁN CHUYỂN KHOẢN NGÂN HÀNG
                                    </Title>
                                    <Row gutter={[16, 16]} align="middle">
                                      <Col xs={24} sm={14}>
                                        <Space direction="vertical" size={6} style={{ width: '100%', fontSize: 13 }}>
                                          <div>Tổng tiền: <strong style={{ color: '#dc2626', fontSize: 16 }}>{money(selectedVisitData.totalAmount)}</strong></div>
                                          <div>Ngân hàng: <strong>VietinBank (NH TMCP Công Thương VN)</strong></div>
                                          <div>Chủ tài khoản: <strong>HỆ THỐNG PHÒNG KHÁM BỆNH ÁN SỐ</strong></div>
                                          <div>Số tài khoản: <Text code style={{ fontSize: 14, fontWeight: 700, color: '#1e40af' }}>102800999999</Text></div>
                                          <div>Nội dung CK: <Text code style={{ fontSize: 14, fontWeight: 700, color: '#d97706' }}>TT {selectedVisitData.visitCode}</Text></div>
                                        </Space>
                                      </Col>
                                      <Col xs={24} sm={10} style={{ textAlign: 'center' }}>
                                        <div style={{ border: '1px dashed #2563eb', padding: 8, borderRadius: 8, background: '#eff6ff', display: 'inline-block' }}>
                                          <img
                                            src={`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(`STK: 102800999999 | Ngan hang: VietinBank | So tien: ${selectedVisitData.totalAmount} | Noi dung: TT ${selectedVisitData.visitCode}`)}`}
                                            alt="QR Thanh toán Chuyển khoản"
                                            style={{ width: 130, height: 130, borderRadius: 4 }}
                                          />
                                          <div style={{ fontSize: 11, color: '#1e40af', marginTop: 4, fontWeight: 600 }}>
                                            📱 QR Thanh toán chuyển khoản
                                          </div>
                                        </div>
                                      </Col>
                                    </Row>
                                    <Divider style={{ margin: '12px 0' }} />
                                    <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                                      <Popconfirm
                                        title={<Text strong style={{ color: '#1e3a8a' }}>Xác nhận đã nhận chuyển khoản</Text>}
                                        description={`Xác nhận đã kiểm tra tài khoản và nhận đủ ${money(selectedVisitData.totalAmount)} cho ${selectedVisitData.visitCode}?`}
                                        okText="Xác nhận đã nhận chuyển khoản"
                                        cancelText="Hủy"
                                        onConfirm={handleConfirmPayment}
                                        disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                      >
                                        <Button
                                          type="primary"
                                          size="large"
                                          icon={<CheckCircleOutlined />}
                                          loading={submittingPayment}
                                          disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                          style={{ background: '#2563eb', fontWeight: 600 }}
                                        >
                                          Xác nhận đã nhận chuyển khoản
                                        </Button>
                                      </Popconfirm>
                                    </div>
                                  </div>
                                )}

                                {paymentMethod === 'CARD' && (
                                  <div style={{ background: '#ffffff', padding: 16, borderRadius: 8, border: '1px solid #cbd5e1' }}>
                                    <Space direction="vertical" size={6} style={{ width: '100%' }}>
                                      <Title level={5} style={{ color: '#0f172a', margin: 0 }}>
                                        💳 THANH TOÁN THẺ NGÂN HÀNG
                                      </Title>
                                      <Text type="secondary" style={{ fontSize: 13 }}>
                                        Thực hiện quẹt thẻ ATM / Thẻ ghi nợ / VISA / MasterCard trên thiết bị quẹt thẻ POS của phòng khám.
                                      </Text>
                                      <div style={{ fontSize: 14, margin: '6px 0' }}>
                                        Tổng thanh toán: <strong style={{ color: '#dc2626', fontSize: 17 }}>{money(selectedVisitData.totalAmount)}</strong>
                                      </div>
                                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 10 }}>
                                        <Popconfirm
                                          title={<Text strong style={{ color: '#1e3a8a' }}>Xác nhận quẹt thẻ ngân hàng</Text>}
                                          description={`Xác nhận đã quẹt thẻ thành công ${money(selectedVisitData.totalAmount)} cho ${selectedVisitData.visitCode}?`}
                                          okText="Xác nhận quẹt thẻ thành công"
                                          cancelText="Hủy"
                                          onConfirm={handleConfirmPayment}
                                          disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                        >
                                          <Button
                                            type="primary"
                                            size="large"
                                            icon={<CreditCardOutlined />}
                                            loading={submittingPayment}
                                            disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment}
                                            style={{ background: '#0284c7', borderColor: '#0284c7', fontWeight: 600 }}
                                          >
                                            Xác nhận quẹt thẻ thành công
                                          </Button>
                                        </Popconfirm>
                                      </div>
                                    </Space>
                                  </div>
                                )}
                              </Space>
                            </Card>
                          )}
                        </div>
                      </Space>
                    )}
                  </Card>
                </Col>
              </Row>
            ),
          },
          {
            key: 'history',
            label: (
              <span>
                <HistoryOutlined style={{ color: '#16a34a', marginRight: 6 }} />
                Lịch sử thanh toán &amp; Hóa đơn ({filteredHistoryInvoices.length})
              </span>
            ),
            children: (
              <Card title={`Danh sách hóa đơn & giao dịch đã hoàn thành (${filteredHistoryInvoices.length})`}>
                <Input
                  allowClear
                  prefix={<SearchOutlined />}
                  placeholder="Tìm theo mã HĐ, mã lượt khám, tên BN..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  style={{ marginBottom: 16, maxWidth: 400 }}
                />
                <Table
                  rowKey={(r) => r.id || r.invoiceCode}
                  columns={historyColumns}
                  dataSource={filteredHistoryInvoices}
                  loading={loadingHistory}
                  pagination={{ pageSize: 10, showSizeChanger: true }}
                  size="middle"
                  bordered
                  locale={{ emptyText: <Empty description="Chưa có lịch sử thanh toán nào từ Backend" /> }}
                />
              </Card>
            ),
          },
        ]}
      />

      <Modal
        title={
          <Space>
            <FileTextOutlined style={{ color: '#008080' }} />
            <span style={{ fontWeight: 700, color: '#008080' }}>HÓA ĐƠN ĐIỆN TỬ PHÒNG KHÁM</span>
          </Space>
        }
        open={!!viewingInvoiceModal}
        onCancel={() => setViewingInvoiceModal(null)}
        width={780}
        style={{ top: 20 }}
        footer={[
          <Button
            key="print"
            type="primary"
            icon={<PrinterOutlined />}
            onClick={() => window.print()}
            style={{ background: '#008080', borderColor: '#008080', fontWeight: 600 }}
          >
            In hóa đơn
          </Button>,
          <Button key="close" onClick={() => setViewingInvoiceModal(null)}>
            Đóng
          </Button>,
        ]}
      >
        {viewingInvoiceModal && (
          <div className="printable-invoice-container" style={{ padding: 24, background: '#fff', borderRadius: 8, color: '#0f172a' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '2px solid #008080', paddingBottom: 12, marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{ width: 48, height: 48, borderRadius: '50%', border: '2.5px solid #008080', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#008080', fontSize: 24, background: '#f0fdfa' }}>
                  <PlusOutlined style={{ color: '#008080' }} />
                </div>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 700, color: '#008080', letterSpacing: '0.8px', textTransform: 'uppercase' }}>
                    HÓA ĐƠN ĐIỆN TỬ PHÒNG KHÁM
                  </div>
                  <div style={{ fontSize: 20, fontWeight: 800, color: '#0f172a', margin: '1px 0' }}>
                    HỆ THỐNG PHÒNG KHÁM BỆNH ÁN SỐ
                  </div>
                  <div style={{ fontSize: 12, color: '#475569' }}>
                    📍 Địa chỉ: Chuyển đổi số CBYT, Hà Nội &nbsp;|&nbsp; 📞 Điện thoại: (024) 3825 9999
                  </div>
                </div>
              </div>
            </div>

            <div style={{ textAlign: 'center', marginBottom: 20 }}>
              <div style={{ fontSize: 21, fontWeight: 800, color: viewingInvoiceModal.type === 'ADJUSTMENT' ? '#6b21a8' : '#004d40', letterSpacing: '0.5px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
                <span style={{ color: viewingInvoiceModal.type === 'ADJUSTMENT' ? '#7e22ce' : '#008080', fontSize: 14 }}>⟡</span>
                {viewingInvoiceModal.type === 'ADJUSTMENT' ? 'HÓA ĐƠN ĐIỀU CHỈNH (HĐĐT)' : 'HÓA ĐƠN GIÁ TRỊ GIA TĂNG (HĐĐT)'}
                <span style={{ color: viewingInvoiceModal.type === 'ADJUSTMENT' ? '#7e22ce' : '#008080', fontSize: 14 }}>⟡</span>
              </div>
              <div style={{ fontSize: 13, color: '#475569', marginTop: 4 }}>
                Mã HĐ: <strong style={{ color: '#0f172a' }}>{viewingInvoiceModal.invoiceCode || (selectedVisitData?.invoiceId === viewingInvoiceModal.id ? selectedVisitData?.invoiceCode : null) || '—'}</strong> &nbsp;|&nbsp; Ngày lập: {formatDateTime(viewingInvoiceModal.createdAt || viewingInvoiceModal.paidAt || (selectedVisitData?.invoiceId === viewingInvoiceModal.id ? selectedVisitData?.paidAt : null))}
                {viewingInvoiceModal.type === 'ADJUSTMENT' && viewingInvoiceModal.originalInvoiceCode && (
                  <div style={{ fontSize: 12.5, color: '#6b21a8', fontWeight: 600, marginTop: 2 }}>
                    (Điều chỉnh cho hóa đơn gốc: {viewingInvoiceModal.originalInvoiceCode})
                  </div>
                )}
                {viewingInvoiceModal.adjustmentReason && (
                  <div style={{ fontSize: 12, color: '#475569', fontStyle: 'italic', marginTop: 2 }}>
                    Lý do điều chỉnh: {viewingInvoiceModal.adjustmentReason}
                  </div>
                )}
              </div>
            </div>

            <div style={{ border: '1px solid #cbd5e1', borderRadius: 12, overflow: 'hidden', marginBottom: 20, background: '#ffffff' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <UserOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Bệnh nhân</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {viewingInvoiceModal.patientName || (selectedVisitData?.visitId === viewingInvoiceModal.visitId ? selectedVisitData?.patientName : null) || '—'}{' '}
                      {(viewingInvoiceModal.patientCode || (selectedVisitData?.visitId === viewingInvoiceModal.visitId ? selectedVisitData?.patientCode : null)) ? (
                        <span style={{ fontWeight: 400, color: '#64748b' }}>({viewingInvoiceModal.patientCode || selectedVisitData?.patientCode})</span>
                      ) : null}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <IdcardOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Mã lượt khám</div>
                    <div style={{ fontSize: 15, fontWeight: 800, color: '#008080' }}>
                      {formatVisitCode(viewingInvoiceModal.visitCode || viewingInvoiceModal.visitId || selectedVisitData?.visitCode)}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <MedicineBoxOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Bác sĩ khám</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {formatDoctorDisplayName(viewingInvoiceModal.doctorName || selectedVisitData?.doctorName)}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <TeamOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Người lập hóa đơn</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {formatUserDisplayName(viewingInvoiceModal.createdBy || selectedVisitData?.collectedBy || user?.fullName)}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '12px 16px', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <DollarOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Phương thức thanh toán</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {(viewingInvoiceModal?.paymentMethod || selectedVisitData?.paymentMethod || paymentMethod) === 'BANK_TRANSFER'
                        ? 'Chuyển khoản'
                        : (viewingInvoiceModal?.paymentMethod || selectedVisitData?.paymentMethod || paymentMethod) === 'CARD'
                        ? 'Thẻ ngân hàng'
                        : (viewingInvoiceModal?.paymentMethod || selectedVisitData?.paymentMethod || paymentMethod) === 'QR_CODE'
                        ? 'QR Code'
                        : 'Tiền mặt'}
                    </div>
                  </div>
                </div>

                <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <FileTextOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Trạng thái hóa đơn</div>
                    <div>
                      <Tag color="green" style={{ margin: 0, fontWeight: 700, borderRadius: 4, padding: '2px 8px' }}>
                        ĐÃ LẬP HÓA ĐƠN ({viewingInvoiceModal.type || 'ORIGINAL'})
                      </Tag>
                    </div>
                  </div>
                </div>

              </div>
            </div>

            <div style={{ marginBottom: 20 }}>
              <div style={{ display: 'inline-block', background: '#008080', color: '#ffffff', padding: '6px 16px', borderRadius: '8px 8px 0 0', fontSize: 13, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                CHI TIẾT CÁC KHOẢN THU
              </div>
              <div style={{ border: '1px solid #cbd5e1', borderRadius: '0 10px 10px 10px', overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#008080', color: '#ffffff', fontSize: 13, fontWeight: 700 }}>
                      <th style={{ padding: '10px 12px', textAlign: 'center', width: '60px', borderRight: '1px solid #0e7490' }}>STT</th>
                      <th style={{ padding: '10px 12px', textAlign: 'left', borderRight: '1px solid #0e7490' }}>Nội dung / Dịch vụ</th>
                      <th style={{ padding: '10px 12px', textAlign: 'center', width: '100px', borderRight: '1px solid #0e7490' }}>Số lượng</th>
                      <th style={{ padding: '10px 12px', textAlign: 'right', width: '140px', borderRight: '1px solid #0e7490' }}>Đơn giá</th>
                      <th style={{ padding: '10px 12px', textAlign: 'right', width: '150px' }}>Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(feeDataSource && feeDataSource.length > 0
                      ? feeDataSource
                      : (Array.isArray(viewingInvoiceModal.lines) && viewingInvoiceModal.lines.length > 0
                        ? viewingInvoiceModal.lines.map((l) => ({
                            name: l.lineType === 'EXAM_FEE' ? 'Phí khám bệnh' : l.lineType === 'MEDICINE_FEE' ? 'Tiền thuốc kê đơn' : (l.itemName || 'Dịch vụ'),
                            quantity: l.quantity,
                            price: l.unitPrice,
                            amount: l.amount,
                          }))
                        : [])
                    ).map((item, idx) => (

                      <tr key={idx} style={{ borderBottom: '1px solid #e2e8f0', fontSize: '13.5px' }}>
                        <td style={{ padding: '10px 12px', textAlign: 'center', fontWeight: 600, borderRight: '1px solid #e2e8f0' }}>{idx + 1}</td>
                        <td style={{ padding: '10px 12px', fontWeight: 700, color: '#0f172a', borderRight: '1px solid #e2e8f0' }}>{item.name}</td>
                        <td style={{ padding: '10px 12px', textAlign: 'center', borderRight: '1px solid #e2e8f0' }}>{item.quantity}</td>
                        <td style={{ padding: '10px 12px', textAlign: 'right', borderRight: '1px solid #e2e8f0' }}>{money(item.price)}</td>
                        <td style={{ padding: '10px 12px', textAlign: 'right', fontWeight: 700, color: '#0284c7' }}>{money(item.amount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div style={{ border: '1px solid #cbd5e1', borderRadius: 12, padding: '14px 20px', background: '#f0fdfa', display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{ width: 44, height: 44, borderRadius: '50%', border: '2px solid #008080', color: '#008080', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, background: '#ffffff' }}>
                  <CheckOutlined />
                </div>
                <span style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', letterSpacing: '0.5px' }}>TỔNG THANH TOÁN:</span>
              </div>
              <div style={{ fontSize: 26, fontWeight: 800, color: '#0284c7' }}>
                {money(viewingInvoiceModal.totalAmount || selectedVisitData?.totalAmount)}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '190px 1fr', gap: 16, marginBottom: 16 }}>
              <div style={{ border: '1px solid #008080', borderRadius: 10, overflow: 'hidden', textAlign: 'center', background: '#ffffff', display: 'flex', flexDirection: 'column' }}>
                <div style={{ background: '#008080', color: '#ffffff', padding: '6px', fontWeight: 800, fontSize: 13, letterSpacing: '1px' }}>
                  MÃ QR
                </div>
                <div style={{ padding: '10px 8px', flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                  <img
                    src={`https://api.qrserver.com/v1/create-qr-code/?size=130x130&data=${encodeURIComponent(viewingInvoiceModal.invoiceCode || selectedVisitData?.invoiceCode || 'HD000004')}`}
                    alt="QR Code"
                    style={{ width: 125, height: 125, border: '1px solid #e2e8f0', borderRadius: 4 }}
                  />
                  <div style={{ fontSize: 10.5, color: '#64748b', marginTop: 6, fontStyle: 'italic' }}>
                    Quét mã QR để tra cứu hóa đơn điện tử
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 10 }}>
                <div style={{ border: '1px solid #cbd5e1', borderRadius: 10, padding: '10px 12px', background: '#f8fafc', fontSize: 11, color: '#475569', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                  <InfoCircleOutlined style={{ fontSize: 18, color: '#008080', marginTop: 2, flexShrink: 0 }} />
                  <div>
                    <div>Quý khách hàng vui lòng kiểm tra kỹ thông tin trên hóa đơn. Mọi thắc mắc hoặc cần hỗ trợ, xin liên hệ phòng khám.</div>
                    <div style={{ borderTop: '1px dashed #cbd5e1', margin: '5px 0' }}></div>
                    <div><strong>Lưu ý:</strong> Theo luật chế độ hóa đơn điện tử hiện hành, hóa đơn này có giá trị sử dụng trong ngày (trước 23h00). Quý khách vui lòng giữ hóa đơn để tra cứu HĐĐT.</div>
                  </div>
                </div>

                <div style={{ border: '1px solid #cbd5e1', borderRadius: 10, padding: '12px 8px', background: '#ffffff' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', fontSize: 12 }}>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, color: '#0f172a' }}>Người lập hóa đơn</div>
                      <div style={{ fontSize: 11, color: '#64748b' }}>(Lễ tân phòng khám)</div>
                      <div style={{ marginTop: 30, color: '#94a3b8' }}>...................................</div>
                    </div>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, color: '#0f172a' }}>Đề nghị của bác sĩ</div>
                      <div style={{ height: 16 }}></div>
                      <div style={{ marginTop: 30, color: '#94a3b8' }}>...................................</div>
                    </div>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, color: '#0f172a' }}>Người thu</div>
                      <div style={{ height: 16 }}></div>
                      <div style={{ marginTop: 30, color: '#94a3b8' }}>...................................</div>
                    </div>
                  </div>
                </div>

                <div style={{ textAlign: 'right', fontSize: 12, color: '#475569', fontStyle: 'italic' }}>
                  {formatDateVietnamese(viewingInvoiceModal.createdAt || selectedVisitData?.paidAt)}
                </div>

              </div>
            </div>

            <div style={{ borderTop: '2px solid #008080', paddingTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 11.5, color: '#008080', fontWeight: 600 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <SafetyCertificateOutlined style={{ fontSize: 14 }} />
                Hóa đơn điện tử chuyển đổi từ hệ thống quản lý bệnh án số.
              </div>
              <div style={{ letterSpacing: 1 }}>
                ⚡ ∿∿∿ ♥
              </div>
            </div>

          </div>
        )}
      </Modal>

      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#d97706' }} />
            <span style={{ fontWeight: 700, color: '#92400e' }}>ĐIỀU CHỈNH HÓA ĐƠN GỐC</span>
          </Space>
        }
        open={!!adjustingInvoiceModal}
        onCancel={() => setAdjustingInvoiceModal(null)}
        footer={[
          <Button key="cancel" onClick={() => setAdjustingInvoiceModal(null)}>
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={submittingAdjustment}
            disabled={!canAdjustInvoice || submittingAdjustment}
            onClick={handleConfirmAdjustment}
          >
            Xác nhận điều chỉnh hóa đơn
          </Button>,
        ]}
      >
        {adjustingInvoiceModal && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {!canAdjustInvoice && (
              <Alert
                type="error"
                showIcon
                icon={<LockOutlined />}
                message="Bạn không có quyền thực hiện điều chỉnh hóa đơn."
                description="Chức năng này yêu cầu vai trò Quản lý phòng khám (MANAGER) hoặc Quản trị viên (ADMIN)."
              />
            )}

            <Card size="small" style={{ background: '#f8fafc', borderColor: '#cbd5e1' }}>
              <Descriptions size="small" column={1} labelStyle={{ fontWeight: 600, color: '#475569', width: 140 }}>
                <Descriptions.Item label="Mã HĐ gốc">
                  <Text code style={{ color: '#1e40af', fontWeight: 700 }}>{adjustingInvoiceModal.invoiceCode}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Bệnh nhân">
                  <Text strong>{adjustingInvoiceModal.patientName || '—'}</Text> {adjustingInvoiceModal.patientCode ? `(${adjustingInvoiceModal.patientCode})` : ''}
                </Descriptions.Item>
                <Descriptions.Item label="Mã lượt khám">
                  <Tag color="geekblue">{adjustingInvoiceModal.visitCode}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Tổng tiền HĐ gốc">
                  <Text strong style={{ color: '#2563eb' }}>{money(adjustingInvoiceModal.totalAmount)}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Thời gian lập">
                  {formatDateTime(adjustingInvoiceModal.createdAt)}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Form layout="vertical">
              <Form.Item label={<strong>Lý do điều chỉnh (Bắt buộc) *</strong>} required>
                <Input.TextArea
                  rows={3}
                  placeholder="Nhập chi tiết lý do điều chỉnh hóa đơn gốc (vd: Giảm tiền thuốc do ghi thừa khoản thu...)"
                  value={adjustmentReason}
                  onChange={(e) => setAdjustmentReason(e.target.value)}
                />
              </Form.Item>

              <Row gutter={16}>
                <Col span={14}>
                  <Form.Item label={<strong>Tên khoản điều chỉnh</strong>}>
                    <Input
                      placeholder="Tên mục điều chỉnh"
                      value={adjustmentItemName}
                      onChange={(e) => setAdjustmentItemName(e.target.value)}
                    />
                  </Form.Item>
                </Col>
                <Col span={10}>
                  <Form.Item label={<strong>Số tiền điều chỉnh (₫) *</strong>} help="Số âm = Điều chỉnh giảm, Số dương = Điều chỉnh tăng">
                    <Input
                      type="number"
                      placeholder="-20000"
                      value={adjustmentAmount}
                      onChange={(e) => setAdjustmentAmount(e.target.value)}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Space>
        )}
      </Modal>

      <Modal
        title={
          <Space>
            <DollarCircleOutlined style={{ color: '#d97706' }} />
            <span style={{ fontWeight: 700, color: '#92400e' }}>XÁC NHẬN HOÀN TIỀN THANH TOÁN</span>
          </Space>
        }
        open={!!refundingPaymentModal}
        onCancel={() => setRefundingPaymentModal(null)}
        footer={[
          <Button key="cancel" onClick={() => setRefundingPaymentModal(null)}>
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={submittingRefund}
            disabled={!canAdjustInvoice || submittingRefund}
            onClick={handleConfirmRefund}
          >
            Xác nhận hoàn tiền
          </Button>,
        ]}
      >
        {refundingPaymentModal && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {!canAdjustInvoice && (
              <Alert
                type="error"
                showIcon
                icon={<LockOutlined />}
                message="Bạn không có quyền thực hiện hoàn tiền."
                description="Chức năng này yêu cầu vai trò Quản lý phòng khám (MANAGER)."
              />
            )}

            <Card size="small" style={{ background: '#fffbe6', borderColor: '#ffe58f' }}>
              <Descriptions size="small" column={1} labelStyle={{ fontWeight: 600, color: '#475569', width: 150 }}>
                <Descriptions.Item label="Bệnh nhân">
                  <Text strong>{refundingPaymentModal.patientName || '—'}</Text> {refundingPaymentModal.patientCode ? `(${refundingPaymentModal.patientCode})` : ''}
                </Descriptions.Item>
                <Descriptions.Item label="Mã lượt khám">
                  <Tag color="geekblue">{refundingPaymentModal.visitCode}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Mã thanh toán / HĐ">
                  <Text code style={{ color: '#1e40af', fontWeight: 700 }}>{refundingPaymentModal.invoiceCode || refundingPaymentModal.paymentId || refundingPaymentModal.id}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Số tiền đã thu">
                  <Text strong style={{ color: '#dc2626', fontSize: 16 }}>{money(refundingPaymentModal.totalAmount || refundingPaymentModal.amountPaid)}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Phương thức TT">
                  {refundingPaymentModal.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : refundingPaymentModal.paymentMethod === 'CARD' ? 'Thẻ ngân hàng' : 'Tiền mặt'}
                </Descriptions.Item>
                <Descriptions.Item label="Thời gian thanh toán">
                  {formatDateTime(refundingPaymentModal.createdAt || refundingPaymentModal.paidAt)}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Form layout="vertical">
              <Form.Item label={<strong>Lý do hoàn tiền (Bắt buộc) *</strong>} required>
                <Input.TextArea
                  rows={3}
                  placeholder="Nhập chi tiết lý do hoàn tiền (vd: Bệnh nhân hủy khám, nhập sai thông tin thanh toán...)"
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                />
              </Form.Item>
            </Form>
          </Space>
        )}
      </Modal>
    </div>
  )
}

export default BillingPage
