import api from './api'

export default {
  // POST /api/v1/visites — CLIENT : { annonceId, dateVisite, commentaire? }
  create(annonceId, dateVisite, commentaire) {
    return api.post('/visites', { annonceId, dateVisite, commentaire })
  },

  // POST /api/v1/visites/invite — PUBLIC : demande de visite d'un visiteur non authentifié
  // payload : { annonceId, nom, prenom?, telephone, email, adresse?, dateVisite, heureVisite?, commentaire? }
  createInvite(payload) {
    return api.post('/visites/invite', payload)
  },

  // GET /api/v1/visites/suivi/{token} — PUBLIC : suivi d'une visite par numéro/token de prospect
  suivreParToken(token) {
    return api.get(`/visites/suivi/${token}`)
  },

  // GET /api/v1/visites/client?page&size&statut
  getClientVisites(page = 0, size = 10, statut = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    return api.get('/visites/client', { params })
  },

  // GET /api/v1/visites/admin?page&size&statut&typeTransaction
  getAllVisites(page = 0, size = 20, statut = null, typeTransaction = null) {
    const params = { page, size }
    if (statut) params.statut = statut
    if (typeTransaction) params.typeTransaction = typeTransaction
    return api.get('/visites/admin', { params })
  },

  // GET /api/v1/visites/{id}
  getById(id) {
    return api.get(`/visites/${id}`)
  },

  // PUT /api/v1/visites/{id}/status — { statut, commentaire? }
  updateStatut(id, statut, commentaire = null) {
    return api.put(`/visites/${id}/status`, { statut, commentaire })
  },

  // PUT /api/v1/visites/{id}/date — ADMIN : { dateVisite, commentaire? }
  updateDate(id, dateVisite, commentaire = null) {
    return api.put(`/visites/${id}/date`, { dateVisite, commentaire })
  },

  // PUT /api/v1/visites/{id}/modifier — CLIENT : modifier date/commentaire (EN_ATTENTE uniquement)
  modifierVisite(id, dateVisite, commentaire = null) {
    return api.put(`/visites/${id}/modifier`, { dateVisite, commentaire })
  },

  // DELETE /api/v1/visites/{id} — CLIENT : annuler
  annuler(id) {
    return api.delete(`/visites/${id}`)
  },

  // PUT /api/v1/visites/{id}/cloture — ADMIN
  // type: 'SANS_SUITE' | 'AVEC_CONTRAT'
  // typeContrat: 'VENTE' | 'LOCATION' (requis si AVEC_CONTRAT)
  // dureeLocationMois: number (requis si LOCATION)
  cloturerVisite(id, { type, typeContrat = null, dureeLocationMois = null }) {
    return api.put(`/visites/${id}/cloture`, { type, typeContrat, dureeLocationMois })
  },

  // ─── Sprint 2 : processus commercial ───

  // PUT /api/v1/visites/{id}/affecter — SUPER_ADMIN : { adminId, commentaire? }
  affecter(id, adminId, commentaire = null) {
    return api.put(`/visites/${id}/affecter`, { adminId, commentaire })
  },

  // PUT /api/v1/visites/{id}/replanification — SUPER_ADMIN : { nouvelleDate, commentaire? }
  demanderReplanification(id, nouvelleDate, commentaire = null) {
    return api.put(`/visites/${id}/replanification`, { nouvelleDate, commentaire })
  },

  // PUT /api/v1/visites/{id}/replanification/accepter — SUPER_ADMIN
  accepterReplanification(id) {
    return api.put(`/visites/${id}/replanification/accepter`)
  },

  // POST /api/v1/visites/{id}/rapport — ADMIN/SUPER_ADMIN : { compteRendu, aboutie }
  creerRapport(id, { compteRendu, aboutie }) {
    return api.post(`/visites/${id}/rapport`, { compteRendu, aboutie })
  },

  // GET /api/v1/visites/{id}/rapport — ADMIN/SUPER_ADMIN
  getRapport(id) {
    return api.get(`/visites/${id}/rapport`)
  },

  // POST /api/v1/visites/{id}/rapport/document — ADMIN/SUPER_ADMIN : multipart
  uploadRapportDocument(id, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/visites/${id}/rapport/document`, formData)
  },
}
