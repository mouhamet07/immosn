<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import FilterTabs from '@/components/FilterTabs.vue'
import { usePagination } from '@/composables/usePagination'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'
import StatusBadge from '@/components/StatusBadge.vue'

const router = useRouter()
const toast = ref(null)

const annonces = ref([])
const loading = ref(true)
const confirmId = ref(null)

// Filtre actif/archivé/tous
const filtreStatut = ref('tous') // 'tous' | 'actif' | 'archive'
const STATUT_TABS = [
  { value: 'tous', label: 'Toutes' },
  { value: 'actif', label: 'Actives' },
  { value: 'archive', label: 'Archivées' },
]

// Filtre vente/location/toutes
const filtreType = ref('')
const TYPE_TABS = [
  { value: '', label: 'Toutes' },
  { value: 'VENTE', label: 'Vente' },
  { value: 'LOCATION', label: 'Location' },
]

const annoncesFiltrees = computed(() => {
  let liste = annonces.value
  if (filtreStatut.value === 'actif')   liste = liste.filter(a => !a.archived)
  if (filtreStatut.value === 'archive') liste = liste.filter(a => a.archived)
  if (filtreType.value)                 liste = liste.filter(a => a.typeTransaction === filtreType.value)
  return liste
})

// Types de biens chargés pour le select dans la modal
const typesBien = ref([])

// Modal de modification
const editModal = ref(false)
const editLoading = ref(false)
const editForm = ref({
  id: null,
  libelle: '',
  description: '',
  nbrPieces: '',
  surface: '',
  prix: '',
  adresse: '',
  typeBienId: null,
  commoditeIds: [],
  images: [],
})

const { currentPage, totalPages, paginated, rangeLabel, prev, next, reset } = usePagination(annoncesFiltrees, 6)

watch([filtreStatut, filtreType], () => reset())

function formatPrix(p) {
  return new Intl.NumberFormat('fr-FR').format(p) + ' FCFA'
}

function formatDate(d) {
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(async () => {
  try {
    const [resAnnonces, resTypes] = await Promise.all([
      annonceService.getAllAnnoncesAdmin(),
      typeBienService.getAllTypesBien(),
    ])
    annonces.value = resAnnonces.data.data
    typesBien.value = resTypes.data.data ?? []
  } catch {
    toast.value?.show('Erreur lors du chargement des annonces.', 'error')
  } finally {
    loading.value = false
  }
})

// Ouvrir modal de modification avec les données de l'annonce
function ouvrirModification(annonce) {
  editForm.value = {
    id: annonce.id,
    libelle: annonce.libelle,
    description: annonce.description || '',
    nbrPieces: annonce.nbrPieces,
    surface: annonce.surface,
    prix: annonce.prix,
    adresse: annonce.adresse,
    typeBienId: annonce.typeBien?.id || null,
    commoditeIds: annonce.commodites?.map(c => c.id) || [],
    images: annonce.images || [],
  }
  editModal.value = true
}

// Modifier une annonce — PUT /api/v1/annonces/{id}
async function sauvegarderModification() {
  editLoading.value = true
  try {
    const res = await annonceService.updateAnnonce(editForm.value.id, {
      libelle:      editForm.value.libelle,
      description:  editForm.value.description,
      nbrPieces:    parseInt(editForm.value.nbrPieces),
      surface:      parseFloat(editForm.value.surface),
      prix:         parseFloat(editForm.value.prix),
      adresse:      editForm.value.adresse,
      typeBienId:   editForm.value.typeBienId,
      commoditeIds: editForm.value.commoditeIds,
      images:       editForm.value.images,
    })
    // Mettre à jour localement — réponse: RestResponse<AnnonceResponseDto>
    const updated = res.data.data
    const idx = annonces.value.findIndex(a => a.id === updated.id)
    if (idx !== -1) annonces.value[idx] = updated
    toast.value.show('Annonce modifiée avec succès.', 'success')
    editModal.value = false
  } catch (err) {
    toast.value.show(err.response?.data?.message || 'Erreur lors de la modification.', 'error')
  } finally {
    editLoading.value = false
  }
}

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

    <!-- Modal de modification -->
    <div v-if="editModal" class="modal-overlay" @click.self="editModal = false">
      <div class="modal-card">
        <h2 class="modal-card__title">Modifier l'annonce</h2>

        <div class="modal-form">
          <div class="field">
            <label class="field__label">LIBELLÉ <span class="req">*</span></label>
            <input v-model="editForm.libelle" type="text" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">DESCRIPTION</label>
            <textarea v-model="editForm.description" class="field__input field__textarea" rows="3"></textarea>
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field__label">PRIX (FCFA)</label>
              <input v-model="editForm.prix" type="number" class="field__input" />
            </div>
            <div class="field">
              <label class="field__label">SURFACE (m²)</label>
              <input v-model="editForm.surface" type="number" class="field__input" />
            </div>
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field__label">NOMBRE DE PIÈCES</label>
              <input v-model="editForm.nbrPieces" type="number" class="field__input" />
            </div>
            <div class="field">
              <label class="field__label">TYPE DE BIEN <span class="req">*</span></label>
              <select v-model="editForm.typeBienId" class="field__input">
                <option :value="null">Sélectionner un type...</option>
                <option v-for="type in typesBien" :key="type.id" :value="type.id">
                  {{ type.libelle }}
                </option>
              </select>
            </div>
          </div>
          <div class="field">
            <label class="field__label">ADRESSE</label>
            <input v-model="editForm.adresse" type="text" class="field__input" />
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="editModal = false">Annuler</button>
          <button class="btn-save" :disabled="editLoading" @click="sauvegarderModification">
            {{ editLoading ? 'Sauvegarde...' : 'Sauvegarder' }}
          </button>
        </div>
      </div>
    </div>

    <!-- En-tête -->
    <div class="page-header">
      <div>
        <h1 class="page-header__title">Annonces</h1>
        <p class="page-header__subtitle">Gérez toutes les annonces de la plateforme.</p>
      </div>
      <div class="page-header__actions">
        <FilterTabs v-model="filtreType" :tabs="TYPE_TABS" />
        <FilterTabs v-model="filtreStatut" :tabs="STATUT_TABS" />
        <button class="btn-add" @click="router.push('/admin/annonces/publier')">
          + Publier une annonce
        </button>
      </div>
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
            <th>Libellé</th>
            <th>Adresse</th>
            <th>Type</th>
            <th>Transaction</th>
            <th>Prix</th>
            <th>Date</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in paginated" :key="a.id" class="data-table__row">
            <td class="table-annonce-title">{{ a.libelle }}</td>
            <td class="td-muted">{{ a.adresse }}</td>
            <td class="td-muted">{{ a.typeBien?.libelle || '–' }}</td>
            <td class="td-type">
              <span v-if="a.typeTransaction" :class="['badge-type', a.typeTransaction === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                {{ a.typeTransaction === 'VENTE' ? 'Vente' : 'Location' }}
              </span>
              <span v-else class="td-muted">–</span>
            </td>
            <td class="table-price">{{ formatPrix(a.prix) }}</td>
            <td class="table-date">{{ formatDate(a.createdAt) }}</td>
            <td>
              <StatusBadge :label="a.archived ? 'Archivé' : 'Actif'" :variant="a.archived ? 'neutral' : 'success'" />
            </td>
            <td>
              <div class="td-actions">
                <button class="action-btn" title="Voir détail" @click="router.push(`/admin/annonces/${a.id}`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
                <button class="action-btn" title="Modifier" @click="router.push(`/admin/annonces/${a.id}/modifier`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button v-if="a.archived" class="action-btn" title="Restaurer" @click="restaurer(a.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-4.5"/></svg>
                </button>
                <button v-else class="action-btn danger" title="Archiver" @click="confirmId = a.id">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="21 8 21 21 3 21 3 8"/><rect x="1" y="3" width="22" height="5"/><line x1="10" y1="12" x2="14" y2="12"/></svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="paginated.length === 0">
            <td colspan="8" class="empty-state">
              <div class="empty-state__content">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40" style="color:#9ca3af"><path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-4h6v4"/></svg>
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

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2rem;
  width: 100%;
  max-width: 560px;
  box-shadow: var(--shadow-card-hover);
}

.modal-card__title {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-text);
  margin-bottom: 1.5rem;
}

.modal-form { display: flex; flex-direction: column; gap: 1rem; }

.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }

.field__label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text);
}

.req { color: var(--color-accent); }

.field__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color 0.2s;
  width: 100%;
}
.field__input:focus { border-color: var(--color-primary); outline: none; }
.field__textarea { resize: vertical; min-height: 80px; }

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

.btn-cancel {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-border);
  background: transparent;
  color: var(--color-text);
  font-size: 0.88rem;
  font-weight: 600;
}

.btn-save {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  transition: background 0.15s;
}
.btn-save:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }

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

/* Table */
.table-wrap {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.data-table { width: 100%; border-collapse: collapse; font-size: 0.88rem; }
.data-table thead tr { background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.data-table th {
  padding: 0.85rem 1rem;
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
.data-table td { padding: 0.9rem 1rem; vertical-align: middle; }
.td-muted { color: var(--color-text); opacity: 0.6; font-size: 0.85rem; }
.td-type { width: 1%; white-space: nowrap; text-align: center; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }

.empty-state { text-align: center; padding: 3rem !important; }
.empty-state__content { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; color: var(--color-text); opacity: 0.5; font-size: 1.5rem; }

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.6;
}

.pagination { display: flex; gap: 0.5rem; }

.page-btn {
  padding: 0.4rem 0.9rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-card);
  font-size: 0.82rem;
  color: var(--color-text);
  transition: background 0.15s;
}
.page-btn:hover:not(:disabled) { background: var(--color-background); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
