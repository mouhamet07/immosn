import api from './api'

export default {
  // POST /api/v1/prospects/{id}/convertir — SUPER_ADMIN : convertit un prospect en compte CLIENT
  convertir(id) {
    return api.post(`/prospects/${id}/convertir`)
  },
}
