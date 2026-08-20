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
  { key: 'SYMPTOMS', label: 'Triệu chứng & Dấu hiệu bất thường (R00-R99)' },
]

export const getIcd10CategoryByCode = (code = '') => {
  const prefix = String(code).trim().charAt(0).toUpperCase()
  switch (prefix) {
    case 'A':
    case 'B':
      return 'INFECTIOUS'
    case 'C':
    case 'D':
      return 'NEOPLASMS'
    case 'E':
      return 'ENDOCRINE'
    case 'F':
      return 'MENTAL'
    case 'G':
      return 'NERVOUS'
    case 'H':
      return 'SENSORY'
    case 'I':
      return 'CIRCULATORY'
    case 'J':
      return 'RESPIRATORY'
    case 'K':
      return 'DIGESTIVE'
    case 'L':
      return 'SKIN'
    case 'M':
      return 'MUSCULOSKELETAL'
    case 'N':
      return 'GENITOURINARY'
    case 'R':
      return 'SYMPTOMS'
    default:
      return 'OTHER'
  }
}
