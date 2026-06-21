<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, Pencil, Archive, Phone, Mail } from 'lucide-vue-next'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import FilterTabs from '@/components/FilterTabs.vue'
import TablePagination from '@/components/TablePagination.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PhoneInput from '@/components/PhoneInput.vue'
import proprietaireService from '@/services/proprietaireService'

const router = useRouter()
const toast = ref(null)

const proprietaires = ref([])
const loading = ref(true)
const confirmId = ref(null)

const filtreStatut = ref('tous') // 'tous' | 'actif' | 'archive'
const STATUT_TABS = [
  { value: 'tous', label: 'Tous' },
  { value: 'actif', label: 'Actifs' },
  { value: 'archive', label: 'Archivés' },
]

const proprietairesFiltres = computed(() => {
  if (filtreStatut.value === 'actif') return proprietaires.value.filter(p => !p.isArchived)
  if (filtreStatut.value === 'archive') return proprietaires.value.filter(p => p.isArchived)
  return proprietaires.value
})

const currentPage = ref(1)
const PAGE_SIZE = 8
const totalPages = computed(() => Math.max(1, Math.ceil(proprietairesFiltres.value.length / PAGE_SIZE)))
const paginated = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return proprietairesFiltres.value.slice(start, start + PAGE_SIZE)
})

// Modal création / modification
const formModal = ref(false)
const formLoading = ref(false)
const editId = ref(null)
const form = ref({ nomComplet: '', telephone: '', email: '', adresse: '', notes: '' })

async function fetchProprietaires() {
  loading.value = true
  try {
    const res = await proprietaireService.getAll({ page: 0, size: 200 })
    proprietaires.value = res.data.data ?? []
  } catch {
    toast.value?.show('Erreur lors du chargement des propriétaires.', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(fetchProprietaires)

function openCreate() {
  editId.value = null
  form.value = { nomComplet: '', telephone: '', email: '', adresse: '', notes: '' }
  formModal.value = true
}

function openEdit(p) {
  editId.value = p.id
  form.value = {
    nomComplet: p.nomComplet,
    telephone: p.telephone,
    email: p.email || '',
    adresse: p.adresse || '',
    notes: p.notes || '',
  }
  formModal.value = true
}

async function saveForm() {
  if (!form.value.nomComplet.trim() || !form.value.telephone.trim()) {
    toast.value.show('Le nom et le téléphone sont obligatoires.', 'error')
    return
  }
  formLoading.value = true
  try {
    if (editId.value) {
      await proprietaireService.update(editId.value, form.value)
      toast.value.show('Propriétaire modifié avec succès.', 'success')
    } else {
      await proprietaireService.create(form.value)
      toast.value.show('Propriétaire créé avec succès.', 'success')
    }
    formModal.value = false
    await fetchProprietaires()
  } catch (err) {
    toast.value.show(err.response?.data?.message || 'Erreur lors de la sauvegarde.', 'error')
  } finally {
    formLoading.value = false
  }
}

async function archiver() {
  try {
    await proprietaireService.archive(confirmId.value)
    const idx = proprietaires.value.findIndex(p => p.id === confirmId.value)
    if (idx !== -1) proprietaires.value[idx].isArchived = true
    toast.value.show('Propriétaire archivé.', 'success')
  } catch (err) {
    toast.value.show(err.response?.data?.message || "Erreur lors de l'archivage.", 'error')
  } finally {
    confirmId.value = null
  }
}
</script>

<template>
  <div class="proprietaires-admin">
    <ToastNotification ref="toast" />
    <ConfirmModal
      v-if="confirmId"
      title="Archiver le propriétaire"
      message="Ce propriétaire n'apparaîtra plus dans le select de création d'annonce, mais son historique sera conservé."
      @confirm="archiver"
      @cancel="confirmId = null"
    />

    <!-- Modal création/modification -->
    <div v-if="formModal" class="modal-overlay" @click.self="formModal = false">
      <div class="modal-card">
        <h2 class="modal-card__title">{{ editId ? 'Modifier le propriétaire' : 'Ajouter un propriétaire' }}</h2>

        <div class="modal-form">
          <div class="field">
            <label class="field__label" for="prop-nom">NOM COMPLET <span class="req">*</span></label>
            <input id="prop-nom" v-model="form.nomComplet" type="text" class="field__input" placeholder="ex. Mamadou Diop" />
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field__label" for="prop-tel">TÉLÉPHONE <span class="req">*</span></label>
              <PhoneInput id="prop-tel" v-model="form.telephone" />
            </div>
            <div class="field">
              <label class="field__label" for="prop-email">EMAIL</label>
              <input id="prop-email" v-model="form.email" type="email" class="field__input" placeholder="ex. mamadou@mail.com" />
            </div>
          </div>
          <div class="field">
            <label class="field__label" for="prop-adresse">ADRESSE</label>
            <input id="prop-adresse" v-model="form.adresse" type="text" class="field__input" placeholder="ex. Sicap Liberté 6, Dakar" />
          </div>
          <div class="field">
            <label class="field__label" for="prop-notes">NOTES</label>
            <textarea id="prop-notes" v-model="form.notes" class="field__input field__textarea" rows="3"></textarea>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="formModal = false">Annuler</button>
          <button class="btn-save" :disabled="formLoading" @click="saveForm">
            {{ formLoading ? 'Sauvegarde...' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>

    <!-- En-tête -->
    <div class="page-header">
      <div>
        <h1 class="page-header__title">Propriétaires</h1>
        <p class="page-header__subtitle">Gérez les propriétaires des biens immobiliers de la plateforme.</p>
      </div>
      <div class="page-header__actions">
        <FilterTabs v-model="filtreStatut" :tabs="STATUT_TABS" />
        <button class="btn-add" @click="openCreate">
          + Ajouter un propriétaire
        </button>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="skeleton-table">
      <div v-for="i in 5" :key="i" class="skeleton-row"></div>
    </div>

    <!-- Table Card -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Nom</th>
              <th>Contact</th>
              <th>Biens</th>
              <th>Annonces</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in paginated" :key="p.id" class="data-table__row">
              <td class="table-prop-name">{{ p.nomComplet }}</td>
              <td>
                <p class="contact-row"><Phone :size="12" /> {{ p.telephone }}</p>
                <p v-if="p.email" class="contact-row"><Mail :size="12" /> {{ p.email }}</p>
              </td>
              <td class="td-center">{{ p.statistiques?.totalBiens ?? 0 }}</td>
              <td class="td-center">{{ p.statistiques?.annoncesActives ?? 0 }}</td>
              <td>
                <StatusBadge :label="p.isArchived ? 'Archivé' : 'Actif'" :variant="p.isArchived ? 'neutral' : 'success'" />
              </td>
              <td>
                <div class="td-actions">
                  <button class="action-btn" title="Voir détail" @click="router.push(`/admin/proprietaires/${p.id}`)">
                    <Eye :size="16" />
                  </button>
                  <button class="action-btn" title="Modifier" @click="openEdit(p)">
                    <Pencil :size="16" />
                  </button>
                  <button v-if="!p.isArchived" class="action-btn danger" title="Archiver" @click="confirmId = p.id">
                    <Archive :size="16" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="paginated.length === 0">
              <td colspan="6" class="empty-state">
                <p>Aucun propriétaire trouvé.</p>
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
.proprietaires-admin { display: flex; flex-direction: column; gap: 1.5rem; }

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

.table-prop-name { font-weight: 600; color: var(--color-text); }
.contact-row { display: flex; align-items: center; gap: 0.35rem; font-size: 0.8rem; color: var(--color-text); opacity: 0.7; }
.contact-row + .contact-row { margin-top: 0.2rem; }
.td-center { text-align: center; }

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
