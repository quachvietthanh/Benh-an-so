const appointmentCodePattern = /^[A-Z0-9-]{4,20}$/
const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/

const getLocalIsoDate = (date = new Date()) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const isRealIsoDate = (value) => {
  if (!isoDatePattern.test(value)) return false

  const [year, month, day] = value.split('-').map(Number)
  const parsed = new Date(year, month - 1, day)
  return parsed.getFullYear() === year
    && parsed.getMonth() === month - 1
    && parsed.getDate() === day
}

const statusMeta = {
  SCHEDULED: {
    label: 'Đã đặt lịch',
    tone: 'blue',
    description: 'Lịch hẹn đã được ghi nhận. Vui lòng có mặt trước giờ hẹn khoảng 15 phút.',
  },
  IN_PROGRESS: {
    label: 'Đang tiếp nhận',
    tone: 'orange',
    description: 'Lượt khám đang được tiếp nhận hoặc xử lý tại cơ sở y tế.',
  },
  COMPLETED: {
    label: 'Đã hoàn tất',
    tone: 'green',
    description: 'Lượt khám đã hoàn tất. Vui lòng liên hệ cơ sở y tế nếu cần hỗ trợ sau khám.',
  },
  UNAVAILABLE: {
    label: 'Không còn hiệu lực',
    tone: 'gray',
    description: 'Lịch hẹn không còn hiệu lực. Vui lòng liên hệ cơ sở y tế để được hỗ trợ.',
  },
}

export const normalizeAppointmentCode = (value = '') => (
  String(value).trim().toUpperCase()
)

export const validateLookupInput = ({ appointmentCode, dateOfBirth }) => {
  const normalizedCode = normalizeAppointmentCode(appointmentCode)
  const errors = {}

  if (!normalizedCode) {
    errors.appointmentCode = 'Vui lòng nhập mã lịch hẹn.'
  } else if (!appointmentCodePattern.test(normalizedCode)) {
    errors.appointmentCode = 'Mã lịch hẹn gồm 4–20 ký tự chữ, số hoặc dấu gạch ngang.'
  }

  if (!dateOfBirth) {
    errors.dateOfBirth = 'Vui lòng chọn ngày sinh.'
  } else if (!isRealIsoDate(dateOfBirth) || dateOfBirth > getLocalIsoDate()) {
    errors.dateOfBirth = 'Ngày sinh không hợp lệ.'
  }

  return errors
}

export const getPublicLookupStatus = (careState) => (
  statusMeta[careState] || {
    label: 'Chưa xác định',
    tone: 'gray',
    description: 'Vui lòng liên hệ cơ sở y tế để kiểm tra thêm thông tin.',
  }
)

export const getLookupErrorMessage = (status) => {
  if (status === 429) {
    return 'Bạn đã tra cứu quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.'
  }
  if (status === 400 || status === 404 || status === 422) {
    return 'Không tìm thấy lịch hẹn phù hợp. Vui lòng kiểm tra lại mã hẹn và ngày sinh.'
  }
  if (status >= 500) {
    return 'Hệ thống tra cứu đang tạm thời gián đoạn. Vui lòng thử lại sau.'
  }
  return 'Không thể kết nối đến hệ thống tra cứu. Vui lòng kiểm tra mạng và thử lại.'
}
