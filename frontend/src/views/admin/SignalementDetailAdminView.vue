<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import signalementService from '@/services/signalementService'
import StatusBadge from '@/components/StatusBadge.vue'
import { useToastStore } from '@/stores/toastStore'

const route  = useRoute()
const router = useRouter()
const toast  = useToastStore()

const sig     = ref(null)
const loading = ref(false)
const error   = ref('')

// Modal réponse / mise à jour statut
const showReply   = ref(false)
const replyStatut = ref('')
const replyText   = ref('')
const replying    = ref(false)

const STATUT_LABELS   = { OUVERT: 'Ouvert', EN_COURS: 'En cours', RESOLU: 'Résolu', FERME: 'Fermé' }
const STATUT_VARIANTS = { OUVERT: 'warning', EN_COURS: 'info', RESOLU: 'success', FERME: 'neutral' }
const ALL_STATUTS     = ['OUVERT', 'EN_COURS', 'RESOLU', 'FERME']

async function fetchSignalement() {
  loading.value = true
  error.value   = ''
  try {
    const res = await signalementService.getById(route.params.id)
    sig.value = res.data.data
    if (!sig.value.isRead) {
      signalementService.markAsRead(sig.value.id).catch(() => {})
    }
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Signalement introuvable.'
      : 'Impossible de charger ce signalement.'
  } finally {
    loading.value = false
  }
}

function openReply() {
  replyStatut.value = sig.value.statut
  replyText.value   = sig.value.reponseAdmin || ''
  showReply.value   = true
}

async function submitReply() {
  if (replying.value) return
  replying.value = true
  try {
    await signalementService.updateStatut(sig.value.id, replyStatut.value, replyText.value || null)
    toast.success('Signalement mis à jour.')
    showReply.value = false
    await fetchSignalement()
  } catch (err) {
    toast.error(err?.response?.data?.message || 'Erreur lors de la mise à jour.')
  } finally {
    replying.value = false
  }
}

function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}

function formatDateTime(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('fr-FR', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

onMounted(fetchSignalement)
</script>

<template>
  <div class="sd-page">

    <!-- Retour -->
    <div class="sd-back">
      <button class="sd-back__btn" @click="router.push('/admin/signalements')">
        <ArrowLeft :size="16" /> Retour aux signalements
      </button>
    </div>

    <div v-if="loading" class="sd-loading"><div class="spinner"></div></div>
    <div v-else-if="error" class="sd-error">{{ error }}</div>

    <template v-else-if="sig">

      <!-- En-tête -->
      <div class="sd-header">
        <div class="sd-header__left">
          <span class="sd-header__id">Signalement {{ sig.id }}</span>
          <StatusBadge :label="STATUT_LABELS[sig.statut]" :variant="STATUT_VARIANTS[sig.statut]" />
          <span v-if="!sig.isRead" class="sd-new">Nouveau</span>
        </div>
        <button class="btn btn--primary" @click="openReply">Traiter / Répondre</button>
      </div>

      <!-- Grille principale -->
      <div class="sd-grid">

        <!-- Colonne principale -->
        <div class="sd-col">

          <!-- Contenu du signalement -->
          <section class="sd-section">
            <h2 class="sd-section__title">Contenu du signalement</h2>
            <div class="sd-section__body">
              <p class="sd-contenu">{{ sig.contenu }}</p>
            </div>
          </section>

          <!-- Réponse admin -->
          <section class="sd-section">
            <div class="sd-section__head-flex">
              <h2 class="sd-section__title">Réponse administrateur</h2>
              <button class="btn btn--ghost btn--sm" @click="openReply">Modifier</button>
            </div>
            <div class="sd-section__body">
              <p v-if="sig.reponseAdmin" class="sd-reponse">{{ sig.reponseAdmin }}</p>
              <p v-else class="sd-empty">Aucune réponse enregistrée.</p>
            </div>
          </section>

        </div>

        <!-- Colonne latérale -->
        <div class="sd-sidebar">

          <!-- Client -->
          <section class="sd-section">
            <h2 class="sd-section__title">Client</h2>
            <div class="sd-section__body">
              <div class="sd-row">
                <div class="sd-avatar">{{ sig.clientNom?.charAt(0)?.toUpperCase() || '?' }}</div>
                <div>
                  <p class="sd-row__main">{{ sig.clientNom }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Annonce / Contrat -->
          <section class="sd-section">
            <h2 class="sd-section__title">Annonce concernée</h2>
            <div class="sd-section__body">
              <dl class="sd-dl">
                <dt>Libellé</dt>
                <dd class="sd-annonce-link">{{ sig.annonceLibelle }}</dd>
                <dt>Contrat lié</dt>
                <dd>
                  <RouterLink v-if="sig.contratId" :to="`/admin/contrats/${sig.contratId}`" class="sd-link">
                    Contrat {{ sig.contratId }} →
                  </RouterLink>
                  <span v-else>—</span>
                </dd>
              </dl>
            </div>
          </section>

          <!-- Dates -->
          <section class="sd-section">
            <h2 class="sd-section__title">Informations</h2>
            <div class="sd-section__body">
              <dl class="sd-dl">
                <dt>Créé le</dt>
                <dd>{{ formatDateTime(sig.createdAt) }}</dd>
                <dt>Lu</dt>
                <dd>{{ sig.isRead ? 'Oui' : 'Non' }}</dd>
              </dl>
            </div>
          </section>

        </div>
      </div>

    </template>

    <!-- Modal traitement -->
    <Teleport to="body">
      <div v-if="showReply" class="modal-overlay" @click.self="showReply = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Traiter le signalement {{ sig?.id }}</h2>
          <div class="modal-box__field">
            <label>Nouveau statut</label>
            <select v-model="replyStatut" class="modal-box__input">
              <option v-for="s in ALL_STATUTS" :key="s" :value="s">{{ STATUT_LABELS[s] }}</option>
            </select>
          </div>
          <div class="modal-box__field">
            <label>Réponse au client (optionnel)</label>
            <textarea v-model="replyText" class="modal-box__textarea" rows="5"
              placeholder="Votre réponse visible par le client…" />
          </div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showReply = false">Annuler</button>
            <button class="modal-box__submit" :disabled="replying" @click="submitReply">
              {{ replying ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.sd-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; display: flex; flex-direction: column; gap: 1.25rem; }

.sd-back { display: flex; }
.sd-back__btn { display: inline-flex; align-items: center; gap: .4rem; background: none; border: none; font-size: .85rem; font-weight: 600; color: var(--color-text-secondary); cursor: pointer; transition: color .15s; }
.sd-back__btn:hover { color: var(--color-primary); }

.sd-loading { display: flex; justify-content: center; padding: 4rem; }
.sd-error   { text-align: center; padding: 3rem; color: var(--color-accent); }

.sd-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; }
.sd-header__left { display: flex; align-items: center; gap: .75rem; flex-wrap: wrap; }
.sd-header__id { font-size: 1.3rem; font-weight: 800; color: var(--color-text); }
.sd-new { font-size: .72rem; font-weight: 700; color: var(--color-primary); background: rgba(74,124,111,.1); padding: .15rem .5rem; border-radius: 10px; }

.sd-grid { display: grid; grid-template-columns: 1fr 300px; gap: 1.25rem; align-items: start; }
.sd-col, .sd-sidebar { display: flex; flex-direction: column; gap: 1.25rem; }

.sd-section { background: var(--color-card); border-radius: var(--radius); padding: 1.5rem; box-shadow: var(--shadow-card); }
.sd-section__title { font-size: .88rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text-secondary); margin-bottom: 1rem; padding-bottom: .65rem; border-bottom: 1px solid var(--color-border); }
.sd-section__head-flex { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; padding-bottom: .65rem; border-bottom: 1px solid var(--color-border); }
.sd-section__head-flex .sd-section__title { margin-bottom: 0; padding-bottom: 0; border-bottom: none; }
.sd-section__body { font-size: .9rem; }

.sd-contenu { font-size: .93rem; line-height: 1.7; color: var(--color-text); white-space: pre-wrap; }
.sd-reponse { font-size: .9rem; line-height: 1.6; color: var(--color-text); font-style: italic; padding: .75rem 1rem; background: var(--color-background); border-radius: var(--radius-sm); }
.sd-empty { font-size: .85rem; color: var(--color-text-secondary); font-style: italic; }

.sd-row { display: flex; align-items: center; gap: .75rem; }
.sd-avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--color-primary); color: #fff; font-weight: 800; font-size: .9rem; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sd-row__main { font-weight: 600; color: var(--color-text); font-size: .9rem; }
.sd-row__sub { font-size: .75rem; color: var(--color-text-secondary); }

.sd-dl { display: grid; grid-template-columns: auto 1fr; gap: .45rem 1rem; }
.sd-dl dt { font-size: .75rem; font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: .04em; padding-top: .1rem; }
.sd-dl dd { font-size: .88rem; color: var(--color-text); word-break: break-word; }
.sd-link { color: var(--color-primary); font-weight: 600; }
.sd-annonce-link { color: var(--color-primary); font-weight: 500; }

/* Boutons */
.btn { display: inline-flex; align-items: center; gap: .35rem; padding: .55rem 1.1rem; border-radius: var(--radius-sm); font-size: .88rem; font-weight: 600; cursor: pointer; border: none; transition: all .15s; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--primary:hover { background: var(--color-primary-hover); }
.btn--ghost { background: transparent; color: var(--color-text-secondary); border: 1.5px solid var(--color-border); }
.btn--ghost:hover { color: var(--color-primary); border-color: var(--color-primary); }
.btn--sm { font-size: .78rem; padding: .3rem .7rem; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .88rem; background: var(--color-background); color: var(--color-text); width: 100%; box-sizing: border-box; }
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) { .sd-grid { grid-template-columns: 1fr; } }
</style>
