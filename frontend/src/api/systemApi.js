import { getServices } from '../services/mockDataService.js'

const SERVICES_KEY = 'app_system_services'
const CLINIC_KEY = 'app_clinic_config'

const read = (key, fallback) => {
  if (typeof localStorage === 'undefined') return fallback
  try {
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : fallback
  } catch {
    return fallback
  }
}

const write = (key, value) => {
  if (typeof localStorage !== 'undefined') localStorage.setItem(key, JSON.stringify(value))
  return value
}

const defaultClinic = {
  clinicName: 'Bệnh Án Số', address: '', phone: '',
  openingTime: '07:00:00', closingTime: '17:00:00', examinationRooms: [],
}

// Backend chưa có controller cấu hình hệ thống; không phát sinh request tới /system/*.
const systemApi = {
  services: async () => ({ data: read(SERVICES_KEY, getServices()) }),
  createService: async (data) => {
    const created = { id: `svc-${Date.now()}`, ...data }
    write(SERVICES_KEY, [created, ...read(SERVICES_KEY, getServices())])
    return { data: created }
  },
  updateService: async (id, data) => {
    const services = read(SERVICES_KEY, getServices())
    const updated = { ...(services.find((item) => String(item.id) === String(id)) || {}), ...data, id }
    write(SERVICES_KEY, [updated, ...services.filter((item) => String(item.id) !== String(id))])
    return { data: updated }
  },
  clinic: async () => ({ data: read(CLINIC_KEY, defaultClinic) }),
  updateClinic: async (data) => ({ data: write(CLINIC_KEY, data) }),
}

export default systemApi
