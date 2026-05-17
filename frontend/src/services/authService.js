import api from './api'
import router from '@/router'

export default {
  // Inscription d'un nouvel utilisateur
  register(nomComplet, email, telephone, motDePasse) {
    return api.post('/auth/register', { nomComplet, email, telephone, motDePasse })
  },

  // Connexion et récupération du token JWT
  login(email, motDePasse) {
    return api.post('/auth/login', { email, motDePasse })
  },

  // Inscription d'un administrateur
  registerAdmin(nomComplet, email, telephone, motDePasse) {
    return api.post('/auth/admin', { nomComplet, email, telephone, motDePasse })
  },

  // Déconnexion
  logout() {
    return api.post('/auth/logout')
      .then((res) => {
        localStorage.removeItem('token')
        router.push({ name: 'connexion' })
        return res
      })
  },
  // Récupérer le profil de l'utilisateur connecté
  getProfile() {
    return api.get('/auth/profile')
  },
  //Liste des admins
  getAdmins(page = 0, size = 10) {
    return api.get('/auth/admins', { params: { page, size } })
  },
}
