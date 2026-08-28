/**
 * Constants and Legal Text for Personal Data Processing Consent (Nghị định 13/2023/NĐ-CP)
 */

export const DEFAULT_CONSENT_VERSION = 'v1.0'

export const CONSENT_STATUSES = {
  AGREED: 'AGREED',
  WITHDRAWN: 'WITHDRAWN',
  UNCONSENTED: 'UNCONSENTED',
}

export const CONSENT_SUMMARY_POINTS = [
  {
    key: 'purpose',
    title: 'Mục đích xử lý dữ liệu',
    description: 'Tiếp nhận khám bệnh, lập bệnh án điện tử, theo dõi phác đồ điều trị, thanh toán viện phí và thông báo lịch hẹn chăm sóc sức khỏe.',
  },
  {
    key: 'dataTypes',
    title: 'Phạm vi dữ liệu thu thập',
    description: 'Thông tin định danh (Họ tên, ngày sinh, CCCD, SĐT, địa chỉ) và Dữ liệu sức khỏe (chẩn đoán, tiền sử bệnh, kết quả xét nghiệm, đơn thuốc).',
  },
  {
    key: 'rights',
    title: 'Quyền của người bệnh',
    description: 'Người bệnh có quyền được biết, truy cập, chỉnh sửa, yêu cầu cung cấp bản sao hồ sơ hoặc rút lại sự đồng ý theo quy định pháp luật.',
  },
]

export const FULL_CONSENT_DOCUMENT = {
  title: 'PHIẾU ĐỒNG Ý XỬ LÝ DỮ LIỆU CÁ NHÂN VÀ DỮ LIỆU SỨC KHỎE',
  subtitle: '(Ban hành theo quy định của Nghị định 13/2023/NĐ-CP về Bảo vệ dữ liệu cá nhân)',
  version: 'v1.0',
  sections: [
    {
      title: 'Điều 1: Giải thích từ ngữ và Căn cứ pháp lý',
      content: 'Phiếu đồng ý này được lập căn cứ theo Nghị định 13/2023/NĐ-CP của Chính phủ về bảo vệ dữ liệu cá nhân và Luật Khám bệnh, chữa bệnh. Phòng khám cam kết bảo vệ tuyệt đối dữ liệu cá nhân và dữ liệu sức khỏe của người bệnh theo chuẩn bảo mật y tế.',
    },
    {
      title: 'Điều 2: Loại dữ liệu cá nhân được thu thập và xử lý',
      content: '1. Dữ liệu cá nhân cơ bản: Họ và tên, ngày tháng năm sinh, giới tính, số điện thoại, địa chỉ cư trú, số định danh cá nhân (CCCD/CMND), mã thẻ bảo hiểm y tế, thông tin người liên hệ khẩn cấp.\n2. Dữ liệu cá nhân nhạy cảm (Dữ liệu y tế): Tiền sử bệnh, triệu chứng lâm sàng, chẩn đoán ICD-10, kết quả xét nghiệm, kết quả chẩn đoán hình ảnh, đơn thuốc và hồ sơ bệnh án điện tử.',
    },
    {
      title: 'Điều 3: Mục đích và Phạm vi xử lý dữ liệu',
      content: '- Thực hiện quy trình khám bệnh, chẩn đoán, kê đơn và điều trị y khoa an toàn.\n- Khởi tạo và lưu trữ bệnh án điện tử, quản lý hồ sơ sức khỏe liên tục suốt đời.\n- Thanh toán chi phí khám chữa bệnh, liên thông bảo hiểm y tế và đơn thuốc quốc gia.\n- Liên hệ nhắc lịch hẹn, hướng dẫn chăm sóc sau khám và chăm sóc khách hàng.',
    },
    {
      title: 'Điều 4: Quyền và Nghĩa vụ của Người bệnh',
      content: '- Người bệnh có quyền xem, tra cứu, yêu cầu chỉnh sửa thông tin chưa chính xác hoặc yêu cầu cung cấp bản sao hồ sơ bệnh án theo quy định.\n- Người bệnh có quyền rút lại sự đồng ý xử lý dữ liệu cá nhân đối với các mục đích phi điều trị.\n- Việc rút lại sự đồng ý không làm ảnh hưởng đến tính hợp pháp của việc xử lý dữ liệu đã được thực hiện trước đó và các nghĩa vụ lưu trữ bệnh án bắt buộc theo Luật Khám chữa bệnh.',
    },
    {
      title: 'Điều 5: Cam kết bảo mật của Cơ sở khám chữa bệnh',
      content: '- Toàn bộ dữ liệu được mã hóa và lưu trữ an toàn trên hệ thống máy chủ y tế chuyên dụng.\n- Chỉ nhân viên y tế được phân quyền và có trách nhiệm trực tiếp mới được phép truy cập theo từng ca khám.\n- Mọi hành vi truy xuất đều được ghi nhật ký kiểm toán hệ thống (Audit Log) minh bạch.',
    },
  ],
}

/**
 * Format helper for consent status
 */
export function getPatientConsentStatus(patient) {
  if (!patient) return { status: CONSENT_STATUSES.UNCONSENTED, label: 'Chưa ghi nhận', color: 'default' }

  if (patient.consentWithdrawn || patient.isConsentWithdrawn) {
    return {
      status: CONSENT_STATUSES.WITHDRAWN,
      label: 'Đã rút đồng ý',
      color: 'red',
      withdrawnAt: patient.consentWithdrawnAt,
      reason: patient.consentWithdrawnReason,
    }
  }

  if (patient.consentAgreed || patient.isConsentAgreed) {
    return {
      status: CONSENT_STATUSES.AGREED,
      label: `Đã đồng ý (${patient.consentVersion || DEFAULT_CONSENT_VERSION})`,
      color: 'green',
      agreedAt: patient.consentAgreedAt,
      version: patient.consentVersion || DEFAULT_CONSENT_VERSION,
    }
  }

  return {
    status: CONSENT_STATUSES.UNCONSENTED,
    label: 'Chưa có phiếu đồng ý',
    color: 'orange',
  }
}
