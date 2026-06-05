<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import AnnonceCard from '@/components/AnnonceCard.vue'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'
import commoditeService from '@/services/commoditeService'
import SvgIcon from '@/components/SvgIcon.vue'

const typesBien   = ref([])
const commodites  = ref([])
const annonces    = ref([])
const loading     = ref(false)
const error       = ref('')
const selectedCommoditeIds = ref([])

const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const PAGE_SIZE   = 9

const pageNumbers = computed(() =>
  Array.from({ length: totalPages.value }, (_, i) => i + 1)
)

const filtres = reactive({
  typeBienId:    null,
  prixMin:       null,
  prixMax:       null,
  adresse:       '',
  chambresRange: '',
  sortBy:        'createdAt',
  sortDir:       'DESC',
})

// Présets budget marché sénégalais
const budgetPresets = [
  { label: '< 150K',       min: 0,       max: 150000  },
  { label: '150K — 300K', min: 150000,  max: 300000  },
  { label: '300K — 600K', min: 300000,  max: 600000  },
  { label: '600K — 1M',  min: 600000,  max: 1000000 },
  { label: '> 1M',         min: 1000000, max: null    },
]

function applyBudgetPreset(preset) {
  filtres.prixMin = preset.min
  filtres.prixMax = preset.max
}

// Convertit la tranche de chambres en nbrMin/nbrMax pour l'API
function parseChambresFilter(range) {
  if (!range) return {}
  if (range === '10+') return { nbrPieces: 10 }
  const [min] = range.split('-').map(Number)
  return { nbrPieces: min }
}

function resetFiltres() {
  filtres.typeBienId    = null
  filtres.prixMin       = null
  filtres.prixMax       = null
  filtres.adresse       = ''
  filtres.chambresRange = ''
  selectedCommoditeIds.value = []
  fetchAnnonces(0)
}

async function fetchAnnonces(page = 0) {
  loading.value = true
  error.value   = ''
  try {
    const body = { page, size: PAGE_SIZE, sortBy: filtres.sortBy, sortDir: filtres.sortDir }
    if (filtres.typeBienId)       body.typeBienId = filtres.typeBienId
    if (filtres.adresse?.trim())  body.adresse    = filtres.adresse.trim()
    if (filtres.prixMin)          body.prixMin    = filtres.prixMin
    if (filtres.prixMax)          body.prixMax    = filtres.prixMax
    if (filtres.chambresRange)    Object.assign(body, parseChambresFilter(filtres.chambresRange))
    if (selectedCommoditeIds.value.length) body.commoditeIds = selectedCommoditeIds.value

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

onMounted(async () => {
  const [resTypes, resCommodites] = await Promise.all([
    typeBienService.getAllTypesBien().catch(() => null),
    commoditeService.getAllCommodites().catch(() => null),
  ])
  if (resTypes)      typesBien.value  = resTypes.data.data ?? []
  if (resCommodites) commodites.value = resCommodites.data.data ?? []
  fetchAnnonces(0)
})
</script>

<template>
  <div class="liste-page">
    <main class="liste-main">

      <!-- Hero -->
      <div class="liste-hero">
        <h1 class="liste-hero__title">Explorer les Propriétés</h1>
        <p class="liste-hero__sub">
          Découvrez notre sélection exclusive de biens immobiliers de luxe au Sénégal,
          des villas côtières aux appartements contemporains.
        </p>
      </div>

      <!-- Filtres horizontaux -->
      <div class="liste-filters">
        <div class="filters-grid">
          <div class="filter-field">
            <label class="filter-label">Quartier</label>
            <div class="filter-input-wrap">
              <SvgIcon name="map-pin" :size="16" class="filter-icon" />
              <input v-model="filtres.adresse" type="text" class="filter-input" placeholder="Almadies, Plateau…" />
            </div>
          </div>

          <div class="filter-field">
            <label class="filter-label">Type de bien</label>
            <div class="filter-input-wrap">
              <SvgIcon name="home" :size="16" class="filter-icon" />
              <select v-model="filtres.typeBienId" class="filter-select">
                <option :value="null">Tous types</option>
                <option v-for="t in typesBien" :key="t.id" :value="t.id">{{ t.libelle }}</option>
              </select>
            </div>
          </div>

          <!-- FIX 4 : budget min/max -->
          <div class="filter-field">
            <label class="filter-label">Budget min (FCFA)</label>
            <input v-model.number="filtres.prixMin" type="number" class="filter-input filter-input--noicon" placeholder="Ex: 150 000" min="0" step="50000" />
          </div>
          <div class="filter-field">
            <label class="filter-label">Budget max (FCFA)</label>
            <input v-model.number="filtres.prixMax" type="number" class="filter-input filter-input--noicon" placeholder="Ex: 600 000" min="0" step="50000" />
          </div>

          <div class="filter-field">
            <label class="filter-label">Chambres</label>
            <div class="filter-input-wrap">
              <SvgIcon name="bed" :size="16" class="filter-icon" />
              <select v-model="filtres.chambresRange" class="filter-select">
                <option value="">Toutes les chambres</option>
                <option value="1-3">Studio — 1 à 3 pièces</option>
                <option value="4-6">Appartement — 4 à 6 pièces</option>
                <option value="7-10">Villa — 7 à 10 pièces</option>
                <option value="10+">Domaine — 10 pièces et plus</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Présets budget -->
        <div class="budget-presets">
          <button
            v-for="preset in budgetPresets"
            :key="preset.label"
            class="preset-btn"
            :class="{ active: filtres.prixMin === preset.min && filtres.prixMax === preset.max }"
            @click="applyBudgetPreset(preset)"
          >
            {{ preset.label }}
          </button>
        </div>

        <!-- FIX 4 : commodités -->
        <div v-if="commodites.length" class="commodites-filter">
          <label class="filter-label">Équipements</label>
          <div class="commodites-grid">
            <label
              v-for="c in commodites"
              :key="c.id"
              class="commodite-chip"
              :class="{ selected: selectedCommoditeIds.includes(c.id) }"
            >
              <input type="checkbox" :value="c.id" v-model="selectedCommoditeIds" hidden />
              {{ c.libelle }}
            </label>
          </div>
        </div>

        <button class="filters-btn" @click="fetchAnnonces(0)">
          <SvgIcon name="search" :size="16" />
          Rechercher
        </button>
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
          v-for="(annonce, i) in annonces"
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
          :isNew="i % 2 === 0"
        />
      </div>

      <!-- Vide -->
      <div v-else class="liste-empty">
        <SvgIcon name="home" :size="48" class="liste-empty__icon" />
        <p class="liste-empty__title">Aucune annonce trouvée</p>
        <p class="liste-empty__sub">Essayez de modifier vos critères de recherche.</p>
        <button class="filters-btn" @click="resetFiltres">Effacer les filtres</button>
      </div>

      <!-- Pagination -->
      <nav v-if="totalPages > 1" class="liste-pagination">
        <button class="pagination-nav" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">
          <SvgIcon name="chevron-left" :size="18" />
          <span>Précédent</span>
        </button>

        <div class="pagination-pages">
          <button
            v-for="page in pageNumbers"
            :key="page"
            class="pagination-page"
            :class="{ 'pagination-page--active': page - 1 === currentPage }"
            @click="goToPage(page - 1)"
          >
            {{ page }}
          </button>
        </div>

        <button class="pagination-nav" :disabled="currentPage === totalPages - 1" @click="goToPage(currentPage + 1)">
          <span>Suivant</span>
          <SvgIcon name="chevron-right" :size="18" />
        </button>
      </nav>

    </main>
  </div>
</template>

<style scoped>
.liste-page { background: var(--color-background); }

.liste-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem 4rem;
}

/* Hero */
.liste-hero { margin-bottom: 2rem; }
.liste-hero__title {
  font-family: var(--font-serif);
  font-size: 3rem;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.1;
  margin-bottom: 0.5rem;
}
.liste-hero__sub {
  font-size: 1.05rem;
  color: var(--color-text-muted);
  max-width: 600px;
  line-height: 1.6;
}

/* Filtres */
.liste-filters {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2.5rem;
  box-shadow: var(--shadow-card);
}
.filters-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 1rem;
}
.filter-field { display: flex; flex-direction: column; gap: 0.35rem; }
.filter-label {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}
.filter-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.filter-icon {
  position: absolute;
  left: 0.65rem;
  color: var(--color-text-muted);
  pointer-events: none;
  flex-shrink: 0;
}
.filter-input,
.filter-select {
  width: 100%;
  padding: 0.6rem 0.75rem 0.6rem 2.2rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-background);
  font-size: 0.88rem;
  color: var(--color-text);
  transition: border-color 0.2s;
  appearance: none;
}
.filter-input--noicon {
  padding-left: 0.75rem;
}
.filter-input:focus,
.filter-select:focus { border-color: var(--color-primary); }

/* Présets budget */
.budget-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.preset-btn {
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  font-size: 12px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 150ms ease;
}
.preset-btn:hover, .preset-btn.active {
  border-color: var(--color-primary);
  background: #E8F2EF;
  color: var(--color-primary);
}

/* Commodités */
.commodites-filter { display: flex; flex-direction: column; gap: 8px; }
.commodites-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.commodite-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  cursor: pointer;
  font-size: 12px;
  color: var(--color-text);
  transition: all 150ms ease;
  user-select: none;
}
.commodite-chip:hover { border-color: var(--color-primary); color: var(--color-primary); }
.commodite-chip.selected {
  border-color: var(--color-primary);
  background: #E8F2EF;
  color: var(--color-primary);
}

.filters-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.5rem;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  font-weight: 600;
  white-space: nowrap;
  transition: background 0.2s;
  flex-shrink: 0;
  align-self: flex-start;
}
.filters-btn:hover { background: var(--color-primary-hover); }

/* Chargement */
.liste-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 4rem;
  color: var(--color-text-muted);
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
  gap: 1.5rem;
  margin-bottom: 2.5rem;
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
.liste-empty__icon { color: var(--color-text-muted); opacity: 0.3; margin-bottom: 0.5rem; }
.liste-empty__title { font-size: 1.1rem; font-weight: 700; color: var(--color-text); }
.liste-empty__sub { font-size: 0.9rem; color: var(--color-text-muted); margin-bottom: 0.5rem; }

/* Pagination */
.liste-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
}
.pagination-nav {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  transition: background 0.2s, color 0.2s;
}
.pagination-nav:hover:not(:disabled) {
  background: var(--color-border);
  color: var(--color-text);
}
.pagination-nav:disabled { opacity: 0.35; cursor: not-allowed; }
.pagination-pages { display: flex; gap: 0.25rem; }
.pagination-page {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  transition: background 0.2s, color 0.2s;
}
.pagination-page:hover { background: var(--color-border); color: var(--color-text); }
.pagination-page--active {
  background: var(--color-primary);
  color: #fff;
}

/* Responsive */
@media (max-width: 1024px) {
  .filters-grid { grid-template-columns: repeat(2, 1fr); }
  .liste-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .liste-filters { flex-direction: column; align-items: stretch; }
  .filters-btn { width: 100%; justify-content: center; }
}
@media (max-width: 600px) {
  .filters-grid { grid-template-columns: 1fr; }
  .liste-grid { grid-template-columns: 1fr; }
  .liste-hero__title { font-size: 2rem; }
}
</style>
