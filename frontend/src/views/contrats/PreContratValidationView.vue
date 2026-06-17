<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import contratService from '@/services/contratService'
import { useToastStore } from '@/stores/toastStore'
import StatusBadge from '@/components/StatusBadge.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'

const route = useRoute()
const router = useRouter()
const toastStore = useToastStore()

const contrat = ref(null)
const loading = ref(false)
const error = ref('')
const validating = ref(false)

const STATUT_LABELS = {
  BROUILLON:                          'Pré-contrat (brouillon)',
  EN_ATTENTE_VALIDATION_CLIENT:       'À valider par vous',
  EN_ATTENTE_VALIDATION_SUPER_ADMIN:  'En attente d\'activation',
  ACTIF:                              'Actif',
}
const STATUT_VARIANTS = {
  BROUILLON:                          'neutral',
  EN_ATTENTE_VALIDATION_CLIENT:       'warning',
  EN_ATTENTE_VALIDATION_SUPER_ADMIN:  'info',
  ACTIF:                              'success',
}

const peutValider = computed(() =>
  contrat.value &&
  ['BROUILLON', 'EN_ATTENTE_VALIDATION_CLIENT'].includes(contrat.value.statut))

async function fetchContrat() {
  loading.value = true
  error.value = ''
  try {
    const res = await contratService.getById(route.params.id)
    contrat.value = res.data.data
  } catch (e) {
    error.value = e.response?.status === 404 ? 'Pré-contrat introuvable.' : 'Impossible de charger ce pré-contrat.'
  } finally {
    loading.value = false
  }
}

async function valider() {
  validating.value = true
  try {
    await contratService.validerPrecontrat(contrat.value.id)
    toastStore.success('Pré-contrat validé. Il est transmis à l\'agence pour activation.')
    await fetchContrat()
  } catch (e) {
    toastStore.error(e.response?.data?.message || 'Erreur lors de la validation.')
  } finally {
    validating.value = false
  }
}

function formatMontant(v) {
  return v != null ? new Intl.NumberFormat('fr-SN').format(v) + ' FCFA' : '–'
}
function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' }) : '–'
}

onMounted(fetchContrat)
</script>

<template>
  <div class="pc-page">
    <div class="pc-container">
      <button class="pc-back" @click="router.push('/mes-contrats')">
        <ArrowLeft :size="16" /> Mes contrats
      </button>

      <div v-if="loading" class="pc-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="pc-error">{{ error }}</div>

      <template v-else-if="contrat">
        <div class="pc-header">
          <StatusBadge :label="STATUT_LABELS[contrat.statut] || contrat.statut" :variant="STATUT_VARIANTS[contrat.statut] || 'neutral'" />
          <h1 class="pc-title">Pré-contrat n°{{ contrat.id }}</h1>
          <p class="pc-sub">{{ contrat.annonceLibelle }}</p>
        </div>

        <div class="pc-card">
          <h2 class="pc-card__title">Conditions proposées</h2>
          <dl class="pc-dl">
            <div class="pc-dl__row"><dt>Bien</dt><dd>{{ contrat.annonceLibelle }}</dd></div>
            <div class="pc-dl__row"><dt>Adresse</dt><dd>{{ contrat.annonceAdresse || '–' }}</dd></div>
            <div class="pc-dl__row"><dt>Type</dt><dd>{{ contrat.typeContrat || '–' }}</dd></div>
            <div class="pc-dl__row"><dt>Montant</dt><dd class="pc-montant">{{ formatMontant(contrat.montant) }}</dd></div>
            <div class="pc-dl__row"><dt>Début</dt><dd>{{ formatDate(contrat.dateDebut) }}</dd></div>
            <div class="pc-dl__row" v-if="contrat.dateFin"><dt>Fin</dt><dd>{{ formatDate(contrat.dateFin) }}</dd></div>
            <div class="pc-dl__row" v-if="contrat.dureeLocationMois"><dt>Durée</dt><dd>{{ contrat.dureeLocationMois }} mois</dd></div>
          </dl>
          <a v-if="contrat.documentUrl" :href="contrat.documentUrl" target="_blank" class="pc-doc">Consulter le document du contrat</a>
        </div>

        <div class="pc-actions">
          <template v-if="peutValider">
            <p class="pc-note">
              En validant, vous acceptez les conditions ci-dessus. Le contrat sera ensuite activé par l'agence.
            </p>
            <ButtonPrimary :disabled="validating" @click="valider">
              {{ validating ? 'Validation…' : 'Valider le pré-contrat' }}
            </ButtonPrimary>
          </template>
          <p v-else-if="contrat.statut === 'EN_ATTENTE_VALIDATION_SUPER_ADMIN'" class="pc-note">
            Vous avez validé ce pré-contrat. Il est en attente d'activation par l'agence.
          </p>
          <p v-else class="pc-note">Ce contrat n'est pas en attente de votre validation (statut : {{ STATUT_LABELS[contrat.statut] || contrat.statut }}).</p>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.pc-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.pc-container { max-width: 720px; margin: 0 auto; }
.pc-back {
  display: inline-flex; align-items: center; gap: .4rem; background: none; border: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: var(--color-text); opacity: .6; padding: 0; margin-bottom: 1.5rem;
}
.pc-back:hover { opacity: 1; }
.pc-loading { display: flex; justify-content: center; padding: 4rem; }
.pc-error { text-align: center; padding: 3rem; color: var(--color-accent); font-weight: 600; }
.pc-header { margin-bottom: 1.5rem; }
.pc-title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); margin: .3rem 0 0; }
.pc-sub { font-size: .9rem; color: var(--color-text); opacity: .6; margin: .15rem 0 0; }
.pc-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); margin-bottom: 1.5rem; }
.pc-card__title { font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; opacity: .5; margin: 0 0 .85rem; }
.pc-dl { display: flex; flex-direction: column; gap: .55rem; }
.pc-dl__row { display: flex; justify-content: space-between; gap: .5rem; }
.pc-dl__row dt { font-size: .82rem; opacity: .55; }
.pc-dl__row dd { font-size: .88rem; font-weight: 600; text-align: right; }
.pc-montant { color: var(--color-accent); font-size: 1rem !important; }
.pc-doc { display: inline-block; margin-top: 1rem; color: var(--color-primary); font-weight: 600; font-size: .85rem; }
.pc-actions { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); display: flex; flex-direction: column; gap: 1rem; align-items: flex-start; }
.pc-note { font-size: .88rem; color: var(--color-text); opacity: .7; line-height: 1.6; }
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
