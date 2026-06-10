import { Client } from '@stomp/stompjs'

/**
 * URL du endpoint WebSocket.
 * En développement : ws://localhost:8080/ws  (VITE_WS_URL dans .env.local)
 * En production    : même domaine que la page (nginx proxy /ws → backend:8080/ws)
 */
function buildWsUrl() {
  const env = import.meta.env.VITE_WS_URL
  if (env) return env
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

const subscriptions = new Map()

let client            = null
let reconnectAttempts = 0
const MAX_RECONNECT   = 10
const BASE_DELAY_MS   = 3000

/**
 * Ouvre la connexion WebSocket STOMP.
 *
 * @param {string}      token                  JWT de l'utilisateur connecté
 * @param {boolean}     isAdmin                true si ADMIN ou SUPER_ADMIN
 * @param {Function}    onNotification         callback(NotificationPayload)
 * @param {number|null} lastSeenNotificationId cursor pour le replay — envoyé au serveur au CONNECT
 */
function connect(token, isAdmin, onNotification, lastSeenNotificationId = null) {
  if (client && client.connected) return

  const connectHeaders = { Authorization: `Bearer ${token}` }
  if (lastSeenNotificationId != null) {
    connectHeaders.lastSeenNotificationId = String(lastSeenNotificationId)
  }

  client = new Client({
    brokerURL: buildWsUrl(),
    connectHeaders,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 0, // géré manuellement avec backoff exponentiel

    onConnect() {
      reconnectAttempts = 0
      console.debug('[WS] Connecté')

      _subscribe('/user/queue/notifications', onNotification)
      if (isAdmin) {
        _subscribe('/topic/admin.notifications', onNotification)
      }
    },

    onDisconnect() {
      console.debug('[WS] Déconnecté')
    },

    onStompError(frame) {
      console.error('[WS] Erreur STOMP :', frame.headers['message'])
      _scheduleReconnect(token, isAdmin, onNotification, lastSeenNotificationId)
    },

    onWebSocketError() {
      console.error('[WS] Erreur WebSocket')
      _scheduleReconnect(token, isAdmin, onNotification, lastSeenNotificationId)
    },

    onWebSocketClose() {
      console.debug('[WS] Connexion fermée')
      _scheduleReconnect(token, isAdmin, onNotification, lastSeenNotificationId)
    },
  })

  client.activate()
}

/**
 * Ferme proprement la connexion et nettoie les souscriptions.
 */
function disconnect() {
  subscriptions.forEach(sub => sub.unsubscribe())
  subscriptions.clear()

  if (client) {
    client.deactivate()
    client = null
  }
  reconnectAttempts = 0
  console.debug('[WS] Déconnexion propre')
}

function _subscribe(destination, callback) {
  if (!client || !client.connected) return
  if (subscriptions.has(destination)) return

  const sub = client.subscribe(destination, message => {
    try {
      const payload = JSON.parse(message.body)
      callback(payload)
    } catch (e) {
      console.error('[WS] Payload JSON invalide', e)
    }
  })

  subscriptions.set(destination, sub)
  console.debug('[WS] Abonné à', destination)
}

function _scheduleReconnect(token, isAdmin, onNotification, lastSeenNotificationId) {
  if (reconnectAttempts >= MAX_RECONNECT) {
    console.warn('[WS] Nombre maximum de reconnexions atteint — abandon')
    return
  }

  const delay = Math.min(BASE_DELAY_MS * 2 ** reconnectAttempts, 30000)
  reconnectAttempts++
  console.debug(`[WS] Reconnexion dans ${delay}ms (tentative ${reconnectAttempts}/${MAX_RECONNECT})`)

  setTimeout(() => {
    if (client) {
      client.deactivate()
      client = null
    }
    connect(token, isAdmin, onNotification, lastSeenNotificationId)
  }, delay)
}

export const websocketService = { connect, disconnect }
