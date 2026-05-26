import { ref } from 'vue'
import { defineStore } from 'pinia'

let nextId = 0

/**
 * Store centralisé pour les toasts/notifications.
 * Usage : const toast = useToastStore()
 *         toast.success('Annonce publiée !')
 *         toast.error('Erreur de connexion')
 */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  function add(message, type = 'info', duration = 4000) {
    const id = ++nextId
    toasts.value.push({ id, message, type })
    if (duration > 0) {
      setTimeout(() => remove(id), duration)
    }
    return id
  }

  function remove(id) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  const success = (msg, d)  => add(msg, 'success', d)
  const error   = (msg, d)  => add(msg, 'error',   d)
  const warning = (msg, d)  => add(msg, 'warning',  d)
  const info    = (msg, d)  => add(msg, 'info',     d)

  return { toasts, add, remove, success, error, warning, info }
})
