import api from './api'

export default {
  // POST /api/v1/signalements — CLIENT : { contratId, contenu }
  create(contratId, contenu) {
    return api.post('/signalements', { contratId, contenu })
  },

  // GET /api/v1/signalements/client?page&size&statut
  getClientSignalements(page = 0, size = 10, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/signalements/client', { params })
  },

  // GET /api/v1/signalements/admin?page&size&statut
  getAll(page = 0, size = 20, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/signalements/admin', { params })
  },

  // GET /api/v1/signalements/{id} — ADMIN
  getById(id) {
    return api.get(`/signalements/${id}`)
  },

  // PUT /api/v1/signalements/{id}/status — ADMIN : { statut, reponseAdmin? }
  updateStatut(id, statut, reponseAdmin = null) {
    return api.put(`/signalements/${id}/status`, { statut, reponseAdmin })
  },

  // PUT /api/v1/signalements/{id}/read — ADMIN
  markAsRead(id) {
    return api.put(`/signalements/${id}/read`)
  },
}
