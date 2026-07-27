import axiosClient from './axiosClient'
export default {services:()=>axiosClient.get('/system/services'),createService:d=>axiosClient.post('/system/services',d),updateService:(id,d)=>axiosClient.put(`/system/services/${id}`,d),clinic:()=>axiosClient.get('/system/clinic'),updateClinic:d=>axiosClient.put('/system/clinic',d)}
