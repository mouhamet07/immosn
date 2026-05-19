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

  // POST /api/v1/auth/admin — créer un admin (SUPER_ADMIN uniquement)
  // Champs: nomComplet, email, telephone, motDePasse
  createAdmin({ nomComplet, email, telephone, motDePasse }) {
    return api.post('/auth/admin', { nomComplet, email, telephone, motDePasse })
  },

  // GET /api/v1/auth/admins — liste paginée des admins (SUPER_ADMIN uniquement)
  getAdmins(page = 0, size = 20) {
    return api.get('/auth/admins', { params: { page, size } })
  },

  // PATCH /api/v1/auth/admins/{id}/archive — archiver un admin (SUPER_ADMIN uniquement)
  archiveAdmin(id) {
    return api.patch(`/auth/admins/${id}/archive`)
  },
}
