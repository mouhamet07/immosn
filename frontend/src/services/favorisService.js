import api from './api'

export default {
  // POST /api/v1/favoris/{annonceId}/toggle — CLIENT : toggle favori
  toggle(annonceId) {
    return api.post(`/favoris/${annonceId}/toggle`)
  },

  // GET /api/v1/favoris?page&size — liste des favoris du client connecté
  getClientFavoris(page = 0, size = 12) {
    return api.get('/favoris', { params: { page, size } })
  },

  // GET /api/v1/favoris/{annonceId}/check — vérifier si une annonce est favorite
  checkFavoris(annonceId) {
    return api.get(`/favoris/${annonceId}/check`)
  },
}
