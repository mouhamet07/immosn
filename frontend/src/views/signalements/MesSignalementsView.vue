<script setup>
import { ref, onMounted } from 'vue'
import { Wrench } from 'lucide-vue-next'
import signalementService from '@/services/signalementService'
import contratService from '@/services/contratService'
import FilterSelect from '@/components/FilterSelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const signalements = ref([])
const loading      = ref(false)
const error        = ref('')
const currentPage  = ref(0)
const totalPages   = ref(1)
const filtreStatut = ref('')

// Nouveau signalement
const showForm  = ref(false)
const contrats  = ref([])
const form      = ref({ contratId: '', contenu: '' })
const submitting = ref(false)
const formError  = ref('')

const STATUTS = ['', 'OUVERT', 'EN_COURS', 'RESOLU', 'FERME']
const STATUT_LABELS = { OUVERT: 'Ouvert', EN_COURS: 'En cours', RESOLU: 'Résolu', FERME: 'Fermé' }
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les statuts' }))
const STATUT_VARIANTS = { OUVERT: 'warning', EN_COURS: 'info', RESOLU: 'success', FERME: 'neutral' }

async function fetchSignalements(page = 0) {
  loading.value = true; error.value = ''
  try {
    const res = await signalementService.getClientSignalements(page, 10, filtreStatut.value || null)
    signalements.value = res.data.data
    currentPage.value  = res.data.currentPage
    totalPages.value   = res.data.totalPages
  } catch { error.value = 'Impossible de charger vos signalements.' }
  finally { loading.value = false }
}

async function openForm() {
  showForm.value = true; formError.value = ''
  try {
    const res = await contratService.getClientContrats(0, 100)
    contrats.value = (res.data.data || []).filter(c => c.statut === 'ACTIF')
  } catch { contrats.value = [] }
}

async function submitForm() {
  if (!form.value.contratId || !form.value.contenu.trim()) {
    formError.value = 'Veuillez sélectionner un contrat et rédiger votre signalement.'
    return
  }
  submitting.value = true; formError.value = ''
  try {
    await signalementService.create(Number(form.value.contratId), form.value.contenu.trim())
    showForm.value = false
    form.value = { contratId: '', contenu: '' }
    await fetchSignalements(0)
  } catch { formError.value = 'Erreur lors de l\'envoi.' }
  finally { submitting.value = false }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}

onMounted(() => fetchSignalements(0))
</script>

<template>
  <div class="ms-page">
    <div class="ms-container">

      <div class="ms-header">
        <div>
          <h1 class="ms-header__title">Mes signalements SAV</h1>
          <p class="ms-header__sub">Suivez vos demandes de service après-vente</p>
        </div>
        <button class="ms-header__btn" @click="openForm">+ Nouveau signalement</button>
      </div>

      <FilterSelect
        style="margin-bottom: 1.5rem"
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchSignalements(0) }"
      />

      <div v-if="loading" class="ms-loading"><div class="spinner"></div></div>
      <div v-else-if="error" class="ms-error">{{ error }}</div>

      <div v-else-if="!signalements.length" class="ms-empty">
        <Wrench :size="48" class="ms-empty__icon" />
        <p class="ms-empty__title">Aucun signalement</p>
        <button class="ms-empty__btn" @click="openForm">Créer un signalement</button>
      </div>

      <div v-else class="ms-list">
        <div v-for="s in signalements" :key="s.id" class="ms-card">
          <div class="ms-card__header">
            <StatusBadge :label="STATUT_LABELS[s.statut]" :variant="STATUT_VARIANTS[s.statut]" />
            <span class="ms-card__date">{{ formatDate(s.createdAt) }}</span>
          </div>
          <div class="ms-card__body">
            <p class="ms-card__annonce">Contrat : <strong>{{ s.annonceLibelle }}</strong></p>
            <p class="ms-card__contenu">{{ s.contenu }}</p>
          </div>
          <div v-if="s.reponseAdmin" class="ms-card__reponse">
            <p class="ms-card__reponse-label">Réponse de l'agence :</p>
            <p class="ms-card__reponse-text">{{ s.reponseAdmin }}</p>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="ms-pager">
        <button :disabled="currentPage === 0" @click="fetchSignalements(currentPage - 1)">← Précédent</button>
        <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button :disabled="currentPage === totalPages - 1" @click="fetchSignalements(currentPage + 1)">Suivant →</button>
      </div>
    </div>

    <!-- Modal nouveau signalement -->
    <Teleport to="body">
      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Nouveau signalement SAV</h2>
          <div class="modal-box__field">
            <label>Contrat concerné</label>
            <select v-model="form.contratId" class="modal-box__input">
              <option value="">-- Sélectionner un contrat actif --</option>
              <option v-for="c in contrats" :key="c.id" :value="c.id">
                {{ c.annonceLibelle }} — {{ c.annonceAdresse }}
              </option>
            </select>
          </div>
          <div class="modal-box__field">
            <label>Description du problème</label>
            <textarea v-model="form.contenu" class="modal-box__textarea" rows="5"
              placeholder="Décrivez votre problème ou votre demande SAV…" />
          </div>
          <p v-if="formError" class="modal-box__err">{{ formError }}</p>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showForm = false">Annuler</button>
            <button class="modal-box__submit" :disabled="submitting" @click="submitForm">
              {{ submitting ? 'Envoi…' : 'Envoyer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.ms-page { background: var(--color-background); min-height: 100vh; }
.ms-container { max-width: 860px; margin: 0 auto; padding: 2rem 1.5rem; }

.ms-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.ms-header__title { font-size: 1.6rem; font-weight: 800; color: var(--color-text); }
.ms-header__sub { font-size: .88rem; color: var(--color-text); opacity: .55; margin-top: .25rem; }
.ms-header__btn { padding: .6rem 1.2rem; background: var(--color-primary); color: #fff; border-radius: var(--radius-sm); font-weight: 600; font-size: .88rem; border: none; cursor: pointer; transition: opacity .2s; }
.ms-header__btn:hover { opacity: .85; }


.ms-loading { display: flex; justify-content: center; padding: 4rem; }
.ms-error { text-align: center; padding: 2rem; color: var(--color-accent); }
.ms-empty { display: flex; flex-direction: column; align-items: center; gap: .75rem; padding: 4rem; text-align: center; }
.ms-empty__icon { color: var(--color-text-muted); opacity: 0.3; }
.ms-empty__title { font-size: 1rem; font-weight: 700; color: var(--color-text); opacity: .6; }
.ms-empty__btn { padding: .5rem 1.2rem; background: var(--color-primary); color: #fff; border-radius: var(--radius-sm); border: none; cursor: pointer; font-size: .88rem; font-weight: 600; }

.ms-list { display: flex; flex-direction: column; gap: 1rem; }
.ms-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.ms-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: .75rem; }
.ms-card__date { font-size: .75rem; color: var(--color-text); opacity: .45; }
.ms-card__annonce { font-size: .82rem; color: var(--color-text); opacity: .65; margin-bottom: .5rem; }
.ms-card__contenu { font-size: .9rem; color: var(--color-text); line-height: 1.6; }
.ms-card__reponse { margin-top: .75rem; padding-top: .75rem; border-top: 1px dashed var(--color-border); }
.ms-card__reponse-label { font-size: .72rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-primary); margin-bottom: .25rem; }
.ms-card__reponse-text { font-size: .88rem; color: var(--color-text); opacity: .8; font-style: italic; }

.ms-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; font-size: .88rem; }
.ms-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.ms-pager button:disabled { opacity: .4; cursor: not-allowed; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1.1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .9rem; background: var(--color-background); color: var(--color-text); width: 100%; box-sizing: border-box; }
.modal-box__textarea { resize: vertical; }
.modal-box__err { font-size: .82rem; color: #e53e3e; margin-top: -.5rem; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }


.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
