import api from './api'

export default {
  // Récupérer toutes les annonces actives (clients) — GET /api/v1/annonces
  // Réponse: PagedResponse<AnnonceListDto> → { data, totalElements, totalPages, currentPage, pageSize, isFirst, isLast }
  getAllAnnonces({ page = 0, size, sort = 'createdAt', direction = 'DESC' } = {}) {
    return api.get('/annonces', { params: { page, size, sort, direction } })
  },

  // Récupérer toutes les annonces admin — GET /api/v1/annonces/admin
  getAllAnnoncesAdmin({ page = 0, size, sort = 'createdAt', direction = 'DESC' } = {}) {
    return api.get('/annonces/admin', { params: { page, size, sort, direction } })
  },

  // Récupérer le détail d'une annonce — GET /api/v1/annonces/{id}
  getAnnonceById(id) {
    return api.get(`/annonces/${id}`)
  },

  // Créer une annonce (ADMIN) — POST /api/v1/annonces
  createAnnonce(data) {
    return api.post('/annonces', data)
  },

  // Modifier une annonce (ADMIN) — PUT /api/v1/annonces/{id}
  updateAnnonce(id, data) {
    return api.put(`/annonces/${id}`, data)
  },

  // Archiver une annonce (ADMIN) — DELETE /api/v1/annonces/{id}
  archiveAnnonce(id) {
    return api.delete(`/annonces/${id}`)
  },

  // Restaurer une annonce archivée (ADMIN) — PATCH /api/v1/annonces/{id}/restore
  restoreAnnonce(id) {
    return api.patch(`/annonces/${id}/restore`)
  },
}
