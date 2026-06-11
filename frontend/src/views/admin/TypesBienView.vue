<script setup>
import { ref, computed, onMounted } from 'vue'
import { ChevronLeft, ChevronRight, Pencil, Archive, RotateCcw } from 'lucide-vue-next'
import typeBienService from '@/services/typeBienService'
import FilterTabs from '@/components/FilterTabs.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const allItems     = ref([])
const loading      = ref(false)
const activeFilter = ref('tous')
const ITEMS_PER_PAGE = 8
const currentPage  = ref(1)

const showModal    = ref(false)
const editId       = ref(null)
const modalLibelle = ref('')
const saving       = ref(false)

const showConfirm = ref(false)
const deleteId    = ref(null)
const deleting    = ref(false)

const toast = ref({ show: false, message: '', type: 'success' })
function notify(message, type = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function fetchItems() {
  loading.value = true
  try {
    const res = await typeBienService.getAllTypesBienPaged({ page: 0, size: 1000 })
    allItems.value = res.data.data ?? []
  } catch {
    allItems.value = []
    notify('Erreur lors du chargement.', 'error')
  } finally {
    loading.value = false
  }
}

const filteredItems = computed(() => {
  if (activeFilter.value === 'actifs')   return allItems.value.filter(i => !i.isArchived)
  if (activeFilter.value === 'archives') return allItems.value.filter(i => i.isArchived)
  return allItems.value
})

const totalPages = computed(() => Math.ceil(filteredItems.value.length / ITEMS_PER_PAGE))
const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * ITEMS_PER_PAGE
  return filteredItems.value.slice(start, start + ITEMS_PER_PAGE)
})

function setFilter(f) {
  activeFilter.value = f
  currentPage.value  = 1
}

async function restaurer(id) {
  try {
    await typeBienService.restoreTypeBien(id)
    notify('Type de bien restauré ✓')
    await fetchItems()
  } catch (err) {
    notify(err.response?.data?.message || 'Erreur lors de la restauration.', 'error')
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
      await typeBienService.updateTypeBien(editId.value, modalLibelle.value.trim())
      notify('Type de bien modifié.')
    } else {
      await typeBienService.createTypeBien(modalLibelle.value.trim())
      notify('Type de bien créé.')
    }
    showModal.value = false
    await fetchItems()
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
    await typeBienService.archiveTypeBien(deleteId.value)
    notify('Type de bien archivé.')
    showConfirm.value = false
    await fetchItems()
  } catch (err) {
    notify(err.response?.data?.message || 'Erreur lors de la suppression.', 'error')
  } finally {
    deleting.value = false
  }
}

onMounted(() => fetchItems())
</script>

<template>
  <div class="tb-page">
    <transition name="fade">
      <div v-if="toast.show" class="tb-toast" :class="toast.type === 'error' ? 'tb-toast--error' : 'tb-toast--success'">
        {{ toast.message }}
      </div>
    </transition>

    <div class="tb-toolbar">
      <div>
        <h1 class="tb-toolbar__title">Types de bien</h1>
        <p class="tb-toolbar__count">{{ filteredItems.length }} type{{ filteredItems.length !== 1 ? 's' : '' }}</p>
      </div>
      <div class="tb-toolbar__right">
        <FilterTabs
          :model-value="activeFilter"
          :tabs="[
            { value: 'tous', label: 'Tous' },
            { value: 'actifs', label: 'Actifs' },
            { value: 'archives', label: 'Archivés' },
          ]"
          @update:model-value="setFilter"
        />
        <button class="btn-primary" @click="openCreate">+ Nouveau type</button>
      </div>
    </div>

    <div class="tb-card">
      <div v-if="loading" class="tb-loading"><div class="spinner"></div></div>
      <div v-else-if="!paginatedItems.length" class="tb-empty">Aucun type de bien enregistré.</div>

      <table v-else class="tb-table">
        <thead>
          <tr><th>ID</th><th>Libellé</th><th>Statut</th><th>Actions</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedItems" :key="item.id">
            <td class="tb-id">{{ item.id }}</td>
            <td class="tb-label">{{ item.libelle }}</td>
            <td>
              <StatusBadge :label="item.isArchived ? 'Archivé' : 'Actif'" :variant="item.isArchived ? 'neutral' : 'success'" />
            </td>
            <td class="tb-actions">
              <template v-if="!item.isArchived">
                <button class="action-btn" title="Modifier" @click="openEdit(item)"><Pencil :size="15" /></button>
                <button class="action-btn danger" title="Archiver" @click="confirmDelete(item.id)"><Archive :size="15" /></button>
              </template>
              <button v-else class="action-btn" title="Restaurer" @click="restaurer(item.id)"><RotateCcw :size="15" /></button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="tb-pagination">
        <button class="pagination-nav" :disabled="currentPage === 1" @click="currentPage--">
          <ChevronLeft :size="16" />
          <span>Précédent</span>
        </button>

        <div class="pagination-pages">
          <button
            v-for="p in totalPages" :key="p"
            class="pagination-page" :class="{ 'pagination-page--active': p === currentPage }"
            @click="currentPage = p"
          >{{ p }}</button>
        </div>

        <button class="pagination-nav" :disabled="currentPage === totalPages" @click="currentPage++">
          <span>Suivant</span>
          <ChevronRight :size="16" />
        </button>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h2 class="modal__title">{{ editId ? 'Modifier' : 'Nouveau' }} type de bien</h2>
        <div class="field">
          <label class="field__label">Libellé <span class="req">*</span></label>
          <input v-model="modalLibelle" type="text" class="field__input" placeholder="ex. Villa, Appartement, Terrain..." @keyup.enter="saveModal" />
        </div>
        <div class="modal__actions">
          <button class="btn-cancel" @click="showModal = false">Annuler</button>
          <button class="btn-primary" :disabled="saving || !modalLibelle.trim()" @click="saveModal">
            {{ saving ? 'Sauvegarde...' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
      <div class="modal">
        <h2 class="modal__title">Archiver ce type de bien ?</h2>
        <p class="modal__body">Cette action est réversible. Le type de bien ne sera plus proposé lors de la création d'annonces.</p>
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
.tb-page { display: flex; flex-direction: column; gap: 1.5rem; position: relative; }

.tb-toast { position: fixed; bottom: 2rem; right: 2rem; padding: 0.85rem 1.5rem; border-radius: 10px; font-size: 0.9rem; font-weight: 600; z-index: 200; box-shadow: 0 8px 24px rgba(0,0,0,.18); min-width: 260px; max-width: 380px; }
.tb-toast--success { background: var(--color-primary); color: #fff; }
.tb-toast--error   { background: var(--color-accent);  color: #fff; }
.fade-enter-active, .fade-leave-active { transition: opacity .25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.tb-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.tb-toolbar__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.tb-toolbar__count { font-size: 0.85rem; color: #6b7280; margin-top: 0.2rem; }
.tb-toolbar__right { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }


.tb-card { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }

.tb-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.tb-table th { padding: 0.5rem 0.65rem; text-align: center; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: #6b7280; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.tb-table th:first-child { width: 10%; }
.tb-table th:nth-child(2) { width: 34%; }
.tb-table th:nth-child(3) { width: 22%; }
.tb-table th:nth-child(4) { width: 34%; }
.tb-table td { padding: 0.5rem 0.65rem; border-bottom: 1px solid var(--color-border); font-size: 0.88rem; color: var(--color-text); text-align: center; line-height: 1.2; }
.tb-table tbody tr:last-child td { border-bottom: none; }
.tb-table tbody tr:hover { background: var(--color-hover-row); }
.tb-id, .tb-label { text-align: center; }
.tb-actions { display: flex; gap: 0.5rem; justify-content: center; }


.tb-pagination { display: flex; align-items: center; justify-content: center; gap: 0.5rem; padding: 1rem; border-top: 1px solid var(--color-border); }
.tb-pagination .pagination-nav {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  transition: background 0.2s, color 0.2s;
}
.tb-pagination .pagination-nav:hover:not(:disabled) {
  background: var(--color-border);
  color: var(--color-text);
}
.tb-pagination .pagination-nav:disabled { opacity: 0.35; cursor: not-allowed; }
.tb-pagination .pagination-pages { display: flex; gap: 0.25rem; }
.tb-pagination .pagination-page {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  transition: background 0.2s, color 0.2s;
}
.tb-pagination .pagination-page:hover { background: var(--color-border); color: var(--color-text); }
.tb-pagination .pagination-page--active {
  background: var(--color-primary);
  color: #fff;
}

.tb-loading, .tb-empty { display: flex; align-items: center; justify-content: center; padding: 3rem; color: #6b7280; font-size: 0.9rem; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 100; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--color-card); border-radius: var(--radius); padding: 2rem; width: 100%; max-width: 440px; box-shadow: 0 8px 32px rgba(0,0,0,.2); }
.modal__title { font-size: 1.1rem; font-weight: 800; margin-bottom: 1.25rem; color: var(--color-text); }
.modal__body  { font-size: 0.88rem; color: #6b7280; margin-bottom: 1.25rem; line-height: 1.5; }
.modal__actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }

.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field__label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); }
.req { color: var(--color-accent); }
.field__input { padding: 0.7rem 0.9rem; border: 1.5px solid #e8e0d4; border-radius: var(--radius-sm); font-size: 0.9rem; color: var(--color-text); background: #fff; transition: border-color .2s; width: 100%; }
.field__input:focus { border-color: var(--color-primary); outline: none; }

.btn-primary { padding: 0.55rem 1.1rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-size: 0.88rem; font-weight: 600; cursor: pointer; transition: background .15s; }
.btn-primary:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }
.btn-cancel  { padding: 0.55rem 1rem; background: transparent; border: 1px solid var(--color-border); color: var(--color-text); border-radius: var(--radius-sm); font-size: 0.88rem; cursor: pointer; }
.btn-cancel:hover { background: var(--color-hover-row); }
</style>
