<script setup>
import { ref, onMounted } from 'vue'
import contratService from '@/services/contratService'
import FilterSelect from '@/components/FilterSelect.vue'
import FilterTabs from '@/components/FilterTabs.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const contrats    = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')
const filtreType    = ref('')

const TYPE_TABS = [
  { value: '', label: 'Tous' },
  { value: 'VENTE', label: 'Vente' },
  { value: 'LOCATION', label: 'Location' },
]

const STATUTS       = ['', 'EN_ATTENTE', 'ACTIF', 'EXPIRE', 'RESILIE', 'EN_ATTENTE_RESILIATION', 'PROLONGATION_EN_ATTENTE']
const STATUT_LABELS = {
  EN_ATTENTE:              'Pré-contrat',
  ACTIF:                   'Actif',
  EXPIRE:                  'Expiré',
  RESILIE:                 'Résilié',
  EN_ATTENTE_RESILIATION:  'Résiliation en attente',
  PROLONGATION_EN_ATTENTE: 'Prolongation en attente',
}
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les statuts' }))
const STATUT_VARIANTS = {
  EN_ATTENTE:              'warning',
  ACTIF:                   'success',
  EXPIRE:                  'neutral',
  RESILIE:                 'danger',
  EN_ATTENTE_RESILIATION:  'warning',
  PROLONGATION_EN_ATTENTE: 'warning',
}

async function fetchContrats(page = 0) {
  loading.value = true
  try {
    const res = await contratService.getAllContrats(page, 20, filtreStatut.value || null, filtreType.value || null)
    contrats.value    = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch (err) { console.error('[ContratsAdminView] fetchContrats error:', err?.response?.status, err?.message); contrats.value = [] }
  finally { loading.value = false }
}

function formatMontant(v) { return new Intl.NumberFormat('fr-SN').format(v) + ' FCFA' }

onMounted(() => fetchContrats(0))
</script>

<template>
  <div class="ca-page">
    <div class="ca-toolbar">
      <div>
        <h1 class="ca-toolbar__title">Contrats</h1>
        <p class="ca-toolbar__count">{{ totalItems }} contrat{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <div class="ca-filters">
        <FilterTabs
          :model-value="filtreType"
          :tabs="TYPE_TABS"
          @update:model-value="(v) => { filtreType = v; fetchContrats(0) }"
        />
        <FilterSelect
          :model-value="filtreStatut"
          :options="filterOptions"
          @update:model-value="(v) => { filtreStatut = v; fetchContrats(0) }"
        />
      </div>
    </div>

    <div v-if="loading" class="ca-loading"><div class="spinner"></div></div>
    <div v-else-if="!contrats.length" class="ca-empty">Aucun contrat.</div>

    <div v-else class="ca-table-wrap">
      <table class="ca-table">
        <thead>
          <tr>
            <th>Client</th><th>Annonce</th><th>Type</th><th>Montant</th>
            <th>Statut</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in contrats" :key="c.id">
            <td class="table-client">
              {{ c.clientNom || c.prospectNom || '–' }}
              <span v-if="!c.clientNom && c.prospectNom" class="ca-td-sub">(prospect)</span>
            </td>
            <td>
              <RouterLink :to="`/admin/annonces/${c.annonceId}`" class="ca-td-link">{{ c.annonceLibelle }}</RouterLink>
              <p class="ca-td-sub">{{ c.annonceAdresse }}</p>
            </td>
            <td>
              <span v-if="c.typeContrat" :class="['badge-type', c.typeContrat === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                {{ c.typeContrat === 'VENTE' ? 'Vente' : 'Location' }}
                <span v-if="c.typeContrat === 'LOCATION' && c.dureeLocationMois" class="badge-type__duree">{{ c.dureeLocationMois }} mois</span>
              </span>
              <span v-else class="ca-td-sub">–</span>
            </td>
            <td class="table-price">{{ formatMontant(c.montant) }}</td>
            <td><StatusBadge :label="STATUT_LABELS[c.statut]" :variant="STATUT_VARIANTS[c.statut]" /></td>
            <td>
              <div class="td-actions">
                <RouterLink :to="`/admin/contrats/${c.id}`" class="action-btn" title="Voir le détail">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </RouterLink>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="ca-pager">
      <button :disabled="currentPage === 0" @click="fetchContrats(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchContrats(currentPage + 1)">Suivant →</button>
    </div>

  </div>
</template>

<style scoped>
.ca-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.ca-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.ca-filters { display: flex; gap: .6rem; flex-wrap: wrap; }
.ca-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.ca-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.ca-loading { display: flex; justify-content: center; padding: 4rem; }
.ca-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }
.ca-table-wrap { overflow-x: auto; }
.ca-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.ca-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.ca-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.ca-table tr:last-child td { border-bottom: none; }
.ca-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.ca-td-sub { font-size: .75rem; opacity: .5; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }
.badge-type__duree { font-size: .68rem; font-weight: 500; opacity: .75; }
.ca-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.ca-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.ca-pager button:disabled { opacity: .4; cursor: not-allowed; }
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
