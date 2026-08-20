export const icd10Categories = [
  { key: 'ALL', label: 'Tất cả nhóm bệnh' },
  { key: 'CIRCULATORY', label: 'Tim mạch - Mạch máu (I00-I99)' },
  { key: 'RESPIRATORY', label: 'Hô hấp (J00-J99)' },
  { key: 'DIGESTIVE', label: 'Tiêu hóa (K00-K95)' },
  { key: 'ENDOCRINE', label: 'Nội tiết, Chuyển hóa (E00-E89)' },
  { key: 'MUSCULOSKELETAL', label: 'Cơ xương khớp (M00-M99)' },
  { key: 'NERVOUS', label: 'Thần kinh (G00-G99)' },
  { key: 'INFECTIOUS', label: 'Nhiễm trùng, Ký sinh trùng (A00-B99)' },
  { key: 'GENITOURINARY', label: 'Tiết niệu - Sinh dục (N00-N99)' },
  { key: 'NEOPLASMS', label: 'Khối u - Ung bướu (C00-D49)' },
  { key: 'EYE', label: 'Mắt & Phần phụ (H00-H59)' },
  { key: 'EAR', label: 'Tai & Xương chũm (H60-H95)' },
  { key: 'SKIN', label: 'Da & Mô dưới da (L00-L99)' },
  { key: 'SYMPTOMS', label: 'Triệu chứng & Dấu hiệu bất thường (R00-R99)' },
  { key: 'INJURY', label: 'Chấn thương & Ngộ độc (S00-T88)' },
  { key: 'FACTORS', label: 'Yếu tố sức khỏe & Khám tổng quát (Z00-Z99)' },
]

export const getCategoryFromIcdCode = (code = '') => {
  const c = String(code).trim().toUpperCase()
  if (!c) return 'ALL'
  const letter = c[0]
  if (letter === 'A' || letter === 'B') return 'INFECTIOUS'
  if (letter === 'C' || letter === 'D') return 'NEOPLASMS'
  if (letter === 'E') return 'ENDOCRINE'
  if (letter === 'F' || letter === 'G') return 'NERVOUS'
  if (letter === 'H') {
    const num = parseInt(c.slice(1), 10)
    return !isNaN(num) && num >= 60 ? 'EAR' : 'EYE'
  }
  if (letter === 'I') return 'CIRCULATORY'
  if (letter === 'J') return 'RESPIRATORY'
  if (letter === 'K') return 'DIGESTIVE'
  if (letter === 'L') return 'SKIN'
  if (letter === 'M') return 'MUSCULOSKELETAL'
  if (letter === 'N') return 'GENITOURINARY'
  if (letter === 'R') return 'SYMPTOMS'
  if (letter === 'S' || letter === 'T') return 'INJURY'
  if (letter === 'Z') return 'FACTORS'
  return 'ALL'
}

// Dữ liệu mã ICD-10 được tải trực tiếp 100% từ Backend (bảng icd10_catalog)
export const commonIcd10List = []

export const searchIcd10 = () => []

export const getPopularIcd10 = () => []
