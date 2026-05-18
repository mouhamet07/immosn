import api from './api'

export default {
  // POST /api/v1/visites — CLIENT : { annonceId, dateVisite, commentaire? }
  create(annonceId, dateVisite, commentaire) {
    return api.post('/visites', { annonceId, dateVisite, commentaire })
  },

  // GET /api/v1/visites/client?page&size&statut
  getClientVisites(page = 0, size = 10, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/visites/client', { params })
  },

  // GET /api/v1/visites/admin?page&size&statut
  getAllVisites(page = 0, size = 20, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/visites/admin', { params })
  },

  // PUT /api/v1/visites/{id}/status — { statut, commentaire? }
  updateStatut(id, statut, commentaire = null) {
    return api.put(`/visites/${id}/status`, { statut, commentaire })
  },

  // PUT /api/v1/visites/{id}/date — ADMIN : { dateVisite, commentaire? }
  updateDate(id, dateVisite, commentaire = null) {
    return api.put(`/visites/${id}/date`, { dateVisite, commentaire })
  },

  // DELETE /api/v1/visites/{id} — CLIENT : annuler
  annuler(id) {
    return api.delete(`/visites/${id}`)
  },
}
