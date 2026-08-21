import axiosClient from './axiosClient'

const roleApi = {
  getRoles: () => axiosClient.get('/roles'),
  getPermissions: () => axiosClient.get('/permissions'),
  updateRolePermissions: (roleId, permissionCodes) =>
    axiosClient.put(`/roles/${roleId}/permissions`, { permissionCodes }),
}

export default roleApi
