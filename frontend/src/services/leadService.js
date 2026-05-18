import api from './api'

export default {
  // POST /api/v1/leads — ADMIN : { clientId, annonceId, visiteId?, noteAdmin? }
  create(data) {
    return api.post('/leads', data)
  },

  // GET /api/v1/leads?page&size&statut
  getAll(page = 0, size = 20, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/leads', { params })
  },

  // GET /api/v1/leads/{id}
  getById(id) {
    return api.get(`/leads/${id}`)
  },

  // PUT /api/v1/leads/{id}/status — { statut, noteAdmin? }
  updateStatut(id, statut, noteAdmin = null) {
    return api.put(`/leads/${id}/status`, { statut, noteAdmin })
  },
}
