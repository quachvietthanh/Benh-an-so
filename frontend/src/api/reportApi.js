import axiosClient from './axiosClient'
export default {
 summary:(params)=>axiosClient.get('/reports/summary',{params}),
 timeline:(params)=>axiosClient.get('/reports/visits-timeline',{params}),
 topMedicines:(params)=>axiosClient.get('/reports/top-medicines',{params}),
 audit:(params)=>axiosClient.get('/reports/audit-logs',{params}),
 export:(params)=>axiosClient.get('/reports/export',{params,responseType:'blob'}),
}
