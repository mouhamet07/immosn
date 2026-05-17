import api from './api'

export default {
  // POST /api/v1/auth/register — champs: nomComplet, email, telephone, motDePasse
  register(nomComplet, email, telephone, motDePasse) {
    return api.post('/auth/register', { nomComplet, email, telephone, motDePasse })
  },

  // POST /api/v1/auth/login — champs: email, motDePasse
  login(email, motDePasse) {
    return api.post('/auth/login', { email, motDePasse })
  },

  // POST /api/v1/auth/logout — nécessite header Authorization: Bearer {token}
  logout() {
    return api.post('/auth/logout')
  },

  // GET /api/v1/auth/profile — profil de l'utilisateur connecté
  getProfile() {
    return api.get('/auth/profile')
  },
}
