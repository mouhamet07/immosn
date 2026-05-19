<script setup>
import { ref, onMounted } from 'vue'
import commoditeService from '@/services/commoditeService'

const items       = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const PAGE_SIZE   = 10

// Modal création / édition
const showModal    = ref(false)
const editId       = ref(null)
const modalLibelle = ref('')
const saving       = ref(false)

// Modal confirmation suppression
const showConfirm = ref(false)
const deleteId    = ref(null)
const deleting    = ref(false)

// Toast inline
const toast = ref({ show: false, message: '', type: 'success' })
function notify(message, type = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function fetchItems(page = 0) {
  loading.value = true
  try {
    const res = await commoditeService.getAllCommoditesPaged({ page, size: PAGE_SIZE })
    items.value       = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch {
    items.value = []
    notify('Erreur lors du chargement.', 'error')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value       = null
  modalLibelle.value = ''
  showModal.value    = true
}

function openEdit(item) {
  editId.value       = item.id
  modalLibelle.value = item.libelle
  showModal.value    = true
}

async function saveModal() {
  if (!modalLibelle.value.trim()) return
  saving.value = true
  try {
    if (editId.value) {
      await commoditeService.updateCommodite(editId.value, modalLibelle.value.trim())
      notify('Commodité modifiée.')
    } else {
      await commoditeService.createCommodite(modalLibelle.value.trim())
      notify('Commodité créée.')
    }
    showModal.value = false
    await fetchItems(currentPage.value)
  } catch (err) {
    notify(err.response?.data?.message || 'Erreur lors de la sauvegarde.', 'error')
  } finally {
    saving.value = false
  }
}

function confirmDelete(id) {
  deleteId.value    = id
  showConfirm.value = true
}

async function doDelete() {
  deleting.value = true
  try {
    await commoditeService.archiveCommodite(deleteId.value)
    notify('Commodité archivée.')
    showConfirm.value = false
    await fetchItems(currentPage.value)
  } catch (err) {
    notify(err.response?.data?.message || 'Erreur lors de la suppression.', 'error')
  } finally {
    deleting.value = false
  }
}

onMounted(() => fetchItems(0))
</script>

<template>
  <div class="cm-page">
    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" class="cm-toast" :class="toast.type === 'error' ? 'cm-toast--error' : 'cm-toast--success'">
        {{ toast.message }}
      </div>
    </transition>

    <!-- Toolbar -->
    <div class="cm-toolbar">
      <div>
        <h1 class="cm-toolbar__title">Commodités</h1>
        <p class="cm-toolbar__count">{{ totalItems }} commodité{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <button class="btn-primary" @click="openCreate">+ Nouvelle commodité</button>
    </div>

    <!-- Tableau -->
    <div class="cm-card">
      <div v-if="loading" class="cm-loading"><div class="spinner"></div></div>
      <div v-else-if="!items.length" class="cm-empty">Aucune commodité enregistrée.</div>

      <table v-else class="cm-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Libellé</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td class="cm-id">{{ item.id }}</td>
            <td class="cm-label">{{ item.libelle }}</td>
            <td>
              <span class="badge" :class="item.isArchived ? 'badge--neutral' : 'badge--success'">
                {{ item.isArchived ? 'Archivée' : 'Active' }}
              </span>
            </td>
            <td class="cm-actions">
              <button class="btn-edit" @click="openEdit(item)">Modifier</button>
              <button class="btn-delete" @click="confirmDelete(item.id)">Archiver</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="cm-pagination">
        <button :disabled="currentPage === 0" @click="fetchItems(currentPage - 1)">‹ Préc.</button>
        <span>Page {{ currentPage + 1 }} / {{ totalPages }}</span>
        <button :disabled="currentPage >= totalPages - 1" @click="fetchItems(currentPage + 1)">Suiv. ›</button>
      </div>
    </div>

    <!-- Modal création / édition -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h2 class="modal__title">{{ editId ? 'Modifier' : 'Nouvelle' }} commodité</h2>
        <div class="field">
          <label class="field__label">Libellé <span class="req">*</span></label>
          <input
            v-model="modalLibelle"
            type="text"
            class="field__input"
            placeholder="ex. Piscine, Garage, Climatisation..."
            @keyup.enter="saveModal"
          />
        </div>
        <div class="modal__actions">
          <button class="btn-cancel" @click="showModal = false">Annuler</button>
          <button class="btn-primary" :disabled="saving || !modalLibelle.trim()" @click="saveModal">
            {{ saving ? 'Sauvegarde...' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal confirmation suppression -->
    <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
      <div class="modal">
        <h2 class="modal__title">Archiver cette commodité ?</h2>
        <p class="modal__body">Cette action est réversible. La commodité ne sera plus proposée lors de la création d'annonces.</p>
        <div class="modal__actions">
          <button class="btn-cancel" @click="showConfirm = false">Annuler</button>
          <button class="btn-delete" :disabled="deleting" @click="doDelete">
            {{ deleting ? 'Archivage...' : 'Confirmer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cm-page { display: flex; flex-direction: column; gap: 1.5rem; position: relative; }

/* Toast */
.cm-toast {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 200;
  box-shadow: 0 4px 16px rgba(0,0,0,.15);
}
.cm-toast--success { background: #22c55e; color: #fff; }
.cm-toast--error   { background: #ef4444; color: #fff; }
.fade-enter-active, .fade-leave-active { transition: opacity .25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Toolbar */
.cm-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
.cm-toolbar__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.cm-toolbar__count { font-size: 0.85rem; color: #6b7280; margin-top: 0.2rem; }

/* Card */
.cm-card {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

/* Table */
.cm-table { width: 100%; border-collapse: collapse; }
.cm-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .05em;
  color: #6b7280;
  background: var(--color-background);
  border-bottom: 1px solid var(--color-border);
}
.cm-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
  color: var(--color-text);
}
.cm-table tbody tr:last-child td { border-bottom: none; }
.cm-table tbody tr:hover { background: var(--color-hover-row); }
.cm-id    { color: #9ca3af; font-size: 0.8rem; }
.cm-label { font-weight: 600; }
.cm-actions { display: flex; gap: 0.5rem; }

/* Badges */
.badge { padding: 0.25rem 0.6rem; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
.badge--success { background: #dcfce7; color: #16a34a; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

/* Pagination */
.cm-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 1rem;
  font-size: 0.85rem;
  color: #6b7280;
  border-top: 1px solid var(--color-border);
}
.cm-pagination button {
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-card);
  cursor: pointer;
  font-size: 0.82rem;
  transition: background .15s;
}
.cm-pagination button:hover:not(:disabled) { background: var(--color-hover-row); }
.cm-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

/* Loading / empty */
.cm-loading, .cm-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: #6b7280;
  font-size: 0.9rem;
}
.spinner {
  width: 32px; height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.45);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2rem;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 8px 32px rgba(0,0,0,.2);
}
.modal__title { font-size: 1.1rem; font-weight: 800; margin-bottom: 1.25rem; color: var(--color-text); }
.modal__body  { font-size: 0.88rem; color: #6b7280; margin-bottom: 1.25rem; line-height: 1.5; }
.modal__actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }

/* Champs */
.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field__label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); }
.req { color: var(--color-accent); }
.field__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid #e8e0d4;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color .2s;
  width: 100%;
}
.field__input:focus { border-color: var(--color-primary); outline: none; }

/* Boutons */
.btn-primary {
  padding: 0.55rem 1.1rem;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.btn-primary:hover:not(:disabled) { background: var(--color-accent-hover); }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }

.btn-edit {
  padding: 0.35rem 0.75rem;
  background: transparent;
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.btn-edit:hover { background: rgba(74,124,111,.1); }

.btn-delete {
  padding: 0.35rem 0.75rem;
  background: transparent;
  border: 1px solid #ef4444;
  color: #ef4444;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.btn-delete:hover:not(:disabled) { background: rgba(239,68,68,.1); }
.btn-delete:disabled { opacity: .6; cursor: not-allowed; }

.btn-cancel {
  padding: 0.55rem 1rem;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  cursor: pointer;
  transition: background .15s;
}
.btn-cancel:hover { background: var(--color-hover-row); }
</style>
