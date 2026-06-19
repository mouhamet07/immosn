<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, MapPin, Pencil, Check, X } from 'lucide-vue-next'
import visiteService from '@/services/visiteService'
import authService from '@/services/authService'
import historyService from '@/services/historyService'
import { useAuthStore } from '@/stores/authStore'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'

const route  = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.role === 'SUPER_ADMIN')

const visite    = ref(null)
const loading   = ref(false)
const error     = ref('')
const updating  = ref(false)

// Sprint 2 — affectation
const admins        = ref([])
const selectedAdmin = ref('')
const affecting     = ref(false)

// Sprint 2 — rapport de visite
const rapport         = ref(null)
const rapportCR       = ref('')
const rapportAboutie  = ref(true)
const rapportDocFile  = ref(null)
const rapportDocSaving = ref(false)

// Modal modifier date
const showDateModal = ref(false)
const newDate       = ref('')
const dateComment   = ref('')

// Clôture (fusionnée dans la carte rapport)
const cloturing          = ref(false)
const showDureeModal     = ref(false)
const clotureDureeValue  = ref(12)
const dureeModalCreeRapport = ref(false) // true si la modal doit d'abord créer le rapport

const STATUT_LABELS = {
  EN_ATTENTE:               'En attente',
  ACCEPTEE:                 'Acceptée',
  AFFECTEE:                 'Affectée',
  REPLANIFICATION_DEMANDEE: 'Replanification demandée',
  RAPPORT_REDIGE:           'Rapport rédigé',
  REFUSEE:                  'Refusée',
  ANNULEE:                  'Annulée',
  CLOTUREE_SANS_SUITE:      'Clôturée sans suite',
  CLOTUREE_AVEC_CONTRAT:    'Clôturée avec contrat',
  TERMINEE:                 'Terminée',
}
const STATUT_VARIANTS = {
  EN_ATTENTE:               'warning',
  ACCEPTEE:                 'success',
  AFFECTEE:                 'info',
  REPLANIFICATION_DEMANDEE: 'warning',
  RAPPORT_REDIGE:           'info',
  REFUSEE:                  'danger',
  ANNULEE:                  'neutral',
  CLOTUREE_SANS_SUITE:      'neutral',
  CLOTUREE_AVEC_CONTRAT:    'info',
  TERMINEE:                 'info',
}

async function fetchVisite() {
  loading.value = true
  error.value   = ''
  try {
    const res = await visiteService.getById(route.params.id)
    visite.value = res.data.data
    rapportCR.value = ''
    rapportAboutie.value = true
    await fetchRapport()
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Demande de visite introuvable.'
      : 'Impossible de charger cette demande.'
  } finally {
    loading.value = false
  }
}

// Charge le rapport existant (404 silencieux si absent)
async function fetchRapport() {
  rapport.value = null
  try {
    const res = await visiteService.getRapport(route.params.id)
    rapport.value = res.data.data
  } catch { /* pas encore de rapport */ }
}

// Liste des admins pour l'affectation (SUPER_ADMIN uniquement)
async function fetchAdmins() {
  if (!isSuperAdmin.value) return
  try {
    const res = await authService.getAdmins(0, 100)
    admins.value = (res.data.content ?? res.data.data ?? []).filter(a => !a.archived)
  } catch { /* liste indisponible */ }
}

async function affecter() {
  if (!selectedAdmin.value) { alert('Veuillez choisir un administrateur.'); return }
  affecting.value = true
  try {
    await visiteService.affecter(visite.value.id, Number(selectedAdmin.value))
    selectedAdmin.value = ''
    await fetchVisite()
  } catch (e) {
    alert(e.response?.data?.message || "Erreur lors de l'affectation.")
  } finally {
    affecting.value = false
  }
}

// Admin à affecter dès l'acceptation (EN_ATTENTE) — choix obligatoire avant d'accepter
const acceptAdmin = ref('')

async function accepterReplanification() {
  updating.value = true
  try {
    await visiteService.accepterReplanification(visite.value.id)
    await fetchVisite()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur.')
  } finally {
    updating.value = false
  }
}

// Rédige le rapport puis clôture la visite dans le même geste. Si la visite est
// aboutie et que l'annonce est une LOCATION, on demande la durée du bail avant
// de créer le rapport (pour éviter un état RAPPORT_REDIGE orphelin si l'admin annule).
async function submitRapportEtCloture() {
  if (!rapportCR.value.trim()) { alert('Le compte-rendu est obligatoire.'); return }
  if (rapportAboutie.value && visite.value.typeTransaction === 'LOCATION') {
    clotureDureeValue.value = 12
    dureeModalCreeRapport.value = true
    showDureeModal.value = true
    return
  }
  await creerRapportPuisCloturer(null)
}

async function confirmerDureeEtCloturer() {
  showDureeModal.value = false
  const duree = Number(clotureDureeValue.value)
  if (dureeModalCreeRapport.value) {
    await creerRapportPuisCloturer(duree)
  } else {
    cloturing.value = true
    try {
      await cloturerApresRapport(duree)
    } finally {
      cloturing.value = false
    }
  }
}

async function creerRapportPuisCloturer(dureeLocationMois) {
  cloturing.value = true
  try {
    await visiteService.creerRapport(visite.value.id, {
      compteRendu: rapportCR.value.trim(),
      aboutie: rapportAboutie.value,
    })
    await cloturerApresRapport(dureeLocationMois)
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors de l\'enregistrement du rapport.')
    await fetchVisite()
  } finally {
    cloturing.value = false
  }
}

// Clôture une visite déjà au statut RAPPORT_REDIGE (recouvrement si la clôture
// avait échoué juste après la création du rapport).
async function cloturerApresRapport(dureeLocationMois) {
  const type = rapportAboutie.value ? 'AVEC_CONTRAT' : 'SANS_SUITE'
  try {
    const res = await visiteService.cloturerVisite(visite.value.id, {
      type,
      dureeLocationMois: type === 'AVEC_CONTRAT' && visite.value.typeTransaction === 'LOCATION'
        ? dureeLocationMois : null,
    })
    const contratId = res?.data?.data?.id
    if (type === 'AVEC_CONTRAT' && contratId) {
      router.push(`/admin/contrats/${contratId}`)
    } else {
      await fetchVisite()
    }
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors de la clôture.')
    await fetchVisite()
  }
}

// Recouvrement manuel : la visite est restée en RAPPORT_REDIGE (clôture précédente échouée).
async function cloturerDepuisRapportExistant() {
  rapportAboutie.value = rapport.value.aboutie
  if (rapport.value.aboutie && visite.value.typeTransaction === 'LOCATION') {
    clotureDureeValue.value = 12
    dureeModalCreeRapport.value = false
    showDureeModal.value = true
    return
  }
  cloturing.value = true
  try {
    await cloturerApresRapport(null)
  } finally {
    cloturing.value = false
  }
}

function onRapportFile(e) {
  rapportDocFile.value = e.target.files?.[0] ?? null
}

async function uploadRapportDoc() {
  if (!rapportDocFile.value) { alert('Veuillez sélectionner un fichier.'); return }
  rapportDocSaving.value = true
  try {
    await visiteService.uploadRapportDocument(visite.value.id, rapportDocFile.value)
    rapportDocFile.value = null
    await fetchRapport()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors du téléversement.')
  } finally {
    rapportDocSaving.value = false
  }
}

async function accepter() {
  if (isSuperAdmin.value && !acceptAdmin.value) {
    alert('Veuillez choisir un administrateur responsable avant d\'accepter cette demande.')
    return
  }
  updating.value = true
  try {
    await visiteService.updateStatut(visite.value.id, 'ACCEPTEE')
    if (isSuperAdmin.value && acceptAdmin.value) {
      await visiteService.affecter(visite.value.id, Number(acceptAdmin.value))
    }
    acceptAdmin.value = ''
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
  CREATION_INVITE:       'Création (visiteur non authentifié)',
  ACCEPTATION:           'Acceptation',
  REFUS:                 'Refus',
  ANNULATION:            'Annulation',
  ABANDON:               'Abandon automatique',
  CLOTURE_SANS_SUITE:    'Clôturée sans suite',
  CLOTURE_AVEC_CONTRAT:  'Clôturée avec contrat',
  REPROGRAMMATION:       'Reprogrammation (admin)',
  REPROGRAMMATION_CLIENT:'Reprogrammation (client)',
  AFFECTATION:              'Affectation à un responsable',
  REPLANIFICATION_DEMANDEE: 'Replanification demandée',
  REPLANIFICATION_ACCEPTEE: 'Replanification acceptée',
  RAPPORT_REDIGE:           'Rapport rédigé',
  CHANGEMENT_STATUT:        'Changement de statut',
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

onMounted(() => { fetchVisite(); fetchAdmins() })
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
            <h1 class="vda-header__title">Demande de visite {{ visite.id }}</h1>
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
            <h2 class="vda-card__title">{{ visite.clientId ? 'Client' : 'Prospect (visiteur)' }}</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row"><dt>Nom</dt><dd>{{ visite.clientNom || '–' }}</dd></div>
              <div class="vda-dl__row">
                <dt>Type</dt>
                <dd>{{ visite.clientId ? `Compte client #${visite.clientId}` : 'Visiteur non authentifié' }}</dd>
              </div>
            </dl>
          </div>

          <div class="vda-card">
            <h2 class="vda-card__title">Responsable</h2>
            <dl class="vda-dl">
              <div class="vda-dl__row">
                <dt>Administrateur</dt>
                <dd>{{ visite.adminResponsableNom || 'Non affecté' }}</dd>
              </div>
              <div class="vda-dl__row" v-if="visite.dateReplanificationProposee">
                <dt>Date proposée</dt>
                <dd class="vda-date-val">{{ formatDate(visite.dateReplanificationProposee) }}</dd>
              </div>
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
        </div>

        <!-- Actions admin selon statut -->
        <div class="vda-actions-section">
          <h2 class="vda-actions-section__title">Actions disponibles</h2>

          <!-- EN_ATTENTE : choisir un admin responsable puis accepter, ou refuser (SUPER_ADMIN) -->
          <div v-if="visite.statut === 'EN_ATTENTE'" class="vda-actions vda-actions--col">
            <template v-if="isSuperAdmin">
              <div class="vda-affect">
                <label class="vda-affect__label">Administrateur responsable (obligatoire avant acceptation)</label>
                <select v-model="acceptAdmin" class="vda-select">
                  <option value="">— Choisir un administrateur —</option>
                  <option v-for="a in admins" :key="a.id" :value="a.id">{{ a.nomComplet }} ({{ a.email }})</option>
                </select>
              </div>
              <div class="vda-actions">
                <button class="vda-btn vda-btn--accept" :disabled="updating || !acceptAdmin" @click="accepter"><Check :size="14" /> Accepter</button>
                <button class="vda-btn vda-btn--refuse" :disabled="updating" @click="refuser"><X :size="14" /> Refuser</button>
              </div>
            </template>
            <p v-else class="vda-readonly">En attente de validation par un SUPER_ADMIN.</p>
          </div>

          <!-- ACCEPTEE / AFFECTEE : affecter (SUPER_ADMIN) -->
          <div v-else-if="visite.statut === 'ACCEPTEE' || visite.statut === 'AFFECTEE'" class="vda-actions vda-actions--col">
            <div v-if="isSuperAdmin" class="vda-affect">
              <label class="vda-affect__label">{{ visite.statut === 'AFFECTEE' ? 'Réaffecter à' : 'Affecter à un responsable' }}</label>
              <div class="vda-affect__row">
                <select v-model="selectedAdmin" class="vda-select">
                  <option value="">— Choisir un administrateur —</option>
                  <option v-for="a in admins" :key="a.id" :value="a.id">{{ a.nomComplet }} ({{ a.email }})</option>
                </select>
                <button class="vda-btn vda-btn--cloture" :disabled="affecting || !selectedAdmin" @click="affecter">
                  {{ affecting ? '…' : 'Affecter' }}
                </button>
              </div>
            </div>
            <p v-else class="vda-readonly">Rédigez le rapport de visite ci-dessous pour clôturer.</p>
          </div>

          <!-- REPLANIFICATION_DEMANDEE : accepter la nouvelle date (SUPER_ADMIN) -->
          <div v-else-if="visite.statut === 'REPLANIFICATION_DEMANDEE'" class="vda-actions">
            <template v-if="isSuperAdmin">
              <button class="vda-btn vda-btn--accept" :disabled="updating" @click="accepterReplanification">
                <Check :size="14" /> Accepter la replanification
              </button>
            </template>
            <p v-else class="vda-readonly">Replanification en attente de validation par un SUPER_ADMIN.</p>
          </div>

          <!-- RAPPORT_REDIGE : la clôture se fait depuis la carte rapport ci-dessous -->
          <div v-else-if="visite.statut === 'RAPPORT_REDIGE'" class="vda-actions">
            <p class="vda-readonly">Rapport rédigé — clôturez la visite depuis la carte « Rapport de visite » ci-dessous.</p>
          </div>

          <!-- Autres statuts : lecture seule -->
          <div v-else class="vda-actions">
            <p class="vda-readonly">Ce dossier est en lecture seule (statut : {{ STATUT_LABELS[visite.statut] }}).</p>
          </div>
        </div>

        <!-- Rapport de visite -->
        <div class="vda-actions-section" v-if="['ACCEPTEE','AFFECTEE','RAPPORT_REDIGE','CLOTUREE_SANS_SUITE','CLOTUREE_AVEC_CONTRAT'].includes(visite.statut)">
          <h2 class="vda-actions-section__title">Rapport de visite</h2>

          <!-- Rapport existant -->
          <template v-if="rapport">
            <dl class="vda-dl">
              <div class="vda-dl__row"><dt>Auteur</dt><dd>{{ rapport.auteurAdminNom }}</dd></div>
              <div class="vda-dl__row"><dt>Issue</dt><dd>{{ rapport.aboutie ? 'Visite aboutie' : 'Sans suite' }}</dd></div>
              <div class="vda-dl__row"><dt>Rédigé le</dt><dd>{{ formatDatetime(rapport.createdAt) }}</dd></div>
            </dl>
            <p class="vda-rapport-cr">{{ rapport.compteRendu }}</p>
            <div class="vda-rapport-doc">
              <a v-if="rapport.documentUrl" :href="rapport.documentUrl" target="_blank" class="vda-link">Voir le document joint</a>
              <span v-else class="vda-readonly">Aucun document joint.</span>
              <div class="vda-affect__row">
                <input type="file" accept=".pdf,.png,.jpg,.jpeg" @change="onRapportFile" />
                <button class="vda-btn vda-btn--outline" :disabled="rapportDocSaving || !rapportDocFile" @click="uploadRapportDoc">
                  {{ rapportDocSaving ? '…' : (rapport.documentUrl ? 'Remplacer' : 'Téléverser') }}
                </button>
              </div>
            </div>

            <!-- Recouvrement : le rapport existe mais la clôture précédente a échoué -->
            <div v-if="visite.statut === 'RAPPORT_REDIGE'" class="vda-affect__row" style="margin-top: .85rem; padding-top: .85rem; border-top: 1px solid var(--color-border);">
              <button class="vda-btn vda-btn--cloture" :disabled="cloturing" @click="cloturerDepuisRapportExistant">
                {{ cloturing ? 'Clôture en cours…' : 'Clôturer la visite' }}
              </button>
            </div>
          </template>

          <!-- Formulaire de rédaction + clôture (si pas encore de rapport et état rapportable) -->
          <template v-else-if="['ACCEPTEE','AFFECTEE'].includes(visite.statut)">
            <div class="modal-box__field">
              <label>Compte-rendu *</label>
              <textarea v-model="rapportCR" class="modal-box__input" rows="4"
                placeholder="Décrivez le déroulement de la visite, l'intérêt du client, les points abordés…" />
            </div>
            <div class="vda-affect__row">
              <label class="vda-checkbox">
                <input type="checkbox" v-model="rapportAboutie" /> Visite aboutie (le client souhaite poursuivre)
              </label>
              <button class="vda-btn vda-btn--cloture" :disabled="cloturing || !rapportCR.trim()" @click="submitRapportEtCloture">
                {{ cloturing ? 'Enregistrement…' : 'Clôturer la visite' }}
              </button>
            </div>
          </template>

          <p v-else class="vda-readonly">Aucun rapport pour cette visite.</p>
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

    <!-- Mini-modal : durée du bail (LOCATION uniquement, avant clôture) -->
    <Teleport to="body">
      <div v-if="showDureeModal" class="modal-overlay" @click.self="showDureeModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Durée du bail</h2>
          <p class="modal-box__desc">Ce contrat est une location : précisez la durée du bail avant de clôturer la visite.</p>
          <div class="modal-box__field">
            <div class="duree-radios">
              <label v-for="d in [6, 12, 24, 36]" :key="d" class="type-radio" :class="{ '--active': clotureDureeValue === d }">
                <input type="radio" v-model="clotureDureeValue" :value="d" /> {{ d }} mois
              </label>
            </div>
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showDureeModal = false">Annuler</button>
            <button class="modal-box__submit" :disabled="cloturing" @click="confirmerDureeEtCloturer">
              {{ cloturing ? 'En cours…' : 'Confirmer' }}
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

.vda-actions-section { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); margin-bottom: 1.5rem; }
.vda-actions-section:last-child { margin-bottom: 0; }
.vda-actions-section__title { font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .5; margin: 0 0 1rem; }
.vda-actions { display: flex; gap: .75rem; flex-wrap: wrap; align-items: center; }
.vda-actions--col { flex-direction: column; align-items: stretch; gap: 1rem; }
.vda-readonly { font-size: .88rem; color: var(--color-text); opacity: .55; font-style: italic; }

/* Sprint 2 — affectation & rapport */
.vda-affect { display: flex; flex-direction: column; gap: .5rem; }
.vda-affect__label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.vda-affect__row { display: flex; gap: .75rem; align-items: center; flex-wrap: wrap; margin-top: .5rem; }
.vda-select {
  flex: 1; min-width: 220px; padding: .55rem .8rem;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .88rem; background: var(--color-background); color: var(--color-text);
}
.vda-checkbox { display: inline-flex; align-items: center; gap: .45rem; font-size: .85rem; color: var(--color-text); cursor: pointer; }
.vda-checkbox input { accent-color: var(--color-primary); }
.vda-rapport-cr { font-size: .9rem; color: var(--color-text); line-height: 1.6; margin: .75rem 0; white-space: pre-wrap; }
.vda-rapport-doc { display: flex; flex-direction: column; gap: .6rem; padding-top: .75rem; border-top: 1px solid var(--color-border); }

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
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .35rem; }
.modal-box__desc { font-size: .85rem; color: var(--color-text); opacity: .6; margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; transition: opacity .15s; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.duree-radios { display: flex; gap: .5rem; flex-wrap: wrap; }
.type-radio {
  display: flex; align-items: center; gap: .35rem;
  padding: .35rem .85rem; border: 1.5px solid var(--color-border);
  border-radius: 20px; font-size: .82rem; font-weight: 500;
  color: var(--color-text); cursor: pointer; transition: all .15s;
}
.type-radio input[type="radio"] { display: none; }
.type-radio.--active { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }

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
