import api from './api'

export default {
  // Récupérer tous les types de bien actifs — GET /api/v1/types-bien
  // Réponse: RestResponse<List<TypeBienResponseDto>> → { id, libelle }
  getAllTypesBien() {
    return api.get('/types-bien')
  },

  // Récupérer les types de bien paginés — GET /api/v1/types-bien/paged
  getAllTypesBienPaged({ page = 0, size = 10 } = {}) {
    return api.get('/types-bien/paged', { params: { page, size } })
  },

  // Récupérer un type de bien par ID — GET /api/v1/types-bien/{id}
  getTypeBienById(id) {
    return api.get(`/types-bien/${id}`)
  },

  // Créer un type de bien (ADMIN) — POST /api/v1/types-bien
  // Champs: libelle (@NotBlank String)
  createTypeBien(libelle) {
    return api.post('/types-bien', { libelle })
  },

  // Modifier un type de bien (ADMIN) — PUT /api/v1/types-bien/{id}
  updateTypeBien(id, libelle) {
    return api.put(`/types-bien/${id}`, { libelle })
  },

  // Archiver un type de bien (ADMIN) — DELETE /api/v1/types-bien/{id}
  archiveTypeBien(id) {
    return api.delete(`/types-bien/${id}`)
  },
}
