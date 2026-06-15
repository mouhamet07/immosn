<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, MapPin, Pencil, Check, X } from 'lucide-vue-next'
import visiteService from '@/services/visiteService'
import historyService from '@/services/historyService'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'

const route  = useRoute()
const router = useRouter()

const visite    = ref(null)
const loading   = ref(false)
const error     = ref('')
const updating  = ref(false)

// Modal modifier date
const showDateModal = ref(false)
const newDate       = ref('')
const dateComment   = ref('')

// Modal clôture
const showCloture        = ref(false)
const clotureType        = ref('SANS_SUITE')
const clotureContratType = ref('VENTE')
const clotureDuree       = ref(12)
const cloturing          = ref(false)

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
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Demande de visite introuvable.'
      : 'Impossible de charger cette demande.'
  } finally {
    loading.value = false
  }
}

async function accepter() {
  updating.value = true
  try {
    await visiteService.updateStatut(visite.value.id, 'ACCEPTEE')
    await fetchVisite()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur.')
  } finally {
    updating.value = false }
}

async function refuser() {
  if (!confirm('Confirmer le refus de cette demande ?')) return
  updating.value = true
  try {
    await visiteService.updateStatut(visite.value.id, 'REFUSEE')
    await fetchVisite()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur.')
  } finally {
    updating.value = false
  }
}

async function submitDate() {
  if (!newDate.value) return
  try {
    await visiteService.updateDate(visite.value.id, newDate.value, dateComment.value || null)
    showDateModal.value = false
    await fetchVisite()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur.')
  }
}

function openDateModal() {
  newDate.value      = visite.value.dateVisite ? visite.value.dateVisite.slice(0, 16) : ''
  dateComment.value  = ''
  showDateModal.value = true
}

function openCloture() {
  clotureType.value        = 'SANS_SUITE'
  clotureContratType.value = 'VENTE'
  clotureDuree.value       = 12
  showCloture.value        = true
}

async function submitCloture() {
  if (clotureType.value === 'AVEC_CONTRAT' && !clotureContratType.value) {
    alert('Veuillez sélectionner le type de contrat.')
    return
  }
  cloturing.value = true
  try {
    const res = await visiteService.cloturerVisite(visite.value.id, {
      type:              clotureType.value,
      typeContrat:       clotureType.value === 'AVEC_CONTRAT' ? clotureContratType.value : null,
      dureeLocationMois: clotureType.value === 'AVEC_CONTRAT' && clotureContratType.value === 'LOCATION'
                           ? Number(clotureDuree.value) : null,
    })
    showCloture.value = false
    const contratId = res?.data?.data?.id
    if (clotureType.value === 'AVEC_CONTRAT' && contratId) {
      router.push(`/admin/contrats/${contratId}`)
    } else {
      await fetchVisite()
    }
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors de la clôture.')
  } finally {
    cloturing.value = false
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

// Onglet Histoire
const tab               = ref('detail')
const historique        = ref([])
const historiqueLoading = ref(false)

const ACTION_LABELS = {
  CREATION:              'Création',
  ACCEPTATION:           'Acceptation',
  REFUS:                 'Refus',
  ANNULATION:            'Annulation',
  CLOTURE_SANS_SUITE:    'Clôturée sans suite',
  CLOTURE_AVEC_CONTRAT:  'Clôturée avec contrat',
  REPROGRAMMATION:       'Reprogrammation (admin)',
  REPROGRAMMATION_CLIENT:'Reprogrammation (client)',
}

async function fetchHistorique() {
  historiqueLoading.value = true
  try {
    const res = await historyService.getVisiteHistory(route.params.id)
    historique.value = res.data.data ?? []
  } catch { /* silently fail */ } finally { historiqueLoading.value = false }
}

function switchTab(t) {
  tab.value = t
  if (t === 'histoire') fetchHistorique()
}

onMounted(() => fetchVisite())
</script>

<template>
  <div class="vda-page">
    <div class="vda-container">

      <button class="vda-back" @click="router.push('/admin/visites')">
        <ArrowLeft :size="16" /> Visites
      </button>

      <div v-if="loading" class="vda-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="vda-error">{{ error }}</div>

      <template v-else-if="visite">
        <!-- En-tête -->
        <div class="vda-header">
          <div>
            <StatusBadge :label="STATUT_LABELS[visite.statut]" :variant="STATUT_VARIANTS[visite.statut]" />
            <h1 class="vda-header__title">Demande de visite #{{ visite.id }}</h1>
            <p class="vda-header__sub">{{ visite.clientNom }}</p>
          </div>
          <div class="vda-header__actions" v-if="visite.statut === 'ACCEPTEE'">
            <button class="vda-btn vda-btn--outline" @click="openDateModal"><Pencil :size="13" /> Modifier la date</button>
          </div>
        </div>

        <!-- Onglets Détail / Histoire -->
        <div class="tabs">
          <button :class="['tab', { 'tab--active': tab === 'detail' }]" @click="switchTab('detail')">Détail</button>
          <button :class="['tab', { 'tab--active': tab === 'histoire' }]" @click="switchTab('histoire')">Histoire</button>
        </div>

        <div v-show="tab === 'detail'">
        <!-- Image -->
        <div class="vda-image">
          <ImageGallery :images="visite.imagePrincipale ? [visite.imagePrincipale] : []" :alt="visite.annonceLibelle" aspect="16 / 9" max-height="380px" />
        </div>

        <!-- Grille -->
        <div class="vda-grid">
          <div class="vda-card">
            <h2 class="vda-card__title">Client</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row"><dt>Nom</dt><dd>{{ visite.clientNom }}</dd></div>
              <div class="vda-dl__row"><dt>ID client</dt><dd>#{{ visite.clientId }}</dd></div>
            </dl>
          </div>

          <div class="vda-card">
            <h2 class="vda-card__title">Bien immobilier</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row">
                <dt>Libellé</dt>
                <dd><RouterLink :to="`/admin/annonces/${visite.annonceId}`" class="vda-link">{{ visite.annonceLibelle }}</RouterLink></dd>
              </div>
              <div class="vda-dl__row">
                <dt>Adresse</dt>
                <dd><span class="vda-addr"><MapPin :size="11" /> {{ visite.annonceAdresse || '–' }}</span></dd>
              </div>
            </dl>
          </div>

          <div class="vda-card">
            <h2 class="vda-card__title">Détails de la visite</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row">
                <dt>Date souhaitée</dt>
                <dd class="vda-date-val">{{ formatDate(visite.dateVisite) }}</dd>
              </div>
              <div class="vda-dl__row" v-if="visite.commentaire">
                <dt>Commentaire</dt>
                <dd>{{ visite.commentaire }}</dd>
              </div>
            </dl>
          </div>

          <div class="vda-card">
            <h2 class="vda-card__title">Historique</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row"><dt>Créée le</dt><dd>{{ formatDatetime(visite.createdAt) }}</dd></div>
              <div class="vda-dl__row"><dt>Mise à jour</dt><dd>{{ formatDatetime(visite.updatedAt) }}</dd></div>
            </dl>
          </div>
        </div>

        <!-- Actions admin selon statut -->
        <div class="vda-actions-section">
          <h2 class="vda-actions-section__title">Actions disponibles</h2>

          <!-- EN_ATTENTE : accepter ou refuser -->
          <div v-if="visite.statut === 'EN_ATTENTE'" class="vda-actions">
            <button class="vda-btn vda-btn--accept" :disabled="updating" @click="accepter"><Check :size="14" /> Accepter</button>
            <button class="vda-btn vda-btn--refuse" :disabled="updating" @click="refuser"><X :size="14" /> Refuser</button>
          </div>

          <!-- ACCEPTEE : clôturer -->
          <div v-else-if="visite.statut === 'ACCEPTEE'" class="vda-actions">
            <button class="vda-btn vda-btn--cloture" @click="openCloture">Clôturer la visite</button>
          </div>

          <!-- Autres statuts : lecture seule -->
          <div v-else class="vda-actions">
            <p class="vda-readonly">Ce dossier est en lecture seule (statut : {{ STATUT_LABELS[visite.statut] }}).</p>
          </div>
        </div>
        </div><!-- /tab-detail -->

        <!-- Onglet Histoire -->
        <div v-show="tab === 'histoire'" class="hist-section">
          <div v-if="historiqueLoading" class="hist-empty">Chargement de l'historique…</div>
          <div v-else-if="!historique.length" class="hist-empty">Aucun événement enregistré pour cette visite.</div>
          <div v-else class="hist-list">
            <div v-for="evt in historique" :key="evt.id" class="hist-item">
              <div class="hist-item__dot"></div>
              <div class="hist-item__card">
                <div class="hist-item__header">
                  <span class="hist-item__action">{{ ACTION_LABELS[evt.action] || evt.action }}</span>
                  <span class="hist-item__date">{{ formatDatetime(evt.createdAt) }}</span>
                </div>
                <div v-if="evt.ancienStatut || evt.nouveauStatut" class="hist-item__statuts">
                  <span v-if="evt.ancienStatut" class="hist-statut hist-statut--old">{{ STATUT_LABELS[evt.ancienStatut] || evt.ancienStatut }}</span>
                  <span v-if="evt.ancienStatut && evt.nouveauStatut" class="hist-arrow">→</span>
                  <span v-if="evt.nouveauStatut" class="hist-statut hist-statut--new">{{ STATUT_LABELS[evt.nouveauStatut] || evt.nouveauStatut }}</span>
                </div>
                <div v-if="evt.ancienneDateVisite || evt.nouvelleDateVisite" class="hist-item__dates">
                  <span v-if="evt.ancienneDateVisite" class="hist-date hist-date--old">{{ formatDate(evt.ancienneDateVisite) }}</span>
                  <span v-if="evt.ancienneDateVisite && evt.nouvelleDateVisite" class="hist-arrow">→</span>
                  <span v-if="evt.nouvelleDateVisite" class="hist-date hist-date--new">{{ formatDate(evt.nouvelleDateVisite) }}</span>
                </div>
                <div class="hist-item__auteur">{{ evt.auteurEmail }}</div>
                <div v-if="evt.commentaire" class="hist-item__comment">{{ evt.commentaire }}</div>
              </div>
            </div>
          </div>
        </div><!-- /tab-histoire -->

      </template>
    </div>

    <!-- Modal modifier date -->
    <Teleport to="body">
      <div v-if="showDateModal" class="modal-overlay" @click.self="showDateModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Modifier la date de visite</h2>
          <div class="modal-box__field">
            <label>Nouvelle date et heure</label>
            <input v-model="newDate" type="datetime-local" class="modal-box__input" />
          </div>
          <div class="modal-box__field">
            <label>Commentaire (optionnel)</label>
            <input v-model="dateComment" type="text" class="modal-box__input" placeholder="Raison du changement…" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showDateModal = false">Annuler</button>
            <button class="modal-box__submit" @click="submitDate">Confirmer</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal clôture -->
    <Teleport to="body">
      <div v-if="showCloture" class="modal-overlay" @click.self="showCloture = false">
        <div class="modal-box modal-box--lg">
          <h2 class="modal-box__title">Clôturer la visite #{{ visite?.id }}</h2>
          <p class="modal-box__desc">La visite a été effectuée. Quelle est l'issue ?</p>

          <div class="cloture-options">
            <label class="cloture-option" :class="{ '--selected': clotureType === 'SANS_SUITE' }">
              <input type="radio" v-model="clotureType" value="SANS_SUITE" />
              <div class="cloture-option__content">
                <span class="cloture-option__title">Sans suite</span>
                <span class="cloture-option__desc">Le client n'est pas intéressé. Aucun contrat ne sera créé.</span>
              </div>
            </label>
            <label class="cloture-option" :class="{ '--selected': clotureType === 'AVEC_CONTRAT' }">
              <input type="radio" v-model="clotureType" value="AVEC_CONTRAT" />
              <div class="cloture-option__content">
                <span class="cloture-option__title">Avec contrat</span>
                <span class="cloture-option__desc">Le client souhaite poursuivre. Un contrat sera créé automatiquement.</span>
              </div>
            </label>
          </div>

          <div v-if="clotureType === 'AVEC_CONTRAT'" class="cloture-contrat-fields">
            <div class="modal-box__field">
              <label>Type de contrat *</label>
              <div class="type-radios">
                <label class="type-radio" :class="{ '--active': clotureContratType === 'VENTE' }">
                  <input type="radio" v-model="clotureContratType" value="VENTE" /> Vente
                </label>
                <label class="type-radio" :class="{ '--active': clotureContratType === 'LOCATION' }">
                  <input type="radio" v-model="clotureContratType" value="LOCATION" /> Location
                </label>
              </div>
            </div>
            <div v-if="clotureContratType === 'LOCATION'" class="modal-box__field">
              <label>Durée du bail *</label>
              <div class="duree-radios">
                <label v-for="d in [6, 12, 24, 36]" :key="d" class="type-radio" :class="{ '--active': clotureDuree === d }">
                  <input type="radio" v-model="clotureDuree" :value="d" /> {{ d }} mois
                </label>
              </div>
            </div>
            <p class="cloture-info">
              Le montant sera pré-rempli avec le prix de l'annonce et pourra être ajusté dans la fiche contrat.
            </p>
          </div>

          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showCloture = false">Annuler</button>
            <button class="modal-box__submit"
              :class="clotureType === 'AVEC_CONTRAT' ? '' : 'modal-box__submit--neutral'"
              :disabled="cloturing" @click="submitCloture">
              {{ cloturing ? 'En cours…' : clotureType === 'AVEC_CONTRAT' ? 'Clôturer et créer le contrat' : 'Clôturer sans suite' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.vda-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.vda-container { max-width: 900px; margin: 0 auto; }

.vda-back {
  display: inline-flex; align-items: center; gap: .4rem;
  background: none; border: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: var(--color-text); opacity: .6;
  padding: 0; margin-bottom: 1.5rem; transition: opacity .15s;
}
.vda-back:hover { opacity: 1; }

.vda-loading { display: flex; justify-content: center; padding: 4rem; }
.vda-error { text-align: center; padding: 3rem; color: var(--color-accent); font-weight: 600; }

.vda-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.vda-header__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); margin: .3rem 0 0; }
.vda-header__sub { font-size: .9rem; color: var(--color-text); opacity: .6; margin: .15rem 0 0; }
.vda-header__actions { display: flex; gap: .5rem; align-items: flex-start; }

.vda-image { margin-bottom: 1.5rem; }

.vda-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.vda-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.vda-card__title {
  font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
  color: var(--color-text); opacity: .5; margin: 0 0 .85rem;
}

.vda-dl { display: flex; flex-direction: column; gap: .55rem; }
.vda-dl__row { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.vda-dl__row dt { font-size: .82rem; color: var(--color-text); opacity: .55; flex-shrink: 0; }
.vda-dl__row dd { font-size: .88rem; font-weight: 600; color: var(--color-text); text-align: right; }
.vda-date-val { color: var(--color-primary); font-size: .9rem !important; }
.vda-addr { display: inline-flex; align-items: center; gap: .3rem; }
.vda-link { color: var(--color-primary); text-decoration: none; font-weight: 600; }

.vda-actions-section { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.vda-actions-section__title { font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .5; margin: 0 0 1rem; }
.vda-actions { display: flex; gap: .75rem; flex-wrap: wrap; align-items: center; }
.vda-readonly { font-size: .88rem; color: var(--color-text); opacity: .55; font-style: italic; }

.vda-btn {
  padding: .5rem 1.1rem; border-radius: var(--radius-sm); font-size: .85rem;
  font-weight: 600; cursor: pointer; border: none; transition: opacity .15s;
}
.vda-btn:disabled { opacity: .4; cursor: not-allowed; }
.vda-btn--accept  { background:none; border: 1.5px solid var(--color-primary); color: var(--color-primary); }
.vda-btn--accept:hover:not(:disabled)  { background: var(--color-primary); opacity: 1; color: #fff;}
.vda-btn--refuse  { background: none; border: 1.5px solid var(--color-accent); color: var(--color-accent); }
.vda-btn--refuse:hover:not(:disabled)  { background: var(--color-accent); opacity: 1; color: #fff; }
.vda-btn--cloture { background: var(--color-primary); color: #fff; }
.vda-btn--cloture:hover:not(:disabled) { background: var(--color-primary-hover); opacity: 1; }
.vda-btn--outline { background: none; border: 1.5px solid var(--color-primary); color: var(--color-primary); }
.vda-btn--outline:hover { background: var(--color-primary); color: #fff; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box--lg { max-width: 520px; }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .35rem; }
.modal-box__desc { font-size: .85rem; color: var(--color-text); opacity: .6; margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; transition: opacity .15s; }
.modal-box__submit--neutral { background: #6b7280; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.cloture-options { display: flex; flex-direction: column; gap: .6rem; margin-bottom: 1.25rem; }
.cloture-option {
  display: flex; align-items: flex-start; gap: .75rem;
  padding: .85rem 1rem; border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm); cursor: pointer; transition: border-color .15s, background .15s;
}
.cloture-option input[type="radio"] { margin-top: .1rem; accent-color: var(--color-primary); flex-shrink: 0; }
.cloture-option.--selected { border-color: var(--color-primary); background: rgba(74,124,111,.06); }
.cloture-option__content { display: flex; flex-direction: column; gap: .2rem; }
.cloture-option__title { font-size: .9rem; font-weight: 700; color: var(--color-text); }
.cloture-option__desc { font-size: .78rem; color: var(--color-text); opacity: .6; }

.cloture-contrat-fields { border-top: 1px solid var(--color-border); padding-top: 1rem; margin-top: .25rem; }
.type-radios, .duree-radios { display: flex; gap: .5rem; flex-wrap: wrap; }
.type-radio {
  display: flex; align-items: center; gap: .35rem;
  padding: .35rem .85rem; border: 1.5px solid var(--color-border);
  border-radius: 20px; font-size: .82rem; font-weight: 500;
  color: var(--color-text); cursor: pointer; transition: all .15s;
}
.type-radio input[type="radio"] { display: none; }
.type-radio.--active { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }

.cloture-info {
  font-size: .75rem; color: var(--color-text); opacity: .55; font-style: italic;
  margin-top: .5rem; padding: .5rem .75rem; background: var(--color-background); border-radius: 6px;
}

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) { .vda-grid { grid-template-columns: 1fr; } }

/* Onglets */
.tabs { display: flex; gap: .25rem; margin-bottom: 1.5rem; border-bottom: 2px solid var(--color-border); }
.tab {
  padding: .6rem 1.25rem; background: none; border: none; cursor: pointer;
  font-size: .88rem; font-weight: 600; color: var(--color-text); opacity: .5;
  border-bottom: 2px solid transparent; margin-bottom: -2px; transition: opacity .15s, border-color .15s;
}
.tab:hover { opacity: .75; }
.tab--active { opacity: 1; border-bottom-color: var(--color-primary); color: var(--color-primary); }

/* Timeline historique */
.hist-section { padding-top: .5rem; }
.hist-empty { text-align: center; padding: 3rem 1rem; font-size: .88rem; color: var(--color-text); opacity: .45; font-style: italic; }
.hist-list { display: flex; flex-direction: column; position: relative; padding-left: 1.5rem; }
.hist-list::before { content: ''; position: absolute; left: .45rem; top: .6rem; bottom: .6rem; width: 2px; background: var(--color-border); }
.hist-item { display: flex; gap: 1rem; position: relative; padding-bottom: 1.25rem; }
.hist-item:last-child { padding-bottom: 0; }
.hist-item__dot { position: absolute; left: -1.5rem; top: .35rem; width: 10px; height: 10px; border-radius: 50%; background: var(--color-primary); border: 2px solid var(--color-background); flex-shrink: 0; }
.hist-item__card { flex: 1; background: var(--color-card); border-radius: var(--radius-sm); padding: .85rem 1rem; box-shadow: var(--shadow-card); }
.hist-item__header { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; flex-wrap: wrap; margin-bottom: .4rem; }
.hist-item__action { font-size: .88rem; font-weight: 700; color: var(--color-text); }
.hist-item__date { font-size: .75rem; color: var(--color-text); opacity: .5; white-space: nowrap; }
.hist-item__statuts, .hist-item__dates { display: flex; align-items: center; gap: .4rem; margin-bottom: .35rem; flex-wrap: wrap; }
.hist-statut, .hist-date { padding: .15rem .55rem; border-radius: 8px; font-size: .75rem; font-weight: 600; }
.hist-statut--old, .hist-date--old { background: rgba(107,114,128,.1); color: #6b7280; }
.hist-statut--new, .hist-date--new { background: rgba(74,124,111,.12); color: #3a6b5e; }
.hist-arrow { font-size: .8rem; color: var(--color-text); opacity: .4; }
.hist-item__auteur { font-size: .78rem; color: var(--color-text); opacity: .5; }
.hist-item__comment { font-size: .82rem; color: var(--color-text); opacity: .7; font-style: italic; margin-top: .3rem; line-height: 1.5; }
</style>
