export const MEDICAL_RECORD_FIELD_CODES = [
  {
    code: 'CHIEF_COMPLAINT',
    defaultLabel: 'Lý do khám',
    description: 'Lý do bệnh nhân đến khám bệnh hoặc nhập viện',
    placeholder: 'Nhập lý do vào viện / khám bệnh (VD: Đau ngực, sốt cao, tái khám định kỳ...)',
    defaultOrder: 1,
    defaultRequired: true,
  },
  {
    code: 'SYMPTOMS',
    defaultLabel: 'Triệu chứng lâm sàng',
    description: 'Các triệu chứng cơ năng người bệnh cảm nhận và mô tả',
    placeholder: 'Mô tả triệu chứng cơ năng (VD: Ho nhiều về đêm, sốt 38.5 độ 3 ngày, mệt mỏi...)',
    defaultOrder: 2,
    defaultRequired: true,
  },
  {
    code: 'MEDICAL_HISTORY',
    defaultLabel: 'Tiền sử bệnh',
    description: 'Tiền sử bệnh lý bản thân, gia đình và các yếu tố nguy cơ, dị ứng',
    placeholder: 'Ghi nhận tiền sử bệnh nội khoa, ngoại khoa, dị ứng thuốc/thức ăn, tiền sử gia đình...',
    defaultOrder: 3,
    defaultRequired: false,
  },
  {
    code: 'PHYSICAL_EXAMINATION',
    defaultLabel: 'Khám thực thể',
    description: 'Khám toàn thân, các cơ quan, tim phổi, tiêu hóa, thần kinh...',
    placeholder: 'Ghi nhận kết quả khám lâm sàng toàn thân và các cơ quan...',
    defaultOrder: 4,
    defaultRequired: false,
  },
  {
    code: 'CLINICAL_PROGRESS',
    defaultLabel: 'Diễn tiến bệnh',
    description: 'Quá trình diễn biến của bệnh trong đợt khám / điều trị',
    placeholder: 'Ghi nhận diễn tiến triệu chứng và đáp ứng ban đầu...',
    defaultOrder: 5,
    defaultRequired: false,
  },
  {
    code: 'TREATMENT_PLAN',
    defaultLabel: 'Kế hoạch điều trị',
    description: 'Hướng xử trí, phương pháp điều trị, chỉ định cận lâm sàng hoặc thuốc',
    placeholder: 'Kế hoạch điều trị, chăm sóc, chỉ định chuyên khoa...',
    defaultOrder: 6,
    defaultRequired: false,
  },
  {
    code: 'DOCTOR_INSTRUCTIONS',
    defaultLabel: 'Dặn dò của bác sĩ',
    description: 'Lời dặn về chế độ ăn uống, sinh hoạt, lịch tái khám hoặc dấu hiệu trở nặng',
    placeholder: 'Lời dặn bệnh nhân về dùng thuốc, chế độ ăn, theo dõi triệu chứng và lịch tái khám...',
    defaultOrder: 7,
    defaultRequired: false,
  },
  {
    code: 'CONCLUSION',
    defaultLabel: 'Kết luận',
    description: 'Tổng kết chẩn đoán sơ bộ, tiên lượng tình trạng bệnh',
    placeholder: 'Kết luận chẩn đoán và tiên lượng...',
    defaultOrder: 8,
    defaultRequired: false,
  },
]

export const FIELD_CODE_MAP = MEDICAL_RECORD_FIELD_CODES.reduce((acc, item) => {
  acc[item.code] = item
  return acc
}, {})

export const getFieldMeta = (fieldCode) => {
  return FIELD_CODE_MAP[fieldCode] || {
    code: fieldCode,
    defaultLabel: fieldCode,
    description: '',
    placeholder: 'Nhập thông tin...',
    defaultOrder: 99,
    defaultRequired: false,
  }
}

export const DEFAULT_TEMPLATE_SECTIONS = [
  { fieldCode: 'CHIEF_COMPLAINT', label: 'Lý do khám', required: true, displayOrder: 1 },
  { fieldCode: 'SYMPTOMS', label: 'Triệu chứng lâm sàng', required: true, displayOrder: 2 },
  { fieldCode: 'MEDICAL_HISTORY', label: 'Tiền sử bệnh', required: false, displayOrder: 3 },
  { fieldCode: 'PHYSICAL_EXAMINATION', label: 'Khám thực thể', required: false, displayOrder: 4 },
  { fieldCode: 'CLINICAL_PROGRESS', label: 'Diễn tiến bệnh', required: false, displayOrder: 5 },
  { fieldCode: 'TREATMENT_PLAN', label: 'Kế hoạch điều trị', required: false, displayOrder: 6 },
  { fieldCode: 'DOCTOR_INSTRUCTIONS', label: 'Dặn dò của bác sĩ', required: false, displayOrder: 7 },
  { fieldCode: 'CONCLUSION', label: 'Kết luận', required: false, displayOrder: 8 },
]
