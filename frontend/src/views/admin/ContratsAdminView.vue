<script setup>
import { ref, computed, onMounted } from 'vue'
import { Eye } from 'lucide-vue-next'
import contratService from '@/services/contratService'
import FilterSelect from '@/components/FilterSelect.vue'
import TableTabs from '@/components/TableTabs.vue'
import TablePagination from '@/components/TablePagination.vue'
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

// TablePagination est 1-indexée (comme TypeBienView) — la pagination serveur est 0-indexée
const currentPageDisplay = computed(() => currentPage.value + 1)

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

function onPageChange(page) {
  fetchContrats(page - 1)
}

function onTypeChange(v) {
  filtreType.value = v
  fetchContrats(0)
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
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchContrats(0) }"
      />
    </div>

    <div class="ca-card">
      <TableTabs :model-value="filtreType" :tabs="TYPE_TABS" @update:model-value="onTypeChange" />

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
                    <Eye :size="16" />
                  </RouterLink>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <TablePagination :current-page="currentPageDisplay" :total-pages="totalPages" @update:current-page="onPageChange" />
    </div>
  </div>
</template>

<style scoped>
.ca-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
.ca-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; }
.ca-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.ca-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }

.ca-card { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }

.ca-loading { display: flex; justify-content: center; padding: 4rem; }
.ca-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }
.ca-table-wrap { overflow-x: auto; }
.ca-table { width: 100%; border-collapse: collapse; }
.ca-table th { padding: .85rem 1.1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.ca-table td { padding: 1rem 1.1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.ca-table tr:last-child td { border-bottom: none; }
.ca-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.ca-td-sub { font-size: .75rem; opacity: .5; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }
.badge-type__duree { font-size: .68rem; font-weight: 500; opacity: .75; }

.td-actions { display: flex; gap: 0.5rem; }
.action-btn {
  width: 32px; height: 32px; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  color: var(--color-text); background: none;
  text-decoration: none; transition: all .15s; display: inline-flex; align-items: center; justify-content: center;
}
.action-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
