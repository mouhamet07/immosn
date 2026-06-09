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


// Convertit la tranche de chambres en nbrPieces pour l'API (champ exact SearchAnnonceRequestDto)
function parseChambresFilter(range) {
  if (!range) return {}
  if (range === '1-3')  return { nbrPieces: 1 }
  if (range === '4-6')  return { nbrPieces: 4 }
  if (range === '7-10') return { nbrPieces: 7 }
  if (range === '10+')  return { nbrPieces: 10 }
  return {}
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
    // Corps exact selon SearchAnnonceRequestDto du backend
    const body = { page, size: PAGE_SIZE, sortBy: filtres.sortBy, sortDir: filtres.sortDir }
    if (filtres.typeBienId)              body.typeBienId   = filtres.typeBienId
    if (filtres.adresse?.trim())         body.adresse      = filtres.adresse.trim()
    if (filtres.prixMin)                 body.prixMin      = filtres.prixMin
    if (filtres.prixMax)                 body.prixMax      = filtres.prixMax
    if (filtres.chambresRange)           Object.assign(body, parseChambresFilter(filtres.chambresRange))
    if (selectedCommoditeIds.value.length > 0) body.commoditeIds = selectedCommoditeIds.value

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

      <!-- Hero h1 -->
      <h1 class="liste-hero__title">Explorer les Propriétés</h1>

      <!-- Sous-titre page annonces -->
      <p class="liste-hero__sub">
        Découvrez notre sélection exclusive de biens immobiliers de luxe au Sénégal,
        des villas côtières aux appartements contemporains.
      </p>

      <!-- Filtres compacts sur une ligne -->
      <div class="liste-filters">
        <div class="filter-input-wrap">
          <SvgIcon name="map-pin" :size="14" class="filter-icon" />
          <input v-model="filtres.adresse" type="text" class="filter-input" placeholder="Quartier..." />
        </div>

        <div class="filter-input-wrap">
          <SvgIcon name="home" :size="14" class="filter-icon" />
          <select v-model="filtres.typeBienId" class="filter-select">
            <option :value="null">Type de bien</option>
            <option v-for="t in typesBien" :key="t.id" :value="t.id">{{ t.libelle }}</option>
          </select>
        </div>

        <input v-model.number="filtres.prixMin" type="number" class="filter-input filter-input--noicon" placeholder="Prix min" min="0" step="50000" />
        <input v-model.number="filtres.prixMax" type="number" class="filter-input filter-input--noicon" placeholder="Prix max" min="0" step="50000" />

        <div class="filter-input-wrap">
          <SvgIcon name="bed" :size="14" class="filter-icon" />
          <select v-model="filtres.chambresRange" class="filter-select">
            <option value="">Pièces</option>
            <option value="1-3">1 — 3 pièces</option>
            <option value="4-6">4 — 6 pièces</option>
            <option value="7-10">7 — 10 pièces</option>
            <option value="10+">10+ pièces</option>
          </select>
        </div>

        <button class="filters-btn" @click="fetchAnnonces(0)">
          <SvgIcon name="search" :size="14" />
          Rechercher
        </button>

        <button v-if="filtres.adresse || filtres.typeBienId || filtres.prixMin || filtres.prixMax || filtres.chambresRange || selectedCommoditeIds.length" class="filters-reset" @click="resetFiltres">
          <SvgIcon name="x" :size="14" />
        </button>
      </div>

      <!-- Filtres commodités sous la barre principale -->
      <div v-if="commodites.length" class="commodites-filter">
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
.liste-hero__title {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  font-weight: 800;
  color: #4a7b6f;
  line-height: 1.15;
  margin-bottom: 0.6rem;
}

/* Sous-titre */
.liste-hero__sub {
  font-size: 0.95rem;
  color: var(--color-text-muted);
  max-width: 600px;
  line-height: 1.6;
  margin-bottom: 1.25rem;
}

/* Filtres compacts */
.liste-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 1rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 0.75rem 1rem;
  box-shadow: var(--shadow-card);
}

.filter-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.filter-icon {
  position: absolute;
  left: 0.55rem;
  color: var(--color-text-muted);
  pointer-events: none;
  flex-shrink: 0;
}
.filter-input,
.filter-select {
  width: 100%;
  padding: 0.45rem 0.6rem 0.45rem 1.8rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-background);
  font-size: 0.82rem;
  color: var(--color-text);
  transition: border-color 0.2s;
  appearance: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.filter-input--noicon {
  padding-left: 0.6rem;
  flex: 1;
  min-width: 80px;
}
.filter-input:hover,
.filter-select:hover { border-color: var(--color-border-hover); }
.filter-input:focus,
.filter-select:focus { border-color: var(--color-primary); outline: none; }

.filters-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0.45rem 0.9rem;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  transition: background 0.2s;
  flex-shrink: 0;
}
.filters-btn:hover { background: var(--color-primary-hover); }

.filters-reset {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.filters-reset:hover { border-color: var(--color-accent); color: var(--color-accent); }

/* Commodités sous la barre */
.commodites-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 1.5rem;
}
.commodite-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  cursor: pointer;
  font-size: 12px;
  color: var(--color-text);
  transition: all 150ms ease;
  user-select: none;
}
.commodite-chip:hover { border-color: var(--color-primary); color: var(--color-primary); }
.commodite-chip.selected { border-color: var(--color-primary); background: #E8F2EF; color: var(--color-primary); }

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
  .liste-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .liste-main { padding: 1.5rem 1rem 3rem; }
  .liste-hero__title { font-size: 1.8rem; }
  .liste-filters { flex-direction: column; align-items: stretch; gap: 6px; }
  .filter-input-wrap { flex: none; }
  .filter-input--noicon { flex: none; min-width: 0; }
  .filters-btn { width: 100%; justify-content: center; padding: 0.65rem; }
  .filters-reset { width: 100%; height: 36px; }
}

@media (max-width: 480px) {
  .liste-grid { grid-template-columns: 1fr; }
  .liste-hero__title { font-size: 1.5rem; }
  .liste-hero__sub { font-size: 0.88rem; }
}
</style>