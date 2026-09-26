const ATTACHMENTS_KEY = 'app_attachments'

export const demoAttachments = [
  {
    id: 'att-101',
    attachmentCode: 'KQ-20260330-001',
    patientId: 'p1',
    patientName: 'Nguyễn Văn An',
    patientCode: 'BN001',
    category: 'Công thức máu',
    categoryLabel: 'Công thức máu 18 chỉ số',
    testDate: '2026-03-30 09:15',
    doctorName: 'ThS. BS. Nguyễn Văn B',
    status: 'NORMAL',
    statusLabel: 'Bình thường',
    resultSummary: 'RBC: 4.8 T/L, WBC: 7.2 G/L, PLT: 250 G/L, Hb: 142 g/L. Tất cả chỉ số nằm trong giới hạn tham chiếu bình thường.',
    note: 'Bệnh nhân không có dấu hiệu nhiễm trùng hoặc thiếu máu.',
    fileName: 'XetNghiem_CongThucMau_BN001.pdf',
    fileType: 'application/pdf',
    fileSize: '1.2 MB',
    fileUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
    createdAt: '2026-03-30T09:15:00Z',
  },
  {
    id: 'att-102',
    attachmentCode: 'KQ-20260330-002',
    patientId: 'p2',
    patientName: 'Trần Thị Bình',
    patientCode: 'BN002',
    category: 'X-quang',
    categoryLabel: 'X-quang Phổi Thẳng',
    testDate: '2026-03-30 10:30',
    doctorName: 'BS. Lê Thị C',
    status: 'ABNORMAL',
    statusLabel: 'Cần chú ý',
    resultSummary: 'Hình ảnh thâm nhiễm mờ nhẹ ở đáy phổi phải, các góc sườn hoành hai bên sáng. Tim không to.',
    note: 'Theo dõi viêm phổi thùy dưới phải, đề nghị kết hợp với kết quả xét nghiệm máu.',
    fileName: 'XQuang_Phoi_TranThiBinh.png',
    fileType: 'image/png',
    fileSize: '3.4 MB',
    fileUrl: 'https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=800&q=80',
    createdAt: '2026-03-30T10:30:00Z',
  },
  {
    id: 'att-103',
    attachmentCode: 'KQ-20260329-003',
    patientId: 'p3',
    patientName: 'Lê Hoàng Cường',
    patientCode: 'BN003',
    category: 'Siêu âm',
    categoryLabel: 'Siêu âm Ổ bụng Tổng quát',
    testDate: '2026-03-29 14:20',
    doctorName: 'BS. Trần Văn D',
    status: 'NORMAL',
    statusLabel: 'Bình thường',
    resultSummary: 'Gan, mật, tụy, lách, hai thận hình dạng và kích thước bình thường. Không thấy sỏi hay dịch tự do.',
    note: 'Không phát hiện bất thường trên siêu âm bụng.',
    fileName: 'SieuAm_Obung_LeHoangCuong.jpg',
    fileType: 'image/jpeg',
    fileSize: '2.1 MB',
    fileUrl: 'https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80',
    createdAt: '2026-03-29T14:20:00Z',
  },
  {
    id: 'att-104',
    attachmentCode: 'KQ-20260328-004',
    patientId: 'p1',
    patientName: 'Nguyễn Văn An',
    patientCode: 'BN001',
    category: 'Sinh hóa máu',
    categoryLabel: 'Sinh hóa máu (Glucose, Ure, Creatinine, AST, ALT)',
    testDate: '2026-03-28 11:00',
    doctorName: 'ThS. BS. Nguyễn Văn B',
    status: 'ABNORMAL',
    statusLabel: 'Chỉ số tăng nhẹ',
    resultSummary: 'Glucose: 7.8 mmol/L (Tăng nhẹ), Ure: 5.2 mmol/L, Creatinine: 88 umol/L, ALT: 45 U/L.',
    note: 'Đường huyết lúc đói tăng nhẹ, khuyến cáo theo dõi thêm chỉ số HbA1c.',
    fileName: 'SinhHoaMau_BN001_2803.pdf',
    fileType: 'application/pdf',
    fileSize: '850 KB',
    fileUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
    createdAt: '2026-03-28T11:00:00Z',
  },
]

export const getStoredAttachments = () => {
  try {
    const raw = localStorage.getItem(ATTACHMENTS_KEY)
    if (!raw) {
      localStorage.setItem(ATTACHMENTS_KEY, JSON.stringify(demoAttachments))
      return demoAttachments
    }
    return JSON.parse(raw)
  } catch {
    return demoAttachments
  }
}

export const saveStoredAttachment = (attachment) => {
  try {
    const current = getStoredAttachments()
    const updated = [attachment, ...current.filter((item) => String(item.id) !== String(attachment.id))]
    localStorage.setItem(ATTACHMENTS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const deleteStoredAttachment = (id) => {
  try {
    const current = getStoredAttachments()
    const updated = current.filter((item) => String(item.id) !== String(id))
    localStorage.setItem(ATTACHMENTS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeAttachments = (apiAttachments = []) => {
  const localItems = getStoredAttachments()
  const map = new Map()

  localItems.forEach((item) => map.set(String(item.id), item))
  if (Array.isArray(apiAttachments) && apiAttachments.length) {
    apiAttachments.forEach((item) => {
      const existing = map.get(String(item.id))
      map.set(String(item.id), existing ? { ...existing, ...item } : item)
    })
  }

  return Array.from(map.values())
}
