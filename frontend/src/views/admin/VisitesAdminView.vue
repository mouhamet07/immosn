<script setup>
import { ref, onMounted } from 'vue'
import visiteService from '@/services/visiteService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const visites     = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')
const updating    = ref(null)

// Modal modifier date
const showDateModal = ref(false)
const dateModalId   = ref(null)
const newDate       = ref('')
const dateComment   = ref('')

// Modal clôture
const showCloture   = ref(false)
const clotureVisiteId = ref(null)
const clotureType   = ref('SANS_SUITE')       // 'SANS_SUITE' | 'AVEC_CONTRAT'
const clotureContratType = ref('VENTE')        // 'VENTE' | 'LOCATION'
const clotureDuree  = ref(12)                  // mois
const cloturing     = ref(false)

const STATUTS = ['', 'EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE', 'CLOTUREE_SANS_SUITE', 'CLOTUREE_AVEC_CONTRAT', 'TERMINEE']
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
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Toutes les visites' }))

async function fetchVisites(page = 0) {
  loading.value = true
  try {
    const res = await visiteService.getAllVisites(page, 20, filtreStatut.value || null)
    visites.value     = res.data.content ?? res.data.data ?? []
    currentPage.value = res.data.currentPage ?? res.data.number ?? 0
    totalPages.value  = res.data.totalPages ?? 1
    totalItems.value  = res.data.totalElements ?? 0
  } catch { visites.value = [] }
  finally { loading.value = false }
}

async function changeStatut(id, statut) {
  updating.value = id
  try {
    await visiteService.updateStatut(id, statut)
    await fetchVisites(currentPage.value)
  } catch { alert('Erreur.') }
  finally { updating.value = null }
}

async function submitDate() {
  if (!newDate.value) return
  try {
    await visiteService.updateDate(dateModalId.value, newDate.value, dateComment.value)
    showDateModal.value = false
    await fetchVisites(currentPage.value)
  } catch { alert('Erreur.') }
}

function openDateModal(id, currentDate) {
  dateModalId.value = id
  newDate.value = currentDate ? currentDate.slice(0, 16) : ''
  dateComment.value = ''
  showDateModal.value = true
}

function openCloture(visiteId) {
  clotureVisiteId.value   = visiteId
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
    await visiteService.cloturerVisite(clotureVisiteId.value, {
      type:              clotureType.value,
      typeContrat:       clotureType.value === 'AVEC_CONTRAT' ? clotureContratType.value : null,
      dureeLocationMois: clotureType.value === 'AVEC_CONTRAT' && clotureContratType.value === 'LOCATION'
                           ? Number(clotureDuree.value) : null,
    })
    showCloture.value = false
    await fetchVisites(currentPage.value)
  } catch (err) {
    alert(err.response?.data?.message || 'Erreur lors de la clôture.')
  } finally {
    cloturing.value = false
  }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchVisites(0))
</script>

<template>
  <div class="va-page">
    <div class="va-toolbar">
      <div>
        <h1 class="va-toolbar__title">Demandes de visites</h1>
        <p class="va-toolbar__count">{{ totalItems }} demande{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchVisites(0) }"
      />
    </div>

    <div v-if="loading" class="va-loading"><div class="spinner"></div></div>

    <div v-else-if="!visites.length" class="va-empty">Aucune demande.</div>

    <div v-else class="va-table-wrap">
      <table class="va-table">
        <thead>
          <tr>
            <th>Client</th><th>Annonce</th><th>Date souhaitée</th>
            <th>Statut</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in visites" :key="v.id">
            <td class="table-client">{{ v.clientNom }}</td>
            <td>
              <RouterLink :to="`/annonces/${v.annonceId}`" class="va-td-link">{{ v.annonceLibelle }}</RouterLink>
              <p class="va-td-sub">{{ v.annonceAdresse }}</p>
            </td>
            <td>
              <p class="table-date">{{ formatDate(v.dateVisite) }}</p>
              <button class="va-edit-date" @click="openDateModal(v.id, v.dateVisite)">✎ Modifier</button>
            </td>
            <td><StatusBadge :label="STATUT_LABELS[v.statut] ?? v.statut" :variant="STATUT_VARIANTS[v.statut] ?? 'neutral'" /></td>
            <td>
              <div class="td-actions">
                <RouterLink :to="`/admin/visites/${v.id}`" class="action-btn" title="Voir le détail">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </RouterLink>
                <template v-if="v.statut === 'EN_ATTENTE'">
                  <button class="va-btn va-btn--accept" :disabled="updating === v.id"
                    @click="changeStatut(v.id, 'ACCEPTEE')">Accepter</button>
                  <button class="va-btn va-btn--refuse" :disabled="updating === v.id"
                    @click="changeStatut(v.id, 'REFUSEE')">Refuser</button>
                </template>
                <button v-else-if="v.statut === 'ACCEPTEE'" class="va-btn va-btn--cloture" :disabled="updating === v.id"
                  @click="openCloture(v.id)">Clôturer</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="va-pager">
      <button :disabled="currentPage === 0" @click="fetchVisites(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchVisites(currentPage + 1)">Suivant →</button>
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

    <!-- Modal clôture visite -->
    <Teleport to="body">
      <div v-if="showCloture" class="modal-overlay" @click.self="showCloture = false">
        <div class="modal-box modal-box--lg">
          <h2 class="modal-box__title">Clôturer la visite #{{ clotureVisiteId }}</h2>
          <p class="modal-box__desc">La visite a été effectuée. Quelle est l'issue ?</p>

          <!-- Choix du type de clôture -->
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

          <!-- Champs supplémentaires si AVEC_CONTRAT -->
          <div v-if="clotureType === 'AVEC_CONTRAT'" class="cloture-contrat-fields">
            <div class="modal-box__field">
              <label>Type de contrat *</label>
              <div class="type-radios">
                <label class="type-radio" :class="{ '--active': clotureContratType === 'VENTE' }">
                  <input type="radio" v-model="clotureContratType" value="VENTE" />
                  Vente
                </label>
                <label class="type-radio" :class="{ '--active': clotureContratType === 'LOCATION' }">
                  <input type="radio" v-model="clotureContratType" value="LOCATION" />
                  Location
                </label>
              </div>
            </div>
            <div v-if="clotureContratType === 'LOCATION'" class="modal-box__field">
              <label>Durée du bail *</label>
              <div class="duree-radios">
                <label v-for="d in [6, 12, 24, 36]" :key="d"
                  class="type-radio" :class="{ '--active': clotureDuree === d }">
                  <input type="radio" v-model="clotureDuree" :value="d" />
                  {{ d }} mois
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
              :class="clotureType === 'AVEC_CONTRAT' ? 'modal-box__submit--primary' : 'modal-box__submit--neutral'"
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
.va-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.va-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.va-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.va-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; margin-top: .15rem; }
.va-loading { display: flex; justify-content: center; padding: 4rem; }
.va-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }

.va-table-wrap { overflow-x: auto; }
.va-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.va-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.va-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.va-table tr:last-child td { border-bottom: none; }
.va-table tr:hover td { background: rgba(0,0,0,.02); }

.va-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.va-td-sub { font-size: .75rem; opacity: .5; margin-top: .15rem; }
.va-edit-date { font-size: .72rem; color: var(--color-primary); background: none; border: none; cursor: pointer; padding: 0; margin-top: .15rem; }

.va-btn { padding: .3rem .7rem; border-radius: 6px; font-size: .78rem; font-weight: 600; border: none; cursor: pointer; transition: opacity .15s; text-decoration: none; display: inline-block; }
.va-btn:disabled { opacity: .4; cursor: not-allowed; }
.va-btn--accept  { background: var(--color-primary); color: #fff; }
.va-btn--refuse  { background: var(--color-accent);  color: #fff; }
.va-btn--cloture { background: var(--color-primary); color: #fff; }

.va-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.va-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.va-pager button:disabled { opacity: .4; cursor: not-allowed; }

/* Modal générique */
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

/* Options de clôture */
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

/* Radios inline type/durée */
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
  font-size: .75rem; color: var(--color-text); opacity: .55;
  font-style: italic; margin-top: .5rem;
  padding: .5rem .75rem; background: var(--color-background); border-radius: 6px;
}

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
