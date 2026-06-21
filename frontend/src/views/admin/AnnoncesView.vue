<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, Pencil, Archive, RotateCcw, Phone } from 'lucide-vue-next'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import FilterTabs from '@/components/FilterTabs.vue'
import TableTabs from '@/components/TableTabs.vue'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import annonceService from '@/services/annonceService'
import StatusBadge from '@/components/StatusBadge.vue'

const router = useRouter()
const toast = ref(null)

const annonces = ref([])
const loading = ref(true)
const confirmId = ref(null)

// Onglet vente/location — intégré à la table
const filtreType = ref('')
const TYPE_TABS = [
  { value: '', label: 'Tous' },
  { value: 'VENTE', label: 'Vente' },
  { value: 'LOCATION', label: 'Location' },
]

// Filtre actif/archivé (conservé, hors onglets table)
const filtreStatut = ref('tous') // 'tous' | 'actif' | 'archive'

const annoncesFiltrees = computed(() => {
  let liste = annonces.value
  if (filtreStatut.value === 'actif')   liste = liste.filter(a => !a.archived)
  if (filtreStatut.value === 'archive') liste = liste.filter(a => a.archived)
  if (filtreType.value)                 liste = liste.filter(a => a.typeTransaction === filtreType.value)
  return liste
})

const { currentPage, totalPages, paginated, reset } = usePagination(annoncesFiltrees, 8)

watch([filtreStatut, filtreType], () => reset())

function formatPrix(p) {
  return new Intl.NumberFormat('fr-FR').format(p) + ' FCFA'
}

function formatDate(d) {
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(async () => {
  try {
    const res = await annonceService.getAllAnnoncesAdmin()
    annonces.value = res.data.data
  } catch {
    toast.value?.show('Erreur lors du chargement des annonces.', 'error')
  } finally {
    loading.value = false
  }
})

// Archiver une annonce — met à jour l'état local sans retirer de la liste
async function archiver() {
  try {
    await annonceService.archiveAnnonce(confirmId.value)
    const idx = annonces.value.findIndex(a => a.id === confirmId.value)
    if (idx !== -1) annonces.value[idx].archived = true
    toast.value.show('Annonce archivée.', 'success')
  } catch (err) {
    toast.value.show(err.response?.data?.message || "Erreur lors de l'archivage.", 'error')
  } finally {
    confirmId.value = null
  }
}

// Restaurer une annonce archivée — PATCH /api/v1/annonces/{id}/restore
async function restaurer(id) {
  try {
    await annonceService.restoreAnnonce(id)
    const idx = annonces.value.findIndex(a => a.id === id)
    if (idx !== -1) annonces.value[idx].archived = false
    toast.value.show('Annonce restaurée avec succès.', 'success')
  } catch (err) {
    toast.value.show(err.response?.data?.message || 'Erreur lors de la restauration.', 'error')
  }
}
</script>

<template>
  <div class="annonces-admin">
    <ToastNotification ref="toast" />
    <ConfirmModal
      v-if="confirmId"
      title="Archiver l'annonce"
      message="Cette annonce sera archivée et ne sera plus visible par les clients."
      @confirm="archiver"
      @cancel="confirmId = null"
    />

    <!-- En-tête -->
    <div class="page-header">
      <div>
        <h1 class="page-header__title">Annonces</h1>
        <p class="page-header__subtitle">Gérez toutes les annonces de la plateforme.</p>
      </div>
      <div class="page-header__actions">
        <FilterTabs
          v-model="filtreStatut"
          :tabs="[
            { value: 'tous', label: 'Toutes' },
            { value: 'actif', label: 'Actives' },
            { value: 'archive', label: 'Archivées' },
          ]"
        />
        <button class="btn-add" @click="router.push('/admin/annonces/publier')">
          + Publier une annonce
        </button>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="skeleton-table">
      <div v-for="i in 5" :key="i" class="skeleton-row"></div>
    </div>

    <!-- Table Card avec onglets intégrés -->
    <div v-else class="table-card">
      <TableTabs v-model="filtreType" :tabs="TYPE_TABS" />

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Libellé</th>
              <th>Adresse</th>
              <th>Transaction</th>
              <th>Propriétaire</th>
              <th>Prix</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in paginated" :key="a.id" class="data-table__row">
              <td class="table-annonce-title">
                {{ a.libelle }}
                <p class="td-sub">{{ a.typeBien?.libelle }} · {{ formatDate(a.createdAt) }}</p>
              </td>
              <td class="td-muted">{{ a.adresse }}</td>
              <td class="td-type">
                <span v-if="a.typeTransaction" :class="['badge-type', a.typeTransaction === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                  {{ a.typeTransaction === 'VENTE' ? 'Vente' : 'Location' }}
                </span>
                <span v-else class="td-muted">–</span>
              </td>
              <td>
                <template v-if="a.owner">
                  <p class="owner-name">{{ a.owner.nomComplet }}</p>
                  <p class="owner-contact"><Phone :size="11" /> {{ a.owner.telephone }}</p>
                </template>
                <span v-else class="td-muted">—</span>
              </td>
              <td class="table-price">{{ formatPrix(a.prix) }}</td>
              <td>
                <StatusBadge :label="a.archived ? 'Archivé' : 'Actif'" :variant="a.archived ? 'neutral' : 'success'" />
              </td>
              <td>
                <div class="td-actions">
                  <button class="action-btn" title="Voir détail" @click="router.push(`/admin/annonces/${a.id}`)">
                    <Eye :size="16" />
                  </button>
                  <button class="action-btn" title="Modifier" @click="router.push(`/admin/annonces/${a.id}/modifier`)">
                    <Pencil :size="16" />
                  </button>
                  <button v-if="a.archived" class="action-btn" title="Restaurer" @click="restaurer(a.id)">
                    <RotateCcw :size="16" />
                  </button>
                  <button v-else class="action-btn danger" title="Archiver" @click="confirmId = a.id">
                    <Archive :size="16" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="paginated.length === 0">
              <td colspan="7" class="empty-state">
                <p>Aucune annonce trouvée.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <TablePagination v-model:current-page="currentPage" :total-pages="totalPages" />
    </div>
  </div>
</template>

<style scoped>
.annonces-admin { display: flex; flex-direction: column; gap: 1.5rem; }

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.page-header__subtitle { font-size: 0.88rem; color: var(--color-text); opacity: 0.6; margin-top: 0.25rem; }

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn-add {
  background: var(--color-primary);
  color: #fff;
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-add:hover { background: var(--color-primary-hover); }

/* Skeleton */
.skeleton-table { display: flex; flex-direction: column; gap: 0.75rem; }
.skeleton-row {
  height: 52px;
  background: linear-gradient(90deg, var(--color-background) 25%, var(--color-border) 50%, var(--color-background) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: var(--radius-sm);
}
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Table Card */
.table-card {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.table-wrap { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 0.88rem; }
.data-table thead tr { background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.data-table th {
  padding: 0.9rem 1.25rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text);
  opacity: 0.6;
}

.data-table__row { border-bottom: 1px solid var(--color-border); transition: background 0.15s; }
.data-table__row:hover { background: var(--color-background); }
.data-table__row:last-child { border-bottom: none; }
.data-table td { padding: 1.1rem 1.25rem; vertical-align: middle; }

.table-annonce-title { font-weight: 600; color: var(--color-text); }
.td-sub { font-size: 0.75rem; color: var(--color-text); opacity: 0.5; margin-top: 0.2rem; font-weight: 400; }
.td-muted { color: var(--color-text); opacity: 0.6; font-size: 0.85rem; }
.td-type { white-space: nowrap; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }

.owner-name { font-size: 0.85rem; font-weight: 600; color: var(--color-text); }
.owner-contact { display: flex; align-items: center; gap: 0.3rem; font-size: 0.75rem; color: var(--color-text); opacity: 0.6; margin-top: 0.15rem; }

.empty-state { text-align: center; padding: 3rem !important; color: var(--color-text); opacity: 0.5; }

.td-actions { display: flex; gap: 0.5rem; }
.action-btn {
  width: 32px; height: 32px; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  color: var(--color-text); background: none;
  display: inline-flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all .15s;
}
.action-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.action-btn.danger:hover { border-color: var(--color-accent); color: var(--color-accent); }
</style>
