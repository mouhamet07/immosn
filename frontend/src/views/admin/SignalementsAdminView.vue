<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import signalementService from '@/services/signalementService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useToastStore } from '@/stores/toastStore'

const router = useRouter()
const toast  = useToastStore()

const signalements = ref([])
const loading      = ref(false)
const currentPage  = ref(0)
const totalPages   = ref(1)
const totalItems   = ref(0)
const filtreStatut = ref('')

const STATUTS       = ['', 'OUVERT', 'EN_COURS', 'RESOLU', 'FERME']
const STATUT_LABELS = { OUVERT: 'Ouvert', EN_COURS: 'En cours', RESOLU: 'Résolu', FERME: 'Fermé' }
const STATUT_VARIANTS = { OUVERT: 'warning', EN_COURS: 'info', RESOLU: 'success', FERME: 'neutral' }
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les signalements' }))

async function fetchSignalements(page = 0) {
  loading.value = true
  try {
    const res = await signalementService.getAll(page, 20, filtreStatut.value || null)
    signalements.value = res.data.data
    currentPage.value  = res.data.currentPage
    totalPages.value   = res.data.totalPages
    totalItems.value   = res.data.totalElements
  } catch (err) {
    console.error('[Signalements] fetchSignalements error:', err?.response?.status, err?.message)
    toast.error('Impossible de charger les signalements.')
    signalements.value = []
  } finally { loading.value = false }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function truncate(text, max = 60) {
  if (!text) return '—'
  return text.length > max ? text.slice(0, max) + '…' : text
}

onMounted(() => fetchSignalements(0))
</script>

<template>
  <div class="sa-page">
    <div class="sa-toolbar">
      <div>
        <h1 class="sa-toolbar__title">Signalements SAV</h1>
        <p class="sa-toolbar__count">{{ totalItems }} signalement{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchSignalements(0) }"
      />
    </div>

    <div v-if="loading" class="sa-loading"><div class="spinner"></div></div>
    <div v-else-if="!signalements.length" class="sa-empty">Aucun signalement.</div>

    <div v-else class="sa-table-wrap">
      <table class="sa-table">
        <thead>
          <tr>
            <th>Sujet</th>
            <th>Auteur</th>
            <th>Statut</th>
            <th>Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in signalements" :key="s.id" :class="{ '--unread': !s.isRead }">
            <td>
              <p class="sa-sujet">{{ truncate(s.contenu) }}</p>
              <p class="sa-td-sub">{{ s.annonceLibelle }}</p>
            </td>
            <td class="table-client">{{ s.clientNom }}</td>
            <td>
              <div class="sa-status-wrap">
                <StatusBadge :label="STATUT_LABELS[s.statut]" :variant="STATUT_VARIANTS[s.statut]" />
                <span v-if="!s.isRead" class="sa-new">Nouveau</span>
              </div>
            </td>
            <td class="table-date">{{ formatDate(s.createdAt) }}</td>
            <td>
              <div class="td-actions">
                <button class="action-btn" title="Voir le détail"
                  @click="router.push(`/admin/signalements/${s.id}`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="sa-pager">
      <button :disabled="currentPage === 0" @click="fetchSignalements(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchSignalements(currentPage + 1)">Suivant →</button>
    </div>
  </div>
</template>

<style scoped>
.sa-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.sa-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.sa-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.sa-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.sa-loading { display: flex; justify-content: center; padding: 4rem; }
.sa-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.sa-table-wrap { overflow-x: auto; }
.sa-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.sa-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.sa-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.sa-table tr:last-child td { border-bottom: none; }
.sa-table tr:hover td { background: rgba(0,0,0,.02); }
.sa-table tr.--unread td:first-child { border-left: 3px solid var(--color-primary); }

.sa-sujet { font-weight: 500; color: var(--color-text); }
.sa-td-sub { font-size: .72rem; color: var(--color-primary); margin-top: .1rem; }
.sa-status-wrap { display: flex; align-items: center; gap: .4rem; flex-wrap: wrap; }
.sa-new { font-size: .68rem; font-weight: 700; color: var(--color-primary); background: rgba(74,124,111,.1); padding: .1rem .45rem; border-radius: 10px; white-space: nowrap; }

.sa-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.sa-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.sa-pager button:disabled { opacity: .4; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
