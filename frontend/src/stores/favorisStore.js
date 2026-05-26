import { ref } from 'vue'
import { defineStore } from 'pinia'
import favorisService from '@/services/favorisService'
import { useAuthStore } from './authStore'

/**
 * Store Pinia des favoris.
 *
 * Maintient un Set<Long> des IDs d'annonces favorites du client connecté.
 * Les IDs sont chargés via GET /api/v1/favoris/ids (sans limite fixe),
 * ce qui permet à AnnonceCard d'afficher l'état ❤️ sans appel réseau par carte.
 */
export const useFavorisStore = defineStore('favoris', () => {
  const favorisIds = ref(new Set())
  const loaded     = ref(false)

  /** Charge tous les IDs favoris depuis le backend (sans limite hardcodée) */
  async function loadFavoris() {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated || authStore.role !== 'CLIENT') {
      favorisIds.value = new Set()
      loaded.value = true
      return
    }

    try {
      // GET /api/v1/favoris/ids → RestResponse<List<Long>>
      const res = await favorisService.getFavorisIds()
      favorisIds.value = new Set(res.data.data ?? [])
    } catch {
      favorisIds.value = new Set()
    } finally {
      loaded.value = true
    }
  }

  /** Toggle favori avec mise à jour optimiste + rollback sur erreur */
  async function toggle(annonceId) {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated) return false

    const numId = Number(annonceId)
    const was   = favorisIds.value.has(numId)

    // Optimistic update
    if (was) favorisIds.value.delete(numId)
    else      favorisIds.value.add(numId)

    try {
      const res  = await favorisService.toggle(numId)
      const isFav = res.data.data?.isFavoris ?? !was
      // Synchroniser avec la réponse serveur
      if (isFav) favorisIds.value.add(numId)
      else       favorisIds.value.delete(numId)
      return isFav
    } catch {
      // Rollback si erreur réseau
      if (was) favorisIds.value.add(numId)
      else     favorisIds.value.delete(numId)
      return was
    }
  }

  function isFavori(annonceId) {
    return favorisIds.value.has(Number(annonceId))
  }

  function reset() {
    favorisIds.value = new Set()
    loaded.value     = false
  }

  return { favorisIds, loaded, loadFavoris, toggle, isFavori, reset }
})
