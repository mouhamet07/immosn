import api from './api'

export default {
  getContratHistory(id, page = 0, size = 50) {
    return api.get(`/contrats/${id}/historique`, { params: { page, size } })
  },
  getLeadHistory(id, page = 0, size = 50) {
    return api.get(`/leads/${id}/historique`, { params: { page, size } })
  },
  getVisiteHistory(id, page = 0, size = 50) {
    return api.get(`/visites/${id}/historique`, { params: { page, size } })
  },
}
