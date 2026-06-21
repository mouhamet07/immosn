import api from './api'

export default {
  // Liste paginée des propriétaires — GET /api/v1/proprietaires
  // actifsUniquement=true : utilisé par le select annonce (exclut les archivés)
  getAll({ page = 0, size = 20, actifsUniquement = false } = {}) {
    return api.get('/proprietaires', { params: { page, size, actifsUniquement } })
  },

  // Détail d'un propriétaire (profil + stats) — GET /api/v1/proprietaires/{id}
  getById(id) {
    return api.get(`/proprietaires/${id}`)
  },

  // Statistiques seules — GET /api/v1/proprietaires/{id}/stats
  getStats(id) {
    return api.get(`/proprietaires/${id}/stats`)
  },

  // Biens associés au propriétaire — GET /api/v1/proprietaires/{id}/biens
  getBiens(id, { page = 0, size = 20 } = {}) {
    return api.get(`/proprietaires/${id}/biens`, { params: { page, size } })
  },

  // Créer un propriétaire — POST /api/v1/proprietaires
  // Champs : nomComplet, telephone, email, adresse, notes
  create(data) {
    return api.post('/proprietaires', data)
  },

  // Modifier un propriétaire — PUT /api/v1/proprietaires/{id}
  update(id, data) {
    return api.put(`/proprietaires/${id}`, data)
  },

  // Archiver un propriétaire (soft delete) — PATCH /api/v1/proprietaires/{id}/archive
  archive(id) {
    return api.patch(`/proprietaires/${id}/archive`)
  },
}
