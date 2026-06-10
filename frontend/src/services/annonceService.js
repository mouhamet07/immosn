import api from './api'

function validateId(id) {
  const parsed = Number(id)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`ID invalide : "${id}"`)
  }
  return parsed
}

export default {
  // GET /api/v1/annonces — liste paginée des annonces actives (client)
  getAllAnnonces(filters = {}) {
    const params = {}
    if (filters.page !== undefined) params.page = filters.page
    if (filters.size !== undefined) params.size = filters.size
    if (filters.typeBienId) params.typeBienId = filters.typeBienId
    if (filters.adresse) params.adresse = filters.adresse
    if (filters.prixMin) params.prixMin = filters.prixMin
    if (filters.prixMax) params.prixMax = filters.prixMax
    return api.get('/annonces', { params })
  },

  // GET /api/v1/annonces/admin — liste complète admin (inclut archivées)
  getAllAnnoncesAdmin(filters = {}) {
    return api.get('/annonces/admin', { params: filters })
  },

  // GET /api/v1/annonces/{id} — public, archived annonces return 404
  getAnnonceById(id) {
    return api.get(`/annonces/${validateId(id)}`)
  },

  // GET /api/v1/annonces/admin/{id} — admin only, includes archived annonces
  getAnnonceByIdAdmin(id) {
    return api.get(`/annonces/admin/${validateId(id)}`)
  },

  // POST /api/v1/annonces — champs exacts: libelle, description, nbrPieces, surface, prix, adresse, typeBienId, commoditeIds, images
  createAnnonce(data) {
    return api.post('/annonces', data)
  },

  // PUT /api/v1/annonces/{id}
  updateAnnonce(id, data) {
    return api.put(`/annonces/${validateId(id)}`, data)
  },

  // DELETE /api/v1/annonces/{id} — soft delete (archived = true)
  archiveAnnonce(id) {
    return api.delete(`/annonces/${validateId(id)}`)
  },

  // PATCH /api/v1/annonces/{id}/restore
  restoreAnnonce(id) {
    return api.patch(`/annonces/${validateId(id)}/restore`)
  },

  // POST /api/v1/annonces/search — recherche avancée avec filtres combinables (Sprint 2)
  // Body: { typeBienId, prixMin, prixMax, adresse, nbrPieces, commoditeIds[], page, size, sortBy, sortDir }
  searchAnnonces(filters = {}) {
    return api.post('/annonces/search', filters)
  },
}
