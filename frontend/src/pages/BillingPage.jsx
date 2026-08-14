import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
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
import billingApi from '../api/billingApi'
import medicalRecordApi from '../api/medicalRecordApi'
import pharmacyApi from '../api/pharmacyApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'
import { getStoredPrescriptions, mergeMedicines } from '../utils/storageHelpers'


const { Text, Title } = Typography

const money = (val) => `${Number(val || 0).toLocaleString('vi-VN')} ₫`

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  return isNaN(date.getTime()) ? '—' : date.toLocaleString('vi-VN')
}

const formatDateVietnamese = (val) => {
  const d = val ? new Date(val) : new Date()
  if (isNaN(d.getTime())) return `Ngày ${new Date().getDate()} tháng ${new Date().getMonth() + 1} năm ${new Date().getFullYear()}`
  return `Ngày ${d.getDate()} tháng ${String(d.getMonth() + 1).padStart(2, '0')} năm ${d.getFullYear()}`
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


// Map chính xác theo PaymentMethod enum từ Backend Java: CASH, BANK_TRANSFER, CARD, QR_CODE, E_WALLET
const PAYMENT_METHODS = [
  { value: 'CASH', label: '💵 Tiền mặt' },
  { value: 'BANK_TRANSFER', label: '🏦 Chuyển khoản' },
  { value: 'CARD', label: '💳 Thẻ ngân hàng' },
]

function BillingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // 1. Phân quyền vai trò Lễ tân (RECEPTIONIST) & Admin
  const userRoles = useMemo(() => {
    const raw = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return raw.map((r) => String(r || '').toLowerCase().replace(/^role_/, '')).filter(Boolean)
  }, [user])

  const canCollectPayment = userRoles.includes('receptionist') || userRoles.includes('admin')

  // State quản lý Tabs & Danh sách lượt khám / Lịch sử
  const [activeTab, setActiveTab] = useState('pending') // 'pending' | 'history'
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
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [apiError, setApiError] = useState('')
  const [viewingInvoiceModal, setViewingInvoiceModal] = useState(null)

  // 2A. Tải danh sách Lịch sử thanh toán từ Backend REST API (GET /invoices)
  const loadHistoryInvoices = useCallback(async () => {
    setLoadingHistory(true)
    try {
      const res = await billingApi.getAll({ page: 0, size: 50 })
      const data = res?.data
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      setHistoryInvoices(list)
      return list
    } catch (err) {
      console.error('[BillingPage] Lỗi loadHistoryInvoices:', err)
      return []
    } finally {
      setLoadingHistory(false)
    }
  }, [])

  // 2B. Tải danh sách Chờ thanh toán từ Backend API (GET /invoices/payable)
  const loadPendingVisits = useCallback(async (currentHistory = []) => {
    setLoadingVisits(true)
    setApiError('')
    try {
      const res = await billingApi.getPayable({ page: 0, size: 50 })
      const data = res?.data
      const payableList = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []

      // Loại bỏ những lượt khám đã nằm trong Lịch sử Hóa đơn/Thanh toán Backend
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
  }, [location.state, selectedVisitId])

  // Tải đồng bộ cả 2 danh sách từ Backend
  const refreshAllData = useCallback(async () => {
    const historyList = await loadHistoryInvoices()
    await loadPendingVisits(historyList)
  }, [loadHistoryInvoices, loadPendingVisits])

  useEffect(() => {
    refreshAllData()
  }, [])

  // 3. Tải dữ liệu chi tiết của 1 Lượt khám từ Backend API & Storage
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
      // 1. Tải danh mục thuốc để tra cứu đơn giá & tên thuốc
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

      // 2A. Tải thông tin Hóa đơn theo visitId từ Backend API (GET /invoices?visitId={visitId})
      let invoiceData = null
      try {
        const invoiceRes = await billingApi.getByVisit(visitId)
        const rawData = invoiceRes?.data
        const list = Array.isArray(rawData?.content) ? rawData.content : Array.isArray(rawData) ? rawData : (rawData ? [rawData] : [])
        invoiceData = list.find((i) => i && (i.id || i.invoiceCode)) || null
      } catch (e) {
        console.warn('[BillingPage] Lỗi getByVisit invoice:', e?.message)
      }

      // 2B. Tải Đơn thuốc thuộc visitId từ Backend API
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

      // 3. Fallback lấy đơn thuốc từ Local Storage (nếu API bị 403 / rỗng / offline)
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
          const unit = item.unit || matchedMed?.unit || 'viên'
          const dosageInfo = [item.dosage, item.frequency].filter(Boolean).join(' - ')

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

      // Tính chi phí
      const medicineFee = prescriptionItems.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      const examFee = 100000
      const totalAmount = invoiceData?.totalAmount ? Number(invoiceData.totalAmount) : (examFee + medicineFee)

      const hasInvoice = !!(invoiceData && (invoiceData.id || invoiceData.invoiceCode))

      setSelectedVisitData((prev) => {
        const currentPaymentId = invoiceData?.paymentId || (prev?.visitId === visitId ? prev?.paymentId : null)
        const isPaid = hasInvoice || !!currentPaymentId || (prev?.visitId === visitId && prev?.paymentStatus === 'PAID')

        return {
          visitId,
          paymentId: currentPaymentId,
          invoiceId: invoiceData?.id || (prev?.visitId === visitId ? prev?.invoiceId : null),
          invoiceCode: invoiceData?.invoiceCode || (prev?.visitId === visitId ? prev?.invoiceCode : null),
          invoiceType: invoiceData?.type || 'ORIGINAL',
          invoiceLines: invoiceData?.lines || [],
          invoiceCreatedAt: invoiceData?.createdAt || null,
          visitCode: matchedVisit?.visitCode || matchedVisit?.queueCode || invoiceData?.visitCode || visitId,
          patientName: matchedVisit?.patientName || invoiceData?.patientName || 'Bệnh nhân',
          patientCode: matchedVisit?.patientCode || invoiceData?.patientCode || '—',
          doctorName: matchedVisit?.doctorName || invoiceData?.doctorName || 'Bác sĩ khám',
          visitStatus,
          prescriptionCode,
          prescriptionStatus,
          isVisitCompleted,
          isDispensingCompleted,
          isEligibleToPay: isVisitCompleted && isDispensingCompleted,
          examFee,
          medicineFee,
          totalAmount,
          paymentStatus: isPaid ? 'PAID' : 'UNPAID',
          paidAt: invoiceData?.paidAt || (prev?.visitId === visitId ? prev?.paidAt : null) || invoiceData?.createdAt || null,
          paymentMethod: (prev?.visitId === visitId ? prev?.paymentMethod : null) || 'CASH',
          collectedBy: invoiceData?.createdBy || (prev?.visitId === visitId ? prev?.collectedBy : null) || user?.fullName || 'Lễ tân',
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
    const kw = searchKeyword.trim().toLowerCase()
    if (!kw) return historyInvoices
    return historyInvoices.filter((i) =>
      [i.invoiceCode, i.visitId, i.patientName, i.patientCode]
        .some((val) => String(val || '').toLowerCase().includes(kw)),
    )
  }, [historyInvoices, searchKeyword])

  // 4. Ghi nhận thanh toán (POST /invoices/payments) & Xử lý lỗi 409
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

      // Cập nhật state chính xác từ PaymentResponse Backend
      setSelectedVisitData((prev) => ({
        ...prev,
        paymentId: paymentRes.id,
        paymentStatus: isPaidSuccess ? 'PAID' : 'UNPAID',
        paymentMethod: paymentRes.paymentMethod || paymentMethod,
        paidAt: paymentRes.paidAt || paymentRes.createdAt,
        collectedBy: paymentRes.collectedBy || user?.fullName || 'Lễ tân',
        totalAmount: Number(paymentRes.amountPaid || prev.totalAmount),
      }))

      // Xóa khỏi hàng đợi Chờ thanh toán và Tải lại Lịch sử từ Backend
      setPendingVisits((prev) => prev.filter((v) => String(v.visitId || v.id) !== String(selectedVisitId)))
      await loadHistoryInvoices()

    } catch (err) {
      console.error('[BillingPage] Lỗi payment:', err?.config?.url, err?.response?.status, err?.response?.data)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 409) {
        // XỬ LÝ LỖI 409: Khoản thu đã được thanh toán trước đó trên Backend!
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

          // Xóa khỏi danh sách chờ thanh toán
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

  // 5. Lập hóa đơn điện tử (POST /invoices) theo CreateInvoiceRequest DTO { visitId, paymentId }
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

      setSelectedVisitData((prev) => ({
        ...prev,
        invoiceId: detailInvoice.id,
        invoiceCode: detailInvoice.invoiceCode,
        invoiceType: detailInvoice.type || 'ORIGINAL',
        invoiceLines: detailInvoice.lines || [],
        invoiceCreatedAt: detailInvoice.createdAt,
        totalAmount: Number(detailInvoice.totalAmount || prev.totalAmount),
        paymentStatus: 'PAID',
      }))

      // Tải lại Lịch sử thanh toán từ Backend
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

  // 6. Xem hóa đơn điện tử từ Backend API GET /invoices/{invoiceId}
  const handleViewInvoice = async (invoiceId) => {
    const targetId = invoiceId || selectedVisitData?.invoiceId
    if (targetId) {
      try {
        setLoadingData(true)
        const res = await billingApi.getById(targetId)
        if (res?.data) {
          setViewingInvoiceModal(res.data)
          return
        }
      } catch (err) {
        console.warn('[BillingPage] Lỗi getById, fallback từ state:', err?.message)
      } finally {
        setLoadingData(false)
      }
    }

    if (selectedVisitData?.invoiceCode) {
      setViewingInvoiceModal({
        id: selectedVisitData.invoiceId,
        invoiceCode: selectedVisitData.invoiceCode,
        type: selectedVisitData.invoiceType || 'ORIGINAL',
        visitId: selectedVisitData.visitId,
        paymentId: selectedVisitData.paymentId,
        totalAmount: selectedVisitData.totalAmount,
        createdAt: selectedVisitData.invoiceCreatedAt || selectedVisitData.paidAt,
        lines: selectedVisitData.invoiceLines || [],
      })
    }
  }

  const feeColumns = [
    { title: 'Khoản thu / Dịch vụ', key: 'name', render: (_, r) => <strong>{r.name}</strong> },
    { title: 'Số lượng', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'center' },
    { title: 'Đơn giá', dataIndex: 'price', key: 'price', width: 150, align: 'right', render: (v) => money(v) },
    { title: 'Thành tiền', dataIndex: 'amount', key: 'amount', width: 160, align: 'right', render: (v) => <Text strong style={{ color: '#1677ff' }}>{money(v)}</Text> },
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

    return items
  }, [selectedVisitData])


  // Columns cho Tab Lịch sử thanh toán (Backend GET /invoices)
  const historyColumns = [
    {
      title: 'Mã HĐ',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
      render: (v, r) => <Text code style={{ color: '#1e40af', fontWeight: 700 }}>{v || r.id?.substring(0, 8)}</Text>,
    },
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitId',
      key: 'visitId',
      render: (v) => <Text strong>{v?.substring(0, 8)}...</Text>,
    },
    {
      title: 'Số tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (v) => <Text strong style={{ color: '#dc2626' }}>{money(v)}</Text>,
    },
    {
      title: 'Loại HĐ',
      dataIndex: 'type',
      key: 'type',
      align: 'center',
      render: (v) => <Tag color="blue">{v || 'ORIGINAL'}</Tag>,
    },
    {
      title: 'Thời gian lập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v) => formatDateTime(v),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      align: 'center',
      render: () => <Tag color="green" icon={<CheckCircleOutlined />}>ĐÃ LẬP HÓA ĐƠN</Tag>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'center',
      render: (_, r) => (
        <Space size="small">
          <Button
            type="primary"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewInvoice(r.id)}
          >
            Xem HĐ
          </Button>
          <Button
            size="small"
            icon={<PrinterOutlined />}
            onClick={() => {
              handleViewInvoice(r.id)
              setTimeout(() => window.print(), 300)
            }}
          >
            In HĐ
          </Button>
        </Space>
      ),
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

      {!canCollectPayment && (
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Bạn không có quyền thực hiện thu phí."
          description="Chức năng chỉ dành riêng cho tài khoản Lễ tân (RECEPTIONIST) hoặc Quản trị viên (ADMIN)."
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

      {/* 2 TABS CHUẨN FRONTEND: CHỜ THANH TOÁN vs LỊCH SỬ THANH TOÁN */}
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
                {/* Cột trái: Danh sách Lượt khám Chờ thanh toán */}
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

                {/* Cột phải: Chi tiết Khoản Thu & Thanh Toán */}
                <Col xs={24} xl={15}>
                  <Card title={selectedVisitData ? `Tổng hợp khoản thu: ${selectedVisitData.visitCode}` : 'Chi tiết khoản thu & Hóa đơn'} style={{ height: '100%' }}>
                    {!selectedVisitData ? (
                      <Empty description="Vui lòng chọn lượt khám ở danh sách bên trái" />
                    ) : (
                      <Space direction="vertical" size="large" style={{ width: '100%' }}>
                        {/* 1. Thông tin lượt khám Read-Only */}
                        <div>
                          <Title level={5} style={{ marginBottom: 10, color: '#1e3a8a' }}>1. Thông tin lượt khám &amp; Trạng thái</Title>
                          <Descriptions
                            bordered
                            size="small"
                            column={{ xs: 1, sm: 1, md: 2, lg: 2, xl: 2, xxl: 2 }}
                            labelStyle={{ width: '120px', fontWeight: 600, color: '#475569', background: '#f8fafc' }}
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

                        {/* Kiểm tra điều kiện nghiệp vụ Backend */}
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

                        {/* 2. Chi tiết các khoản phải thu */}
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

                        {/* 3. Ghi nhận thanh toán & Lập hóa đơn (Luồng 3 Trạng thái chuẩn) */}
                        <div>
                          <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>3. Ghi nhận thanh toán &amp; Hóa đơn điện tử</Title>

                          {/* TRẠNG THÁI 3: ĐÃ CÓ HÓA ĐƠN */}
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
                            /* TRẠNG THÁI 2: ĐÃ THANH TOÁN (Chờ lập hóa đơn) */
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
                            /* TRẠNG THÁI 1: CHƯA THANH TOÁN */
                            <Card style={{ backgroundColor: '#f0f7ff', borderColor: '#bae6fd' }}>
                              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
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

                                {/* 1. THANH TOÁN TIỀN MẶT */}
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

                                {/* 2. THANH TOÁN CHUYỂN KHOẢN */}
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

                                {/* 3. THANH TOÁN THẺ NGÂN HÀNG */}
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

      {/* Modal Xem & In Hóa đơn Điện tử chuẩn giao diện HĐĐT (Matching User Template) */}
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
            
            {/* 1. HEADER (Logo + Tên Cơ sở + Địa chỉ) */}
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

            {/* 2. INVOICE TITLE & META */}
            <div style={{ textAlign: 'center', marginBottom: 20 }}>
              <div style={{ fontSize: 21, fontWeight: 800, color: '#004d40', letterSpacing: '0.5px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
                <span style={{ color: '#008080', fontSize: 14 }}>⟡</span>
                HÓA ĐƠN GIÁ TRỊ GIA TĂNG (HĐĐT)
                <span style={{ color: '#008080', fontSize: 14 }}>⟡</span>
              </div>
              <div style={{ fontSize: 13, color: '#475569', marginTop: 4 }}>
                Mã HĐ: <strong style={{ color: '#0f172a' }}>{viewingInvoiceModal.invoiceCode || selectedVisitData?.invoiceCode || 'HD000004'}</strong> &nbsp;|&nbsp; Ngày lập: {formatDateTime(viewingInvoiceModal.createdAt || selectedVisitData?.paidAt)}
              </div>
            </div>

            {/* 3. GRID 6 Ô THÔNG TIN BỆNH NHÂN & LƯỢT KHÁM */}
            <div style={{ border: '1px solid #cbd5e1', borderRadius: 12, overflow: 'hidden', marginBottom: 20, background: '#ffffff' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                
                {/* Row 1: Bệnh nhân | Mã lượt khám */}
                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <UserOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Bệnh nhân</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {selectedVisitData?.patientName || viewingInvoiceModal.patientName || 'Pham Ngoc Diep'} <span style={{ fontWeight: 400, color: '#64748b' }}>({selectedVisitData?.patientCode || 'BN000004'})</span>
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
                      {selectedVisitData?.visitCode || 'VIS000011'}
                    </div>
                  </div>
                </div>

                {/* Row 2: Bác sĩ khám | Người lập hóa đơn */}
                <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <MedicineBoxOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Bác sĩ khám</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {selectedVisitData?.doctorName || 'Dr. Nguyen Minh Anh'}
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
                      {selectedVisitData?.collectedBy || user?.fullName || 'Lễ tân phòng khám'}
                    </div>
                  </div>
                </div>

                {/* Row 3: Phương thức thanh toán | Trạng thái hóa đơn */}
                <div style={{ padding: '12px 16px', borderRight: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}>
                    <DollarOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 12, color: '#64748b' }}>Phương thức thanh toán</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {selectedVisitData?.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : selectedVisitData?.paymentMethod === 'CARD' ? 'Thẻ ngân hàng' : selectedVisitData?.paymentMethod === 'QR_CODE' ? 'QR Code' : 'Tiền mặt'}
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

            {/* 4. BẢNG CHI TIẾT CÁC KHOẢN THU */}
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

            {/* 5. TỔNG THANH TOÁN */}
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

            {/* 6. MÃ QR & CHỮ KÝ & GHI CHÚ */}
            <div style={{ display: 'grid', gridTemplateColumns: '190px 1fr', gap: 16, marginBottom: 16 }}>
              
              {/* Box Mã QR */}
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

              {/* Box Bên phải: Thông tin ghi chú & Chữ ký */}
              <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 10 }}>
                
                {/* Note Box */}
                <div style={{ border: '1px solid #cbd5e1', borderRadius: 10, padding: '10px 12px', background: '#f8fafc', fontSize: 11, color: '#475569', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                  <InfoCircleOutlined style={{ fontSize: 18, color: '#008080', marginTop: 2, flexShrink: 0 }} />
                  <div>
                    <div>Quý khách hàng vui lòng kiểm tra kỹ thông tin trên hóa đơn. Mọi thắc mắc hoặc cần hỗ trợ, xin liên hệ phòng khám.</div>
                    <div style={{ borderTop: '1px dashed #cbd5e1', margin: '5px 0' }}></div>
                    <div><strong>Lưu ý:</strong> Theo luật chế độ hóa đơn điện tử hiện hành, hóa đơn này có giá trị sử dụng trong ngày (trước 23h00). Quý khách vui lòng giữ hóa đơn để tra cứu HĐĐT.</div>
                  </div>
                </div>

                {/* Signatures Box */}
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

                {/* Date footer line */}
                <div style={{ textAlign: 'right', fontSize: 12, color: '#475569', fontStyle: 'italic' }}>
                  {formatDateVietnamese(viewingInvoiceModal.createdAt || selectedVisitData?.paidAt)}
                </div>

              </div>
            </div>

            {/* 7. FOOTER BANNER */}
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
    </div>
  )
}

export default BillingPage
