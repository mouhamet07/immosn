<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Home, MapPin, Calendar, Search, AlertCircle, User } from 'lucide-vue-next'
import visiteService from '@/services/visiteService'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const route = useRoute()

const token = ref(typeof route.query.token === 'string' ? route.query.token : '')
const loading = ref(false)
const error = ref('')
const result = ref(null)

const STATUT_LABELS = {
  EN_ATTENTE:               'En attente',
  ACCEPTEE:                 'Acceptée',
  AFFECTEE:                 'Affectée',
  REPLANIFICATION_DEMANDEE: 'Replanification demandée',
  REFUSEE:                  'Refusée',
  ANNULEE:                  'Annulée',
  CLOTUREE_SANS_SUITE:      'Clôturée sans suite',
  CLOTUREE_AVEC_CONTRAT:    'Clôturée avec contrat',
  TERMINEE:                 'Terminée',
}
const STATUT_VARIANTS = {
  EN_ATTENTE:               'warning',
  ACCEPTEE:                 'success',
  AFFECTEE:                 'success',
  REPLANIFICATION_DEMANDEE: 'warning',
  REFUSEE:                  'danger',
  ANNULEE:                  'neutral',
  CLOTUREE_SANS_SUITE:      'neutral',
  CLOTUREE_AVEC_CONTRAT:    'info',
  TERMINEE:                 'info',
}

function formatDate(dt) {
  if (!dt) return null
  const d = new Date(dt)
  const date = d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
  const heure = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  return `${date} à ${heure.replace(':', 'H')}`
}

async function rechercher() {
  const t = token.value.trim()
  if (!t) { error.value = 'Veuillez saisir votre numéro de suivi.'; return }
  loading.value = true
  error.value = ''
  result.value = null
  try {
    const res = await visiteService.suivreParToken(t)
    result.value = res.data.data
  } catch (e) {
    error.value = e.response?.data?.message || 'Aucun suivi trouvé pour ce numéro.'
  } finally {
    loading.value = false
  }
}

if (token.value.trim()) rechercher()
</script>

<template>
  <div class="sv-page">
    <div class="sv-container">

      <div class="sv-header">
        <h1 class="sv-header__title">Suivre ma visite</h1>
        <p class="sv-header__sub">
          Saisissez le numéro de suivi reçu lors de votre demande de visite pour consulter son statut.
        </p>
      </div>

      <div class="sv-search">
        <input
          v-model="token"
          type="text"
          class="sv-search__input"
          placeholder="Ex. : 3f2a9c10-8b1d-4e22-9c4a-1234567890ab"
          @keyup.enter="rechercher"
        />
        <ButtonPrimary :loading="loading" @click="rechercher">
          <Search :size="16" /> Suivre
        </ButtonPrimary>
      </div>

      <p v-if="error" class="sv-error"><AlertCircle :size="16" /> {{ error }}</p>

      <div v-if="result" class="sv-result">
        <p class="sv-result__owner"><User :size="14" /> Suivi de {{ result.prospectNom }}</p>

        <div v-if="!result.visites.length" class="sv-empty">
          <Calendar :size="48" color="var(--color-border)" />
          <p class="sv-empty__title">Aucune demande de visite trouvée.</p>
        </div>

        <div v-else class="sv-list">
          <div v-for="v in result.visites" :key="v.id" class="sv-card">
            <div class="sv-card__thumb">
              <img v-if="v.imagePrincipale" :src="v.imagePrincipale" :alt="v.annonceLibelle" />
              <span v-else class="sv-card__thumb-ph"><Home :size="24" /></span>
            </div>
            <div class="sv-card__body">
              <div class="sv-card__top">
                <StatusBadge :label="STATUT_LABELS[v.statut] || v.statut" :variant="STATUT_VARIANTS[v.statut] || 'neutral'" />
                <span class="sv-card__date">Demandé le {{ formatDate(v.createdAt) }}</span>
              </div>
              <p class="sv-card__title">{{ v.annonceLibelle }}</p>
              <p class="sv-card__addr"><MapPin :size="12" /> {{ v.annonceAdresse }}</p>
              <div v-if="v.dateVisite" class="sv-card__row">
                <span class="sv-card__row-label">Date prévue :</span>
                <span class="sv-card__row-val">{{ formatDate(v.dateVisite) }}</span>
              </div>
              <div v-if="v.dateReplanificationProposee" class="sv-card__row">
                <span class="sv-card__row-label">Nouvelle date proposée :</span>
                <span class="sv-card__row-val">{{ formatDate(v.dateReplanificationProposee) }}</span>
              </div>
              <div v-if="v.adminResponsableNom" class="sv-card__row">
                <span class="sv-card__row-label">Administrateur affecté :</span>
                <span class="sv-card__row-val">{{ v.adminResponsableNom }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.sv-page { background: var(--color-background); min-height: 100vh; }
.sv-container { max-width: 720px; margin: 0 auto; padding: 2rem 1.5rem; }

.sv-header { margin-bottom: 1.5rem; }
.sv-header__title { font-size: 1.6rem; font-weight: 800; color: var(--color-text); }
.sv-header__sub { font-size: 0.88rem; color: var(--color-text); opacity: .6; margin-top: .4rem; }

.sv-search { display: flex; gap: .75rem; margin-bottom: 1rem; }
.sv-search__input {
  flex: 1; padding: .7rem .9rem; box-sizing: border-box;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text); background: var(--color-card);
}
.sv-search__input:focus { border-color: var(--color-primary); outline: none; }

.sv-error {
  display: flex; align-items: center; gap: .4rem;
  font-size: 0.85rem; color: var(--color-accent); margin-bottom: 1rem;
}

.sv-result__owner {
  display: flex; align-items: center; gap: .4rem;
  font-size: 0.85rem; font-weight: 600; color: var(--color-text); opacity: .7;
  margin-bottom: 1rem;
}

.sv-empty { display: flex; flex-direction: column; align-items: center; gap: .6rem; padding: 3rem 1rem; text-align: center; }
.sv-empty__title { font-size: 0.92rem; font-weight: 600; color: var(--color-text); opacity: .7; }

.sv-list { display: flex; flex-direction: column; gap: 1rem; }
.sv-card {
  display: flex; gap: 1rem; align-items: flex-start;
  background: var(--color-card); border-radius: var(--radius);
  padding: 1.1rem; box-shadow: var(--shadow-card);
}
.sv-card__thumb {
  width: 64px; height: 64px; border-radius: 8px; overflow: hidden;
  flex-shrink: 0; background: var(--color-border);
  display: flex; align-items: center; justify-content: center;
}
.sv-card__thumb img { width: 100%; height: 100%; object-fit: cover; }
.sv-card__body { flex: 1; min-width: 0; }
.sv-card__top { display: flex; align-items: center; gap: .65rem; margin-bottom: .4rem; flex-wrap: wrap; }
.sv-card__date { font-size: .72rem; color: var(--color-text); opacity: .45; }
.sv-card__title { font-size: .92rem; font-weight: 700; color: var(--color-text); }
.sv-card__addr { font-size: .78rem; color: var(--color-text); opacity: .55; margin: .25rem 0; display: flex; align-items: center; gap: .3rem; }
.sv-card__row { display: flex; gap: .4rem; font-size: .82rem; margin-top: .3rem; }
.sv-card__row-label { font-weight: 600; color: var(--color-text); opacity: .65; }
.sv-card__row-val { color: var(--color-primary); font-weight: 600; }

@media (max-width: 480px) {
  .sv-search { flex-direction: column; }
}
</style>
