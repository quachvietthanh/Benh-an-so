import axiosClient from './axiosClient'

/**
 * RESTful API Client for Clinical Orders, Services Catalog & ICD-10 Diagnosis Catalog
 * Backend Controllers:
 * - ClinicalOrderController.java (@RequestMapping("/clinical-orders"))
 * - ClinicalServiceCatalogController.java (@RequestMapping("/clinical-services"))
 * - DiagnosisCatalogController.java (@RequestMapping("/diagnosis-catalog"))
 * - ExaminationDiagnosisController.java (@RequestMapping("/examinations/{examinationId}"))
 */
const clinicalOrderApi = {
  // POST /api/v1/clinical-orders/visits/{visitId}
  createOrder: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, data)
  },

  // GET /api/v1/clinical-orders/visits/{visitId}
  getOrdersByVisitId: (visitId, params = {}) => {
    return axiosClient.get(`/clinical-orders/visits/${visitId}`, { params })
  },

  // GET /api/v1/clinical-services (Search service catalog)
  searchServices: (params = {}) => {
    return axiosClient.get('/clinical-services', { params })
  },

  // GET /api/v1/diagnosis-catalog (Search ICD-10 catalog)
  searchICD10: (searchQuery) => {
    return axiosClient.get('/diagnosis-catalog', { params: { search: searchQuery } })
  },

  // POST /api/v1/examinations/{examinationId}/diagnosis
  recordDiagnosis: (examinationId, data) => {
    return axiosClient.post(`/examinations/${examinationId}/diagnosis`, data)
  },

  // GET /api/v1/examinations/{examinationId}/diagnosis
  getDiagnosis: (examinationId) => {
    return axiosClient.get(`/examinations/${examinationId}/diagnosis`)
  },
}

export default clinicalOrderApi
