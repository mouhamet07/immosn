<script setup>
import { ref, onMounted } from 'vue'
import { FileText } from 'lucide-vue-next'
import leadService from '@/services/leadService'
import contratService from '@/services/contratService'

const leads       = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')
const updating    = ref(null)

// Modal conversion → contrat
const showConvertModal = ref(false)
const convertLead      = ref(null)
const convertForm      = ref({ dateDebut: '', dateFin: '', montant: '', documentUrl: '', notes: '' })
const converting       = ref(false)

const STATUTS       = ['', 'EN_COURS', 'CONVERTI', 'ABANDONNE']
const STATUT_LABELS = { EN_COURS: 'En cours', CONVERTI: 'Converti', ABANDONNE: 'Abandonné' }
const STATUT_COLORS = { EN_COURS: 'badge--info', CONVERTI: 'badge--success', ABANDONNE: 'badge--neutral' }

async function fetchLeads(page = 0) {
  loading.value = true
  try {
    const res = await leadService.getAll(page, 20, filtreStatut.value || null)
    leads.value       = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch { leads.value = [] }
  finally { loading.value = false }
}

async function changeStatut(id, statut) {
  updating.value = id
  try {
    await leadService.updateStatut(id, statut)
    await fetchLeads(currentPage.value)
  } catch { alert('Erreur.') }
  finally { updating.value = null }
}

function openConvert(lead) {
  convertLead.value = lead
  convertForm.value = { dateDebut: '', dateFin: '', montant: '', documentUrl: '', notes: '' }
  showConvertModal.value = true
}

async function submitConvert() {
  if (!convertForm.value.dateDebut || !convertForm.value.montant) {
    alert('Date de début et montant requis.')
    return
  }
  converting.value = true
  try {
    await contratService.create({
      clientId:    convertLead.value.clientId,
      annonceId:   convertLead.value.annonceId,
      leadId:      convertLead.value.id,
      dateDebut:   convertForm.value.dateDebut,
      dateFin:     convertForm.value.dateFin || null,
      montant:     Number(convertForm.value.montant),
      documentUrl: convertForm.value.documentUrl || null,
      notes:       convertForm.value.notes || null,
    })
    showConvertModal.value = false
    await fetchLeads(currentPage.value)
  } catch { alert('Erreur lors de la création du contrat.') }
  finally { converting.value = false }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(() => fetchLeads(0))
</script>

<template>
  <div class="la-page">
    <div class="la-toolbar">
      <div>
        <h1 class="la-toolbar__title">Leads</h1>
        <p class="la-toolbar__count">{{ totalItems }} lead{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <div class="la-filters">
        <button v-for="s in STATUTS" :key="s" class="filter-tab"
          :class="{ active: filtreStatut === s }"
          @click="filtreStatut = s; fetchLeads(0)">
          {{ s ? STATUT_LABELS[s] : 'Tous' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="la-loading"><div class="spinner"></div></div>

    <div v-else-if="!leads.length" class="la-empty">Aucun lead.</div>

    <div v-else class="la-pipeline">
      <div v-for="lead in leads" :key="lead.id" class="la-card">
        <div class="la-card__header">
          <span :class="['badge', STATUT_COLORS[lead.statut]]">{{ STATUT_LABELS[lead.statut] }}</span>
          <span class="la-card__date">{{ formatDate(lead.createdAt) }}</span>
        </div>
        <div class="la-card__body">
          <div class="la-card__avatar">{{ lead.clientNom?.charAt(0)?.toUpperCase() || '?' }}</div>
          <div class="la-card__info">
            <p class="la-card__client">{{ lead.clientNom }}</p>
            <p class="la-card__email">{{ lead.clientEmail }}</p>
            <RouterLink :to="`/annonces/${lead.annonceId}`" class="la-card__annonce">
              {{ lead.annonceLibelle }}
            </RouterLink>
            <p class="la-card__addr">{{ lead.annonceAdresse }}</p>
          </div>
        </div>
        <p v-if="lead.noteAdmin" class="la-card__note"><FileText :size="13" /> {{ lead.noteAdmin }}</p>
        <div class="la-card__actions">
          <button v-if="lead.statut === 'EN_COURS'" class="la-btn la-btn--convert"
            @click="openConvert(lead)">Créer un contrat</button>
          <button v-if="lead.statut === 'EN_COURS'" class="la-btn la-btn--abandon"
            :disabled="updating === lead.id"
            @click="changeStatut(lead.id, 'ABANDONNE')">Abandonner</button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="la-pager">
      <button :disabled="currentPage === 0" @click="fetchLeads(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchLeads(currentPage + 1)">Suivant →</button>
    </div>

    <!-- Modal conversion contrat -->
    <Teleport to="body">
      <div v-if="showConvertModal" class="modal-overlay" @click.self="showConvertModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Créer un contrat</h2>
          <p class="modal-box__desc">Client : <strong>{{ convertLead?.clientNom }}</strong> — {{ convertLead?.annonceLibelle }}</p>

          <div class="modal-box__row">
            <div class="modal-box__field">
              <label>Date de début *</label>
              <input v-model="convertForm.dateDebut" type="date" class="modal-box__input" />
            </div>
            <div class="modal-box__field">
              <label>Date de fin</label>
              <input v-model="convertForm.dateFin" type="date" class="modal-box__input" />
            </div>
          </div>
          <div class="modal-box__field">
            <label>Montant (FCFA) *</label>
            <input v-model="convertForm.montant" type="number" min="0" class="modal-box__input" placeholder="Ex: 150000" />
          </div>
          <div class="modal-box__field">
            <label>URL document</label>
            <input v-model="convertForm.documentUrl" type="text" class="modal-box__input" placeholder="https://…" />
          </div>
          <div class="modal-box__field">
            <label>Notes</label>
            <textarea v-model="convertForm.notes" class="modal-box__textarea" rows="3" />
          </div>

          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showConvertModal = false">Annuler</button>
            <button class="modal-box__submit" :disabled="converting" @click="submitConvert">
              {{ converting ? 'Création…' : 'Créer le contrat' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.la-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.la-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.la-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.la-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.la-filters { display: flex; flex-wrap: wrap; gap: .4rem; }
.filter-tab { padding: 6px 16px; border-radius: 20px; border: 1px solid var(--color-border); background: var(--color-card); color: var(--color-text-secondary, #6B7280); font-size: 13px; cursor: pointer; transition: all 150ms ease; }
.filter-tab:hover { border-color: var(--color-primary); color: var(--color-primary); }
.filter-tab.active { background: var(--color-primary); border-color: var(--color-primary); color: white; }

.la-loading { display: flex; justify-content: center; padding: 4rem; }
.la-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.la-pipeline { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }

.la-card { background: var(--color-card); border-radius: var(--radius); padding: 1rem; box-shadow: var(--shadow-card); display: flex; flex-direction: column; gap: .75rem; }
.la-card__header { display: flex; justify-content: space-between; align-items: center; }
.la-card__date { font-size: .72rem; color: var(--color-text); opacity: .4; }
.la-card__body { display: flex; gap: .75rem; align-items: flex-start; }
.la-card__avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: .95rem; flex-shrink: 0; }
.la-card__info { flex: 1; min-width: 0; }
.la-card__client { font-size: .9rem; font-weight: 700; color: var(--color-text); }
.la-card__email { font-size: .75rem; color: var(--color-text); opacity: .5; }
.la-card__annonce { font-size: .82rem; color: var(--color-primary); font-weight: 600; text-decoration: none; display: block; margin-top: .2rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.la-card__addr { font-size: .75rem; color: var(--color-text); opacity: .5; }
.la-card__note { font-size: .78rem; color: var(--color-text); opacity: .65; font-style: italic; padding: .5rem .75rem; background: var(--color-background); border-radius: 6px; display: flex; align-items: center; gap: .35rem; }
.la-card__actions { display: flex; gap: .5rem; flex-wrap: wrap; }

.la-btn { padding: .35rem .8rem; border-radius: 6px; font-size: .78rem; font-weight: 600; border: none; cursor: pointer; transition: opacity .15s; }
.la-btn:disabled { opacity: .4; cursor: not-allowed; }
.la-btn--convert { background: var(--color-primary); color: #fff; }
.la-btn--abandon { background: #f3f4f6; color: #6b7280; }

.la-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; font-size: .88rem; }
.la-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.la-pager button:disabled { opacity: .4; cursor: not-allowed; }

.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--info    { background: #dbeafe; color: #2563eb; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1.1rem; font-weight: 700; color: var(--color-text); margin-bottom: .5rem; }
.modal-box__desc { font-size: .88rem; color: var(--color-text); opacity: .65; margin-bottom: 1.25rem; }
.modal-box__row { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
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
