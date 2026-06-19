<script setup>
import { ref, onMounted } from 'vue'
import { FileText, Edit3, X } from 'lucide-vue-next'
import leadService from '@/services/leadService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useToastStore } from '@/stores/toastStore'

const toast = useToastStore()

const leads        = ref([])
const loading      = ref(false)
const currentPage  = ref(0)
const totalPages   = ref(1)
const totalItems   = ref(0)
const filtreStatut = ref('')
const showNoteModal = ref(false)
const modalLead     = ref(null)
const noteValue     = ref('')
const savingNote    = ref(false)

const STATUTS       = ['', 'EN_COURS', 'CONVERTI', 'ABANDONNE']
const STATUT_LABELS = { EN_COURS: 'En cours', CONVERTI: 'Converti', ABANDONNE: 'Abandonné' }
const STATUT_VARIANTS = { EN_COURS: 'info', CONVERTI: 'success', ABANDONNE: 'neutral' }
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les leads' }))

async function fetchLeads(page = 0) {
  loading.value = true
  try {
    const res = await leadService.getAll(page, 20, filtreStatut.value || null)
    leads.value       = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch (err) {
    console.error('[LeadsAdminView] fetchLeads error:', err?.response?.status, err?.message)
    leads.value = []
  } finally {
    loading.value = false
  }
}

function openNoteModal(lead) {
  modalLead.value     = lead
  noteValue.value     = lead.noteAdmin ?? ''
  showNoteModal.value = true
}

function closeNoteModal() {
  showNoteModal.value = false
  modalLead.value     = null
  noteValue.value     = ''
}

async function saveNote() {
  if (!modalLead.value || savingNote.value) return
  savingNote.value = true
  try {
    await leadService.updateNote(modalLead.value.id, noteValue.value || null)
    toast.success('Note enregistrée.')
    closeNoteModal()
    await fetchLeads(currentPage.value)
  } catch (err) {
    toast.error(err?.response?.data?.message || 'Erreur lors de la sauvegarde de la note.')
  } finally {
    savingNote.value = false
  }
}

function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function formatDateTime(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchLeads(0))
</script>

<template>
  <div class="la-page">

    <!-- Toolbar -->
    <div class="la-toolbar">
      <div>
        <h1 class="la-toolbar__title">Leads</h1>
        <p class="la-toolbar__count">{{ totalItems }} lead{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchLeads(0) }"
      />
    </div>

    <div v-if="loading" class="la-loading"><div class="spinner"></div></div>

    <div v-else-if="!leads.length" class="la-empty">Aucun lead pour ce filtre.</div>

    <div v-else class="la-pipeline">
      <div v-for="lead in leads" :key="lead.id" class="la-card">

        <!-- Header -->
        <div class="la-card__header">
          <StatusBadge :label="STATUT_LABELS[lead.statut]" :variant="STATUT_VARIANTS[lead.statut]" />
          <span class="la-card__date">{{ formatDate(lead.createdAt) }}</span>
        </div>

        <!-- Client + annonce -->
        <div class="la-card__body">
          <div class="la-card__avatar">{{ lead.clientNom?.charAt(0)?.toUpperCase() || '?' }}</div>
          <div class="la-card__info">
            <p class="la-card__client table-client">{{ lead.clientNom }}</p>
            <p class="la-card__email">{{ lead.clientEmail }}</p>
            <RouterLink :to="`/admin/annonces/${lead.annonceId}`" class="la-card__annonce">
              {{ lead.annonceLibelle }}
            </RouterLink>
            <p class="la-card__addr">{{ lead.annonceAdresse }}</p>
          </div>
        </div>

        <!-- Liens croisés -->
        <div v-if="lead.visiteId || lead.contratId" class="la-card__links">
          <RouterLink
            v-if="lead.visiteId"
            :to="`/admin/visites/${lead.visiteId}`"
            class="la-card__link la-card__link--visite"
          >Visite {{ lead.visiteId }}</RouterLink>
          <RouterLink
            v-if="lead.contratId"
            :to="`/admin/contrats/${lead.contratId}`"
            class="la-card__link la-card__link--contrat"
          >Contrat {{ lead.contratId }}</RouterLink>
        </div>

        <!-- Dates secondaires -->
        <div class="la-card__dates">
          <span v-if="lead.convertedAt" class="la-card__date-tag la-card__date-tag--green">
            Converti le {{ formatDate(lead.convertedAt) }}
          </span>
          <span v-if="lead.updatedAt !== lead.createdAt" class="la-card__date-tag">
            Modifié {{ formatDateTime(lead.updatedAt) }}
          </span>
        </div>

        <!-- Note -->
        <p v-if="lead.noteAdmin" class="la-card__note">
          <FileText :size="13" /> {{ lead.noteAdmin }}
        </p>

        <!-- Actions -->
        <div class="la-card__actions">
          <button class="btn btn--ghost btn--sm" @click="openNoteModal(lead)">
            <Edit3 :size="12" /> Note
          </button>
          <RouterLink :to="`/admin/leads/${lead.id}`" class="btn btn--outline btn--sm">
            Détail
          </RouterLink>
        </div>

      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="la-pager">
      <button :disabled="currentPage === 0" @click="fetchLeads(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchLeads(currentPage + 1)">Suivant →</button>
    </div>

    <!-- Modal note -->
    <Teleport to="body">
      <div v-if="showNoteModal" class="modal-backdrop" @click.self="closeNoteModal">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal__header">
            <h2 class="modal__title">Note admin — Lead {{ modalLead?.id }}</h2>
            <button class="modal__close" @click="closeNoteModal"><X :size="16" /></button>
          </div>
          <div class="modal__body">
            <p class="modal__meta">{{ modalLead?.clientNom }} · {{ modalLead?.annonceLibelle }}</p>
            <textarea
              v-model="noteValue"
              class="modal__textarea"
              placeholder="Note interne sur ce lead (max 2 000 caractères)…"
              maxlength="2000"
              rows="6"
            ></textarea>
            <p class="modal__counter">{{ noteValue.length }} / 2 000</p>
          </div>
          <div class="modal__footer">
            <button class="btn btn--ghost" @click="closeNoteModal">Annuler</button>
            <button class="btn btn--primary" :disabled="savingNote" @click="saveNote">
              {{ savingNote ? 'Enregistrement…' : 'Enregistrer' }}
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
.la-loading { display: flex; justify-content: center; padding: 4rem; }
.la-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.la-pipeline { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }

/* Card */
.la-card { background: var(--color-card); border-radius: var(--radius); padding: 1rem; box-shadow: var(--shadow-card); display: flex; flex-direction: column; gap: .65rem; }
.la-card__header { display: flex; align-items: center; gap: .5rem; }
.la-card__id { font-size: .78rem; font-weight: 700; color: var(--color-text); opacity: .35; text-decoration: none; transition: opacity .15s; }
.la-card__id:hover { opacity: .7; }
.la-card__date { font-size: .72rem; color: var(--color-text); opacity: .4; margin-left: auto; }
.la-card__body { display: flex; gap: .75rem; align-items: flex-start; }
.la-card__avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: .95rem; flex-shrink: 0; }
.la-card__info { flex: 1; min-width: 0; }
.la-card__client { font-size: .9rem; font-weight: 700; color: var(--color-text); }
.la-card__email { font-size: .75rem; color: var(--color-text); opacity: .5; }
.la-card__annonce { font-size: .82rem; color: var(--color-primary); font-weight: 600; text-decoration: none; display: block; margin-top: .2rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.la-card__addr { font-size: .75rem; color: var(--color-text); opacity: .5; }

/* Liens croisés */
.la-card__links { display: flex; gap: .5rem; flex-wrap: wrap; }
.la-card__link { font-size: .75rem; font-weight: 600; padding: .2rem .6rem; border-radius: 10px; text-decoration: none; transition: opacity .15s; }
.la-card__link--visite  { background: #eff6ff; color: #2563eb; }
.la-card__link--contrat { background: #ecfdf5; color: #059669; }
.la-card__link:hover { opacity: .75; }

/* Tags de dates */
.la-card__dates { display: flex; flex-direction: column; gap: .2rem; }
.la-card__date-tag { font-size: .72rem; color: var(--color-text); opacity: .5; }
.la-card__date-tag--green { color: #059669; opacity: 1; font-weight: 600; }

/* Note */
.la-card__note { font-size: .78rem; color: var(--color-text); opacity: .65; font-style: italic; padding: .45rem .65rem; background: var(--color-background); border-radius: 6px; display: flex; align-items: flex-start; gap: .35rem; line-height: 1.4; }

/* Actions */
.la-card__actions { display: flex; gap: .4rem; flex-wrap: wrap; padding-top: .25rem; border-top: 1px solid var(--color-border); }

/* Boutons */
.btn { display: inline-flex; align-items: center; gap: .3rem; padding: .38rem .85rem; border-radius: var(--radius-sm); font-size: .82rem; font-weight: 600; border: none; cursor: pointer; text-decoration: none; transition: opacity .15s, background .15s; }
.btn:disabled { opacity: .45; cursor: not-allowed; }
.btn--sm { padding: .28rem .65rem; font-size: .78rem; }
.btn--primary  { background: var(--color-primary); color: #fff; }
.btn--success  { background: #059669; color: #fff; }
.btn--neutral  { background: #f3f4f6; color: #374151; border: 1.5px solid #e5e7eb; }
.btn--ghost    { background: transparent; color: var(--color-text); border: 1.5px solid var(--color-border); }
.btn--outline  { background: transparent; color: var(--color-primary); border: 1.5px solid var(--color-primary); }
.btn:not(:disabled):hover { opacity: .82; }

/* Pagination */
.la-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; font-size: .88rem; }
.la-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.la-pager button:disabled { opacity: .4; cursor: not-allowed; }


/* Modal */
.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem; }
.modal { background: var(--color-card); border-radius: var(--radius); width: 100%; max-width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,.25); display: flex; flex-direction: column; gap: 0; overflow: hidden; }
.modal__header { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid var(--color-border); }
.modal__title { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.modal__close { background: none; border: none; font-size: 1.1rem; cursor: pointer; color: var(--color-text); opacity: .45; line-height: 1; padding: .2rem .4rem; }
.modal__close:hover { opacity: .8; }
.modal__body { padding: 1.25rem; display: flex; flex-direction: column; gap: .75rem; }
.modal__meta { font-size: .82rem; color: var(--color-text); opacity: .5; }
.modal__textarea { width: 100%; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); padding: .7rem .9rem; font-size: .88rem; font-family: inherit; resize: vertical; background: var(--color-background); color: var(--color-text); outline: none; transition: border-color .15s; box-sizing: border-box; }
.modal__textarea:focus { border-color: var(--color-primary); }
.modal__counter { font-size: .72rem; color: var(--color-text); opacity: .4; text-align: right; }
.modal__footer { display: flex; justify-content: flex-end; gap: .6rem; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }

/* Spinner */
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
