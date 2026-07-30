// Danh mục mã bệnh tiêu chuẩn ICD-10 phục vụ chẩn đoán y khoa

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

export const commonIcd10List = [
  { code: 'J00', name: 'Viêm mũi họng cấp (Cảm lạnh thông thường)', category: 'RESPIRATORY', isPopular: true },
  { code: 'J02', name: 'Viêm họng cấp', category: 'RESPIRATORY', isPopular: true },
  { code: 'J03', name: 'Viêm amiđan cấp', category: 'RESPIRATORY', isPopular: true },
  { code: 'J18', name: 'Viêm phổi, tác nhân không xác định', category: 'RESPIRATORY', isPopular: true },
  { code: 'J20', name: 'Viêm phế quản cấp', category: 'RESPIRATORY', isPopular: true },
  { code: 'J45', name: 'Hen phế quản (Suyễn)', category: 'RESPIRATORY', isPopular: false },

  { code: 'I10', name: 'Tăng huyết áp vô căn (nguyên phát)', category: 'CIRCULATORY', isPopular: true },
  { code: 'I20', name: 'Cơn đau thắt ngực', category: 'CIRCULATORY', isPopular: false },
  { code: 'I25', name: 'Bệnh tim do thiếu máu cục bộ mãn tính', category: 'CIRCULATORY', isPopular: false },
  { code: 'I50', name: 'Suy tim', category: 'CIRCULATORY', isPopular: false },

  { code: 'K21', name: 'Bệnh trào ngược dạ dày - thực quản (GERD)', category: 'DIGESTIVE', isPopular: true },
  { code: 'K29', name: 'Viêm dạ dày và tá tràng', category: 'DIGESTIVE', isPopular: true },
  { code: 'K25', name: 'Loét dạ dày', category: 'DIGESTIVE', isPopular: true },
  { code: 'K58', name: 'Hội chứng ruột kích thích (IBS)', category: 'DIGESTIVE', isPopular: false },
  { code: 'K70', name: 'Bệnh gan do rượu', category: 'DIGESTIVE', isPopular: false },

  { code: 'E11', name: 'Đái tháo đường týp 2 (Không phụ thuộc Insulin)', category: 'ENDOCRINE', isPopular: true },
  { code: 'E10', name: 'Đái tháo đường týp 1 (Phụ thuộc Insulin)', category: 'ENDOCRINE', isPopular: false },
  { code: 'E03', name: 'Suy giáp khác', category: 'ENDOCRINE', isPopular: false },
  { code: 'E78', name: 'Rối loạn chuyển hóa Lipoprotein (Rối loạn mỡ máu)', category: 'ENDOCRINE', isPopular: true },

  { code: 'M54', name: 'Đau lưng / Đau thắt lưng', category: 'MUSCULOSKELETAL', isPopular: true },
  { code: 'M17', name: 'Thoái hóa khớp gối', category: 'MUSCULOSKELETAL', isPopular: true },
  { code: 'M47', name: 'Thoái hóa cột sống', category: 'MUSCULOSKELETAL', isPopular: false },
  { code: 'M10', name: 'Bệnh Gút (Gout)', category: 'MUSCULOSKELETAL', isPopular: true },

  { code: 'G44', name: 'Hội chứng đau đầu khác (Đau đầu căng thẳng / Nửa đầu)', category: 'NERVOUS', isPopular: true },
  { code: 'G47', name: 'Rối loạn giấc ngủ (Mất ngủ)', category: 'NERVOUS', isPopular: true },
  { code: 'G45', name: 'Cơn thiếu máu não cục bộ thoáng qua', category: 'NERVOUS', isPopular: false },

  { code: 'A09', name: 'Tiêu chảy và viêm dạ dày ruột nhiễm khuẩn', category: 'INFECTIOUS', isPopular: true },
  { code: 'B00', name: 'Nhiễm vi rút Herpes', category: 'INFECTIOUS', isPopular: false },
  { code: 'A91', name: 'Sốt xuất huyết Dengue', category: 'INFECTIOUS', isPopular: true },

  { code: 'N39', name: 'Rối loạn hệ tiết niệu (Nhiễm trùng đường tiết niệu)', category: 'GENITOURINARY', isPopular: true },
  { code: 'N20', name: 'Sỏi thận và sỏi niệu quản', category: 'GENITOURINARY', isPopular: true },

  { code: 'R50', name: 'Sốt không rõ nguyên nhân', category: 'SYMPTOMS', isPopular: true },
  { code: 'R51', name: 'Đau đầu', category: 'SYMPTOMS', isPopular: true },
  { code: 'R05', name: 'Ho', category: 'SYMPTOMS', isPopular: true },
  { code: 'R10', name: 'Đau bụng và đau vùng chậu', category: 'SYMPTOMS', isPopular: true },
  { code: 'R42', name: 'Chóng mặt và choáng váng', category: 'SYMPTOMS', isPopular: true },
]

export const searchIcd10 = (query = '', category = 'ALL') => {
  const normalizedQuery = query.trim().toLowerCase()
  return commonIcd10List.filter((item) => {
    const matchesCategory = category === 'ALL' || item.category === category
    const matchesQuery =
      !normalizedQuery ||
      item.code.toLowerCase().includes(normalizedQuery) ||
      item.name.toLowerCase().includes(normalizedQuery)
    return matchesCategory && matchesQuery
  })
}

export const getPopularIcd10 = () => commonIcd10List.filter((item) => item.isPopular)
