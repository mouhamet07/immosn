import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import router from '@/router'
import api from '@/services/api'
import authService from '@/services/authService'
import { websocketService } from '@/services/websocketService'
import { useToastStore } from '@/stores/toastStore'

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
 * Valide que le rôle reçu fait partie des valeurs autorisées (RoleType enum backend).
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

  function _handleNotification(payload) {
    try {
      const toast = useToastStore()
      toast.info(payload.message ?? payload.title ?? 'Nouvelle notification')
    } catch {
      // toastStore peut ne pas être disponible hors composant — silencieux
    }
  }

  async function login(email, motDePasse) {
    // POST /api/v1/auth/login via authService — Réponse: RestResponse<AuthResponseDto> → response.data.data
    const response = await authService.login(email, motDePasse)
    const data = response.data.data
    // Champ exact du backend: accessToken (AuthResponseDto)
    token.value = data.accessToken
    localStorage.setItem('token', token.value)
    api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
    // roles est un Set<String> sérialisé en array JSON — valeurs: ADMIN, CLIENT, EMPLOYE
    const roles = Array.isArray(data.roles) ? data.roles : []
    role.value = sanitizeRole(roles.length ? roles[0] : null)
    localStorage.setItem('role', role.value)
    user.value = data

    // Connexion WebSocket après authentification réussie
    websocketService.connect(token.value, isAdmin.value, _handleNotification)

    // Redirection : page demandée avant la connexion en priorité, sinon selon le rôle
    const redirect = localStorage.getItem('redirectAfterLogin')
    if (redirect) {
      localStorage.removeItem('redirectAfterLogin')
      router.push(redirect)
    } else if (role.value === 'ADMIN' || role.value === 'SUPER_ADMIN') {
      router.push('/admin/dashboard')
    } else {
      router.push('/annonces')
    }
  }

  async function logout() {
    try {
      await authService.logout()
    } catch {
      // Ignorer les erreurs serveur
    } finally {
      websocketService.disconnect()
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
      // Réponse: RestResponse<AuthResponseDto> → { data: { id, nomComplet, email, telephone, roles, accessToken, ... } }
      const response = await authService.getProfile()
      user.value = response.data.data
      // roles est un Set<String> côté backend, sérialisé en array JSON
      const roles = Array.isArray(response.data.data?.roles) ? response.data.data.roles : []
      if (roles.length > 0) {
        role.value = sanitizeRole(roles[0])
        localStorage.setItem('role', role.value)
      }
    } catch {
      await logout()
    }
  }

  async function init() {
    if (token.value) {
      await fetchProfile()
      // Rétablir la connexion WebSocket après un refresh de page
      if (token.value) {
        websocketService.connect(token.value, isAdmin.value, _handleNotification)
      }
    }
  }

  return { user, token, role, isAuthenticated, isAdmin, login, logout, fetchProfile, init }
})
