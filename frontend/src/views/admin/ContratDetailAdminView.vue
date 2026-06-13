<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, FileText, Paperclip, Pencil, Check, X } from 'lucide-vue-next'
import contratService from '@/services/contratService'
import { uploadPdf } from '@/services/cloudinaryService'
import historyService from '@/services/historyService'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'

const route  = useRoute()
const router = useRouter()

const contrat  = ref(null)
const loading  = ref(false)
const error    = ref('')

// Modal édition statut général
const showEdit        = ref(false)
const editStatut      = ref('')
const editNotes       = ref('')
const editDocumentUrl = ref('')
const pdfFile         = ref(null)
const pdfUploading    = ref(false)
const saving          = ref(false)
const activating      = ref(false)

// Modal résiliation (accepter / refuser)
const showResiliationModal = ref(false)
const resiliationAction    = ref('')   // 'accepter' | 'refuser'
const resiliationMotif     = ref('')
const resiliationLoading   = ref(false)

// Modal prolongation (accepter / refuser)
const showProlongationModal = ref(false)
const prolongationAction    = ref('')  // 'accepter' | 'refuser'
const prolongationDate      = ref('')
const prolongationMotif     = ref('')
const prolongationLoading   = ref(false)

const STATUT_LABELS = {
  EN_ATTENTE:              'En attente',
  ACTIF:                   'Actif',
  EXPIRE:                  'Expiré',
  RESILIE:                 'Résilié',
  EN_ATTENTE_RESILIATION:  'Résiliation en attente',
  PROLONGATION_EN_ATTENTE: 'Prolongation en attente',
}
const STATUT_VARIANTS = {
  EN_ATTENTE:              'warning',
  ACTIF:                   'success',
  EXPIRE:                  'neutral',
  RESILIE:                 'danger',
  EN_ATTENTE_RESILIATION:  'warning',
  PROLONGATION_EN_ATTENTE: 'warning',
}

async function fetchContrat() {
  loading.value = true
  error.value   = ''
  try {
    const res = await contratService.getById(route.params.id)
    contrat.value = res.data.data
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Contrat introuvable.'
      : 'Impossible de charger ce contrat.'
  } finally {
    loading.value = false
  }
}

// Activation / rejet EN_ATTENTE

async function activer() {
  if (activating.value) return
  activating.value = true
  try {
    await contratService.update(contrat.value.id, { statut: 'ACTIF' })
    await fetchContrat()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors de la validation.')
  } finally {
    activating.value = false
  }
}

async function rejeter() {
  if (activating.value) return
  if (!confirm('Confirmer le rejet de ce contrat ?')) return
  activating.value = true
  try {
    await contratService.update(contrat.value.id, { statut: 'RESILIE' })
    await fetchContrat()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors du rejet.')
  } finally {
    activating.value = false
  }
}

// Modal résiliation

function openResiliation(action) {
  resiliationAction.value = action
  resiliationMotif.value  = ''
  showResiliationModal.value = true
}

async function submitResiliation() {
  if (resiliationLoading.value) return
  resiliationLoading.value = true
  try {
    if (resiliationAction.value === 'accepter') {
      await contratService.accepterResiliation(contrat.value.id, resiliationMotif.value || null)
    } else {
      await contratService.refuserResiliation(contrat.value.id, resiliationMotif.value || null)
    }
    showResiliationModal.value = false
    await fetchContrat()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors du traitement de la résiliation.')
  } finally {
    resiliationLoading.value = false
  }
}

// Modal prolongation

function openProlongation(action) {
  prolongationAction.value  = action
  prolongationDate.value    = contrat.value.dateFin ?? ''
  prolongationMotif.value   = ''
  showProlongationModal.value = true
}

async function submitProlongation() {
  if (prolongationLoading.value) return
  if (prolongationAction.value === 'accepter' && contrat.value.typeContrat && !prolongationDate.value) {
    alert('Veuillez saisir la nouvelle date de fin.')
    return
  }
  prolongationLoading.value = true
  try {
    if (prolongationAction.value === 'accepter') {
      await contratService.accepterProlongation(
        contrat.value.id,
        prolongationDate.value || null,
        prolongationMotif.value || null
      )
    } else {
      await contratService.refuserProlongation(contrat.value.id, prolongationMotif.value || null)
    }
    showProlongationModal.value = false
    await fetchContrat()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors du traitement de la prolongation.')
  } finally {
    prolongationLoading.value = false
  }
}

// Modal édition générale

function openEdit() {
  editStatut.value      = contrat.value.statut
  editNotes.value       = contrat.value.notes ?? ''
  editDocumentUrl.value = contrat.value.documentUrl ?? ''
  pdfFile.value         = null
  showEdit.value        = true
}

async function saveEdit() {
  saving.value = true
  try {
    // Upload PDF si un nouveau fichier est sélectionné
    if (pdfFile.value) {
      pdfUploading.value = true
      editDocumentUrl.value = await uploadPdf(pdfFile.value)
      pdfUploading.value = false
    }
    await contratService.update(contrat.value.id, {
      statut:      editStatut.value      || null,
      notes:       editNotes.value       || null,
      documentUrl: editDocumentUrl.value || null,
    })
    showEdit.value = false
    await fetchContrat()
  } catch (e) {
    pdfUploading.value = false
    alert(e.response?.data?.message || 'Erreur lors de la modification.')
  } finally {
    saving.value = false
  }
}

function formatDate(d) {
  if (!d) return '–'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}
function formatDatetime(d) {
  if (!d) return '–'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function formatMontant(v) {
  return new Intl.NumberFormat('fr-SN').format(v) + ' FCFA'
}

// Onglet Histoire
const tab               = ref('detail')
const historique        = ref([])
const historiqueLoading = ref(false)

const ACTION_LABELS = {
  CREATION:                'Création',
  CREATION_AUTO_VISITE:    'Créé depuis visite',
  VALIDATION:              'Validation',
  REJET:                   'Rejet',
  DEMANDE_RESILIATION:     'Demande de résiliation',
  ACCEPTATION_RESILIATION: 'Résiliation acceptée',
  REFUS_RESILIATION:       'Résiliation refusée',
  DEMANDE_PROLONGATION:    'Demande de prolongation',
  ACCEPTATION_PROLONGATION:'Prolongation acceptée',
  REFUS_PROLONGATION:      'Prolongation refusée',
  EXPIRATION_AUTOMATIQUE:  'Expiration automatique',
  CHANGEMENT_STATUT:       'Changement de statut',
}

async function fetchHistorique() {
  historiqueLoading.value = true
  try {
    const res = await historyService.getContratHistory(route.params.id)
    historique.value = res.data.data ?? []
  } catch { /* silently fail */ } finally { historiqueLoading.value = false }
}

function switchTab(t) {
  tab.value = t
  if (t === 'histoire') fetchHistorique()
}

onMounted(() => fetchContrat())
</script>

<template>
  <div class="cda-page">
    <div class="cda-container">

      <button class="cda-back" @click="router.push('/admin/contrats')">
        <ArrowLeft :size="16" /> Contrats
      </button>

      <div v-if="loading" class="cda-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="cda-error">{{ error }}</div>

      <template v-else-if="contrat">
        <!-- En-tête -->
        <div class="cda-header">
          <div class="cda-header__left">
            <StatusBadge :label="STATUT_LABELS[contrat.statut]" :variant="STATUT_VARIANTS[contrat.statut]" />
            <h1 class="cda-header__title">Contrat #{{ contrat.id }}</h1>
            <p class="cda-header__sub">{{ contrat.annonceLibelle }}</p>
          </div>
          <div class="cda-header__right">
            <a v-if="contrat.documentUrl" :href="contrat.documentUrl" target="_blank" class="cda-btn cda-btn--outline">
              <Download :size="14" /> Télécharger
            </a>
            <button class="cda-btn cda-btn--primary" @click="openEdit"><Pencil :size="13" /> Modifier</button>
          </div>
        </div>

        <!-- Onglets Détail / Histoire -->
        <div class="tabs">
          <button :class="['tab', { 'tab--active': tab === 'detail' }]" @click="switchTab('detail')">Détail</button>
          <button :class="['tab', { 'tab--active': tab === 'histoire' }]" @click="switchTab('histoire')">Histoire</button>
        </div>

        <div v-show="tab === 'detail'">
        <!-- Image -->
        <div class="cda-image">
          <ImageGallery :images="contrat.imagePrincipale ? [contrat.imagePrincipale] : []" :alt="contrat.annonceLibelle" height="220px" />
        </div>

        <!-- Grille -->
        <div class="cda-grid">
          <div class="cda-card">
            <h2 class="cda-card__title">Client</h2>
            <dl class="cda-dl">
              <div class="cda-dl__row"><dt>Nom</dt><dd>{{ contrat.clientNom }}</dd></div>
              <div class="cda-dl__row"><dt>ID client</dt><dd>#{{ contrat.clientId }}</dd></div>
            </dl>
          </div>

          <div class="cda-card">
            <h2 class="cda-card__title">Bien immobilier</h2>
            <dl class="cda-dl">
              <div class="cda-dl__row">
                <dt>Libellé</dt>
                <dd><RouterLink :to="`/admin/annonces/${contrat.annonceId}`" class="cda-link">{{ contrat.annonceLibelle }}</RouterLink></dd>
              </div>
              <div class="cda-dl__row"><dt>Adresse</dt><dd>{{ contrat.annonceAdresse || '–' }}</dd></div>
            </dl>
          </div>

          <div class="cda-card">
            <h2 class="cda-card__title">Détails du contrat</h2>
            <dl class="cda-dl">
              <div class="cda-dl__row">
                <dt>Type</dt>
                <dd>
                  <span v-if="contrat.typeContrat" :class="['badge-type', contrat.typeContrat === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                    {{ contrat.typeContrat === 'VENTE' ? 'Vente' : 'Location' }}
                    <span v-if="contrat.typeContrat === 'LOCATION' && contrat.dureeLocationMois">{{ contrat.dureeLocationMois }} mois</span>
                  </span>
                  <span v-else>–</span>
                </dd>
              </div>
              <div class="cda-dl__row"><dt>Montant</dt><dd class="cda-montant">{{ formatMontant(contrat.montant) }}</dd></div>
              <div class="cda-dl__row"><dt>Début</dt><dd>{{ formatDate(contrat.dateDebut) }}</dd></div>
              <div class="cda-dl__row"><dt>Fin</dt><dd>{{ formatDate(contrat.dateFin) }}</dd></div>
            </dl>
          </div>

          <div class="cda-card">
            <h2 class="cda-card__title">Historique</h2>
            <dl class="cda-dl">
              <div class="cda-dl__row"><dt>Créé le</dt><dd>{{ formatDatetime(contrat.createdAt) }}</dd></div>
              <div class="cda-dl__row"><dt>Mis à jour</dt><dd>{{ formatDatetime(contrat.updatedAt) }}</dd></div>
              <div v-if="contrat.leadId" class="cda-dl__row"><dt>Lead lié</dt><dd>#{{ contrat.leadId }}</dd></div>
              <div v-if="contrat.visiteId" class="cda-dl__row"><dt>Visite liée</dt><dd>#{{ contrat.visiteId }}</dd></div>
            </dl>
          </div>

          <!-- Motif résiliation client -->
          <div class="cda-card cda-card--full" v-if="contrat.motifResiliation">
            <h2 class="cda-card__title cda-card__title--warn">Motif de résiliation (client)</h2>
            <p class="cda-motif">{{ contrat.motifResiliation }}</p>
          </div>

          <!-- Motif prolongation client -->
          <div class="cda-card cda-card--full" v-if="contrat.motifProlongation">
            <h2 class="cda-card__title cda-card__title--info">Motif de prolongation (client)</h2>
            <p class="cda-motif">{{ contrat.motifProlongation }}</p>
          </div>

          <!-- Notes admin -->
          <div class="cda-card cda-card--full" v-if="contrat.notes">
            <h2 class="cda-card__title"><FileText :size="14" /> Notes administratives</h2>
            <p class="cda-notes">{{ contrat.notes }}</p>
          </div>
        </div>

        <!-- Actions admin selon statut -->
        <div class="cda-actions-section">
          <h2 class="cda-actions-section__title">Actions disponibles</h2>

          <!-- EN_ATTENTE → Valider ou Rejeter -->
          <div v-if="contrat.statut === 'EN_ATTENTE'" class="cda-actions">
            <button class="cda-btn cda-btn--success" :disabled="activating" @click="activer"><template v-if="!activating"><Check :size="14" /> Valider le contrat</template><template v-else>…</template></button>
            <button class="cda-btn cda-btn--danger"  :disabled="activating" @click="rejeter"><template v-if="!activating"><X :size="14" /> Rejeter le contrat</template><template v-else>…</template></button>
          </div>

          <!-- EN_ATTENTE_RESILIATION → Accepter ou Refuser -->
          <div v-else-if="contrat.statut === 'EN_ATTENTE_RESILIATION'" class="cda-actions">
            <button class="cda-btn cda-btn--success" @click="openResiliation('accepter')">
              <Check :size="14" /> Accepter la résiliation
            </button>
            <button class="cda-btn cda-btn--danger" @click="openResiliation('refuser')">
              <X :size="14" /> Refuser la résiliation
            </button>
          </div>

          <!-- PROLONGATION_EN_ATTENTE → Accepter ou Refuser -->
          <div v-else-if="contrat.statut === 'PROLONGATION_EN_ATTENTE'" class="cda-actions">
            <button class="cda-btn cda-btn--success" @click="openProlongation('accepter')">
              <Check :size="14" /> Accepter la prolongation
            </button>
            <button class="cda-btn cda-btn--danger" @click="openProlongation('refuser')">
              <X :size="14" /> Refuser la prolongation
            </button>
          </div>

          <!-- Autres statuts : lecture seule -->
          <div v-else class="cda-actions">
            <p class="cda-readonly">Ce contrat est en lecture seule (statut : {{ STATUT_LABELS[contrat.statut] }}).</p>
          </div>
        </div>
        </div><!-- /tab-detail -->

        <!-- Onglet Histoire -->
        <div v-show="tab === 'histoire'" class="hist-section">
          <div v-if="historiqueLoading" class="hist-empty">Chargement de l'historique…</div>
          <div v-else-if="!historique.length" class="hist-empty">Aucun événement enregistré pour ce contrat.</div>
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
                <div class="hist-item__auteur">{{ evt.auteurEmail }}</div>
                <div v-if="evt.commentaire" class="hist-item__comment">{{ evt.commentaire }}</div>
              </div>
            </div>
          </div>
        </div><!-- /tab-histoire -->

      </template>
    </div>

    <!-- Modal résiliation -->
    <Teleport to="body">
      <div v-if="showResiliationModal" class="modal-overlay" @click.self="showResiliationModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">
            <template v-if="resiliationAction === 'accepter'"><Check :size="14" /> Accepter la résiliation</template>
            <template v-else><X :size="14" /> Refuser la résiliation</template>
          </h2>
          <p class="modal-box__desc">
            Contrat : <strong>{{ contrat?.annonceLibelle }}</strong> — Client : <strong>{{ contrat?.clientNom }}</strong>
          </p>
          <div v-if="contrat?.motifResiliation" class="modal-box__motif">
            <span class="modal-box__motif-label">Motif du client :</span>
            <span class="modal-box__motif-text">{{ contrat.motifResiliation }}</span>
          </div>
          <div class="modal-box__field">
            <label>Commentaire admin (optionnel)</label>
            <textarea v-model="resiliationMotif" class="modal-box__textarea" rows="3"
              :placeholder="resiliationAction === 'accepter'
                ? 'Précisions sur l\'acceptation…'
                : 'Raison du refus…'" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showResiliationModal = false">Annuler</button>
            <button
              :class="['modal-box__submit', resiliationAction === 'refuser' ? 'modal-box__submit--danger' : '']"
              :disabled="resiliationLoading"
              @click="submitResiliation"
            >
              {{ resiliationLoading ? 'En cours…' : (resiliationAction === 'accepter' ? 'Confirmer l\'acceptation' : 'Confirmer le refus') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal prolongation -->
    <Teleport to="body">
      <div v-if="showProlongationModal" class="modal-overlay" @click.self="showProlongationModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">
            <template v-if="prolongationAction === 'accepter'"><Check :size="14" /> Accepter la prolongation</template>
            <template v-else><X :size="14" /> Refuser la prolongation</template>
          </h2>
          <p class="modal-box__desc">
            Contrat : <strong>{{ contrat?.annonceLibelle }}</strong> — Client : <strong>{{ contrat?.clientNom }}</strong>
          </p>
          <div v-if="contrat?.motifProlongation" class="modal-box__motif">
            <span class="modal-box__motif-label">Demande du client :</span>
            <span class="modal-box__motif-text">{{ contrat.motifProlongation }}</span>
          </div>

          <!-- Saisie de la nouvelle date uniquement pour l'acceptation -->
          <template v-if="prolongationAction === 'accepter'">
            <div class="modal-box__field">
              <label>
                Nouvelle date de fin
                <span v-if="contrat?.typeContrat === 'LOCATION'" class="modal-box__hint">
                  (LOCATION : la durée et le montant seront recalculés automatiquement)
                </span>
              </label>
              <input v-model="prolongationDate" type="date" class="modal-box__input"
                :min="contrat?.dateFin" />
            </div>
            <p v-if="contrat?.typeContrat === 'LOCATION' && contrat?.dateFin" class="modal-box__info">
              Date de fin actuelle : {{ formatDate(contrat.dateFin) }}
            </p>
          </template>

          <div class="modal-box__field">
            <label>Commentaire admin (optionnel)</label>
            <textarea v-model="prolongationMotif" class="modal-box__textarea" rows="3"
              :placeholder="prolongationAction === 'accepter'
                ? 'Précisions sur la prolongation accordée…'
                : 'Raison du refus de prolongation…'" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showProlongationModal = false">Annuler</button>
            <button
              :class="['modal-box__submit', prolongationAction === 'refuser' ? 'modal-box__submit--danger' : '']"
              :disabled="prolongationLoading"
              @click="submitProlongation"
            >
              {{ prolongationLoading ? 'En cours…' : (prolongationAction === 'accepter' ? 'Confirmer la prolongation' : 'Confirmer le refus') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal édition générale -->
    <Teleport to="body">
      <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Modifier le contrat #{{ contrat?.id }}</h2>
          <div class="modal-box__field">
            <label>Statut</label>
            <select v-model="editStatut" class="modal-box__input">
              <option v-for="s in ['EN_ATTENTE','ACTIF','RESILIE']" :key="s" :value="s">{{ STATUT_LABELS[s] }}</option>
            </select>
          </div>
          <div class="modal-box__field">
            <label>Document contrat (PDF)</label>
            <label class="pdf-upload-zone">
              <Paperclip :size="15" />
              <span v-if="pdfFile">{{ pdfFile.name }}</span>
              <span v-else-if="editDocumentUrl">Remplacer le document existant</span>
              <span v-else>Sélectionner un fichier PDF</span>
              <input
                type="file"
                accept="application/pdf"
                @change="e => pdfFile = e.target.files[0] || null"
                hidden
              />
            </label>
            <a v-if="editDocumentUrl && !pdfFile" :href="editDocumentUrl" target="_blank" class="pdf-current-link">
              <Download :size="12" /> Voir le document actuel
            </a>
          </div>
          <div class="modal-box__field">
            <label>Notes administratives</label>
            <textarea v-model="editNotes" class="modal-box__textarea" rows="4" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showEdit = false">Annuler</button>
            <button class="modal-box__submit" :disabled="saving || pdfUploading" @click="saveEdit">
              {{ pdfUploading ? 'Upload PDF…' : saving ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.cda-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.cda-container { max-width: 900px; margin: 0 auto; }

.cda-back {
  display: inline-flex; align-items: center; gap: .4rem;
  background: none; border: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: var(--color-text); opacity: .6;
  padding: 0; margin-bottom: 1.5rem; transition: opacity .15s;
}
.cda-back:hover { opacity: 1; }

.cda-loading { display: flex; justify-content: center; padding: 4rem; }
.cda-error { text-align: center; padding: 3rem; color: var(--color-accent); font-weight: 600; }

.cda-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.cda-header__left { display: flex; flex-direction: column; gap: .4rem; }
.cda-header__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); margin: 0; }
.cda-header__sub { font-size: .9rem; color: var(--color-text); opacity: .6; margin: 0; }
.cda-header__right { display: flex; gap: .5rem; align-items: flex-start; }

.cda-image { margin-bottom: 1.5rem; }

.cda-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.cda-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.cda-card--full { grid-column: 1 / -1; }
.cda-card__title {
  font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
  color: var(--color-text); opacity: .5; margin: 0 0 .85rem;
  display: flex; align-items: center; gap: .35rem;
}
.cda-card__title--warn { color: #d97706; opacity: 1; }
.cda-card__title--info { color: #2563eb; opacity: 1; }

.cda-dl { display: flex; flex-direction: column; gap: .55rem; }
.cda-dl__row { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.cda-dl__row dt { font-size: .82rem; color: var(--color-text); opacity: .55; flex-shrink: 0; }
.cda-dl__row dd { font-size: .88rem; font-weight: 600; color: var(--color-text); text-align: right; }
.cda-montant { color: var(--color-accent); font-size: .95rem !important; }
.cda-link { color: var(--color-primary); text-decoration: none; font-weight: 600; }
.cda-notes { font-size: .88rem; color: var(--color-text); opacity: .7; line-height: 1.6; white-space: pre-wrap; margin: 0; }
.cda-motif { font-size: .88rem; color: var(--color-text); line-height: 1.6; white-space: pre-wrap; margin: 0; font-style: italic; }

.cda-actions-section { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.cda-actions-section__title { font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .5; margin: 0 0 1rem; }
.cda-actions { display: flex; gap: .75rem; flex-wrap: wrap; align-items: center; }
.cda-readonly { font-size: .88rem; color: var(--color-text); opacity: .55; font-style: italic; }

.cda-btn {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .5rem 1.1rem; border-radius: var(--radius-sm); font-size: .85rem;
  font-weight: 600; cursor: pointer; border: 1.5px solid transparent; text-decoration: none; transition: all .15s;
}
.cda-btn:disabled { opacity: .35; cursor: not-allowed; }
.cda-btn--primary { background: var(--color-primary); color: #fff; }
.cda-btn--primary:hover:not(:disabled) { opacity: .88; }
.cda-btn--success { background: none; border-color: #059669; color: #059669; }
.cda-btn--success:hover:not(:disabled) { background: rgba(5,150,105,.07); }
.cda-btn--danger  { background: none; border-color: #dc2626; color: #dc2626; }
.cda-btn--danger:hover:not(:disabled)  { background: rgba(220,38,38,.07); }
.cda-btn--outline { background: none; border-color: var(--color-border); color: var(--color-primary); }
.cda-btn--outline:hover:not(:disabled) { border-color: var(--color-primary); }

.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }

/* Modals */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .5rem; }
.modal-box__desc { font-size: .85rem; color: var(--color-text); opacity: .65; margin-bottom: 1rem; }
.modal-box__motif {
  display: flex; flex-direction: column; gap: .25rem;
  padding: .7rem .9rem; background: var(--color-background);
  border-radius: var(--radius-sm); border-left: 3px solid var(--color-primary);
  margin-bottom: 1rem;
}
.modal-box__motif-label { font-size: .72rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text); opacity: .55; }
.modal-box__motif-text { font-size: .88rem; color: var(--color-text); font-style: italic; }
.modal-box__hint { font-size: .72rem; font-weight: 400; opacity: .6; font-style: italic; margin-left: .3rem; }
.modal-box__info { font-size: .78rem; color: var(--color-text); opacity: .55; font-style: italic; margin: -.5rem 0 1rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .88rem; background: var(--color-background); color: var(--color-text); width: 100%; box-sizing: border-box; }
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; transition: opacity .15s; }
.modal-box__submit--danger { background: #dc2626; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.pdf-upload-zone {
  display: inline-flex; align-items: center; gap: .45rem; cursor: pointer;
  padding: .6rem .9rem; border: 1.5px dashed var(--color-border); border-radius: var(--radius-sm);
  font-size: .85rem; color: var(--color-primary); font-weight: 600; transition: border-color .2s;
}
.pdf-upload-zone:hover { border-color: var(--color-primary); }
.pdf-current-link {
  display: inline-flex; align-items: center; gap: .3rem; font-size: .78rem;
  color: var(--color-text); opacity: .55; text-decoration: none; margin-top: .3rem;
}
.pdf-current-link:hover { opacity: .85; text-decoration: underline; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) { .cda-grid { grid-template-columns: 1fr; } }

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
.hist-list { display: flex; flex-direction: column; gap: 0; position: relative; padding-left: 1.5rem; }
.hist-list::before { content: ''; position: absolute; left: .45rem; top: .6rem; bottom: .6rem; width: 2px; background: var(--color-border); }
.hist-item { display: flex; gap: 1rem; position: relative; padding-bottom: 1.25rem; }
.hist-item:last-child { padding-bottom: 0; }
.hist-item__dot {
  position: absolute; left: -1.5rem; top: .35rem;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--color-primary); border: 2px solid var(--color-card); flex-shrink: 0;
}
.hist-item__card { flex: 1; background: var(--color-card); border-radius: var(--radius-sm); padding: .85rem 1rem; box-shadow: var(--shadow-card); }
.hist-item__header { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; flex-wrap: wrap; margin-bottom: .4rem; }
.hist-item__action { font-size: .88rem; font-weight: 700; color: var(--color-text); }
.hist-item__date { font-size: .75rem; color: var(--color-text); opacity: .5; white-space: nowrap; }
.hist-item__statuts { display: flex; align-items: center; gap: .4rem; margin-bottom: .35rem; flex-wrap: wrap; }
.hist-statut { padding: .15rem .55rem; border-radius: 8px; font-size: .75rem; font-weight: 600; }
.hist-statut--old { background: rgba(107,114,128,.1); color: #6b7280; }
.hist-statut--new { background: rgba(74,124,111,.12); color: #3a6b5e; }
.hist-arrow { font-size: .8rem; color: var(--color-text); opacity: .4; }
.hist-item__auteur { font-size: .78rem; color: var(--color-text); opacity: .5; }
.hist-item__comment { font-size: .82rem; color: var(--color-text); opacity: .7; font-style: italic; margin-top: .3rem; line-height: 1.5; }
</style>
