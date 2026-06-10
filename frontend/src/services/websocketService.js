import { Client } from '@stomp/stompjs'

/**
 * URL du endpoint WebSocket.
 * En développement : ws://localhost:8080/ws
 * En production    : le même domaine que la page (nginx proxy /ws → backend:8080/ws)
 */
function buildWsUrl() {
  const env = import.meta.env.VITE_WS_URL
  if (env) return env
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

// Map subscription-id → { unsubscribe fn }
const subscriptions = new Map()

let client = null
let reconnectAttempts = 0
const MAX_RECONNECT = 10
const BASE_DELAY_MS = 3000

/**
 * Ouvre la connexion WebSocket STOMP avec le JWT fourni.
 *
 * Le token est envoyé dans les headers du frame STOMP CONNECT
 * (Authorization: Bearer <token>) — jamais dans l'URL.
 *
 * @param {string}   token           JWT de l'utilisateur connecté
 * @param {boolean}  isAdmin         true si l'utilisateur est ADMIN/SUPER_ADMIN
 * @param {Function} onNotification  callback(NotificationPayload) appelé à chaque notification
 */
function connect(token, isAdmin, onNotification) {
  if (client && client.connected) return

  client = new Client({
    brokerURL: buildWsUrl(),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 0, // géré manuellement avec backoff exponentiel

    onConnect() {
      reconnectAttempts = 0
      console.debug('[WS] Connecté')

      // Toujours s'abonner aux notifications personnelles
      _subscribe('/user/queue/notifications', onNotification)

      // Abonnement supplémentaire pour les admins
      if (isAdmin) {
        _subscribe('/topic/admin.notifications', onNotification)
      }
    },

    onDisconnect() {
      console.debug('[WS] Déconnecté')
    },

    onStompError(frame) {
      console.error('[WS] Erreur STOMP :', frame.headers['message'])
      _scheduleReconnect(token, isAdmin, onNotification)
    },

    onWebSocketError(event) {
      console.error('[WS] Erreur WebSocket')
      _scheduleReconnect(token, isAdmin, onNotification)
    },

    onWebSocketClose() {
      console.debug('[WS] Connexion fermée')
      _scheduleReconnect(token, isAdmin, onNotification)
    },
  })

  client.activate()
}

/**
 * Ferme proprement la connexion et nettoie toutes les souscriptions.
 */
function disconnect() {
  subscriptions.forEach((sub) => sub.unsubscribe())
  subscriptions.clear()

  if (client) {
    client.deactivate()
    client = null
  }
  reconnectAttempts = 0
  console.debug('[WS] Déconnexion propre')
}

// Abonnement interne avec registre pour nettoyage

function _subscribe(destination, callback) {
  if (!client || !client.connected) return
  if (subscriptions.has(destination)) return // évite les doublons

  const sub = client.subscribe(destination, (message) => {
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

// Reconnexion avec backoff exponentiel plafonné à 30s

function _scheduleReconnect(token, isAdmin, onNotification) {
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
    connect(token, isAdmin, onNotification)
  }, delay)
}

export const websocketService = { connect, disconnect }
