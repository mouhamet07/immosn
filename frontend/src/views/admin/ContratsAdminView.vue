<script setup>
import { ref, onMounted } from 'vue'
import contratService from '@/services/contratService'
import FilterSelect from '@/components/FilterSelect.vue'

const contrats    = ref([])
const loading     = ref(false)
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const filtreStatut = ref('')

// Modal édition
const showEdit    = ref(false)
const editId      = ref(null)
const editForm    = ref({})
const editIsLoc   = ref(false)  // true si le contrat édité est de type LOCATION
const saving      = ref(false)

const STATUTS       = ['', 'EN_ATTENTE', 'ACTIF', 'EXPIRE', 'RESILIE']
const STATUT_LABELS = { EN_ATTENTE: 'En attente', ACTIF: 'Actif', EXPIRE: 'Expiré', RESILIE: 'Résilié' }
const filterOptions = STATUTS.map(s => ({ value: s, label: s ? STATUT_LABELS[s] : 'Tous les statuts' }))
const STATUT_COLORS = { EN_ATTENTE: 'badge--warning', ACTIF: 'badge--success', EXPIRE: 'badge--neutral', RESILIE: 'badge--danger' }

async function fetchContrats(page = 0) {
  loading.value = true
  try {
    const res = await contratService.getAllContrats(page, 20, filtreStatut.value || null)
    contrats.value    = res.data.data
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch (err) { console.error('[ContratsAdminView] fetchContrats error:', err?.response?.status, err?.message); contrats.value = [] }
  finally { loading.value = false }
}

function openEdit(c) {
  editId.value    = c.id
  editIsLoc.value = c.typeContrat === 'LOCATION'
  editForm.value  = {
    dateDebut:         c.dateDebut ?? '',
    dateFin:           c.dateFin ?? '',
    montant:           c.montant ?? '',
    dureeLocationMois: c.dureeLocationMois ?? '',
    statut:            c.statut,
    documentUrl:       c.documentUrl ?? '',
    notes:             c.notes ?? '',
  }
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    const payload = {
      dateDebut:   editForm.value.dateDebut   || null,
      statut:      editForm.value.statut      || null,
      documentUrl: editForm.value.documentUrl || null,
      notes:       editForm.value.notes       || null,
    }
    if (editIsLoc.value) {
      // LOCATION : dureeLocationMois déclenche le recalcul côté backend
      payload.dureeLocationMois = editForm.value.dureeLocationMois
        ? Number(editForm.value.dureeLocationMois) : null
    } else {
      // VENTE : montant et dateFin sont libres
      payload.dateFin  = editForm.value.dateFin  || null
      payload.montant  = editForm.value.montant ? Number(editForm.value.montant) : null
    }
    await contratService.update(editId.value, payload)
    showEdit.value = false
    await fetchContrats(currentPage.value)
  } catch (err) {
    alert(err.response?.data?.message || 'Erreur lors de la modification.')
  } finally {
    saving.value = false
  }
}

function formatDate(d) { return d ? new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }) : '–' }
function formatMontant(v) { return new Intl.NumberFormat('fr-SN').format(v) + ' FCFA' }

onMounted(() => fetchContrats(0))
</script>

<template>
  <div class="ca-page">
    <div class="ca-toolbar">
      <div>
        <h1 class="ca-toolbar__title">Contrats</h1>
        <p class="ca-toolbar__count">{{ totalItems }} contrat{{ totalItems !== 1 ? 's' : '' }}</p>
      </div>
      <FilterSelect
        :model-value="filtreStatut"
        :options="filterOptions"
        @update:model-value="(v) => { filtreStatut = v; fetchContrats(0) }"
      />
    </div>

    <div v-if="loading" class="ca-loading"><div class="spinner"></div></div>
    <div v-else-if="!contrats.length" class="ca-empty">Aucun contrat.</div>

    <div v-else class="ca-table-wrap">
      <table class="ca-table">
        <thead>
          <tr>
            <th>#</th><th>Client</th><th>Annonce</th><th>Type</th><th>Montant</th>
            <th>Début</th><th>Fin</th><th>Statut</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in contrats" :key="c.id">
            <td class="ca-td-id">{{ c.id }}</td>
            <td><p class="ca-td-name">{{ c.clientNom }}</p></td>
            <td>
              <RouterLink :to="`/annonces/${c.annonceId}`" class="ca-td-link">{{ c.annonceLibelle }}</RouterLink>
              <p class="ca-td-sub">{{ c.annonceAdresse }}</p>
            </td>
            <td>
              <span v-if="c.typeContrat" :class="['badge-type', c.typeContrat === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                {{ c.typeContrat === 'VENTE' ? 'Vente' : 'Location' }}
                <span v-if="c.typeContrat === 'LOCATION' && c.dureeLocationMois" class="badge-type__duree">{{ c.dureeLocationMois }} mois</span>
              </span>
              <span v-else class="ca-td-sub">–</span>
            </td>
            <td class="ca-td-montant">{{ formatMontant(c.montant) }}</td>
            <td>{{ formatDate(c.dateDebut) }}</td>
            <td>{{ formatDate(c.dateFin) }}</td>
            <td><span :class="['badge', STATUT_COLORS[c.statut]]">{{ STATUT_LABELS[c.statut] }}</span></td>
            <td>
              <button class="ca-btn" @click="openEdit(c)">✎ Modifier</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="ca-pager">
      <button :disabled="currentPage === 0" @click="fetchContrats(currentPage - 1)">← Précédent</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages - 1" @click="fetchContrats(currentPage + 1)">Suivant →</button>
    </div>

    <!-- Modal édition -->
    <Teleport to="body">
      <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Modifier le contrat #{{ editId }}</h2>

          <!-- Champs communs -->
          <div class="modal-box__field">
            <label>Date de début</label>
            <input v-model="editForm.dateDebut" type="date" class="modal-box__input" />
          </div>

          <!-- LOCATION : durée → montant et dateFin recalculés automatiquement -->
          <template v-if="editIsLoc">
            <div class="modal-box__field">
              <label>Durée du bail (mois)</label>
              <input v-model="editForm.dureeLocationMois" type="number" min="1" class="modal-box__input"
                placeholder="Ex: 12" />
            </div>
            <p class="ca-info-box">
              Le montant et la date de fin sont recalculés automatiquement à partir de la durée saisie.
            </p>
          </template>

          <!-- VENTE : montant et dateFin librement éditables -->
          <template v-else>
            <div class="modal-box__row">
              <div class="modal-box__field">
                <label>Date de fin</label>
                <input v-model="editForm.dateFin" type="date" class="modal-box__input" />
              </div>
              <div class="modal-box__field">
                <label>Montant (FCFA)</label>
                <input v-model="editForm.montant" type="number" min="0" class="modal-box__input" />
              </div>
            </div>
          </template>

          <div class="modal-box__field">
            <label>Statut</label>
            <select v-model="editForm.statut" class="modal-box__input">
              <option v-for="s in ['EN_ATTENTE','ACTIF','EXPIRE','RESILIE']" :key="s" :value="s">{{ STATUT_LABELS[s] }}</option>
            </select>
          </div>
          <div class="modal-box__field"><label>URL document</label><input v-model="editForm.documentUrl" type="text" class="modal-box__input" placeholder="https://…" /></div>
          <div class="modal-box__field"><label>Notes</label><textarea v-model="editForm.notes" class="modal-box__textarea" rows="3" /></div>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showEdit = false">Annuler</button>
            <button class="modal-box__submit" :disabled="saving" @click="saveEdit">{{ saving ? 'Enregistrement…' : 'Enregistrer' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.ca-page { background: var(--color-background); min-height: 100%; padding: 1.5rem; }
.ca-toolbar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.ca-toolbar__title { font-size: 1.4rem; font-weight: 800; color: var(--color-text); }
.ca-toolbar__count { font-size: .82rem; color: var(--color-text); opacity: .5; }
.ca-loading { display: flex; justify-content: center; padding: 4rem; }
.ca-empty { text-align: center; padding: 3rem; color: var(--color-text); opacity: .45; }
.ca-table-wrap { overflow-x: auto; }
.ca-table { width: 100%; border-collapse: collapse; background: var(--color-card); border-radius: var(--radius); overflow: hidden; box-shadow: var(--shadow-card); }
.ca-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.ca-table td { padding: .85rem 1rem; font-size: .88rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.ca-table tr:last-child td { border-bottom: none; }
.ca-td-id { font-size: .78rem; opacity: .45; }
.ca-td-name { font-weight: 600; }
.ca-td-link { color: var(--color-primary); font-weight: 600; text-decoration: none; }
.ca-td-sub { font-size: .75rem; opacity: .5; }
.ca-td-montant { font-weight: 700; color: var(--color-accent); }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .2rem .6rem; border-radius: 10px; font-size: .75rem; font-weight: 700; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }
.badge-type__duree { font-size: .68rem; font-weight: 500; opacity: .75; }
.ca-btn { padding: .3rem .7rem; border: 1.5px solid var(--color-border); border-radius: 6px; font-size: .78rem; cursor: pointer; background: none; color: var(--color-text); transition: all .15s; }
.ca-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.ca-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.25rem; font-size: .88rem; }
.ca-pager button { padding: .4rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer; }
.ca-pager button:disabled { opacity: .4; cursor: not-allowed; }
.badge { padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; }
.badge--warning { background: #fef3c7; color: #d97706; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }
.badge--danger  { background: #fee2e2; color: #dc2626; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; }
.modal-box__row { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
.modal-box__field { display: flex; flex-direction: column; gap: .4rem; margin-bottom: 1rem; }
.modal-box__field label { font-size: .78rem; font-weight: 600; color: var(--color-text); opacity: .7; }
.modal-box__input, .modal-box__textarea { padding: .65rem .9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: .88rem; background: var(--color-background); color: var(--color-text); width: 100%; box-sizing: border-box; }
.modal-box__textarea { resize: vertical; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__submit { padding: .5rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__submit:disabled { opacity: .5; cursor: not-allowed; }
.ca-info-box {
  font-size: .78rem; color: var(--color-text); opacity: .6; font-style: italic;
  padding: .5rem .75rem; background: var(--color-background); border-radius: 6px;
  margin-bottom: 1rem;
}
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
