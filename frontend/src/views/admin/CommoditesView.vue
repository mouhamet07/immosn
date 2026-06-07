<script setup>
import { ref, computed, onMounted } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import commoditeService from '@/services/commoditeService'

const allItems     = ref([])
const loading      = ref(false)
const activeFilter = ref('actifs')
const ITEMS_PER_PAGE = 10
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
    const res = await commoditeService.getAllCommoditesPaged({ page: 0, size: 1000 })
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
    await commoditeService.restoreCommodite(id)
    notify('Commodité restaurée ✓')
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
      await commoditeService.updateCommodite(editId.value, modalLibelle.value.trim())
      notify('Commodité modifiée.')
    } else {
      await commoditeService.createCommodite(modalLibelle.value.trim())
      notify('Commodité créée.')
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
    await commoditeService.archiveCommodite(deleteId.value)
    notify('Commodité archivée.')
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
  <div class="cm-page">
    <transition name="fade">
      <div v-if="toast.show" class="cm-toast" :class="toast.type === 'error' ? 'cm-toast--error' : 'cm-toast--success'">
        {{ toast.message }}
      </div>
    </transition>

    <div class="cm-toolbar">
      <div>
        <h1 class="cm-toolbar__title">Commodités</h1>
        <p class="cm-toolbar__count">{{ filteredItems.length }} commodité{{ filteredItems.length !== 1 ? 's' : '' }}</p>
      </div>
      <div class="cm-toolbar__right">
        <div class="cm-filter">
          <button class="cm-filter__btn" :class="{ 'cm-filter__btn--active': activeFilter === 'tous' }"     @click="setFilter('tous')">Tous</button>
          <button class="cm-filter__btn" :class="{ 'cm-filter__btn--active': activeFilter === 'actifs' }"   @click="setFilter('actifs')">Actifs</button>
          <button class="cm-filter__btn" :class="{ 'cm-filter__btn--active': activeFilter === 'archives' }" @click="setFilter('archives')">Archivés</button>
        </div>
        <button class="btn-primary" @click="openCreate">+ Nouvelle commodité</button>
      </div>
    </div>

    <div class="cm-card">
      <div v-if="loading" class="cm-loading"><div class="spinner"></div></div>
      <div v-else-if="!paginatedItems.length" class="cm-empty">Aucune commodité enregistrée.</div>

      <table v-else class="cm-table">
        <thead>
          <tr><th>#</th><th>Libellé</th><th>Statut</th><th>Actions</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedItems" :key="item.id">
            <td class="cm-id">{{ item.id }}</td>
            <td class="cm-label">{{ item.libelle }}</td>
            <td>
              <span class="badge" :class="item.isArchived ? 'badge--neutral' : 'badge--success'">
                {{ item.isArchived ? 'Archivée' : 'Active' }}
              </span>
            </td>
            <td class="cm-actions">
              <template v-if="!item.isArchived">
                <button class="btn-edit" @click="openEdit(item)">Modifier</button>
                <button class="btn-delete" @click="confirmDelete(item.id)">Archiver</button>
              </template>
              <button v-else class="btn-restore" @click="restaurer(item.id)">Restaurer</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="cm-pagination">
        <button :disabled="currentPage === 1" @click="currentPage--"><ChevronLeft :size="16" /></button>
        <button
          v-for="p in totalPages" :key="p"
          class="cm-page-btn" :class="{ 'cm-page-btn--active': p === currentPage }"
          @click="currentPage = p"
        >{{ p }}</button>
        <button :disabled="currentPage === totalPages" @click="currentPage++"><ChevronRight :size="16" /></button>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h2 class="modal__title">{{ editId ? 'Modifier' : 'Nouvelle' }} commodité</h2>
        <div class="field">
          <label class="field__label">Libellé <span class="req">*</span></label>
          <input v-model="modalLibelle" type="text" class="field__input" placeholder="ex. Piscine, Garage, Climatisation..." @keyup.enter="saveModal" />
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
.cm-toast { position: fixed; bottom: 2rem; right: 2rem; padding: 0.85rem 1.5rem; border-radius: 10px; font-size: 0.9rem; font-weight: 600; z-index: 200; box-shadow: 0 8px 24px rgba(0,0,0,.18); min-width: 260px; max-width: 380px; }
.cm-toast--success { background: var(--color-primary); color: #fff; }
.cm-toast--error   { background: var(--color-accent);  color: #fff; }
.fade-enter-active, .fade-leave-active { transition: opacity .25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.cm-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.cm-toolbar__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.cm-toolbar__count { font-size: 0.85rem; color: #6b7280; margin-top: 0.2rem; }
.cm-toolbar__right { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
.cm-filter { display: flex; border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden; }
.cm-filter__btn { padding: 0.45rem 0.9rem; font-size: 0.82rem; font-weight: 600; background: var(--color-card); color: var(--color-text-muted); border: none; cursor: pointer; transition: background 0.15s, color 0.15s; }
.cm-filter__btn:hover { background: var(--color-hover-row); color: var(--color-text); }
.cm-filter__btn--active { background: var(--color-primary); color: #fff; }

.cm-card { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }

.cm-table { width: 100%; border-collapse: collapse; }
.cm-table th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: #6b7280; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.cm-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--color-border); font-size: 0.88rem; color: var(--color-text); }
.cm-table tbody tr:last-child td { border-bottom: none; }
.cm-table tbody tr:hover { background: var(--color-hover-row); }
.cm-id    { color: #9ca3af; font-size: 0.8rem; }
.cm-label { font-weight: 600; }
.cm-actions { display: flex; gap: 0.5rem; }

.badge { padding: 0.25rem 0.6rem; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
.badge--success { background: #dcfce7; color: #16a34a; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

.cm-pagination { display: flex; align-items: center; justify-content: center; gap: 4px; padding: 1rem; border-top: 1px solid var(--color-border); }
.cm-pagination button { width: 32px; height: 32px; border: 1px solid var(--color-border); border-radius: 6px; background: var(--color-card); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 0.82rem; transition: background .15s; }
.cm-pagination button:hover:not(:disabled) { background: var(--color-hover-row); }
.cm-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.cm-page-btn { font-weight: 600; color: var(--color-text); }
.cm-page-btn--active { background: var(--color-primary) !important; color: #fff !important; border-color: var(--color-primary) !important; }

.cm-loading, .cm-empty { display: flex; align-items: center; justify-content: center; padding: 3rem; color: #6b7280; font-size: 0.9rem; }
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
.btn-edit    { padding: 0.35rem 0.75rem; background: transparent; border: 1px solid var(--color-primary); color: var(--color-primary); border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.btn-edit:hover { background: rgba(74,124,111,.1); }
.btn-delete  { padding: 0.35rem 0.75rem; background: transparent; border: 1px solid #ef4444; color: #ef4444; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.btn-delete:hover:not(:disabled) { background: rgba(239,68,68,.1); }
.btn-delete:disabled { opacity: .6; cursor: not-allowed; }
.btn-restore { padding: 0.35rem 0.75rem; background: transparent; border: 1px solid var(--color-primary); color: var(--color-primary); border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.btn-cancel  { padding: 0.55rem 1rem; background: transparent; border: 1px solid var(--color-border); color: var(--color-text); border-radius: var(--radius-sm); font-size: 0.88rem; cursor: pointer; }
.btn-cancel:hover { background: var(--color-hover-row); }
</style>
