<script setup>
import { ref, onMounted } from 'vue'
import { FileText } from 'lucide-vue-next'
import leadService from '@/services/leadService'
import FilterSelect from '@/components/FilterSelect.vue'

const leads       = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')

const STATUTS       = ['', 'EN_COURS', 'CONVERTI', 'ABANDONNE']
const STATUT_LABELS = { EN_COURS: 'En cours', CONVERTI: 'Converti', ABANDONNE: 'Abandonné' }
const STATUT_COLORS = { EN_COURS: 'badge--info', CONVERTI: 'badge--success', ABANDONNE: 'badge--neutral' }
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les leads' }))

async function fetchLeads(page = 0) {
  loading.value = true
  try {
    const res = await leadService.getAll(page, 20, filtreStatut.value || null)
    leads.value       = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch (err) { console.error('[LeadsAdminView] fetchLeads error:', err?.response?.status, err?.message); leads.value = [] }
  finally { loading.value = false }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(() => fetchLeads(0))
</script>

<template>
  <div class="la-page">
    <div class="la-toolbar">
      <div>
        <h1 class="la-toolbar__title">Leads</h1>
        <p class="la-toolbar__count">{{ totalItems }} lead{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchLeads(0) }"
      />
    </div>

    <div v-if="loading" class="la-loading"><div class="spinner"></div></div>

    <div v-else-if="!leads.length" class="la-empty">Aucun lead.</div>

    <div v-else class="la-pipeline">
      <div v-for="lead in leads" :key="lead.id" class="la-card">
        <div class="la-card__header">
          <span :class="['badge', STATUT_COLORS[lead.statut]]">{{ STATUT_LABELS[lead.statut] }}</span>
          <span class="la-card__date">{{ formatDate(lead.createdAt) }}</span>
        </div>
        <div class="la-card__body">
          <div class="la-card__avatar">{{ lead.clientNom?.charAt(0)?.toUpperCase() || '?' }}</div>
          <div class="la-card__info">
            <p class="la-card__client">{{ lead.clientNom }}</p>
            <p class="la-card__email">{{ lead.clientEmail }}</p>
            <RouterLink :to="`/annonces/${lead.annonceId}`" class="la-card__annonce">
              {{ lead.annonceLibelle }}
            </RouterLink>
            <p class="la-card__addr">{{ lead.annonceAdresse }}</p>
          </div>
        </div>
        <p v-if="lead.noteAdmin" class="la-card__note"><FileText :size="13" /> {{ lead.noteAdmin }}</p>
      </div>
    </div>

    <div v-if="totalPages > 1" class="la-pager">
      <button :disabled="currentPage === 0" @click="fetchLeads(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchLeads(currentPage + 1)">Suivant →</button>
    </div>

  </div>
</template>

<style scoped>
.la-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.la-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.la-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.la-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.la-loading { display: flex; justify-content: center; padding: 4rem; }
.la-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.la-pipeline { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }

.la-card { background: var(--color-card); border-radius: var(--radius); padding: 1rem; box-shadow: var(--shadow-card); display: flex; flex-direction: column; gap: .75rem; }
.la-card__header { display: flex; justify-content: space-between; align-items: center; }
.la-card__date { font-size: .72rem; color: var(--color-text); opacity: .4; }
.la-card__body { display: flex; gap: .75rem; align-items: flex-start; }
.la-card__avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: .95rem; flex-shrink: 0; }
.la-card__info { flex: 1; min-width: 0; }
.la-card__client { font-size: .9rem; font-weight: 700; color: var(--color-text); }
.la-card__email { font-size: .75rem; color: var(--color-text); opacity: .5; }
.la-card__annonce { font-size: .82rem; color: var(--color-primary); font-weight: 600; text-decoration: none; display: block; margin-top: .2rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.la-card__addr { font-size: .75rem; color: var(--color-text); opacity: .5; }
.la-card__note { font-size: .78rem; color: var(--color-text); opacity: .65; font-style: italic; padding: .5rem .75rem; background: var(--color-background); border-radius: 6px; display: flex; align-items: center; gap: .35rem; }
.la-card__actions { display: flex; gap: .5rem; flex-wrap: wrap; }

.la-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; font-size: .88rem; }
.la-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.la-pager button:disabled { opacity: .4; cursor: not-allowed; }

.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--info    { background: #dbeafe; color: #2563eb; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

/* Modal */
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
