<script setup>
import { ref, onMounted } from 'vue'
import { Pencil, Eye } from 'lucide-vue-next'
import visiteService from '@/services/visiteService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const visites     = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')
const filtreType    = ref('')

// Modal modifier date
const showDateModal = ref(false)
const dateModalId   = ref(null)
const newDate       = ref('')
const dateComment   = ref('')

const STATUTS = ['', 'EN_ATTENTE', 'ACCEPTEE', 'AFFECTEE', 'REPLANIFICATION_DEMANDEE', 'RAPPORT_REDIGE', 'REFUSEE', 'ANNULEE', 'CLOTUREE_SANS_SUITE', 'CLOTUREE_AVEC_CONTRAT', 'TERMINEE']
const STATUT_LABELS = {
  EN_ATTENTE:               'En attente',
  ACCEPTEE:                 'Acceptée',
  AFFECTEE:                 'Affectée',
  REPLANIFICATION_DEMANDEE: 'Replanification demandée',
  RAPPORT_REDIGE:           'Rapport rédigé',
  REFUSEE:                  'Refusée',
  ANNULEE:                  'Annulée',
  CLOTUREE_SANS_SUITE:      'Clôturée sans suite',
  CLOTUREE_AVEC_CONTRAT:    'Clôturée avec contrat',
  TERMINEE:                 'Terminée',
}
const STATUT_VARIANTS = {
  EN_ATTENTE:               'warning',
  ACCEPTEE:                 'success',
  AFFECTEE:                 'info',
  REPLANIFICATION_DEMANDEE: 'warning',
  RAPPORT_REDIGE:           'info',
  REFUSEE:                  'danger',
  ANNULEE:                  'neutral',
  CLOTUREE_SANS_SUITE:      'neutral',
  CLOTUREE_AVEC_CONTRAT:    'info',
  TERMINEE:                 'info',
}
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Toutes les visites' }))

const TYPES = ['', 'VENTE', 'LOCATION']
const TYPE_LABELS = { VENTE: 'Vente', LOCATION: 'Location' }
const typeFilterOptions = TYPES.map(t => ({ value: t, label: t ? TYPE_LABELS[t] : 'Tous les types' }))

async function fetchVisites(page = 0) {
  loading.value = true
  try {
    const res = await visiteService.getAllVisites(page, 20, filtreStatut.value || null, filtreType.value || null)
    visites.value     = res.data.content ?? res.data.data ?? []
    currentPage.value = res.data.currentPage ?? res.data.number ?? 0
    totalPages.value  = res.data.totalPages ?? 1
    totalItems.value  = res.data.totalElements ?? 0
  } catch { visites.value = [] }
  finally { loading.value = false }
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
        <FilterSelect
          :model-value="filtreType"
          :options="typeFilterOptions"
          @update:model-value="(v) => { filtreType = v; fetchVisites(0) }"
        />
        <FilterSelect
          :model-value="filtreStatut"
          :options="filterOptions"
          @update:model-value="(v) => { filtreStatut = v; fetchVisites(0) }"
        />
      </div>
    </div>

    <div v-if="loading" class="va-loading"><div class="spinner"></div></div>

    <div v-else-if="!visites.length" class="va-empty">Aucune demande.</div>

    <div v-else class="va-table-wrap">
      <table class="va-table">
        <thead>
          <tr>
            <th>Visiteur</th><th>Annonce</th><th>Type</th><th>Date souhaitée</th>
            <th>Statut</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in visites" :key="v.id">
            <td class="table-client">{{ v.clientNom }}</td>
            <td>
              <RouterLink :to="`/admin/annonces/${v.annonceId}`" class="va-td-link">{{ v.annonceLibelle }}</RouterLink>
              <p class="va-td-sub">{{ v.annonceAdresse }}</p>
            </td>
            <td class="td-type">
              <span v-if="v.typeTransaction" :class="['badge-type', v.typeTransaction === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                {{ TYPE_LABELS[v.typeTransaction] }}
              </span>
              <span v-else>–</span>
            </td>
            <td class="td-date">
              <p class="table-date">{{ formatDate(v.dateVisite) }}</p>
              <button class="va-edit-date" @click="openDateModal(v.id, v.dateVisite)"><Pencil :size="13" /> Modifier</button>
            </td>
            <td class="td-statut"><StatusBadge :label="STATUT_LABELS[v.statut] ?? v.statut" :variant="STATUT_VARIANTS[v.statut] ?? 'neutral'" /></td>
            <td>
              <div class="td-actions">
                <RouterLink :to="`/admin/visites/${v.id}`" class="va-detail-link" title="Voir détails" aria-label="Voir détails">
                  <Eye :size="16" />
                </RouterLink>
              </div>
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
.va-filters { display: flex; gap: .6rem; flex-wrap: wrap; }
.va-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.va-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; margin-top: .15rem; }
.va-loading { display: flex; justify-content: center; padding: 4rem; }
.va-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.va-table-wrap { overflow-x: auto; }
.va-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.va-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.va-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.va-table tr:last-child td { border-bottom: none; }
.va-table tr:hover td { background: rgba(0,0,0,.02); }

.va-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.va-td-sub { font-size: .75rem; opacity: .5; margin-top: .15rem; }
.td-statut { width: 1%; white-space: nowrap; text-align: center; }
.td-type { width: 1%; white-space: nowrap; text-align: center; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }
.td-date { white-space: nowrap; padding-left: .5rem; }
.va-edit-date { font-size: .72rem; color: var(--color-primary); background: none; border: none; cursor: pointer; padding: 0; margin-top: .15rem; }

.va-detail-link {
  width: 32px; height: 32px; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  color: var(--color-text); background: none;
  text-decoration: none; transition: all .15s; display: inline-flex; align-items: center; justify-content: center;
}
.va-detail-link:hover { border-color: var(--color-primary); color: var(--color-primary); }

.va-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.va-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.va-pager button:disabled { opacity: .4; cursor: not-allowed; }

/* Modal générique */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box--lg { max-width: 520px; }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .35rem; }
.modal-box__desc { font-size: .85rem; color: var(--color-text); opacity: .6; margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; transition: opacity .15s; }
.modal-box__submit--neutral { background: #6b7280; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
