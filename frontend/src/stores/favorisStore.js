import { ref } from 'vue'
import { defineStore } from 'pinia'
import favorisService from '@/services/favorisService'
import { useAuthStore } from './authStore'

/**
 * Store Pinia des favoris.
 * Maintient un Set<Long> des IDs d'annonces favorites du client connecté.
 * Permet à AnnonceCard et DetailAnnonceView de connaître l'état favori sans appel réseau supplémentaire.
 */
export const useFavorisStore = defineStore('favoris', () => {
  // Set des IDs d'annonces favorites (Long)
  const favorisIds = ref(new Set())
  const loaded = ref(false)

  /** Charger les IDs favoris depuis l'API (première page — 100 max) */
  async function loadFavoris() {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated || authStore.role !== 'CLIENT') {
      favorisIds.value = new Set()
      loaded.value = true
      return
    }
    try {
      const res = await favorisService.getClientFavoris(0, 100)
      const ids = (res.data.data ?? []).map(f => f.annonceId)
      favorisIds.value = new Set(ids)
    } catch {
      favorisIds.value = new Set()
    } finally {
      loaded.value = true
    }
  }

  /** Toggle favori — optimistic update */
  async function toggle(annonceId) {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated) return false

    const was = favorisIds.value.has(annonceId)
    // Mise à jour optimiste
    if (was) favorisIds.value.delete(annonceId)
    else      favorisIds.value.add(annonceId)

    try {
      const res = await favorisService.toggle(annonceId)
      const isFav = res.data.data?.isFavoris ?? !was
      if (isFav) favorisIds.value.add(annonceId)
      else       favorisIds.value.delete(annonceId)
      return isFav
    } catch {
      // Rollback si erreur
      if (was) favorisIds.value.add(annonceId)
      else     favorisIds.value.delete(annonceId)
      return was
    }
  }

  function isFavori(annonceId) {
    return favorisIds.value.has(Number(annonceId))
  }

  function reset() {
    favorisIds.value = new Set()
    loaded.value = false
  }

  return { favorisIds, loaded, loadFavoris, toggle, isFavori, reset }
})
