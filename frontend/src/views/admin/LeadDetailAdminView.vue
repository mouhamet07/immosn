<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FileText, Edit3 } from 'lucide-vue-next'
import leadService from '@/services/leadService'
import { useToastStore } from '@/stores/toastStore'
import historyService from '@/services/historyService'

const route  = useRoute()
const router = useRouter()
const toast  = useToastStore()

const lead      = ref(null)
const loading   = ref(false)
const error     = ref('')
const showNoteModal = ref(false)
const noteValue     = ref('')
const savingNote    = ref(false)

const STATUT_LABELS = { EN_COURS: 'En cours', CONVERTI: 'Converti', ABANDONNE: 'Abandonné' }
const STATUT_COLORS = { EN_COURS: 'badge--info', CONVERTI: 'badge--success', ABANDONNE: 'badge--neutral' }

async function fetchLead() {
  loading.value = true
  error.value   = ''
  try {
    const res = await leadService.getById(route.params.id)
    lead.value = res.data.data
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Lead introuvable.'
      : 'Impossible de charger ce lead.'
  } finally {
    loading.value = false
  }
}

function openNoteModal() {
  noteValue.value     = lead.value?.noteAdmin ?? ''
  showNoteModal.value = true
}

function closeNoteModal() {
  showNoteModal.value = false
  noteValue.value     = ''
}

async function saveNote() {
  if (savingNote.value) return
  savingNote.value = true
  try {
    await leadService.updateNote(lead.value.id, noteValue.value || null)
    toast.success('Note enregistrée.')
    closeNoteModal()
    await fetchLead()
  } catch (err) {
    toast.error(err?.response?.data?.message || 'Erreur lors de la sauvegarde de la note.')
  } finally {
    savingNote.value = false
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
    hour: '2-digit', minute: '2-digit'
  })
}

// Onglet Histoire
const tab               = ref('detail')
const historique        = ref([])
const historiqueLoading = ref(false)

const ACTION_LABELS = {
  CREATION:     'Création',
  CONVERSION:   'Conversion',
  ABANDON:      'Abandon',
  NOTE_MODIFIEE:'Note modifiée',
}

async function fetchHistorique() {
  historiqueLoading.value = true
  try {
    const res = await historyService.getLeadHistory(route.params.id)
    historique.value = res.data.data ?? []
  } catch { /* silently fail */ } finally { historiqueLoading.value = false }
}

function switchTab(t) {
  tab.value = t
  if (t === 'histoire') fetchHistorique()
}

onMounted(fetchLead)
</script>

<template>
  <div class="ld-page">

    <!-- Navigation retour -->
    <div class="ld-back">
      <button class="ld-back__btn" @click="router.push('/admin/leads')">
        <ArrowLeft :size="16" /> Retour aux leads
      </button>
    </div>

    <!-- Chargement -->
    <div v-if="loading" class="ld-loading"><div class="spinner"></div></div>

    <!-- Erreur -->
    <div v-else-if="error" class="ld-error">{{ error }}</div>

    <!-- Contenu -->
    <template v-else-if="lead">

      <!-- En-tête -->
      <div class="ld-header">
        <div class="ld-header__left">
          <span class="ld-header__id">Lead #{{ lead.id }}</span>
          <span :class="['badge', STATUT_COLORS[lead.statut]]">{{ STATUT_LABELS[lead.statut] }}</span>
        </div>
        <div class="ld-header__actions">
          <button
            class="btn btn--primary"
            :disabled="!lead.visiteId"
            @click="lead.visiteId && router.push(`/admin/visites/${lead.visiteId}`)"
          >
            Voir la visite associée
          </button>
          <button class="btn btn--ghost" @click="openNoteModal">
            <Edit3 :size="14" /> Modifier la note
          </button>
        </div>
      </div>

      <!-- Onglets Détail / Histoire -->
      <div class="tabs">
        <button :class="['tab', { 'tab--active': tab === 'detail' }]" @click="switchTab('detail')">Détail</button>
        <button :class="['tab', { 'tab--active': tab === 'histoire' }]" @click="switchTab('histoire')">Histoire</button>
      </div>

      <div v-show="tab === 'detail'">
      <!-- Grille principale -->
      <div class="ld-grid">

        <!-- Colonne principale -->
        <div class="ld-col">

          <!-- Client -->
          <section class="ld-section">
            <h2 class="ld-section__title">Client</h2>
            <div class="ld-section__body">
              <div class="ld-row">
                <div class="ld-avatar">{{ lead.clientNom?.charAt(0)?.toUpperCase() || '?' }}</div>
                <div>
                  <p class="ld-row__main">{{ lead.clientNom }}</p>
                  <p class="ld-row__sub">{{ lead.clientEmail }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Annonce -->
          <section class="ld-section">
            <h2 class="ld-section__title">Annonce</h2>
            <div class="ld-section__body">
              <div v-if="lead.imagePrincipale" class="ld-annonce-img">
                <img :src="lead.imagePrincipale" :alt="lead.annonceLibelle" />
              </div>
              <dl class="ld-dl">
                <dt>Libellé</dt>
                <dd>
                  <RouterLink :to="`/annonces/${lead.annonceId}`" class="ld-link">
                    {{ lead.annonceLibelle }}
                  </RouterLink>
                </dd>
                <dt>Adresse</dt>
                <dd>{{ lead.annonceAdresse }}</dd>
                <dt>ID annonce</dt>
                <dd>#{{ lead.annonceId }}</dd>
              </dl>
            </div>
          </section>

          <!-- Note admin -->
          <section class="ld-section">
            <div class="ld-section__head-flex">
              <h2 class="ld-section__title">Note admin</h2>
              <button class="btn btn--ghost btn--sm" @click="openNoteModal">
                <Edit3 :size="12" /> Modifier
              </button>
            </div>
            <div class="ld-section__body">
              <p v-if="lead.noteAdmin" class="ld-note">
                <FileText :size="14" /> {{ lead.noteAdmin }}
              </p>
              <p v-else class="ld-empty-note">Aucune note. Cliquez sur "Modifier" pour en ajouter une.</p>
            </div>
          </section>

        </div>

        <!-- Colonne latérale -->
        <div class="ld-sidebar">

          <!-- Navigation croisée -->
          <section class="ld-section">
            <h2 class="ld-section__title">Navigation croisée</h2>
            <div class="ld-section__body ld-section__body--links">
              <RouterLink
                v-if="lead.visiteId"
                :to="`/admin/visites/${lead.visiteId}`"
                class="ld-nav-link ld-nav-link--visite"
              >
                <span class="ld-nav-link__label">Visite associée</span>
                <span class="ld-nav-link__id">#{{ lead.visiteId }} →</span>
              </RouterLink>
              <p v-else class="ld-empty-note">Aucune visite liée.</p>

              <RouterLink
                v-if="lead.contratId"
                :to="`/admin/contrats/${lead.contratId}`"
                class="ld-nav-link ld-nav-link--contrat"
              >
                <span class="ld-nav-link__label">Contrat généré</span>
                <span class="ld-nav-link__id">#{{ lead.contratId }} →</span>
              </RouterLink>
              <p v-else-if="lead.statut !== 'CONVERTI'" class="ld-empty-note">Pas encore de contrat.</p>
            </div>
          </section>

          <!-- Historique des dates -->
          <section class="ld-section">
            <h2 class="ld-section__title">Historique</h2>
            <div class="ld-section__body">
              <dl class="ld-dl">
                <dt>Créé le</dt>
                <dd>{{ formatDateTime(lead.createdAt) }}</dd>
                <dt>Modifié le</dt>
                <dd>{{ formatDateTime(lead.updatedAt) }}</dd>
                <dt v-if="lead.convertedAt">Converti le</dt>
                <dd v-if="lead.convertedAt" class="ld-converted">{{ formatDate(lead.convertedAt) }}</dd>
              </dl>
            </div>
          </section>

          <!-- Identifiants -->
          <section class="ld-section">
            <h2 class="ld-section__title">Identifiants</h2>
            <div class="ld-section__body">
              <dl class="ld-dl">
                <dt>Lead</dt><dd>#{{ lead.id }}</dd>
                <dt>Client</dt><dd>#{{ lead.clientId }}</dd>
                <dt>Annonce</dt><dd>#{{ lead.annonceId }}</dd>
                <dt v-if="lead.visiteId">Visite</dt><dd v-if="lead.visiteId">#{{ lead.visiteId }}</dd>
                <dt v-if="lead.contratId">Contrat</dt><dd v-if="lead.contratId">#{{ lead.contratId }}</dd>
              </dl>
            </div>
          </section>

        </div>
      </div>
      </div><!-- /tab-detail -->

      <!-- Onglet Histoire -->
      <div v-show="tab === 'histoire'" class="hist-section">
        <div v-if="historiqueLoading" class="hist-empty">Chargement de l'historique…</div>
        <div v-else-if="!historique.length" class="hist-empty">Aucun événement enregistré pour ce lead.</div>
        <div v-else class="hist-list">
          <div v-for="evt in historique" :key="evt.id" class="hist-item">
            <div class="hist-item__dot"></div>
            <div class="hist-item__card">
              <div class="hist-item__header">
                <span class="hist-item__action">{{ ACTION_LABELS[evt.action] || evt.action }}</span>
                <span class="hist-item__date">{{ formatDateTime(evt.createdAt) }}</span>
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

    <!-- Modal note -->
    <Teleport to="body">
      <div v-if="showNoteModal" class="modal-backdrop" @click.self="closeNoteModal">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal__header">
            <h2 class="modal__title">Note admin — Lead #{{ lead?.id }}</h2>
            <button class="modal__close" @click="closeNoteModal">✕</button>
          </div>
          <div class="modal__body">
            <p class="modal__meta">{{ lead?.clientNom }} · {{ lead?.annonceLibelle }}</p>
            <textarea
              v-model="noteValue"
              class="modal__textarea"
              placeholder="Note interne sur ce lead (max 2 000 caractères)…"
              maxlength="2000"
              rows="6"
            ></textarea>
            <p class="modal__counter">{{ noteValue.length }} / 2 000</p>
          </div>
          <div class="modal__footer">
            <button class="btn btn--ghost" @click="closeNoteModal">Annuler</button>
            <button class="btn btn--primary" :disabled="savingNote" @click="saveNote">
              {{ savingNote ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
.ld-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; display: flex; flex-direction: column; gap: 1.25rem; }

/* Retour */
.ld-back__btn { background: none; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: .4rem; font-size: .88rem; color: var(--color-text); opacity: .55; padding: 0; transition: opacity .15s; }
.ld-back__btn:hover { opacity: 1; }

/* States */
.ld-loading { display: flex; justify-content: center; padding: 4rem; }
.ld-error   { text-align: center; padding: 3rem; color: var(--color-accent); }

/* Header */
.ld-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; }
.ld-header__left { display: flex; align-items: center; gap: .75rem; }
.ld-header__id { font-size: 1.3rem; font-weight: 800; color: var(--color-text); }
.ld-header__actions { display: flex; gap: .5rem; flex-wrap: wrap; }

/* Grille */
.ld-grid { display: grid; grid-template-columns: 1fr 280px; gap: 1.25rem; align-items: start; }
.ld-col, .ld-sidebar { display: flex; flex-direction: column; gap: 1rem; }

/* Sections */
.ld-section { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }
.ld-section__title { font-size: .88rem; font-weight: 700; color: var(--color-text); opacity: .6; text-transform: uppercase; letter-spacing: .05em; padding: .85rem 1.1rem .6rem; border-bottom: 1px solid var(--color-border); }
.ld-section__head-flex { display: flex; align-items: center; justify-content: space-between; padding: .85rem 1.1rem .6rem; border-bottom: 1px solid var(--color-border); }
.ld-section__head-flex .ld-section__title { padding: 0; border-bottom: none; }
.ld-section__body { padding: 1rem 1.1rem; }
.ld-section__body--links { display: flex; flex-direction: column; gap: .6rem; }

/* Client row */
.ld-row { display: flex; gap: .85rem; align-items: center; }
.ld-avatar { width: 42px; height: 42px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 1rem; flex-shrink: 0; }
.ld-row__main { font-size: .95rem; font-weight: 700; color: var(--color-text); }
.ld-row__sub  { font-size: .8rem; color: var(--color-text); opacity: .5; }

/* Annonce image */
.ld-annonce-img { border-radius: var(--radius-sm); overflow: hidden; margin-bottom: .85rem; max-height: 140px; }
.ld-annonce-img img { width: 100%; height: 140px; object-fit: cover; }

/* DL */
.ld-dl { display: grid; grid-template-columns: auto 1fr; gap: .3rem .75rem; font-size: .85rem; }
.ld-dl dt { color: var(--color-text); opacity: .5; font-weight: 600; white-space: nowrap; }
.ld-dl dd { color: var(--color-text); font-weight: 500; margin: 0; }
.ld-converted { color: #059669; font-weight: 700; }

/* Note */
.ld-note { font-size: .88rem; color: var(--color-text); opacity: .75; font-style: italic; display: flex; align-items: flex-start; gap: .4rem; line-height: 1.5; }
.ld-empty-note { font-size: .82rem; color: var(--color-text); opacity: .4; }

/* Liens croisés */
.ld-nav-link { display: flex; justify-content: space-between; align-items: center; padding: .65rem .85rem; border-radius: var(--radius-sm); text-decoration: none; font-size: .85rem; font-weight: 600; transition: opacity .15s; }
.ld-nav-link--visite  { background: #eff6ff; color: #2563eb; }
.ld-nav-link--contrat { background: #ecfdf5; color: #059669; }
.ld-nav-link:hover { opacity: .78; }
.ld-nav-link__id { font-size: .75rem; opacity: .7; }

/* Lien générique */
.ld-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.ld-link:hover { text-decoration: underline; }

/* Boutons */
.btn { display: inline-flex; align-items: center; gap: .3rem; padding: .42rem .95rem; border-radius: var(--radius-sm); font-size: .85rem; font-weight: 600; border: none; cursor: pointer; text-decoration: none; transition: opacity .15s; }
.btn:disabled { opacity: .45; cursor: not-allowed; }
.btn--sm { padding: .28rem .6rem; font-size: .78rem; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--success { background: #059669; color: #fff; }
.btn--neutral { background: #f3f4f6; color: #374151; border: 1.5px solid #e5e7eb; }
.btn--ghost   { background: transparent; color: var(--color-text); border: 1.5px solid var(--color-border); }
.btn:not(:disabled):hover { opacity: .82; }

/* Badges */
.badge { padding: .3rem .8rem; border-radius: 12px; font-size: .82rem; font-weight: 700; }
.badge--info    { background: #dbeafe; color: #2563eb; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

/* Modal */
.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem; }
.modal { background: var(--color-card); border-radius: var(--radius); width: 100%; max-width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,.25); display: flex; flex-direction: column; overflow: hidden; }
.modal__header { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid var(--color-border); }
.modal__title { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.modal__close { background: none; border: none; font-size: 1.1rem; cursor: pointer; color: var(--color-text); opacity: .45; line-height: 1; padding: .2rem .4rem; }
.modal__close:hover { opacity: .8; }
.modal__body { padding: 1.25rem; display: flex; flex-direction: column; gap: .75rem; }
.modal__meta { font-size: .82rem; color: var(--color-text); opacity: .5; }
.modal__textarea { width: 100%; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); padding: .7rem .9rem; font-size: .88rem; font-family: inherit; resize: vertical; background: var(--color-background); color: var(--color-text); outline: none; transition: border-color .15s; box-sizing: border-box; }
.modal__textarea:focus { border-color: var(--color-primary); }
.modal__counter { font-size: .72rem; color: var(--color-text); opacity: .4; text-align: right; }
.modal__footer { display: flex; justify-content: flex-end; gap: .6rem; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }

/* Spinner */
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 750px) { .ld-grid { grid-template-columns: 1fr; } }

/* Onglets */
.tabs { display: flex; gap: .25rem; border-bottom: 2px solid var(--color-border); }
.tab {
  padding: .6rem 1.25rem; background: none; border: none; cursor: pointer;
  font-size: .88rem; font-weight: 600; color: var(--color-text); opacity: .5;
  border-bottom: 2px solid transparent; margin-bottom: -2px; transition: opacity .15s, border-color .15s;
}
.tab:hover { opacity: .75; }
.tab--active { opacity: 1; border-bottom-color: var(--color-primary); color: var(--color-primary); }

/* Timeline historique */
.hist-section { padding-top: .75rem; }
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
.hist-item__statuts { display: flex; align-items: center; gap: .4rem; margin-bottom: .35rem; flex-wrap: wrap; }
.hist-statut { padding: .15rem .55rem; border-radius: 8px; font-size: .75rem; font-weight: 600; }
.hist-statut--old { background: rgba(107,114,128,.1); color: #6b7280; }
.hist-statut--new { background: rgba(74,124,111,.12); color: #3a6b5e; }
.hist-arrow { font-size: .8rem; color: var(--color-text); opacity: .4; }
.hist-item__auteur { font-size: .78rem; color: var(--color-text); opacity: .5; }
.hist-item__comment { font-size: .82rem; color: var(--color-text); opacity: .7; font-style: italic; margin-top: .3rem; line-height: 1.5; }
</style>
