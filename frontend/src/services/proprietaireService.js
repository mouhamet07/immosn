import api from './api'

export default {
  // GET /api/v1/proprietaires?page&size
  getAll(page = 0, size = 100) {
    return api.get('/proprietaires', { params: { page, size } })
  },

  // GET /api/v1/proprietaires/{id} — profil + statistiques
  getById(id) {
    return api.get(`/proprietaires/${id}`)
  },

  // GET /api/v1/proprietaires/{id}/stats
  getStats(id) {
    return api.get(`/proprietaires/${id}/stats`)
  },

  // GET /api/v1/proprietaires/{id}/biens?page&size
  getBiens(id, page = 0, size = 20) {
    return api.get(`/proprietaires/${id}/biens`, { params: { page, size } })
  },

  // POST /api/v1/proprietaires — { nomComplet, telephone, email, adresse, notes }
  create(data) {
    return api.post('/proprietaires', data)
  },

  // PUT /api/v1/proprietaires/{id}
  update(id, data) {
    return api.put(`/proprietaires/${id}`, data)
  },
}
