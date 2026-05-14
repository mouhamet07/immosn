import api from './api'

export default {
  // Récupérer toutes les commodités actives — GET /api/v1/commodites
  // Réponse: RestResponse<List<CommoditeResponseDto>> → { id, libelle }
  getAllCommodites() {
    return api.get('/commodites')
  },

  // Récupérer les commodités paginées — GET /api/v1/commodites/paged
  getAllCommoditesPaged({ page = 0, size = 10 } = {}) {
    return api.get('/commodites/paged', { params: { page, size } })
  },

  // Récupérer une commodité par ID — GET /api/v1/commodites/{id}
  getCommoditeById(id) {
    return api.get(`/commodites/${id}`)
  },

  // Créer une commodité (ADMIN) — POST /api/v1/commodites
  // Champs: libelle (@NotBlank String)
  createCommodite(libelle) {
    return api.post('/commodites', { libelle })
  },

  // Modifier une commodité (ADMIN) — PUT /api/v1/commodites/{id}
  updateCommodite(id, libelle) {
    return api.put(`/commodites/${id}`, { libelle })
  },

  // Archiver une commodité (ADMIN) — DELETE /api/v1/commodites/{id}
  archiveCommodite(id) {
    return api.delete(`/commodites/${id}`)
  },
}
