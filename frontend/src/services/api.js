import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Intercepteur requête : ajouter le token Bearer et gérer FormData
api.interceptors.request.use((config) => {
  // Si c'est FormData, supprimer Content-Type pour que le navigateur le définisse correctement
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }

  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// Intercepteur réponse : gérer les erreurs 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push({ name: 'connexion' })
    }
    return Promise.reject(error)
  },
)

export default api
