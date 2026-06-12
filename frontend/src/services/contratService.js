import api from './api'

export default {
  // POST /api/v1/contrats — ADMIN
  create(data) {
    return api.post('/contrats', data)
  },

  // GET /api/v1/contrats/client?page&size&statut
  getClientContrats(page = 0, size = 10, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/contrats/client', { params })
  },

  // GET /api/v1/contrats/admin?page&size&statut
  getAllContrats(page = 0, size = 20, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/contrats/admin', { params })
  },

  // GET /api/v1/contrats/{id}
  getById(id) {
    return api.get(`/contrats/${id}`)
  },

  // PUT /api/v1/contrats/{id} — ADMIN
  update(id, data) {
    return api.put(`/contrats/${id}`, data)
  },

  // POST /api/v1/contrats/{id}/document — ADMIN (multipart)
  uploadDocument(id, file) {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/contrats/${id}/document`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // PUT /api/v1/contrats/{id}/resiliation — CLIENT
  demanderResiliation(id, motif) {
    return api.put(`/contrats/${id}/resiliation`, { motif })
  },

  // PUT /api/v1/contrats/{id}/prolongation — CLIENT
  demanderProlongation(id, nouvelleDate, motif) {
    return api.put(`/contrats/${id}/prolongation`, { nouvelleDate, motif })
  },

  // PUT /api/v1/contrats/{id}/resiliation/accepter — ADMIN
  accepterResiliation(id, motif = null) {
    return api.put(`/contrats/${id}/resiliation/accepter`, motif ? { motif } : {})
  },

  // PUT /api/v1/contrats/{id}/resiliation/refuser — ADMIN
  refuserResiliation(id, motif = null) {
    return api.put(`/contrats/${id}/resiliation/refuser`, motif ? { motif } : {})
  },

  // PUT /api/v1/contrats/{id}/prolongation/accepter — ADMIN
  // nouvelleDate : nouvelle date de fin (LocalDate ISO)
  accepterProlongation(id, nouvelleDate = null, motif = null) {
    return api.put(`/contrats/${id}/prolongation/accepter`, { nouvelleDate, motif })
  },

  // PUT /api/v1/contrats/{id}/prolongation/refuser — ADMIN
  refuserProlongation(id, motif = null) {
    return api.put(`/contrats/${id}/prolongation/refuser`, motif ? { motif } : {})
  },
}
