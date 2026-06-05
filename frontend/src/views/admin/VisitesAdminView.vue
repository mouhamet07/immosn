<script setup>
import { ref, onMounted } from 'vue'
import visiteService from '@/services/visiteService'

const visites     = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')
const updating    = ref(null)

// Modal date
const showDateModal = ref(false)
const dateModalId   = ref(null)
const newDate       = ref('')
const dateComment   = ref('')

const STATUTS       = ['', 'EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE', 'TERMINEE']
const STATUT_LABELS = { EN_ATTENTE: 'En attente', ACCEPTEE: 'Acceptée', REFUSEE: 'Refusée', ANNULEE: 'Annulée', TERMINEE: 'Terminée' }
const STATUT_COLORS = { EN_ATTENTE: 'badge--warning', ACCEPTEE: 'badge--success', REFUSEE: 'badge--danger', ANNULEE: 'badge--neutral', TERMINEE: 'badge--info' }

async function fetchVisites(page = 0) {
  loading.value = true
  try {
    const res = await visiteService.getAllVisites(page, 20, filtreStatut.value || null)
    visites.value     = res.data.content ?? res.data.data ?? []
    currentPage.value = res.data.currentPage ?? res.data.number ?? 0
    totalPages.value  = res.data.totalPages ?? 1
    totalItems.value  = res.data.totalElements ?? 0
  } catch { visites.value = [] }
  finally { loading.value = false }
}

async function changeStatut(id, statut) {
  updating.value = id
  try {
    await visiteService.updateStatut(id, statut)
    await fetchVisites(currentPage.value)
  } catch { alert('Erreur.') }
  finally { updating.value = null }
}

async function submitDate() {
  if (!newDate.value) return
  try {
    await visiteService.updateDate(dateModalId.value, newDate.value, dateComment.value)
    showDateModal.value = false
    await fetchVisites(currentPage.value)
  } catch { alert('Erreur.') }
}

function openDateModal(id, currentDate) {
  dateModalId.value = id
  newDate.value = currentDate ? currentDate.slice(0, 16) : ''
  dateComment.value = ''
  showDateModal.value = true
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchVisites(0))
</script>

<template>
  <div class="va-page">
    <div class="va-toolbar">
      <div>
        <h1 class="va-toolbar__title">Demandes de visites</h1>
        <p class="va-toolbar__count">{{ totalItems }} demande{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <div class="va-filters">
        <button v-for="s in STATUTS" :key="s" class="filter-tab"
          :class="{ active: filtreStatut === s }"
          @click="filtreStatut = s; fetchVisites(0)">
          {{ s ? STATUT_LABELS[s] : 'Toutes' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="va-loading"><div class="spinner"></div></div>

    <div v-else-if="!visites.length" class="va-empty">Aucune demande.</div>

    <div v-else class="va-table-wrap">
      <table class="va-table">
        <thead>
          <tr>
            <th>#</th><th>Client</th><th>Annonce</th><th>Date souhaitée</th>
            <th>Statut</th><th>Commentaire</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in visites" :key="v.id">
            <td class="va-td-id">{{ v.id }}</td>
            <td>
              <p class="va-td-name">{{ v.clientNom }}</p>
            </td>
            <td>
              <RouterLink :to="`/annonces/${v.annonceId}`" class="va-td-link">{{ v.annonceLibelle }}</RouterLink>
              <p class="va-td-sub">{{ v.annonceAdresse }}</p>
            </td>
            <td>
              <p>{{ formatDate(v.dateVisite) }}</p>
              <button class="va-edit-date" @click="openDateModal(v.id, v.dateVisite)">✎ Modifier</button>
            </td>
            <td><span :class="['badge', STATUT_COLORS[v.statut]]">{{ STATUT_LABELS[v.statut] }}</span></td>
            <td class="va-td-comment">{{ v.commentaire || '–' }}</td>
            <td>
              <div class="va-actions" v-if="v.statut === 'EN_ATTENTE'">
                <button class="va-btn va-btn--accept" :disabled="updating === v.id"
                  @click="changeStatut(v.id, 'ACCEPTEE')">Accepter</button>
                <button class="va-btn va-btn--refuse" :disabled="updating === v.id"
                  @click="changeStatut(v.id, 'REFUSEE')">Refuser</button>
              </div>
              <button v-else-if="v.statut === 'ACCEPTEE'" class="va-btn va-btn--done" :disabled="updating === v.id"
                @click="changeStatut(v.id, 'TERMINEE')">Clôturer</button>
              <span v-else class="va-td-sub">–</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="va-pager">
      <button :disabled="currentPage === 0" @click="fetchVisites(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchVisites(currentPage + 1)">Suivant →</button>
    </div>

    <!-- Modal modifier date -->
    <Teleport to="body">
      <div v-if="showDateModal" class="modal-overlay" @click.self="showDateModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Modifier la date de visite</h2>
          <div class="modal-box__field">
            <label>Nouvelle date et heure</label>
            <input v-model="newDate" type="datetime-local" class="modal-box__input" />
          </div>
          <div class="modal-box__field">
            <label>Commentaire (optionnel)</label>
            <input v-model="dateComment" type="text" class="modal-box__input" placeholder="Raison du changement…" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showDateModal = false">Annuler</button>
            <button class="modal-box__submit" @click="submitDate">Confirmer</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.va-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.va-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.va-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.va-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; margin-top: .15rem; }
.va-filters { display: flex; flex-wrap: wrap; gap: .4rem; }
.filter-tab { padding: 6px 16px; border-radius: 20px; border: 1px solid var(--color-border); background: var(--color-card); color: var(--color-text-secondary, #6B7280); font-size: 13px; cursor: pointer; transition: all 150ms ease; }
.filter-tab:hover { border-color: var(--color-primary); color: var(--color-primary); }
.filter-tab.active { background: var(--color-primary); border-color: var(--color-primary); color: white; }

.va-loading { display: flex; justify-content: center; padding: 4rem; }
.va-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.va-table-wrap { overflow-x: auto; }
.va-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.va-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.va-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.va-table tr:last-child td { border-bottom: none; }
.va-table tr:hover td { background: rgba(0,0,0,.02); }

.va-td-id { font-size: .78rem; opacity: .45; }
.va-td-name { font-weight: 600; }
.va-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.va-td-sub { font-size: .75rem; opacity: .5; margin-top: .15rem; }
.va-td-comment { font-size: .8rem; max-width: 180px; }
.va-edit-date { font-size: .72rem; color: var(--color-primary); background: none; border: none; cursor: pointer; padding: 0; margin-top: .15rem; }

.va-actions { display: flex; gap: .4rem; }
.va-btn { padding: .3rem .7rem; border-radius: 6px; font-size: .78rem; font-weight: 600; border: none; cursor: pointer; transition: opacity .15s; }
.va-btn:disabled { opacity: .4; cursor: not-allowed; }
.va-btn--accept { background: #d1fae5; color: #059669; }
.va-btn--refuse { background: #fee2e2; color: #dc2626; }
.va-btn--done   { background: #dbeafe; color: #2563eb; }

.va-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.va-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.va-pager button:disabled { opacity: .4; cursor: not-allowed; }

/* Badges */
.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--warning { background: #fef3c7; color: #d97706; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--danger  { background: #fee2e2; color: #dc2626; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }
.badge--info    { background: #dbeafe; color: #2563eb; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
