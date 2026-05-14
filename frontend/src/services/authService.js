import api from './api'

export default {
  // Inscription d'un nouvel utilisateur
  register(nomComplet, email, telephone, motDePasse) {
    return api.post('/auth/register', { nomComplet, email, telephone, motDePasse })
  },

  // Connexion et récupération du token JWT
  login(email, motDePasse) {
    return api.post('/auth/login', { email, motDePasse })
  },

  // Déconnexion
  logout() {
    return api.post('/auth/logout')
  },
}
