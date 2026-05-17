import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import router from '@/router'
import api from '@/services/api'

// Décode le payload d'un JWT sans librairie externe
function parseJwt(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(base64))
  } catch {
    return null
  }
}

/**
 * Vérifie qu'un token JWT n'est pas expiré.
 * Réduit la surface XSS en évitant d'utiliser un token périmé stocké en localStorage.
 */
function isTokenExpired(token) {
  const payload = parseJwt(token)
  if (!payload?.exp) return true
  return Date.now() >= payload.exp * 1000
}

/**
 * Valide que le rôle reçu fait partie des valeurs autorisées.
 * Empêche l'injection d'une valeur arbitraire depuis le JWT ou le localStorage.
 */
const ALLOWED_ROLES = ['CLIENT', 'ADMIN', 'SUPER_ADMIN']
function sanitizeRole(role) {
  return ALLOWED_ROLES.includes(role) ? role : 'CLIENT'
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  // Lire le token depuis localStorage uniquement s'il n'est pas expiré
  const storedToken = localStorage.getItem('token')
  const token = ref(storedToken && !isTokenExpired(storedToken) ? storedToken : null)

  // Lire le rôle en le sanitisant pour éviter toute valeur injectée
  const storedRole = localStorage.getItem('role')
  const role = ref(sanitizeRole(storedRole))

  // Nettoyer le localStorage si le token était expiré
  if (!token.value) {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
  }

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN' || role.value === 'SUPER_ADMIN')

  // Initialiser le header axios si token existant
  if (token.value) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
  }

  async function login(email, motDePasse) {
    const response = await api.post('/auth/login', { email, motDePasse })
    token.value = response.data.accessToken
    localStorage.setItem('token', token.value)
    api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`

    // Extraire le rôle depuis le JWT ou la réponse et le sanitiser
    const payload = parseJwt(token.value)
    const tokenRoles = Array.isArray(payload?.roles) ? payload.roles : []
    const responseRoles = Array.isArray(response.data.roles) ? response.data.roles : []
    const firstRole = tokenRoles.length ? tokenRoles[0] : responseRoles.length ? responseRoles[0] : null
    role.value = sanitizeRole(firstRole)
    await fetchProfile()

    // Redirection selon le rôle
    if (role.value === 'ADMIN') {
      router.push('/admin/dashboard')
    } else {
      router.push('/annonces')
    }
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignorer les erreurs serveur
    } finally {
      user.value = null
      token.value = null
      role.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      delete api.defaults.headers.common['Authorization']
      router.push('/connexion')
    }
  }

  async function fetchProfile() {
    try {
      const response = await api.get('/auth/profile')
      user.value = response.data
      // Synchroniser le rôle depuis le profil si disponible
      if (Array.isArray(response.data.roles) && response.data.roles.length > 0) {
        role.value = sanitizeRole(response.data.roles[0])
        localStorage.setItem('role', role.value)
      }
    } catch {
      await logout()
    }
  }

  async function init() {
    if (token.value) {
      await fetchProfile()
    }
  }

  return { user, token, role, isAuthenticated, isAdmin, login, logout, fetchProfile, init }
})
