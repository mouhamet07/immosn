import api from './api'

export default {
  // POST /api/v1/favoris/{annonceId}/toggle — CLIENT : toggle favori
  toggle(annonceId) {
    return api.post(`/favoris/${annonceId}/toggle`)
  },

  // GET /api/v1/favoris?page&size — liste paginée des favoris avec détails (FavorisView)
  getClientFavoris(page = 0, size = 12) {
    return api.get('/favoris', { params: { page, size } })
  },

  // GET /api/v1/favoris/ids — tous les IDs favoris du client (sans limite fixe)
  // Utilisé par favorisStore pour charger l'état ❤️ au démarrage
  getFavorisIds() {
    return api.get('/favoris/ids')
  },

  // GET /api/v1/favoris/{annonceId}/check — vérifier si une annonce est favorite
  checkFavoris(annonceId) {
    return api.get(`/favoris/${annonceId}/check`)
  },
}
