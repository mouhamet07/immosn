<script setup>
import { ref, onMounted } from 'vue'
import signalementService from '@/services/signalementService'
import FilterSelect from '@/components/FilterSelect.vue'
import { useToastStore } from '@/stores/toastStore'

const toast = useToastStore()

const signalements = ref([])
const loading      = ref(false)
const currentPage  = ref(0)
const totalPages   = ref(1)
const totalItems   = ref(0)
const filtreStatut = ref('')

// Modal réponse
const showReply   = ref(false)
const replyId     = ref(null)
const replyStatut = ref('EN_COURS')
const replyText   = ref('')
const replying    = ref(false)

const STATUTS       = ['', 'OUVERT', 'EN_COURS', 'RESOLU', 'FERME']
const STATUT_LABELS = { OUVERT: 'Ouvert', EN_COURS: 'En cours', RESOLU: 'Résolu', FERME: 'Fermé' }
const STATUT_COLORS = { OUVERT: 'badge--warning', EN_COURS: 'badge--info', RESOLU: 'badge--success', FERME: 'badge--neutral' }
const NEXT_STATUTS  = ['OUVERT', 'EN_COURS', 'RESOLU', 'FERME']
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les signalements' }))

async function fetchSignalements(page = 0) {
  loading.value = true
  try {
    const res = await signalementService.getAll(page, 20, filtreStatut.value || null)
    signalements.value = res.data.data
    currentPage.value  = res.data.currentPage
    totalPages.value   = res.data.totalPages
    totalItems.value   = res.data.totalElements
  } catch (err) {
    console.error('[Signalements] fetchSignalements error:', err?.response?.status, err?.message)
    toast.error('Impossible de charger les signalements. Vérifiez votre connexion ou contactez le support.')
    signalements.value = []
  } finally { loading.value = false }
}

function openReply(s) {
  replyId.value     = s.id
  replyStatut.value = s.statut
  replyText.value   = s.reponseAdmin || ''
  showReply.value   = true
  if (!s.isRead) signalementService.markAsRead(s.id).catch(() => {})
}

async function submitReply() {
  if (replying.value) return
  replying.value = true
  try {
    await signalementService.updateStatut(replyId.value, replyStatut.value, replyText.value || null)
    showReply.value = false
    await fetchSignalements(currentPage.value)
  } catch { alert('Erreur.') }
  finally { replying.value = false }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchSignalements(0))
</script>

<template>
  <div class="sa-page">
    <div class="sa-toolbar">
      <div>
        <h1 class="sa-toolbar__title">Signalements SAV</h1>
        <p class="sa-toolbar__count">{{ totalItems }} signalement{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchSignalements(0) }"
      />
    </div>

    <div v-if="loading" class="sa-loading"><div class="spinner"></div></div>
    <div v-else-if="!signalements.length" class="sa-empty">Aucun signalement.</div>

    <div v-else class="sa-list">
      <div v-for="s in signalements" :key="s.id" class="sa-card"
        :class="{ '--unread': !s.isRead }">
        <div class="sa-card__header">
          <div class="sa-card__meta">
            <span :class="['badge', STATUT_COLORS[s.statut]]">{{ STATUT_LABELS[s.statut] }}</span>
            <span v-if="!s.isRead" class="sa-card__new">Nouveau</span>
            <span class="sa-card__date">{{ formatDate(s.createdAt) }}</span>
          </div>
          <div class="sa-card__ids">
            <span class="sa-card__client">{{ s.clientNom }}</span>
            <span class="sa-card__sep">·</span>
            <span class="sa-card__annonce">{{ s.annonceLibelle }}</span>
          </div>
        </div>
        <p class="sa-card__contenu">{{ s.contenu }}</p>
        <div v-if="s.reponseAdmin" class="sa-card__reponse">
          <span class="sa-card__reponse-label">Réponse :</span>
          {{ s.reponseAdmin }}
        </div>
        <div class="sa-card__footer">
          <button class="sa-btn" @click="openReply(s)">✎ Répondre / Mettre à jour</button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="sa-pager">
      <button :disabled="currentPage === 0" @click="fetchSignalements(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchSignalements(currentPage + 1)">Suivant →</button>
    </div>

    <!-- Modal réponse -->
    <Teleport to="body">
      <div v-if="showReply" class="modal-overlay" @click.self="showReply = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Traiter le signalement</h2>
          <div class="modal-box__field">
            <label>Nouveau statut</label>
            <select v-model="replyStatut" class="modal-box__input">
              <option v-for="s in NEXT_STATUTS" :key="s" :value="s">{{ STATUT_LABELS[s] }}</option>
            </select>
          </div>
          <div class="modal-box__field">
            <label>Réponse au client (optionnel)</label>
            <textarea v-model="replyText" class="modal-box__textarea" rows="5"
              placeholder="Votre réponse visible par le client…" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showReply = false">Annuler</button>
            <button class="modal-box__submit" :disabled="replying" @click="submitReply">
              {{ replying ? 'Envoi…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.sa-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.sa-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.sa-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.sa-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.sa-loading { display: flex; justify-content: center; padding: 4rem; }
.sa-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }
.sa-list { display: flex; flex-direction: column; gap: .75rem; }
.sa-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); border-left: 3px solid transparent; transition: border-color .2s; }
.sa-card.--unread { border-left-color: var(--color-primary); }
.sa-card__header { margin-bottom: .75rem; }
.sa-card__meta { display: flex; align-items: center; gap: .5rem; margin-bottom: .35rem; flex-wrap: wrap; }
.sa-card__new { font-size: .7rem; font-weight: 700; color: var(--color-primary); background: rgba(59,130,246,.1); padding: .15rem .5rem; border-radius: 10px; }
.sa-card__date { font-size: .72rem; color: var(--color-text); opacity: .45; margin-left: auto; }
.sa-card__ids { display: flex; align-items: center; gap: .4rem; font-size: .82rem; }
.sa-card__client { font-weight: 700; color: var(--color-text); }
.sa-card__sep { opacity: .35; }
.sa-card__annonce { color: var(--color-primary); }
.sa-card__contenu { font-size: .9rem; color: var(--color-text); line-height: 1.6; margin-bottom: .75rem; }
.sa-card__reponse { font-size: .82rem; color: var(--color-text); opacity: .7; font-style: italic; padding: .5rem .75rem; background: var(--color-background); border-radius: 6px; margin-bottom: .75rem; }
.sa-card__reponse-label { font-weight: 700; font-style: normal; margin-right: .3rem; }
.sa-card__footer { display: flex; justify-content: flex-end; }
.sa-btn { padding: .35rem .8rem; border: 1.5px solid var(--color-border); border-radius: 6px; font-size: .8rem; cursor: pointer; background: none; color: var(--color-text); transition: all .15s; }
.sa-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.sa-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.sa-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.sa-pager button:disabled { opacity: .4; cursor: not-allowed; }
.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--warning { background: #fef3c7; color: #d97706; }
.badge--info    { background: #dbeafe; color: #2563eb; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .88rem; background: var(--color-background); color: var(--color-text); width: 100%; box-sizing: border-box; }
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
