<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import { usePagination } from '@/composables/usePagination'
import { MOCK_ANNONCES } from '@/mocks/annonces'
import api from '@/services/api'

const router = useRouter()
const toast = ref(null)

const annonces = ref([])
const loading = ref(true)
const confirmId = ref(null)

const { currentPage, totalPages, paginated, rangeLabel, prev, next } = usePagination(annonces, 6)

function formatPrix(p) {
  return new Intl.NumberFormat('fr-FR').format(p) + ' FCFA'
}

function formatDate(d) {
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(async () => {
  try {
    const res = await api.get('/annonces')
    annonces.value = res.data
  } catch (err) {
    console.warn('[AnnoncesView] Backend indisponible, utilisation des mocks.', err)
    annonces.value = MOCK_ANNONCES.map(a => ({
      id: a.id,
      titre: a.libelle,
      quartier: a.quartier,
      typeBien: a.typeBien,
      prix: a.prix,
      statut: a.statut,
      dateCreation: a.dateCreation,
    }))
  } finally {
    loading.value = false
  }
})

async function archiver() {
  try {
    await api.delete(`/annonces/${confirmId.value}`)
    annonces.value = annonces.value.filter(a => a.id !== confirmId.value)
    toast.value.show('Annonce archivée.', 'success')
  } catch (err) {
    console.error('[AnnoncesView] Erreur archivage.', err)
    toast.value.show("Erreur lors de l'archivage.", 'error')
  } finally {
    confirmId.value = null
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
      <button class="btn-add" @click="router.push('/admin/annonces/publier')">
        + Publier une annonce
      </button>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="skeleton-table">
      <div v-for="i in 5" :key="i" class="skeleton-row"></div>
    </div>

    <!-- Table -->
    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Titre</th>
            <th>Quartier</th>
            <th>Type</th>
            <th>Prix</th>
            <th>Date</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in paginated" :key="a.id" class="data-table__row">
            <td class="td-title">{{ a.titre }}</td>
            <td class="td-muted">{{ a.quartier }}</td>
            <td class="td-muted">{{ a.typeBien }}</td>
            <td class="td-muted">{{ formatPrix(a.prix) }}</td>
            <td class="td-muted">{{ formatDate(a.dateCreation) }}</td>
            <td>
              <span class="badge" :class="{
                'badge--active':  a.statut === 'ACTIF',
                'badge--neutral': a.statut === 'INACTIF',
                'badge--pending': a.statut === 'EN_ATTENTE',
              }">
                {{ a.statut === 'EN_ATTENTE' ? 'En attente' : a.statut === 'ACTIF' ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td>
              <div class="td-actions">
                <button class="action-btn" title="Archiver" @click="confirmId = a.id">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="21 8 21 21 3 21 3 8"/><rect x="1" y="3" width="22" height="5"/><line x1="10" y1="12" x2="14" y2="12"/></svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="paginated.length === 0">
            <td colspan="7" class="empty-state">
              <div class="empty-state__content">
                <span>🏠</span>
                <p>Aucune annonce trouvée.</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="table-footer">
        <span class="table-footer__info">{{ rangeLabel }} annonces</span>
        <div class="pagination">
          <button class="page-btn" :disabled="currentPage === 1" @click="prev()">Précédent</button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="next()">Suivant</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.annonces-admin { display: flex; flex-direction: column; gap: 1.75rem; }

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.page-header__subtitle { font-size: 0.88rem; color: var(--color-text-muted); margin-top: 0.25rem; }

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
  background: linear-gradient(90deg, var(--color-hover-row) 25%, var(--color-border-solid) 50%, var(--color-hover-row) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: var(--radius-sm);
}
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Table */
.table-wrap {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.data-table { width: 100%; border-collapse: collapse; font-size: 0.88rem; }

.data-table thead tr { background: var(--color-background); border-bottom: 1px solid var(--color-border-solid); }

.data-table th {
  padding: 0.85rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.data-table__row {
  border-bottom: 1px solid var(--color-hover-row);
  transition: background 0.15s;
}
.data-table__row:hover { background: var(--color-hover-row); }
.data-table__row:last-child { border-bottom: none; }

.data-table td { padding: 0.9rem 1rem; vertical-align: middle; }
.td-title { font-weight: 600; color: var(--color-text); }
.td-muted { color: var(--color-text-muted); font-size: 0.85rem; }

.badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge--active  { background: var(--badge-active-bg);  color: var(--badge-active-color); }
.badge--neutral { background: var(--badge-neutral-bg); color: var(--badge-neutral-color); }
.badge--pending { background: var(--badge-pending-bg); color: var(--badge-pending-color); }

.td-actions { display: flex; gap: 0.5rem; }

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid var(--color-border-solid);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.action-btn svg { width: 15px; height: 15px; stroke: var(--color-text-muted); }
.action-btn:hover { background: var(--badge-pending-bg); border-color: var(--color-accent); }
.action-btn:hover svg { stroke: var(--color-accent); }

.empty-state { text-align: center; padding: 3rem !important; }
.empty-state__content { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; color: var(--color-text-muted); font-size: 1.5rem; }

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-top: 1px solid var(--color-border-solid);
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.pagination { display: flex; gap: 0.5rem; }

.page-btn {
  padding: 0.4rem 0.9rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border-solid);
  background: var(--color-card);
  font-size: 0.82rem;
  color: var(--color-text);
  transition: background 0.15s;
}
.page-btn:hover:not(:disabled) { background: var(--color-hover-row); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
