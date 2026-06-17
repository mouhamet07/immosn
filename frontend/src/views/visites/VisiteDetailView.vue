<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, MapPin } from 'lucide-vue-next'
import visiteService from '@/services/visiteService'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'

const route  = useRoute()
const router = useRouter()

const visite     = ref(null)
const loading    = ref(false)
const error      = ref('')
const cancelling = ref(false)

const showModif    = ref(false)
const modifLoading = ref(false)
const modifError   = ref('')
const formModif    = reactive({ dateVisite: '', commentaire: '' })

const STATUT_LABELS = {
  EN_ATTENTE:            'En attente',
  ACCEPTEE:              'Acceptée',
  REFUSEE:               'Refusée',
  ANNULEE:               'Annulée',
  CLOTUREE_SANS_SUITE:   'Clôturée sans suite',
  CLOTUREE_AVEC_CONTRAT: 'Clôturée avec contrat',
  TERMINEE:              'Terminée',
}
const STATUT_VARIANTS = {
  EN_ATTENTE:            'warning',
  ACCEPTEE:              'success',
  REFUSEE:               'danger',
  ANNULEE:               'neutral',
  CLOTUREE_SANS_SUITE:   'neutral',
  CLOTUREE_AVEC_CONTRAT: 'info',
  TERMINEE:              'info',
}

async function fetchVisite() {
  loading.value = true
  error.value   = ''
  try {
    const res = await visiteService.getById(route.params.id)
    visite.value = res.data.data
    // Pré-remplir le formulaire de modification avec la date actuelle
    if (visite.value?.dateVisite) {
      formModif.dateVisite = visite.value.dateVisite.slice(0, 16)
    }
    formModif.commentaire = visite.value?.commentaire || ''
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Demande de visite introuvable.'
      : 'Impossible de charger cette demande.'
  } finally {
    loading.value = false
  }
}

async function annuler() {
  if (!confirm('Confirmer l\'annulation de cette demande ?')) return
  cancelling.value = true
  try {
    await visiteService.annuler(visite.value.id)
    await fetchVisite()
  } catch {
    alert('Erreur lors de l\'annulation.')
  } finally {
    cancelling.value = false
  }
}

async function modifierVisite() {
  if (!formModif.dateVisite) { modifError.value = 'La date est obligatoire.'; return }
  modifLoading.value = true
  modifError.value   = ''
  try {
    await visiteService.modifierVisite(visite.value.id, formModif.dateVisite, formModif.commentaire || null)
    await fetchVisite()
    showModif.value = false
  } catch (e) {
    modifError.value = e.response?.data?.message || 'Erreur lors de la modification.'
  } finally {
    modifLoading.value = false
  }
}

function formatDate(d) {
  if (!d) return '–'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function formatDatetime(d) {
  if (!d) return '–'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchVisite())
</script>

<template>
  <div class="vd-page">
    <div class="vd-container">

      <button class="vd-back" @click="router.push('/mes-visites')">
        <ArrowLeft :size="16" /> Mes demandes de visite
      </button>

      <div v-if="loading" class="vd-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="vd-error">{{ error }}</div>

      <template v-else-if="visite">
        <!-- En-tête -->
        <div class="vd-header">
          <div>
            <StatusBadge :label="STATUT_LABELS[visite.statut]" :variant="STATUT_VARIANTS[visite.statut]" />
            <h1 class="vd-header__title">Demande de visite {{ visite.id }}</h1>
          </div>
        </div>

        <!-- Image annonce -->
        <div class="vd-image">
          <ImageGallery :images="visite.imagePrincipale ? [visite.imagePrincipale] : []" :alt="visite.annonceLibelle" aspect="16 / 9" max-height="380px" />
        </div>

        <!-- Informations -->
        <div class="vd-grid">
          <div class="vd-card">
            <h2 class="vd-card__title">Bien immobilier</h2>
            <dl class="vd-dl">
              <div class="vd-dl__row">
                <dt>Libellé</dt>
                <dd>
                  <RouterLink :to="`/annonces/${visite.annonceId}`" class="vd-link">{{ visite.annonceLibelle }}</RouterLink>
                </dd>
              </div>
              <div class="vd-dl__row">
                <dt>Adresse</dt>
                <dd><span class="vd-addr"><MapPin :size="12" /> {{ visite.annonceAdresse || '–' }}</span></dd>
              </div>
            </dl>
          </div>

          <div class="vd-card">
            <h2 class="vd-card__title">Détails de la visite</h2>
            <dl class="vd-dl">
              <div class="vd-dl__row">
                <dt>Date souhaitée</dt>
                <dd class="vd-date-val">{{ formatDate(visite.dateVisite) }}</dd>
              </div>
              <div class="vd-dl__row" v-if="visite.commentaire">
                <dt>Commentaire</dt>
                <dd>{{ visite.commentaire }}</dd>
              </div>
            </dl>
          </div>

          <div class="vd-card vd-card--full">
            <h2 class="vd-card__title">Historique</h2>
            <dl class="vd-dl">
              <div class="vd-dl__row">
                <dt>Demande créée le</dt>
                <dd>{{ formatDatetime(visite.createdAt) }}</dd>
              </div>
              <div class="vd-dl__row">
                <dt>Mise à jour le</dt>
                <dd>{{ formatDatetime(visite.updatedAt) }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- Actions CLIENT -->
        <div class="vd-actions">
          <!-- EN_ATTENTE : modifier + annuler -->
          <template v-if="visite.statut === 'EN_ATTENTE'">
            <p class="vd-info">Votre demande est en attente de validation par l'administration.</p>

            <div class="vd-btn-row">
              <button class="vd-btn vd-btn--primary" @click="showModif = !showModif">
                {{ showModif ? 'Annuler la modification' : 'Modifier la demande' }}
              </button>
              <button class="vd-btn vd-btn--danger" :disabled="cancelling" @click="annuler">
                {{ cancelling ? '…' : 'Annuler la demande' }}
              </button>
            </div>

            <div v-if="showModif" class="vd-modif-form">
              <label class="vd-modif-label">Nouvelle date souhaitée</label>
              <input
                v-model="formModif.dateVisite"
                type="datetime-local"
                class="vd-modif-input"
              />
              <label class="vd-modif-label">Commentaire (optionnel)</label>
              <textarea
                v-model="formModif.commentaire"
                class="vd-modif-textarea"
                rows="3"
                placeholder="Message à l'administration…"
              ></textarea>
              <p v-if="modifError" class="vd-modif-error">{{ modifError }}</p>
              <button class="vd-btn vd-btn--primary" :disabled="modifLoading" @click="modifierVisite">
                {{ modifLoading ? '…' : 'Enregistrer les modifications' }}
              </button>
            </div>
          </template>

          <!-- ACCEPTEE : annuler reste possible -->
          <template v-else-if="visite.statut === 'ACCEPTEE'">
            <p class="vd-info vd-info--success">Votre visite a été acceptée. La date confirmée est le {{ formatDate(visite.dateVisite) }}.</p>
            <button class="vd-btn vd-btn--danger" :disabled="cancelling" @click="annuler">
              {{ cancelling ? '…' : 'Annuler la visite' }}
            </button>
          </template>

          <!-- REFUSEE -->
          <p v-else-if="visite.statut === 'REFUSEE'" class="vd-info vd-info--danger">
            Votre demande a été refusée.
          </p>

          <!-- ANNULEE -->
          <p v-else-if="visite.statut === 'ANNULEE'" class="vd-info">
            Cette demande a été annulée.
          </p>

          <!-- CLOTUREE_AVEC_CONTRAT -->
          <p v-else-if="visite.statut === 'CLOTUREE_AVEC_CONTRAT'" class="vd-info vd-info--success">
            La visite s'est conclue par un contrat. Retrouvez-le dans <RouterLink to="/mes-contrats" class="vd-link">Mes contrats</RouterLink>.
          </p>

          <!-- CLOTUREE_SANS_SUITE -->
          <p v-else-if="visite.statut === 'CLOTUREE_SANS_SUITE'" class="vd-info">
            La visite a été clôturée sans suite.
          </p>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.vd-page { background: var(--color-background); min-height: 100vh; }
.vd-container { max-width: 780px; margin: 0 auto; padding: 2rem 1.5rem; }

.vd-back {
  display: inline-flex; align-items: center; gap: .4rem;
  background: none; border: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: var(--color-text); opacity: .6;
  padding: 0; margin-bottom: 1.5rem; transition: opacity .15s;
}
.vd-back:hover { opacity: 1; }

.vd-loading { display: flex; justify-content: center; padding: 4rem; }
.vd-error { text-align: center; padding: 3rem; color: var(--color-accent); font-weight: 600; }

.vd-header { margin-bottom: 1.5rem; display: flex; flex-direction: column; gap: .4rem; }
.vd-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); margin: .3rem 0 0; }

.vd-image { margin-bottom: 1.5rem; }

.vd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.vd-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.vd-card--full { grid-column: 1 / -1; }
.vd-card__title {
  font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
  color: var(--color-text); opacity: .5; margin: 0 0 .85rem;
}

.vd-dl { display: flex; flex-direction: column; gap: .6rem; }
.vd-dl__row { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.vd-dl__row dt { font-size: .82rem; color: var(--color-text); opacity: .55; flex-shrink: 0; }
.vd-dl__row dd { font-size: .88rem; font-weight: 600; color: var(--color-text); text-align: right; }

.vd-date-val { color: var(--color-primary); font-size: .9rem !important; }
.vd-addr { display: inline-flex; align-items: center; gap: .3rem; }
.vd-link { color: var(--color-primary); text-decoration: none; font-weight: 600; }
.vd-link:hover { text-decoration: underline; }

.vd-actions { display: flex; flex-direction: column; gap: .75rem; }
.vd-btn-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.vd-info {
  font-size: .88rem; color: var(--color-text); opacity: .7; padding: .75rem 1rem;
  background: var(--color-card); border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary); margin: 0;
}
.vd-info--success { border-left-color: var(--color-primary); }
.vd-info--danger  { border-left-color: var(--color-accent); }

.vd-btn {
  align-self: flex-start; padding: .55rem 1.2rem; border-radius: var(--radius-sm);
  font-size: .88rem; font-weight: 600; cursor: pointer; transition: opacity .2s; border: none;
}
.vd-btn--danger   { background: none; border: 1.5px solid #e53e3e; color: #e53e3e; }
.vd-btn--primary  { background: var(--color-primary); color: #fff; border: none; }
.vd-btn:hover:not(:disabled) { opacity: .8; }
.vd-btn:disabled { opacity: .4; cursor: not-allowed; }

.vd-modif-form {
  background: var(--color-card); border-radius: var(--radius); padding: 1.25rem;
  border: 1.5px solid var(--color-primary); display: flex; flex-direction: column; gap: .75rem;
}
.vd-modif-label { font-size: .8rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text); }
.vd-modif-input, .vd-modif-textarea {
  padding: .65rem .85rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .9rem; color: var(--color-text); background: var(--color-background);
  font-family: inherit; transition: border-color .2s;
}
.vd-modif-input:focus, .vd-modif-textarea:focus { border-color: var(--color-primary); outline: none; }
.vd-modif-textarea { resize: vertical; }
.vd-modif-error { font-size: .85rem; color: var(--color-accent); margin: 0; font-weight: 600; }


.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) { .vd-grid { grid-template-columns: 1fr; } }
</style>
