<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import AnnonceCard from '@/components/AnnonceCard.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'
import commoditeService from '@/services/commoditeService'

// ── Données de référence ───────────────────────────────────
const typesBien    = ref([])
const commodites   = ref([])

// ── Résultats ─────────────────────────────────────────────
const annonces    = ref([])
const loading     = ref(false)
const error       = ref('')

// ── Pagination ─────────────────────────────────────────────
const currentPage  = ref(0)
const totalPages   = ref(1)
const totalItems   = ref(0)
const PAGE_SIZE    = 9

const pageNumbers = computed(() =>
  Array.from({ length: totalPages.value }, (_, i) => i + 1)
)

// ── Filtres ────────────────────────────────────────────────
const filtres = reactive({
  typeBienId:   null,
  prixMin:      null,
  prixMax:      null,
  adresse:      '',
  nbrPieces:    null,
  commoditeIds: [],
  sortBy:       'createdAt',
  sortDir:      'DESC',
})

// ── Chips des filtres actifs ───────────────────────────────
const activeChips = computed(() => {
  const chips = []
  if (filtres.typeBienId) {
    const t = typesBien.value.find(t => t.id === filtres.typeBienId)
    if (t) chips.push({ key: 'typeBienId', label: t.libelle })
  }
  if (filtres.prixMin) chips.push({ key: 'prixMin', label: `Min ${formatPrix(filtres.prixMin)}` })
  if (filtres.prixMax) chips.push({ key: 'prixMax', label: `Max ${formatPrix(filtres.prixMax)}` })
  if (filtres.adresse) chips.push({ key: 'adresse', label: filtres.adresse })
  if (filtres.nbrPieces) chips.push({ key: 'nbrPieces', label: `${filtres.nbrPieces} pièce(s)` })
  filtres.commoditeIds.forEach(id => {
    const c = commodites.value.find(c => c.id === id)
    if (c) chips.push({ key: `commodite_${id}`, label: c.libelle })
  })
  return chips
})

function removeChip(key) {
  if (key === 'typeBienId')  { filtres.typeBienId = null }
  else if (key === 'prixMin')  { filtres.prixMin = null }
  else if (key === 'prixMax')  { filtres.prixMax = null }
  else if (key === 'adresse')  { filtres.adresse = '' }
  else if (key === 'nbrPieces') { filtres.nbrPieces = null }
  else if (key.startsWith('commodite_')) {
    const id = parseInt(key.replace('commodite_', ''))
    filtres.commoditeIds = filtres.commoditeIds.filter(c => c !== id)
  }
  fetchAnnonces(0)
}

function resetFiltres() {
  filtres.typeBienId   = null
  filtres.prixMin      = null
  filtres.prixMax      = null
  filtres.adresse      = ''
  filtres.nbrPieces    = null
  filtres.commoditeIds = []
  filtres.sortBy       = 'createdAt'
  filtres.sortDir      = 'DESC'
  fetchAnnonces(0)
}

// ── Commodités : toggle sélection ─────────────────────────
function toggleCommodite(id) {
  const idx = filtres.commoditeIds.indexOf(id)
  if (idx === -1) filtres.commoditeIds.push(id)
  else filtres.commoditeIds.splice(idx, 1)
}

function isCommoditeSelected(id) {
  return filtres.commoditeIds.includes(id)
}

// ── Appel API recherche ────────────────────────────────────
async function fetchAnnonces(page = 0) {
  loading.value = true
  error.value   = ''
  try {
    const body = {
      page,
      size: PAGE_SIZE,
      sortBy:  filtres.sortBy  || 'createdAt',
      sortDir: filtres.sortDir || 'DESC',
    }
    if (filtres.typeBienId)              body.typeBienId   = filtres.typeBienId
    if (filtres.prixMin)                 body.prixMin      = Number(filtres.prixMin)
    if (filtres.prixMax)                 body.prixMax      = Number(filtres.prixMax)
    if (filtres.adresse?.trim())         body.adresse      = filtres.adresse.trim()
    if (filtres.nbrPieces)               body.nbrPieces    = Number(filtres.nbrPieces)
    if (filtres.commoditeIds?.length)    body.commoditeIds = filtres.commoditeIds

    const response = await annonceService.searchAnnonces(body)
    const paged    = response.data
    annonces.value    = paged.data
    totalPages.value  = paged.totalPages
    currentPage.value = paged.currentPage
    totalItems.value  = paged.totalElements
  } catch {
    error.value = 'Impossible de charger les annonces. Veuillez réessayer.'
  } finally {
    loading.value = false
  }
}

function goToPage(page) {
  if (page >= 0 && page < totalPages.value) {
    fetchAnnonces(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function formatPrix(v) {
  return new Intl.NumberFormat('fr-SN').format(v) + ' F'
}

// ── Chargement initial ─────────────────────────────────────
onMounted(async () => {
  const [resTb, resCom] = await Promise.allSettled([
    typeBienService.getAllTypesBien(),
    commoditeService.getAllCommodites(),
  ])
  if (resTb.status === 'fulfilled')  typesBien.value  = resTb.value.data.data  ?? []
  if (resCom.status === 'fulfilled') commodites.value = resCom.value.data.data ?? []
  fetchAnnonces(0)
})
</script>

<template>
  <div class="liste-page">
    <main class="liste-main">

      <!-- En-tête ─────────────────────────────────────────── -->
      <div class="liste-header">
        <h1 class="liste-header__title">Explorer les Propriétés</h1>
        <p class="liste-header__subtitle">
          Découvrez notre sélection exclusive de biens immobiliers au Sénégal.
        </p>
      </div>

      <div class="liste-layout">

        <!-- ── SIDEBAR FILTRES ─────────────────────────────── -->
        <aside class="liste-sidebar">
          <div class="sidebar-card">
            <div class="sidebar-card__header">
              <h2 class="sidebar-card__title">Filtres</h2>
              <button v-if="activeChips.length" class="sidebar-reset" @click="resetFiltres">
                Réinitialiser
              </button>
            </div>

            <!-- Type de bien -->
            <div class="sidebar-field">
              <label class="sidebar-label">Type de bien</label>
              <select v-model="filtres.typeBienId" class="sidebar-select">
                <option :value="null">Tous</option>
                <option v-for="t in typesBien" :key="t.id" :value="t.id">{{ t.libelle }}</option>
              </select>
            </div>

            <!-- Budget -->
            <div class="sidebar-field">
              <label class="sidebar-label">Budget (FCFA)</label>
              <div class="sidebar-range">
                <input v-model.number="filtres.prixMin" type="number" min="0"
                       class="sidebar-input" placeholder="Min" />
                <span class="sidebar-range__sep">–</span>
                <input v-model.number="filtres.prixMax" type="number" min="0"
                       class="sidebar-input" placeholder="Max" />
              </div>
            </div>

            <!-- Quartier -->
            <div class="sidebar-field">
              <label class="sidebar-label">Quartier / Adresse</label>
              <input v-model="filtres.adresse" type="text" class="sidebar-input --full"
                     placeholder="ex. Almadies, Plateau…" />
            </div>

            <!-- Nombre de pièces -->
            <div class="sidebar-field">
              <label class="sidebar-label">Nombre de pièces</label>
              <select v-model.number="filtres.nbrPieces" class="sidebar-select">
                <option :value="null">Tous</option>
                <option v-for="n in [1,2,3,4,5,6]" :key="n" :value="n">{{ n }}</option>
                <option :value="7">7+</option>
              </select>
            </div>

            <!-- Commodités -->
            <div class="sidebar-field">
              <label class="sidebar-label">Équipements</label>
              <div class="sidebar-commodites">
                <button
                  v-for="c in commodites"
                  :key="c.id"
                  class="sidebar-commodite"
                  :class="{ '--active': isCommoditeSelected(c.id) }"
                  type="button"
                  @click="toggleCommodite(c.id)"
                >
                  {{ c.libelle }}
                </button>
              </div>
            </div>

            <!-- Tri -->
            <div class="sidebar-field">
              <label class="sidebar-label">Trier par</label>
              <div class="sidebar-tri">
                <select v-model="filtres.sortBy" class="sidebar-select">
                  <option value="createdAt">Date</option>
                  <option value="prix">Prix</option>
                </select>
                <select v-model="filtres.sortDir" class="sidebar-select">
                  <option value="DESC">Décroissant</option>
                  <option value="ASC">Croissant</option>
                </select>
              </div>
            </div>

            <ButtonPrimary class="sidebar-btn" @click="fetchAnnonces(0)">
              Rechercher
            </ButtonPrimary>
          </div>
        </aside>

        <!-- ── CONTENU PRINCIPAL ───────────────────────────── -->
        <section class="liste-content">

          <!-- Chips filtres actifs -->
          <div v-if="activeChips.length" class="liste-chips">
            <span
              v-for="chip in activeChips"
              :key="chip.key"
              class="chip"
              @click="removeChip(chip.key)"
            >
              {{ chip.label }} ✕
            </span>
          </div>

          <!-- Compteur résultats + tri mobile -->
          <div class="liste-toolbar">
            <p class="liste-toolbar__count">
              <template v-if="!loading">
                {{ totalItems }} résultat{{ totalItems !== 1 ? 's' : '' }}
              </template>
            </p>
          </div>

          <!-- Chargement -->
          <div v-if="loading" class="liste-loading">
            <div class="liste-loading__spinner"></div>
            <p>Chargement des annonces...</p>
          </div>

          <!-- Erreur -->
          <div v-else-if="error" class="liste-error">{{ error }}</div>

          <!-- Grille -->
          <div v-else-if="annonces.length" class="liste-grid">
            <AnnonceCard
              v-for="annonce in annonces"
              :key="annonce.id"
              :id="annonce.id"
              :title="annonce.libelle"
              :prix="annonce.prix"
              :images="annonce.imagePrincipale ? [annonce.imagePrincipale] : []"
              :nbrChambres="annonce.nbrPieces"
              :nbrSallesBain="0"
              :surface="annonce.surface"
              :adresse="annonce.adresse"
              :badge="annonce.typeBien?.libelle || ''"
            />
          </div>

          <!-- Vide -->
          <div v-else class="liste-empty">
            <div class="liste-empty__icon">🏠</div>
            <p class="liste-empty__title">Aucune annonce trouvée</p>
            <p class="liste-empty__sub">Essayez de modifier vos critères de recherche.</p>
            <ButtonPrimary variant="outline" @click="resetFiltres">Effacer les filtres</ButtonPrimary>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="liste-pagination">
            <button
              class="liste-pagination__btn"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              ← Précédent
            </button>
            <button
              v-for="page in pageNumbers"
              :key="page"
              class="liste-pagination__page"
              :class="{ 'liste-pagination__page--active': page - 1 === currentPage }"
              @click="goToPage(page - 1)"
            >
              {{ page }}
            </button>
            <button
              class="liste-pagination__btn"
              :disabled="currentPage === totalPages - 1"
              @click="goToPage(currentPage + 1)"
            >
              Suivant →
            </button>
          </div>

        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.liste-page {
  background: var(--color-background);
}
.liste-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

/* En-tête */
.liste-header {
  margin-bottom: 2rem;
}
.liste-header__title {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-text);
  margin-bottom: 0.4rem;
}
.liste-header__subtitle {
  color: var(--color-text);
  opacity: 0.6;
}

/* Layout */
.liste-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 2rem;
  align-items: start;
}

/* ── SIDEBAR ── */
.liste-sidebar {
  position: sticky;
  top: 80px;
}
.sidebar-card {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  padding: 1.5rem;
}
.sidebar-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}
.sidebar-card__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}
.sidebar-reset {
  font-size: 0.78rem;
  color: var(--color-primary);
  font-weight: 600;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.sidebar-reset:hover { text-decoration: underline; }

.sidebar-field {
  margin-bottom: 1.25rem;
}
.sidebar-label {
  display: block;
  font-size: 0.73rem;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--color-text);
  opacity: 0.65;
  margin-bottom: 0.45rem;
}
.sidebar-select,
.sidebar-input {
  width: 100%;
  padding: 0.6rem 0.8rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-background);
  font-size: 0.88rem;
  color: var(--color-text);
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.sidebar-select:focus,
.sidebar-input:focus {
  border-color: var(--color-primary);
  outline: none;
}
.sidebar-input.--full { width: 100%; }

.sidebar-range {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.sidebar-range .sidebar-input { flex: 1; }
.sidebar-range__sep {
  font-weight: 700;
  color: var(--color-text);
  opacity: 0.4;
}

.sidebar-tri {
  display: flex;
  gap: 0.5rem;
}
.sidebar-tri .sidebar-select { flex: 1; }

/* Commodités tags */
.sidebar-commodites {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.sidebar-commodite {
  padding: 0.3rem 0.7rem;
  border: 1.5px solid var(--color-border);
  border-radius: 20px;
  font-size: 0.78rem;
  background: var(--color-background);
  color: var(--color-text);
  cursor: pointer;
  transition: all 0.18s;
}
.sidebar-commodite:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.sidebar-commodite.--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.sidebar-btn {
  width: 100%;
  margin-top: 0.5rem;
}

/* ── CONTENU ── */
.liste-content { min-width: 0; }

/* Chips actifs */
.liste-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  background: rgba(var(--color-primary-rgb, 59, 130, 246), 0.1);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.chip:hover { background: rgba(59, 130, 246, 0.2); }

/* Toolbar */
.liste-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}
.liste-toolbar__count {
  font-size: 0.88rem;
  color: var(--color-text);
  opacity: 0.55;
}

/* Chargement */
.liste-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 4rem;
  color: var(--color-text);
  opacity: 0.6;
}
.liste-loading__spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Erreur */
.liste-error {
  text-align: center;
  padding: 3rem;
  color: var(--color-accent);
  background: rgba(212, 113, 74, 0.08);
  border-radius: var(--radius);
}

/* Grille */
.liste-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
  margin-bottom: 2rem;
}

/* Vide */
.liste-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
}
.liste-empty__icon { font-size: 3rem; opacity: 0.3; }
.liste-empty__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
}
.liste-empty__sub {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.55;
  margin-bottom: 0.5rem;
}

/* Pagination */
.liste-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}
.liste-pagination__btn {
  padding: 0.5rem 1rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-text);
  font-size: 0.88rem;
  font-weight: 600;
  transition: all 0.2s;
}
.liste-pagination__btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.liste-pagination__btn:disabled { opacity: 0.4; cursor: not-allowed; }
.liste-pagination__page {
  width: 36px;
  height: 36px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-text);
  font-size: 0.88rem;
  font-weight: 600;
  transition: all 0.2s;
}
.liste-pagination__page:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.liste-pagination__page--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

/* Responsive */
@media (max-width: 1024px) {
  .liste-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 860px) {
  .liste-layout { grid-template-columns: 1fr; }
  .liste-sidebar { position: static; }
}
@media (max-width: 600px) {
  .liste-grid { grid-template-columns: 1fr; }
}
</style>
