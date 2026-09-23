import axiosClient from './axiosClient'

const LOCAL_STORAGE_KEY = 'benh_an_so_special_controlled_registers'
const SPECIAL_CONTROL_MAP_KEY = 'benh_an_so_special_control_map'

export const getStoredSpecialControlMap = () => {
  try {
    const raw = localStorage.getItem(SPECIAL_CONTROL_MAP_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export const saveStoredSpecialControlMap = (medicineId, payload) => {
  try {
    const map = getStoredSpecialControlMap()
    map[String(medicineId)] = payload
    localStorage.setItem(SPECIAL_CONTROL_MAP_KEY, JSON.stringify(map))
    return map
  } catch {
    return {}
  }
}

/**
 * Hợp nhất thông tin kiểm soát đặc biệt từ localStorage vào danh sách thuốc
 * @param {Array} medicinesList
 * @returns {Array}
 */
export const mergeSpecialControlData = (medicinesList = []) => {
  if (!Array.isArray(medicinesList)) return []
  const map = getStoredSpecialControlMap()
  return medicinesList.map((med) => {
    const custom = map[String(med.id)]
    if (custom) {
      return {
        ...med,
        isSpecialControl: Boolean(custom.isSpecialControl),
        specialControlGroup: custom.isSpecialControl ? custom.specialControlGroup : null,
        specialControlNote: custom.isSpecialControl ? custom.specialControlNote : null,
      }
    }
    return med
  })
}

export const getStoredSpecialControlRegisters = () => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (err) {
    console.warn('[SpecialControlApi] Không thể đọc dữ liệu sổ theo dõi từ localStorage:', err)
    return []
  }
}

export const saveStoredSpecialControlRegister = (entry) => {
  try {
    const list = getStoredSpecialControlRegisters()
    const updated = [entry, ...list.filter((item) => item.id !== entry.id)]
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated))
    return entry
  } catch (err) {
    console.warn('[SpecialControlApi] Không thể ghi dữ liệu sổ theo dõi vào localStorage:', err)
    return entry
  }
}

const specialControlledDrugApi = {
  /**
   * Cập nhật thông tin kiểm soát đặc biệt cho một loại thuốc
   * @param {string} medicineId
   * @param {{ isSpecialControl: boolean, specialControlGroup?: string, specialControlNote?: string }} payload
   */
  patchSpecialControl: async (medicineId, payload) => {
    // 1. Luôn lưu bền vững vào localStorage ngay lập tức
    saveStoredSpecialControlMap(medicineId, payload)

    // 2. Thử gửi lên Backend nếu Backend hỗ trợ endpoint
    try {
      const response = await axiosClient.patch(`/medicines/${medicineId}/special-control`, payload)
      return response.data || response
    } catch {
      // Backend chưa có endpoint, đã lưu thành công ở tầng Frontend
      return { id: medicineId, ...payload }
    }
  },

  /**
   * Bác sĩ gửi xác nhận bổ sung khi kê đơn thuốc kiểm soát đặc biệt (QTN-39)
   * @param {string} prescriptionId
   * @param {{ medicineId: string, reason: string }} payload
   */
  confirmPrescribe: async (prescriptionId, payload) => {
    try {
      const response = await axiosClient.post(
        `/prescriptions/${prescriptionId}/special-control-confirm`,
        payload,
      )
      return response.data || response
    } catch (err) {
      if (err?.response?.status === 404) {
        // Fallback ghi nhận bút toán sổ theo dõi cục bộ
        const fallbackEntry = {
          id: `SDB-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
          registerCode: `SDB-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.floor(1000 + Math.random() * 9000)}`,
          actionType: 'PRESCRIBED',
          prescriptionId,
          medicineId: payload.medicineId,
          medicineName: payload.medicineName || 'Thuốc kiểm soát đặc biệt',
          specialControlGroup: payload.specialControlGroup || 'NARCOTIC',
          patientName: payload.patientName || 'Bệnh nhân',
          patientCode: payload.patientCode || 'BN-Chưa cập nhật',
          patientIdCard: payload.patientIdCard || '',
          quantity: payload.quantity || 1,
          unit: payload.unit || 'Viên',
          confirmationReason: payload.reason,
          confirmedBy: payload.confirmedByName || 'Bác sĩ điều trị',
          confirmedAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
        }
        saveStoredSpecialControlRegister(fallbackEntry)
        return fallbackEntry
      }
      throw err
    }
  },

  /**
   * Dược sĩ gửi xác nhận bổ sung khi cấp phát thuốc kiểm soát đặc biệt (QTN-39, QTN-06)
   * @param {string} dispenseId
   * @param {{ batchId: string, quantity: number, reason: string }} payload
   */
  confirmDispense: async (dispenseId, payload) => {
    try {
      const response = await axiosClient.post(
        `/pharmacy/dispense/${dispenseId}/special-control-confirm`,
        payload,
      )
      return response.data || response
    } catch (err) {
      if (err?.response?.status === 404) {
        const fallbackEntry = {
          id: `SDB-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
          registerCode: `SDB-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.floor(1000 + Math.random() * 9000)}`,
          actionType: 'DISPENSED',
          dispenseId,
          medicineId: payload.medicineId,
          medicineName: payload.medicineName || 'Thuốc kiểm soát đặc biệt',
          specialControlGroup: payload.specialControlGroup || 'NARCOTIC',
          medicineBatchId: payload.batchId,
          batchNumber: payload.batchNumber || 'Lô mặc định',
          patientName: payload.patientName || 'Bệnh nhân',
          patientCode: payload.patientCode || 'BN-Chưa cập nhật',
          patientIdCard: payload.patientIdCard || '',
          quantity: payload.quantity || 1,
          unit: payload.unit || 'Viên',
          confirmationReason: payload.reason,
          confirmedBy: payload.confirmedByName || 'Dược sĩ cấp phát',
          confirmedAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
        }
        saveStoredSpecialControlRegister(fallbackEntry)
        return fallbackEntry
      }
      throw err
    }
  },

  /**
   * Lấy danh sách Sổ theo dõi thuốc kiểm soát đặc biệt (có phân trang và lọc)
   * @param {object} params
   */
  getRegisterEntries: async (params = {}) => {
    try {
      const response = await axiosClient.get('/pharmacy/special-controlled-drugs/register', {
        params,
      })
      return response.data || response
    } catch (err) {
      if (err?.response?.status === 404) {
        // Fallback đọc dữ liệu từ localStorage
        let items = getStoredSpecialControlRegisters()

        // Lọc theo nhóm
        if (params.group && params.group !== 'ALL') {
          items = items.filter((item) => item.specialControlGroup === params.group)
        }
        // Lọc theo hành động
        if (params.actionType && params.actionType !== 'ALL') {
          items = items.filter((item) => item.actionType === params.actionType)
        }
        // Lọc theo từ khóa (tên thuốc, tên bệnh nhân, mã sổ)
        if (params.keyword) {
          const kw = params.keyword.toLowerCase()
          items = items.filter(
            (item) =>
              item.medicineName?.toLowerCase().includes(kw) ||
              item.patientName?.toLowerCase().includes(kw) ||
              item.patientCode?.toLowerCase().includes(kw) ||
              item.registerCode?.toLowerCase().includes(kw),
          )
        }

        const page = Number(params.page || 0)
        const size = Number(params.size || 10)
        const start = page * size
        const paged = items.slice(start, start + size)

        return {
          content: paged,
          totalElements: items.length,
          totalPages: Math.ceil(items.length / size) || 1,
          number: page,
          size,
        }
      }
      throw err
    }
  },

  /**
   * Lấy chi tiết một bút toán trong Sổ theo dõi
   * @param {string} registerId
   */
  getRegisterDetail: async (registerId) => {
    try {
      const response = await axiosClient.get(
        `/pharmacy/special-controlled-drugs/register/${registerId}`,
      )
      return response.data || response
    } catch (err) {
      if (err?.response?.status === 404) {
        const items = getStoredSpecialControlRegisters()
        const found = items.find((item) => String(item.id) === String(registerId))
        if (found) return found
      }
      throw err
    }
  },

  /**
   * Xuất báo cáo sổ theo dõi thuốc kiểm soát đặc biệt
   * @param {object} params
   */
  exportRegister: async (params = {}) => {
    const response = await axiosClient.get('/pharmacy/special-controlled-drugs/export', {
      params,
      responseType: 'blob',
    })
    return response
  },
}

export default specialControlledDrugApi
