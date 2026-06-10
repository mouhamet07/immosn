import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { notificationApiService } from '@/services/notificationApiService'
import { useToastStore } from '@/stores/toastStore'

const SESSION_KEY_SEEN = 'seenNotificationIds'
const SESSION_KEY_CURSOR = 'maxSeenNotificationId'

function loadSeenIds() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY_SEEN)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch {
    return new Set()
  }
}

function loadCursor() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY_CURSOR)
    return raw ? parseInt(raw, 10) : null
  } catch {
    return null
  }
}

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref([])
  const unreadCount   = ref(0)

  // Cursor pour le replay WebSocket — envoyé au serveur à chaque reconnexion
  const maxSeenId = ref(loadCursor())

  // Set des ids déjà vus — déduplication des toasts
  const seenIds = ref(loadSeenIds())

  const hasUnread = computed(() => unreadCount.value > 0)

  /**
   * Appelé par authStore à chaque notification WebSocket entrante.
   * Déduplication par notificationId → pas de toast doublon.
   */
  function handleIncoming(payload) {
    if (payload.notificationId != null) {
      if (seenIds.value.has(payload.notificationId)) return
      seenIds.value.add(payload.notificationId)
      _persistSeenIds()

      if (!maxSeenId.value || payload.notificationId > maxSeenId.value) {
        maxSeenId.value = payload.notificationId
        sessionStorage.setItem(SESSION_KEY_CURSOR, String(payload.notificationId))
      }
    }

    unreadCount.value++
    notifications.value.unshift({
      id:         payload.notificationId,
      type:       payload.type,
      title:      payload.title,
      message:    payload.message,
      entityId:   payload.entityId,
      entityType: payload.entityType,
      isRead:     false,
      createdAt:  payload.timestamp,
    })

    try {
      const toastStore = useToastStore()
      toastStore.info(payload.message ?? payload.title ?? 'Nouvelle notification')
    } catch {
      // toastStore peut ne pas être disponible hors composant
    }
  }

  /** Initialise le compteur non-lus depuis l'API (appelé au login et à l'init). */
  async function init() {
    try {
      const res = await notificationApiService.getUnreadCount()
      unreadCount.value = res.data.data ?? 0
    } catch {
      // non critique — le badge affichera 0
    }
  }

  /** Charge la liste complète des non-lus et synchronise seenIds. */
  async function fetchUnread() {
    try {
      const res = await notificationApiService.getUnread()
      const items = res.data.data ?? []
      notifications.value = items
      unreadCount.value   = items.filter(n => !n.isRead).length

      // Marque tous les ids chargés comme "vus" pour éviter les doublons de toasts
      items.forEach(n => {
        if (n.id != null) {
          seenIds.value.add(n.id)
          if (!maxSeenId.value || n.id > maxSeenId.value) {
            maxSeenId.value = n.id
          }
        }
      })
      _persistSeenIds()
      if (maxSeenId.value) {
        sessionStorage.setItem(SESSION_KEY_CURSOR, String(maxSeenId.value))
      }
    } catch {
      // silencieux
    }
  }

  async function markAsRead(id) {
    try {
      await notificationApiService.markAsRead(id)
      const notif = notifications.value.find(n => n.id === id)
      if (notif) notif.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // silencieux
    }
  }

  async function markAllAsRead() {
    try {
      await notificationApiService.markAllAsRead()
      notifications.value.forEach(n => (n.isRead = true))
      unreadCount.value = 0
    } catch {
      // silencieux
    }
  }

  function reset() {
    notifications.value = []
    unreadCount.value   = 0
    // On ne vide pas seenIds ni maxSeenId — ils survivent à la session pour la déduplication
  }

  function _persistSeenIds() {
    try {
      sessionStorage.setItem(SESSION_KEY_SEEN, JSON.stringify([...seenIds.value]))
    } catch {
      // quota sessionStorage atteint — silencieux
    }
  }

  return {
    notifications,
    unreadCount,
    maxSeenId,
    hasUnread,
    handleIncoming,
    init,
    fetchUnread,
    markAsRead,
    markAllAsRead,
    reset,
  }
})
