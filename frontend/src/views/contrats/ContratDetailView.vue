<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, FileText } from 'lucide-vue-next'
import contratService from '@/services/contratService'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'

const route  = useRoute()
const router = useRouter()

const contrat    = ref(null)
const loading    = ref(false)
const error      = ref('')

// Modal prolongation / résiliation
const showModal    = ref(false)
const modalType    = ref('')
const modalDate    = ref('')
const modalMotif   = ref('')
const submitting   = ref(false)

const STATUT_LABELS = {
  EN_ATTENTE:              'Pré-contrat',
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

function openModal(type) {
  modalType.value  = type
  modalDate.value  = contrat.value.dateFin ?? ''
  modalMotif.value = ''
  showModal.value  = true
}

async function submitModal() {
  if (submitting.value) return
  submitting.value = true
  try {
    if (modalType.value === 'prolongation') {
      await contratService.demanderProlongation(contrat.value.id, modalDate.value, modalMotif.value)
    } else {
      await contratService.demanderResiliation(contrat.value.id, modalMotif.value)
    }
    showModal.value = false
    await fetchContrat()
  } catch (e) {
    alert(e.response?.data?.message || 'Erreur lors de la soumission.')
  } finally {
    submitting.value = false
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

onMounted(() => fetchContrat())
</script>

<template>
  <div class="cd-page">
    <div class="cd-container">

      <button class="cd-back" @click="router.push('/mes-contrats')">
        <ArrowLeft :size="16" /> Mes contrats
      </button>

      <div v-if="loading" class="cd-loading"><div class="spinner"></div></div>

      <div v-else-if="error" class="cd-error">{{ error }}</div>

      <template v-else-if="contrat">
        <!-- En-tête -->
        <div class="cd-header">
          <div class="cd-header__left">
            <StatusBadge :label="STATUT_LABELS[contrat.statut]" :variant="STATUT_VARIANTS[contrat.statut]" />
            <h1 class="cd-header__title">Contrat {{ contrat.id }}</h1>
            <p class="cd-header__sub">{{ contrat.annonceLibelle }}</p>
          </div>
          <a v-if="contrat.documentUrl" :href="contrat.documentUrl" target="_blank" class="cd-download">
            <Download :size="16" /> Télécharger le contrat
          </a>
        </div>

        <!-- Image annonce -->
        <div class="cd-image">
          <ImageGallery :images="contrat.imagePrincipale ? [contrat.imagePrincipale] : []" :alt="contrat.annonceLibelle" aspect="16 / 9" max-height="380px" />
        </div>

        <!-- Grille d'informations -->
        <div class="cd-grid">
          <div class="cd-card">
            <h2 class="cd-card__title">Bien immobilier</h2>
            <dl class="cd-dl">
              <div class="cd-dl__row">
                <dt>Libellé</dt>
                <dd>
                  <RouterLink :to="`/annonces/${contrat.annonceId}`" class="cd-link">{{ contrat.annonceLibelle }}</RouterLink>
                </dd>
              </div>
              <div class="cd-dl__row">
                <dt>Adresse</dt>
                <dd>{{ contrat.annonceAdresse || '–' }}</dd>
              </div>
            </dl>
          </div>

          <div class="cd-card">
            <h2 class="cd-card__title">Détails du contrat</h2>
            <dl class="cd-dl">
              <div class="cd-dl__row">
                <dt>Type</dt>
                <dd>
                  <span v-if="contrat.typeContrat" :class="['badge-type', contrat.typeContrat === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                    {{ contrat.typeContrat === 'VENTE' ? 'Vente' : 'Location' }}
                    <span v-if="contrat.typeContrat === 'LOCATION' && contrat.dureeLocationMois">{{ contrat.dureeLocationMois }} mois</span>
                  </span>
                  <span v-else>–</span>
                </dd>
              </div>
              <div class="cd-dl__row">
                <dt>Montant</dt>
                <dd class="cd-montant">{{ formatMontant(contrat.montant) }}</dd>
              </div>
              <div class="cd-dl__row">
                <dt>Date de début</dt>
                <dd>{{ formatDate(contrat.dateDebut) }}</dd>
              </div>
              <div class="cd-dl__row">
                <dt>Date de fin</dt>
                <dd>{{ formatDate(contrat.dateFin) }}</dd>
              </div>
            </dl>
          </div>

          <div class="cd-card cd-card--full" v-if="contrat.motifResiliation">
            <h2 class="cd-card__title cd-card__title--warn">Votre motif de résiliation</h2>
            <p class="cd-motif">{{ contrat.motifResiliation }}</p>
          </div>

          <div class="cd-card cd-card--full" v-if="contrat.motifProlongation">
            <h2 class="cd-card__title cd-card__title--info">Votre demande de prolongation</h2>
            <p class="cd-motif">{{ contrat.motifProlongation }}</p>
          </div>

          <div class="cd-card cd-card--full" v-if="contrat.notes">
            <h2 class="cd-card__title"><FileText :size="14" /> Notes</h2>
            <p class="cd-notes">{{ contrat.notes }}</p>
          </div>

          <div class="cd-card cd-card--full">
            <h2 class="cd-card__title">Historique</h2>
            <dl class="cd-dl">
              <div class="cd-dl__row">
                <dt>Créé le</dt>
                <dd>{{ formatDatetime(contrat.createdAt) }}</dd>
              </div>
              <div class="cd-dl__row">
                <dt>Mis à jour le</dt>
                <dd>{{ formatDatetime(contrat.updatedAt) }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- Actions CLIENT -->
        <div class="cd-actions" v-if="contrat.statut === 'ACTIF'">
          <button class="cd-btn cd-btn--primary" @click="openModal('prolongation')">Demander une prolongation</button>
          <button class="cd-btn cd-btn--danger" @click="openModal('resiliation')">Demander une résiliation</button>
        </div>
        <div class="cd-actions" v-else-if="contrat.statut === 'EN_ATTENTE_RESILIATION'">
          <p class="cd-info">Votre demande de résiliation est en cours de traitement par l'administration.</p>
        </div>
        <div class="cd-actions" v-else-if="contrat.statut === 'PROLONGATION_EN_ATTENTE'">
          <p class="cd-info">Votre demande de prolongation est en cours de traitement par l'administration.</p>
        </div>
      </template>
    </div>

    <!-- Modal prolongation / résiliation -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">
            {{ modalType === 'prolongation' ? 'Demander une prolongation' : 'Demander une résiliation' }}
          </h2>
          <p class="modal-box__desc">Contrat : <strong>{{ contrat?.annonceLibelle }}</strong></p>

          <div v-if="modalType === 'prolongation'" class="modal-box__field">
            <label>Nouvelle date de fin</label>
            <input v-model="modalDate" type="date" class="modal-box__input" />
          </div>

          <div class="modal-box__field">
            <label>Motif (optionnel)</label>
            <textarea v-model="modalMotif" class="modal-box__textarea" rows="3"
              :placeholder="modalType === 'prolongation' ? 'Raison de la prolongation…' : 'Raison de la résiliation…'" />
          </div>

          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showModal = false">Annuler</button>
            <button class="modal-box__submit" :disabled="submitting" @click="submitModal">
              {{ submitting ? 'Envoi…' : 'Confirmer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.cd-page { background: var(--color-background); min-height: 100vh; }
.cd-container { max-width: 860px; margin: 0 auto; padding: 2rem 1.5rem; }

.cd-back {
  display: inline-flex; align-items: center; gap: .4rem;
  background: none; border: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: var(--color-text); opacity: .6;
  padding: 0; margin-bottom: 1.5rem; transition: opacity .15s;
}
.cd-back:hover { opacity: 1; }

.cd-loading { display: flex; justify-content: center; padding: 4rem; }
.cd-error { text-align: center; padding: 3rem; color: var(--color-accent); font-weight: 600; }

.cd-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.cd-header__left { display: flex; flex-direction: column; gap: .4rem; }
.cd-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); margin: 0; }
.cd-header__sub { font-size: .9rem; color: var(--color-text); opacity: .6; margin: 0; }
.cd-download {
  display: inline-flex; align-items: center; gap: .4rem;
  padding: .55rem 1.1rem; background: var(--color-card);
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .85rem; font-weight: 600; color: var(--color-primary);
  text-decoration: none; transition: border-color .15s;
}
.cd-download:hover { border-color: var(--color-primary); }

.cd-image { margin-bottom: 1.5rem; }

.cd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.cd-card {
  background: var(--color-card); border-radius: var(--radius);
  padding: 1.25rem; box-shadow: var(--shadow-card);
}
.cd-card--full { grid-column: 1 / -1; }
.cd-card__title {
  font-size: .8rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
  color: var(--color-text); opacity: .5; margin: 0 0 .9rem;
  display: flex; align-items: center; gap: .4rem;
}

.cd-dl { display: flex; flex-direction: column; gap: .6rem; }
.cd-dl__row { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.cd-dl__row dt { font-size: .82rem; color: var(--color-text); opacity: .55; flex-shrink: 0; }
.cd-dl__row dd { font-size: .88rem; font-weight: 600; color: var(--color-text); text-align: right; }

.cd-montant { color: var(--color-accent); font-size: 1rem !important; }
.cd-link { color: var(--color-primary); text-decoration: none; font-weight: 600; }
.cd-link:hover { text-decoration: underline; }
.cd-notes { font-size: .88rem; color: var(--color-text); opacity: .7; line-height: 1.6; white-space: pre-wrap; margin: 0; }
.cd-motif { font-size: .88rem; color: var(--color-text); line-height: 1.6; white-space: pre-wrap; margin: 0; font-style: italic; }
.cd-card__title--warn { color: #d97706; opacity: 1; }
.cd-card__title--info { color: #2563eb; opacity: 1; }

.cd-actions { display: flex; gap: .75rem; flex-wrap: wrap; }
.cd-btn {
  padding: .6rem 1.25rem; border-radius: var(--radius-sm); font-size: .88rem;
  font-weight: 600; cursor: pointer; transition: opacity .2s; border: none;
}
.cd-btn--primary { background: var(--color-primary); color: #fff; }
.cd-btn--danger { background: none; border: 1.5px solid #e53e3e; color: #e53e3e; }
.cd-btn:hover { opacity: .85; }

.cd-info {
  font-size: .88rem; color: var(--color-text); opacity: .65; font-style: italic;
  padding: .75rem 1rem; background: var(--color-card); border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}


/* Exception métier (Vente/Location) — aligné sur StatusBadge : padding .25/.65, radius 12px, .75rem/700 */
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 440px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1.1rem; font-weight: 700; color: var(--color-text); margin-bottom: .5rem; }
.modal-box__desc { font-size: .88rem; color: var(--color-text); opacity: .65; margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); }
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) { .cd-grid { grid-template-columns: 1fr; } }
</style>
