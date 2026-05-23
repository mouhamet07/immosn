import axios from 'axios'
import router from '@/router'

// Résolution d'URL : en production via Nginx le frontend et l'API sont sur le même domaine
const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ── Intercepteur requête ───────────────────────────────────
api.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── Intercepteur réponse ──────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status  = error.response?.status
    const message = error.response?.data?.message ?? error.message

    if (status === 401) {
      const isLoginRequest = error.config?.url?.includes('/auth/login')
      if (!isLoginRequest) {
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        delete api.defaults.headers.common['Authorization']
        router.push({ name: 'connexion' })
      }
    }

    // Enrichir l'erreur avec un message lisible
    error.userMessage = message || 'Une erreur est survenue. Veuillez réessayer.'
    return Promise.reject(error)
  },
)

export default api
