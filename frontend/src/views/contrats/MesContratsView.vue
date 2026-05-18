<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import contratService from '@/services/contratService'

const router      = useRouter()
const contrats    = ref([])
const loading     = ref(false)
const error       = ref('')
const currentPage = ref(0)
const totalPages  = ref(1)
const filtreStatut = ref('')

// Modal prolongation / résiliation
const showModal    = ref(false)
const modalType    = ref('')  // 'prolongation' | 'resiliation'
const modalContrat = ref(null)
const modalDate    = ref('')
const modalMotif   = ref('')
const submitting   = ref(false)

const STATUTS = ['', 'EN_ATTENTE', 'ACTIF', 'EXPIRE', 'RESILIE']
const STATUT_LABELS = { EN_ATTENTE: 'En attente', ACTIF: 'Actif', EXPIRE: 'Expiré', RESILIE: 'Résilié' }
const STATUT_COLORS = { EN_ATTENTE: 'badge--warning', ACTIF: 'badge--success', EXPIRE: 'badge--neutral', RESILIE: 'badge--danger' }

async function fetchContrats(page = 0) {
  loading.value = true; error.value = ''
  try {
    const res = await contratService.getClientContrats(page, 10, filtreStatut.value || null)
    contrats.value    = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
  } catch { error.value = 'Impossible de charger vos contrats.' }
  finally { loading.value = false }
}

function openModal(type, contrat) {
  modalType.value    = type
  modalContrat.value = contrat
  modalDate.value    = contrat.dateFin ?? ''
  modalMotif.value   = ''
  showModal.value    = true
}

async function submitModal() {
  if (submitting.value) return
  submitting.value = true
  try {
    if (modalType.value === 'prolongation') {
      await contratService.demanderProlongation(modalContrat.value.id, modalDate.value, modalMotif.value)
    } else {
      await contratService.demanderResiliation(modalContrat.value.id, modalMotif.value)
    }
    showModal.value = false
    await fetchContrats(currentPage.value)
  } catch { alert('Erreur lors de la soumission.') }
  finally { submitting.value = false }
}

function formatDate(d) {
  if (!d) return '–'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}
function formatMontant(v) {
  return new Intl.NumberFormat('fr-SN').format(v) + ' FCFA'
}

onMounted(() => fetchContrats(0))
</script>

<template>
  <div class="mc-page">
    <div class="mc-container">

      <div class="mc-header">
        <div>
          <h1 class="mc-header__title">Mes contrats</h1>
          <p class="mc-header__sub">Consultez et gérez vos contrats immobiliers</p>
        </div>
      </div>

      <!-- Filtres -->
      <div class="mc-filters">
        <button v-for="s in STATUTS" :key="s" class="mc-filter-btn"
          :class="{ '--active': filtreStatut === s }"
          @click="filtreStatut = s; fetchContrats(0)">
          {{ s ? STATUT_LABELS[s] : 'Tous' }}
        </button>
      </div>

      <div v-if="loading" class="mc-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="mc-error">{{ error }}</div>

      <div v-else-if="!contrats.length" class="mc-empty">
        <p class="mc-empty__icon">📄</p>
        <p class="mc-empty__title">Aucun contrat trouvé</p>
      </div>

      <div v-else class="mc-grid">
        <div v-for="c in contrats" :key="c.id" class="mc-card">
          <div class="mc-card__header">
            <span :class="['badge', STATUT_COLORS[c.statut]]">{{ STATUT_LABELS[c.statut] }}</span>
            <span class="mc-card__id">#{{ c.id }}</span>
          </div>
          <div class="mc-card__thumb">
            <img v-if="c.imagePrincipale" :src="c.imagePrincipale" :alt="c.annonceLibelle" />
            <span v-else>🏠</span>
          </div>
          <div class="mc-card__body">
            <RouterLink :to="`/annonces/${c.annonceId}`" class="mc-card__title">{{ c.annonceLibelle }}</RouterLink>
            <p class="mc-card__addr">📍 {{ c.annonceAdresse }}</p>
            <div class="mc-card__info">
              <div class="mc-card__info-item">
                <span class="mc-card__info-label">Montant</span>
                <span class="mc-card__info-val mc-card__montant">{{ formatMontant(c.montant) }}</span>
              </div>
              <div class="mc-card__info-item">
                <span class="mc-card__info-label">Début</span>
                <span class="mc-card__info-val">{{ formatDate(c.dateDebut) }}</span>
              </div>
              <div class="mc-card__info-item">
                <span class="mc-card__info-label">Fin</span>
                <span class="mc-card__info-val">{{ formatDate(c.dateFin) }}</span>
              </div>
            </div>
            <p v-if="c.notes" class="mc-card__notes">{{ c.notes }}</p>
          </div>
          <div class="mc-card__footer">
            <a v-if="c.documentUrl" :href="c.documentUrl" target="_blank" class="mc-card__doc">
              📥 Télécharger le contrat
            </a>
            <div class="mc-card__actions">
              <RouterLink :to="`/contrats/${c.id}`" class="mc-card__btn mc-card__btn--outline">
                Détails
              </RouterLink>
              <button v-if="c.statut === 'ACTIF'" class="mc-card__btn mc-card__btn--primary"
                @click="openModal('prolongation', c)">
                Prolonger
              </button>
              <button v-if="c.statut === 'ACTIF'" class="mc-card__btn mc-card__btn--danger"
                @click="openModal('resiliation', c)">
                Résilier
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="mc-pager">
        <button :disabled="currentPage === 0" @click="fetchContrats(currentPage - 1)">← Précédent</button>
        <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button :disabled="currentPage === totalPages - 1" @click="fetchContrats(currentPage + 1)">Suivant →</button>
      </div>
    </div>

    <!-- Modal prolongation / résiliation -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
        <div class="modal-box">
          <h2 class="modal-box__title">
            {{ modalType === 'prolongation' ? 'Demander une prolongation' : 'Demander une résiliation' }}
          </h2>
          <p class="modal-box__desc">Contrat : <strong>{{ modalContrat?.annonceLibelle }}</strong></p>

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
.mc-page { background: var(--color-background); min-height: 100vh; }
.mc-container { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }

.mc-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; }
.mc-header__title { font-size: 1.6rem; font-weight: 800; color: var(--color-text); }
.mc-header__sub { font-size: .88rem; color: var(--color-text); opacity: .55; margin-top: .25rem; }

.mc-filters { display: flex; flex-wrap: wrap; gap: .5rem; margin-bottom: 1.5rem; }
.mc-filter-btn {
  padding: .35rem .9rem; border: 1.5px solid var(--color-border); border-radius: 20px;
  font-size: .8rem; background: var(--color-card); color: var(--color-text); cursor: pointer; transition: all .15s;
}
.mc-filter-btn:hover, .mc-filter-btn.--active { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }

.mc-loading { display: flex; justify-content: center; padding: 4rem; }
.mc-error { text-align: center; padding: 2rem; color: var(--color-accent); }
.mc-empty { display: flex; flex-direction: column; align-items: center; gap: .75rem; padding: 4rem; text-align: center; }
.mc-empty__icon { font-size: 3rem; opacity: .3; }
.mc-empty__title { font-size: 1rem; font-weight: 700; color: var(--color-text); opacity: .6; }

.mc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1.25rem; }

.mc-card {
  background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card);
  overflow: hidden; display: flex; flex-direction: column;
}
.mc-card__header {
  display: flex; justify-content: space-between; align-items: center;
  padding: .75rem 1rem; border-bottom: 1px solid var(--color-border);
}
.mc-card__id { font-size: .75rem; color: var(--color-text); opacity: .4; }
.mc-card__thumb {
  height: 140px; overflow: hidden; background: var(--color-border);
  display: flex; align-items: center; justify-content: center; font-size: 2rem;
}
.mc-card__thumb img { width: 100%; height: 100%; object-fit: cover; }
.mc-card__body { padding: 1rem; flex: 1; }
.mc-card__title {
  font-size: .95rem; font-weight: 700; color: var(--color-text); text-decoration: none;
  display: block; margin-bottom: .25rem;
}
.mc-card__title:hover { color: var(--color-primary); }
.mc-card__addr { font-size: .8rem; color: var(--color-text); opacity: .55; margin-bottom: .75rem; }

.mc-card__info { display: grid; grid-template-columns: repeat(3, 1fr); gap: .5rem; margin-bottom: .75rem; }
.mc-card__info-item { display: flex; flex-direction: column; gap: .1rem; }
.mc-card__info-label { font-size: .68rem; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text); opacity: .5; }
.mc-card__info-val { font-size: .82rem; font-weight: 600; color: var(--color-text); }
.mc-card__montant { color: var(--color-accent); font-size: .9rem; }
.mc-card__notes { font-size: .78rem; color: var(--color-text); opacity: .6; font-style: italic; }

.mc-card__footer { padding: .75rem 1rem; border-top: 1px solid var(--color-border); }
.mc-card__doc {
  display: block; font-size: .8rem; color: var(--color-primary); font-weight: 500;
  text-decoration: none; margin-bottom: .5rem;
}
.mc-card__actions { display: flex; gap: .5rem; flex-wrap: wrap; }
.mc-card__btn {
  padding: .35rem .8rem; border-radius: var(--radius-sm); font-size: .8rem;
  font-weight: 600; cursor: pointer; text-decoration: none; transition: opacity .2s;
  display: inline-block;
}
.mc-card__btn--outline { border: 1.5px solid var(--color-primary); color: var(--color-primary); background: none; }
.mc-card__btn--primary { background: var(--color-primary); color: #fff; border: none; }
.mc-card__btn--danger { background: none; border: 1.5px solid #e53e3e; color: #e53e3e; }
.mc-card__btn:hover { opacity: .8; }

.mc-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; font-size: .88rem; }
.mc-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.mc-pager button:disabled { opacity: .4; cursor: not-allowed; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 440px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1.1rem; font-weight: 700; color: var(--color-text); margin-bottom: .5rem; }
.modal-box__desc { font-size: .88rem; color: var(--color-text); opacity: .65; margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea {
  padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .9rem; background: var(--color-background); color: var(--color-text);
}
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

/* Badges */
.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--warning { background: #fef3c7; color: #d97706; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }
.badge--danger  { background: #fee2e2; color: #dc2626; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) { .mc-grid { grid-template-columns: 1fr; } }
</style>
