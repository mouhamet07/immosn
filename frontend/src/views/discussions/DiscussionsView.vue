<script setup>
import { ref, onMounted, nextTick } from 'vue'
import discussionService from '@/services/discussionService'

const discussions   = ref([])
const loading       = ref(false)
const error         = ref('')
const selectedId    = ref(null)
const selectedChat  = ref(null)
const chatLoading   = ref(false)
const newMessage    = ref('')
const sending       = ref(false)
const messagesRef   = ref(null)

// ── Pagination liste ──────────────────────────────────────
const currentPage = ref(0)
const totalPages  = ref(1)

async function fetchDiscussions(page = 0) {
  loading.value = true
  error.value   = ''
  try {
    const res = await discussionService.getClientDiscussions(page, 10)
    const paged = res.data
    discussions.value = paged.data
    currentPage.value = paged.currentPage
    totalPages.value  = paged.totalPages
  } catch {
    error.value = 'Impossible de charger vos discussions.'
  } finally {
    loading.value = false
  }
}

async function openDiscussion(id) {
  selectedId.value = id
  chatLoading.value = true
  try {
    const res = await discussionService.getMessages(id)
    selectedChat.value = res.data.data
    // Mettre à jour le compteur non lus dans la liste
    const disc = discussions.value.find(d => d.id === id)
    if (disc) disc.unreadCount = 0
    await nextTick()
    scrollBottom()
  } catch {
    selectedChat.value = null
  } finally {
    chatLoading.value = false
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || sending.value) return
  sending.value = true
  try {
    const res = await discussionService.sendMessage(selectedId.value, newMessage.value.trim())
    selectedChat.value.messages.push(res.data.data)
    newMessage.value = ''
    await nextTick()
    scrollBottom()
  } catch {
    // silencieux
  } finally {
    sending.value = false
  }
}

function scrollBottom() {
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
}
function formatTime(dt) {
  return new Date(dt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}
function truncate(str, n = 50) {
  return str && str.length > n ? str.slice(0, n) + '…' : str
}

onMounted(() => fetchDiscussions(0))
</script>

<template>
  <div class="disc-page">
    <div class="disc-layout">

      <!-- ── Liste des discussions ──────────────────────── -->
      <aside class="disc-list">
        <div class="disc-list__header">
          <h1 class="disc-list__title">Mes discussions</h1>
        </div>

        <div v-if="loading" class="disc-list__loading">
          <div class="spinner"></div>
        </div>

        <div v-else-if="error" class="disc-list__error">{{ error }}</div>

        <div v-else-if="!discussions.length" class="disc-list__empty">
          <p>Aucune discussion pour l'instant.</p>
          <RouterLink to="/annonces" class="disc-list__link">Parcourir les annonces →</RouterLink>
        </div>

        <ul v-else class="disc-list__items">
          <li
            v-for="d in discussions"
            :key="d.id"
            class="disc-item"
            :class="{ '--active': selectedId === d.id }"
            @click="openDiscussion(d.id)"
          >
            <div class="disc-item__thumb">
              <img v-if="d.imagePrincipale" :src="d.imagePrincipale" :alt="d.annonceLibelle" />
              <span v-else class="disc-item__thumb-placeholder">🏠</span>
            </div>
            <div class="disc-item__body">
              <p class="disc-item__title">{{ d.annonceLibelle }}</p>
              <p class="disc-item__adresse">{{ d.annonceAdresse }}</p>
              <p class="disc-item__last">{{ truncate(d.dernierMessage) }}</p>
            </div>
            <div class="disc-item__meta">
              <span class="disc-item__date">{{ formatDate(d.dernierMessageAt || d.createdAt) }}</span>
              <span v-if="d.unreadCount" class="disc-item__badge">{{ d.unreadCount }}</span>
            </div>
          </li>
        </ul>

        <!-- Pagination mini -->
        <div v-if="totalPages > 1" class="disc-list__pagination">
          <button :disabled="currentPage === 0" @click="fetchDiscussions(currentPage - 1)">←</button>
          <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button :disabled="currentPage === totalPages - 1" @click="fetchDiscussions(currentPage + 1)">→</button>
        </div>
      </aside>

      <!-- ── Panneau chat ──────────────────────────────── -->
      <section class="disc-chat">

        <!-- Pas de discussion sélectionnée -->
        <div v-if="!selectedId" class="disc-chat__empty">
          <div class="disc-chat__empty-icon">💬</div>
          <p>Sélectionnez une discussion pour afficher les messages</p>
        </div>

        <!-- Chargement -->
        <div v-else-if="chatLoading" class="disc-chat__loading">
          <div class="spinner"></div>
        </div>

        <template v-else-if="selectedChat">
          <!-- Header chat -->
          <div class="disc-chat__header">
            <div class="disc-chat__header-info">
              <p class="disc-chat__annonceTitle">{{ selectedChat.annonceLibelle }}</p>
              <p class="disc-chat__annonceAddr">{{ selectedChat.annonceAdresse }}</p>
            </div>
            <RouterLink :to="`/annonces/${selectedChat.annonceId}`" class="disc-chat__link">
              Voir l'annonce →
            </RouterLink>
          </div>

          <!-- Messages -->
          <div class="disc-chat__messages" ref="messagesRef">
            <div
              v-for="msg in selectedChat.messages"
              :key="msg.id"
              class="bubble"
              :class="msg.senderRole === 'CLIENT' ? 'bubble--right' : 'bubble--left'"
            >
              <span class="bubble__sender">{{ msg.senderRole === 'ADMIN' ? 'Agence' : 'Vous' }}</span>
              <div class="bubble__text">{{ msg.contenu }}</div>
              <span class="bubble__time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>

          <!-- Zone saisie -->
          <div class="disc-chat__compose">
            <input
              v-model="newMessage"
              class="disc-chat__input"
              placeholder="Tapez votre message…"
              @keydown.enter.prevent="sendMessage"
            />
            <button
              class="disc-chat__send"
              :disabled="sending || !newMessage.trim()"
              @click="sendMessage"
            >
              ➤
            </button>
          </div>
        </template>
      </section>

    </div>
  </div>
</template>

<style scoped>
.disc-page {
  background: var(--color-background);
  min-height: 100vh;
}
.disc-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  height: calc(100vh - 64px);
  max-width: 1200px;
  margin: 0 auto;
}

/* ── Liste ── */
.disc-list {
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.disc-list__header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
}
.disc-list__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
}
.disc-list__loading,
.disc-list__error,
.disc-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
  text-align: center;
  color: var(--color-text);
  opacity: 0.55;
  font-size: 0.9rem;
}
.disc-list__link {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 0.88rem;
}
.disc-list__items {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 0;
}

.disc-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.9rem 1.25rem;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
}
.disc-item:hover { background: var(--color-background); }
.disc-item.--active { background: rgba(59, 130, 246, 0.08); }

.disc-item__thumb {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
}
.disc-item__thumb img { width: 100%; height: 100%; object-fit: cover; }
.disc-item__thumb-placeholder { font-size: 1.2rem; }

.disc-item__body { flex: 1; min-width: 0; }
.disc-item__title {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.disc-item__adresse {
  font-size: 0.75rem;
  color: var(--color-text);
  opacity: 0.5;
}
.disc-item__last {
  font-size: 0.78rem;
  color: var(--color-text);
  opacity: 0.6;
  margin-top: 0.2rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.disc-item__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.3rem;
  flex-shrink: 0;
}
.disc-item__date { font-size: 0.72rem; color: var(--color-text); opacity: 0.4; }
.disc-item__badge {
  background: var(--color-primary);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 10px;
}

.disc-list__pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.7;
}
.disc-list__pagination button {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  cursor: pointer;
}
.disc-list__pagination button:disabled { opacity: 0.3; cursor: default; }

/* ── Chat ── */
.disc-chat {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.disc-chat__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--color-text);
  opacity: 0.4;
}
.disc-chat__empty-icon { font-size: 3rem; }
.disc-chat__loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.disc-chat__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
}
.disc-chat__annonceTitle {
  font-weight: 700;
  font-size: 0.95rem;
  color: var(--color-text);
}
.disc-chat__annonceAddr {
  font-size: 0.78rem;
  color: var(--color-text);
  opacity: 0.5;
}
.disc-chat__link {
  font-size: 0.82rem;
  color: var(--color-primary);
  font-weight: 600;
}

.disc-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Bulles */
.bubble { display: flex; flex-direction: column; max-width: 70%; }
.bubble--right { align-self: flex-end; align-items: flex-end; }
.bubble--left  { align-self: flex-start; align-items: flex-start; }
.bubble__sender {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text);
  opacity: 0.5;
  margin-bottom: 0.2rem;
}
.bubble__text {
  padding: 0.65rem 1rem;
  border-radius: 14px;
  font-size: 0.9rem;
  line-height: 1.5;
  word-break: break-word;
}
.bubble--right .bubble__text {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble--left .bubble__text {
  background: var(--color-card);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 4px;
}
.bubble__time {
  font-size: 0.68rem;
  color: var(--color-text);
  opacity: 0.4;
  margin-top: 0.2rem;
}

.disc-chat__compose {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
.disc-chat__input {
  flex: 1;
  padding: 0.65rem 1rem;
  border: 1.5px solid var(--color-border);
  border-radius: 24px;
  font-size: 0.9rem;
  background: var(--color-background);
  color: var(--color-text);
  transition: border-color 0.2s;
}
.disc-chat__input:focus { border-color: var(--color-primary); outline: none; }
.disc-chat__send {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity 0.2s;
}
.disc-chat__send:disabled { opacity: 0.4; cursor: not-allowed; }
.disc-chat__send:hover:not(:disabled) { opacity: 0.85; }

/* Spinner */
.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Responsive */
@media (max-width: 768px) {
  .disc-layout { grid-template-columns: 1fr; }
  .disc-list { border-right: none; border-bottom: 1px solid var(--color-border); max-height: 40vh; }
  .disc-chat { min-height: 50vh; }
}
</style>
