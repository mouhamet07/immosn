<script setup>
import { ref, onMounted } from 'vue'
import { Home, MapPin, Calendar, Search, AlertCircle, RefreshCw } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import visiteService from '@/services/visiteService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const router = useRouter()

const visites     = ref([])
const loading     = ref(false)
const error       = ref('')
const currentPage = ref(0)
const totalPages  = ref(1)
const filtreStatut = ref('')
const cancelling  = ref(null)

const STATUTS = ['', 'EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE', 'CLOTUREE_SANS_SUITE', 'CLOTUREE_AVEC_CONTRAT']

const STATUT_LABELS = {
  EN_ATTENTE:            'En attente',
  ACCEPTEE:              'Acceptée',
  REFUSEE:               'Refusée',
  ANNULEE:               'Annulée',
  CLOTUREE_SANS_SUITE:   'Clôturée sans suite',
  CLOTUREE_AVEC_CONTRAT: 'Clôturée avec contrat',
  TERMINEE:              'Terminée',
}
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les statuts' }))
const STATUT_VARIANTS = {
  EN_ATTENTE:            'warning',
  ACCEPTEE:              'success',
  REFUSEE:               'danger',
  ANNULEE:               'neutral',
  CLOTUREE_SANS_SUITE:   'neutral',
  CLOTUREE_AVEC_CONTRAT: 'info',
  TERMINEE:              'info',
}

async function fetchVisites(page = 0) {
  loading.value = true
  error.value   = ''
  try {
    const res   = await visiteService.getClientVisites(page, 10, filtreStatut.value || null)
    const paged = res.data
    visites.value     = paged.data
    currentPage.value = paged.currentPage
    totalPages.value  = paged.totalPages
  } catch {
    error.value = 'Impossible de charger vos demandes.'
  } finally {
    loading.value = false
  }
}

async function annuler(id) {
  if (!confirm('Confirmer l\'annulation de cette demande ?')) return
  cancelling.value = id
  try {
    await visiteService.annuler(id)
    await fetchVisites(currentPage.value)
  } catch {
    alert('Erreur lors de l\'annulation.')
  } finally {
    cancelling.value = null
  }
}

function formatDate(dt) {
  const d = new Date(dt)
  const jour = d.toLocaleDateString('fr-FR', { weekday: 'long' })
  const date = d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })
  const heure = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  return `${jour.charAt(0).toUpperCase() + jour.slice(1)} ${date} ${heure.replace(':', 'H')}`
}

onMounted(() => fetchVisites(0))
</script>

<template>
  <div class="mv-page">
    <div class="mv-container">

      <!-- Header -->
      <div class="mv-header">
        <div>
          <h1 class="mv-header__title">Mes demandes de visite</h1>
          <p class="mv-header__sub">Suivez l'état de vos rendez-vous</p>
        </div>
      </div>

      <!-- Filtres statut -->
      <FilterSelect
        style="margin-bottom: 1.5rem"
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchVisites(0) }"
      />

      <!-- Chargement -->
      <div v-if="loading" class="mv-loading"><div class="spinner"></div></div>

      <!-- Erreur API -->
      <div v-else-if="error" class="mv-empty">
        <AlertCircle :size="40" color="var(--color-accent)" />
        <p class="mv-empty__title">Impossible de charger vos demandes.</p>
        <button class="mv-empty__retry" @click="fetchVisites(0)">
          <RefreshCw :size="14" /> Réessayer
        </button>
      </div>

      <!-- Vide -->
      <div v-else-if="!visites.length" class="mv-empty">
        <Calendar :size="56" color="var(--color-border)" />
        <p class="mv-empty__title">Aucune demande de visite</p>
        <p class="mv-empty__desc">
          Vous n'avez pas encore demandé de visite.
          Trouvez un bien et cliquez sur <strong>"Réserver une visite"</strong>.
        </p>
        <button class="mv-empty__btn" @click="router.push('/annonces')">
          <Search :size="16" /> Trouver un bien
        </button>
      </div>

      <!-- Liste -->
      <div v-else class="mv-list">
        <div v-for="v in visites" :key="v.id" class="mv-card">
          <div class="mv-card__thumb">
            <img v-if="v.imagePrincipale" :src="v.imagePrincipale" :alt="v.annonceLibelle" />
            <span v-else class="mv-card__thumb-ph"><Home :size="28" /></span>
          </div>
          <div class="mv-card__body">
            <div class="mv-card__top">
              <StatusBadge :label="STATUT_LABELS[v.statut]" :variant="STATUT_VARIANTS[v.statut]" />
              <span class="mv-card__date">Demandé le {{ formatDate(v.createdAt) }}</span>
            </div>
            <RouterLink :to="`/annonces/${v.annonceId}`" class="mv-card__title">{{ v.annonceLibelle }}</RouterLink>
            <p class="mv-card__addr"><MapPin :size="12" /> {{ v.annonceAdresse }}</p>
            <p v-if="v.commentaire" class="mv-card__comment">{{ v.commentaire }}</p>
          </div>
          <div class="mv-card__actions">
            <RouterLink :to="`/visites/${v.id}`" class="mv-card__detail">Voir le détail</RouterLink>
            <button
              v-if="v.statut === 'EN_ATTENTE' || v.statut === 'ACCEPTEE'"
              class="mv-card__cancel"
              :disabled="cancelling === v.id"
              @click="annuler(v.id)"
            >
              {{ cancelling === v.id ? '…' : 'Annuler' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mv-pager">
        <button :disabled="currentPage === 0" @click="fetchVisites(currentPage - 1)">← Précédent</button>
        <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button :disabled="currentPage === totalPages - 1" @click="fetchVisites(currentPage + 1)">Suivant →</button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.mv-page { background: var(--color-background); min-height: 100vh; }
.mv-container { max-width: 860px; margin: 0 auto; padding: 2rem 1.5rem; }

.mv-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.mv-header__title { font-size: 1.6rem; font-weight: 800; color: var(--color-text); }
.mv-header__sub { font-size: 0.88rem; color: var(--color-text); opacity: .55; margin-top: .25rem; }
.mv-header__btn {
  padding: .6rem 1.2rem; background: var(--color-primary); color: #fff;
  border-radius: var(--radius-sm); font-weight: 600; font-size: .88rem; text-decoration: none;
  transition: opacity .2s;
}
.mv-header__btn:hover { opacity: .85; }


.mv-loading { display: flex; justify-content: center; padding: 4rem; }
.mv-empty {
  display: flex; flex-direction: column; align-items: center; gap: .75rem;
  padding: 4rem 2rem; text-align: center;
}
.mv-empty__title { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.mv-empty__desc { font-size: 0.85rem; color: var(--color-text-secondary); line-height: 1.5; max-width: 360px; }
.mv-empty__btn {
  display: flex; align-items: center; gap: 6px;
  padding: .6rem 1.2rem; background: var(--color-primary); color: #fff;
  border: none; border-radius: var(--radius-sm);
  font-size: .88rem; font-weight: 600; cursor: pointer; transition: opacity .15s;
  margin-top: .25rem;
}
.mv-empty__btn:hover { opacity: .85; }
.mv-empty__retry {
  display: flex; align-items: center; gap: 6px;
  padding: .45rem 1rem; background: transparent;
  border: 1px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .82rem; font-weight: 600; color: var(--color-text); cursor: pointer;
  transition: background .15s;
}
.mv-empty__retry:hover { background: var(--color-background); }

.mv-list { display: flex; flex-direction: column; gap: 1rem; }
.mv-card {
  display: flex; gap: 1rem; align-items: flex-start;
  background: var(--color-card); border-radius: var(--radius);
  padding: 1.25rem; box-shadow: var(--shadow-card);
  transition: box-shadow .2s;
}
.mv-card:hover { box-shadow: 0 4px 24px rgba(0,0,0,.1); }

.mv-card__thumb {
  width: 80px; height: 80px; border-radius: 8px; overflow: hidden;
  flex-shrink: 0; background: var(--color-border);
  display: flex; align-items: center; justify-content: center; font-size: 1.5rem;
}
.mv-card__thumb img { width: 100%; height: 100%; object-fit: cover; }

.mv-card__body { flex: 1; min-width: 0; }
.mv-card__top {
  display: flex; align-items: center; gap: .75rem;
  margin-bottom: .5rem; flex-wrap: wrap;
}
.mv-card__date { font-size: .75rem; color: var(--color-text); opacity: .45; }
.mv-card__title {
  font-size: .95rem; font-weight: 700; color: var(--color-text);
  text-decoration: none;
}
.mv-card__title:hover { color: var(--color-primary); }
.mv-card__addr { font-size: .8rem; color: var(--color-text); opacity: .55; margin: .25rem 0; display: flex; align-items: center; gap: .3rem; }
.mv-card__visite { display: flex; gap: .5rem; font-size: .85rem; margin-top: .4rem; }
.mv-card__visite-label { font-weight: 600; color: var(--color-text); opacity: .7; }
.mv-card__visite-val { color: var(--color-primary); font-weight: 600; }
.mv-card__comment {
  font-size: .82rem; color: var(--color-text); opacity: .6;
  font-style: italic; margin-top: .4rem;
}

.mv-card__actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.mv-card__detail {
  padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  color: var(--color-text); font-size: .82rem; font-weight: 600; background: none;
  text-decoration: none; transition: all .15s; display: inline-block; text-align: center;
}
.mv-card__detail:hover { border-color: var(--color-primary); color: var(--color-primary); }
.mv-card__cancel {
  padding: .4rem .9rem; border: 1.5px solid #e53e3e; border-radius: var(--radius-sm);
  color: #e53e3e; font-size: .82rem; font-weight: 600; background: none;
  cursor: pointer; transition: all .15s;
}
.mv-card__cancel:hover:not(:disabled) { background: #e53e3e; color: #fff; }
.mv-card__cancel:disabled { opacity: .4; cursor: not-allowed; }


.mv-pager {
  display: flex; justify-content: center; align-items: center; gap: 1rem;
  margin-top: 1.5rem; font-size: .88rem; color: var(--color-text); opacity: .7;
}
.mv-pager button {
  padding: .4rem .9rem; border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm); background: var(--color-card);
  cursor: pointer; font-size: .88rem; font-weight: 600;
}
.mv-pager button:disabled { opacity: .4; cursor: not-allowed; }

.spinner {
  width: 36px; height: 36px; border: 3px solid var(--color-border);
  border-top-color: var(--color-primary); border-radius: 50%;
  animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .mv-container { padding: 1.5rem 1rem; }
  .mv-card { flex-wrap: wrap; }
  .mv-card__thumb { width: 64px; height: 64px; }
  .mv-card__actions {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
  }
  .mv-card__actions > * { flex: 1; text-align: center; }
}
</style>
