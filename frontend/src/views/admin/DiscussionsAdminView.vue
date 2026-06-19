<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { MessageSquare, ArrowLeft, Home, User, Send, Phone, Mail } from 'lucide-vue-next'
import discussionService from '@/services/discussionService'

const discussions  = ref([])
const loading      = ref(false)
const selectedId   = ref(null)
const selectedChat = ref(null)
const chatLoading  = ref(false)
const newMessage   = ref('')
const sending      = ref(false)
const messagesRef  = ref(null)
const currentPage  = ref(0)
const totalPages   = ref(1)
const totalItems   = ref(0)
const searchQuery  = ref('')

async function fetchDiscussions(page = 0) {
  loading.value = true
  try {
    const res = await discussionService.getAllDiscussions(page, 20)
    const paged = res.data
    discussions.value = paged.content ?? paged.data ?? []
    currentPage.value = paged.currentPage ?? paged.number ?? 0
    totalPages.value  = paged.totalPages ?? 1
    totalItems.value  = paged.totalElements ?? 0
  } catch {
    discussions.value = []
  } finally {
    loading.value = false
  }
}

async function openDiscussion(id) {
  selectedId.value  = id
  chatLoading.value = true
  try {
    const res = await discussionService.getMessages(id)
    selectedChat.value = res.data.data
    const d = discussions.value.find(x => x.id === id)
    if (d) d.unreadCount = 0
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

const filteredDiscussions = computed(() => {
  if (!searchQuery.value) return discussions.value
  const q = searchQuery.value.toLowerCase()
  return discussions.value.filter(d =>
    d.annonceLibelle?.toLowerCase().includes(q) ||
    d.clientNom?.toLowerCase().includes(q)
  )
})

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
function formatTime(dt) {
  return new Date(dt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}
function truncate(str, n = 55) {
  return str && str.length > n ? str.slice(0, n) + '…' : str
}

onMounted(() => fetchDiscussions(0))
</script>

<template>
  <div class="adm-disc">

    <!-- Toolbar -->
    <div class="adm-disc__toolbar">
      <h1 class="adm-disc__title">Messagerie</h1>
      <div class="adm-disc__meta">
        <span class="adm-disc__count">{{ totalItems }} conversation{{ totalItems !== 1 ? 's' : '' }}</span>
        <input
          v-model="searchQuery"
          class="adm-disc__search"
          placeholder="Rechercher client ou annonce…"
        />
      </div>
    </div>

    <div class="adm-disc__layout">

      <!-- ── Liste conversations ───────────────────────── -->
      <aside class="adm-list">

        <div v-if="loading" class="adm-list__loading"><div class="spinner"></div></div>

        <div v-else-if="!discussions.length" class="adm-list__empty">
          Aucune conversation
        </div>

        <ul v-else class="adm-list__items">
          <li
            v-for="d in filteredDiscussions"
            :key="d.id"
            class="adm-item"
            :class="{ '--active': selectedId === d.id, '--unread': d.unreadCount > 0 }"
            @click="openDiscussion(d.id)"
          >
            <div class="adm-item__avatar">
              {{ d.clientNom?.charAt(0)?.toUpperCase() || '?' }}
            </div>
            <div class="adm-item__body">
              <div class="adm-item__top">
                <p class="adm-item__client">
                  {{ d.clientNom }}
                  <span v-if="d.prospectId" class="adm-item__prospect-tag">Prospect</span>
                </p>
                <span class="adm-item__date">{{ formatDate(d.dernierMessageAt || d.createdAt) }}</span>
              </div>
              <!-- Référence annonce -->
              <div class="adm-item__annonce-ref">
                <Home :size="12" />
                <span>{{ d.annonceLibelle }}</span>
              </div>
              <p class="adm-item__last">{{ truncate(d.dernierMessage) }}</p>
            </div>
            <span v-if="d.unreadCount" class="adm-item__badge">{{ d.unreadCount }}</span>
          </li>
        </ul>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="adm-list__pager">
          <button :disabled="currentPage === 0" @click="fetchDiscussions(currentPage - 1)">←</button>
          <span>{{ currentPage + 1 }}/{{ totalPages }}</span>
          <button :disabled="currentPage === totalPages - 1" @click="fetchDiscussions(currentPage + 1)">→</button>
        </div>
      </aside>

      <!-- ── Panneau chat ──────────────────────────────── -->
      <section class="adm-chat">

        <div v-if="!selectedId" class="adm-chat__empty">
          <MessageSquare :size="48" class="adm-chat__empty-icon" />
          <p>Sélectionnez une conversation</p>
        </div>

        <div v-else-if="chatLoading" class="adm-chat__loading"><div class="spinner"></div></div>

        <template v-else-if="selectedChat">
          <!-- Header avec bouton retour + contexte annonce + client -->
          <div class="adm-chat__header">
            <button class="adm-chat__back" @click="selectedId = null; selectedChat = null">
              <ArrowLeft :size="18" />
            </button>
            <div class="adm-chat__header-info">
              <div class="adm-chat__annonce-ref">
                <Home :size="14" />
                <span>{{ selectedChat.annonceLibelle }}</span>
              </div>
              <div class="adm-chat__client-ref">
                <User :size="13" />
                <span>{{ selectedChat.clientNom }}</span>
              </div>
            </div>
            <RouterLink :to="`/admin/annonces/${selectedChat.annonceId}`" class="adm-chat__annonce-link">
              Voir l'annonce →
            </RouterLink>
          </div>

          <!-- Panneau infos prospect non converti : permet à l'agence de répondre hors système -->
          <div v-if="selectedChat.prospectId" class="prospect-panel">
            <p class="prospect-panel__title"><User :size="13" /> Prospect non converti — contact direct possible</p>
            <div class="prospect-panel__grid">
              <span class="prospect-panel__item"><User :size="12" /> {{ selectedChat.prospectPrenom ? selectedChat.prospectPrenom + ' ' : '' }}{{ selectedChat.prospectNom }}</span>
              <span class="prospect-panel__item"><Phone :size="12" /> {{ selectedChat.prospectTelephone || '–' }}</span>
              <span class="prospect-panel__item"><Mail :size="12" /> {{ selectedChat.prospectEmail || '–' }}</span>
            </div>
          </div>

          <!-- Messages -->
          <div class="adm-chat__messages" ref="messagesRef">
            <div
              v-for="msg in selectedChat.messages"
              :key="msg.id"
              class="bubble"
              :class="msg.senderRole === 'ADMIN' ? 'bubble--right' : 'bubble--left'"
            >
              <span class="bubble__sender">{{ msg.senderRole === 'ADMIN' ? 'Vous (Admin)' : selectedChat.clientNom }}</span>
              <div class="bubble__text">{{ msg.contenu }}</div>
              <span class="bubble__time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>

          <!-- Zone saisie -->
          <div class="adm-chat__compose">
            <input
              v-model="newMessage"
              class="adm-chat__input"
              placeholder="Répondre au client…"
              @keydown.enter.prevent="sendMessage"
            />
            <button
              class="adm-chat__send"
              :disabled="sending || !newMessage.trim()"
              @click="sendMessage"
            >
              <Send :size="18" />
            </button>
          </div>
        </template>
      </section>

    </div>
  </div>
</template>

<style scoped>
.adm-disc {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-background);
}

.adm-disc__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-card);
  gap: 1rem;
  flex-wrap: wrap;
}
.adm-disc__title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text);
}
.adm-disc__meta {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.adm-disc__count {
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.55;
  white-space: nowrap;
}
.adm-disc__search {
  padding: 0.5rem 0.9rem;
  border: 1.5px solid var(--color-border);
  border-radius: 20px;
  font-size: 0.85rem;
  background: var(--color-background);
  color: var(--color-text);
  width: 220px;
  transition: border-color 0.2s;
}
.adm-disc__search:focus { border-color: var(--color-primary); outline: none; }

.adm-disc__layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  flex: 1;
  overflow: hidden;
  height: calc(100% - 70px);
}

/* ── Liste ── */
.adm-list {
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.adm-list__loading,
.adm-list__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--color-text);
  opacity: 0.45;
  font-size: 0.9rem;
}
.adm-list__items {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 0;
}

.adm-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.9rem 1.25rem;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
  position: relative;
}
.adm-item:hover { background: var(--color-background); }
.adm-item.--active { background: rgba(59, 130, 246, 0.07); }
.adm-item.--unread .adm-item__client { font-weight: 800; }

.adm-item__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
}
.adm-item__body { flex: 1; min-width: 0; }
.adm-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.1rem;
}
.adm-item__client {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.adm-item__prospect-tag {
  font-size: 0.62rem; font-weight: 700; text-transform: uppercase; letter-spacing: .03em;
  background: #fef3c7; color: #d97706; padding: .1rem .4rem; border-radius: 8px; flex-shrink: 0;
}
.adm-item__date { font-size: 0.7rem; color: var(--color-text); opacity: 0.4; flex-shrink: 0; }
.adm-item__annonce-ref {
  display: flex; align-items: center; gap: 4px;
  font-size: 0.75rem; color: var(--color-primary); font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 0.1rem;
}
.adm-item__last {
  font-size: 0.78rem;
  color: var(--color-text);
  opacity: 0.55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.adm-item__badge {
  background: var(--color-primary);
  color: #fff;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 10px;
  flex-shrink: 0;
}

.adm-list__pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.6;
}
.adm-list__pager button {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  cursor: pointer;
}
.adm-list__pager button:disabled { opacity: 0.3; cursor: default; }

/* ── Chat ── */
.adm-chat {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.adm-chat__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--color-text);
  opacity: 0.35;
}
.adm-chat__empty-icon { color: var(--color-text-muted); opacity: 0.4; }
.adm-chat__loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.adm-chat__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-card);
}
.adm-chat__back {
  background: none; border: none; cursor: pointer;
  color: var(--color-text); display: flex; align-items: center;
  padding: 4px; border-radius: 6px; transition: background 0.15s; flex-shrink: 0;
}
.adm-chat__back:hover { background: var(--color-background); }
.adm-chat__header-info { flex: 1; min-width: 0; }
.adm-chat__annonce-ref {
  display: flex; align-items: center; gap: 6px;
  font-size: 0.92rem; font-weight: 700; color: var(--color-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.adm-chat__client-ref {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.78rem; color: var(--color-text-secondary); margin-top: 2px;
}
.adm-chat__annonce-link {
  font-size: 0.8rem; color: var(--color-primary); font-weight: 600;
  white-space: nowrap; text-decoration: none;
}
.adm-chat__annonce-link:hover { text-decoration: underline; }

/* Panneau infos prospect non converti */
.prospect-panel {
  padding: .75rem 1.5rem;
  background: #fffbeb;
  border-bottom: 1px solid var(--color-border);
}
.prospect-panel__title {
  display: flex; align-items: center; gap: .35rem;
  font-size: .78rem; font-weight: 700; color: #92400e; margin-bottom: .4rem;
}
.prospect-panel__grid {
  display: flex; flex-wrap: wrap; gap: 1rem;
}
.prospect-panel__item {
  display: flex; align-items: center; gap: .3rem;
  font-size: .8rem; color: var(--color-text);
}

.adm-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

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
.bubble__time { font-size: 0.68rem; color: var(--color-text); opacity: 0.4; margin-top: 0.2rem; }

.adm-chat__compose {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-card);
}
.adm-chat__input {
  flex: 1;
  padding: 0.65rem 1rem;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
  background: var(--color-background);
  color: var(--color-text);
  transition: border-color 0.2s;
}
.adm-chat__input:focus { border-color: var(--color-primary); outline: none; }
.adm-chat__send {
  padding: 0.65rem 1.25rem;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.adm-chat__send:disabled { opacity: 0.4; cursor: not-allowed; }
.adm-chat__send:hover:not(:disabled) { opacity: 0.85; }

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

@media (max-width: 860px) {
  .adm-disc__layout { grid-template-columns: 1fr; }
  .adm-list { border-right: none; border-bottom: 1px solid var(--color-border); max-height: 45vh; }
}
</style>
