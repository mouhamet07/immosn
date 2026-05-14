<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import AnnonceCard from '@/components/AnnonceCard.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import { MOCK_ANNONCES } from '@/mocks/annonces'

const annonces = ref([])
const loading = ref(false)
const error = ref('')

// Filtres
const filtres = reactive({
  quartier: '',
  typeBien: '',
  budget: '',
  chambres: '',
})

// Pagination
const currentPage = ref(1)
const itemsPerPage = 9
const totalPages = ref(1)

const paginatedAnnonces = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return annonces.value.slice(start, start + itemsPerPage)
})

const pageNumbers = computed(() => {
  return Array.from({ length: totalPages.value }, (_, i) => i + 1)
})

async function fetchAnnonces() {
  loading.value = true
  error.value = ''
  try {
    await new Promise((r) => setTimeout(r, 300))
    let result = [...MOCK_ANNONCES]

    // Filtrage côté client
    if (filtres.quartier) {
      result = result.filter((a) => a.adresse.toLowerCase().includes(filtres.quartier.toLowerCase()))
    }
    if (filtres.typeBien) {
      result = result.filter((a) => a.typeBien === filtres.typeBien)
    }
    if (filtres.budget) {
      const [min, max] = filtres.budget.replace('+', '-999999999').split('-').map(Number)
      result = result.filter((a) => a.prix >= min && a.prix <= max)
    }
    if (filtres.chambres) {
      const nb = parseInt(filtres.chambres)
      result = filtres.chambres === '4'
        ? result.filter((a) => a.nbreDePieces >= 4)
        : result.filter((a) => a.nbreDePieces === nb)
    }

    annonces.value = result
    totalPages.value = Math.ceil(annonces.value.length / itemsPerPage) || 1
    currentPage.value = 1
  } catch (err) {
    console.warn('[ListeAnnoncesView] Erreur chargement annonces.', err)
    error.value = 'Impossible de charger les annonces. Veuillez réessayer.'
  } finally {
    loading.value = false
  }
}

function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

onMounted(fetchAnnonces)
</script>

<template>
  <div class="liste-page">
    <main class="liste-main">
      <!-- En-tête -->
      <div class="liste-header">
        <h1 class="liste-header__title">Explorer les Propriétés</h1>
        <p class="liste-header__subtitle">
          Découvrez notre sélection exclusive de biens immobiliers au Sénégal.
        </p>
      </div>

      <!-- Barre de filtres -->
      <div class="liste-filtres">
        <div class="liste-filtres__group">
          <label class="liste-filtres__label">QUARTIER</label>
          <select v-model="filtres.quartier" class="liste-filtres__select">
            <option value="">Tous les quartiers</option>
            <option value="almadies">Almadies</option>
            <option value="plateau">Plateau</option>
            <option value="mermoz">Mermoz</option>
            <option value="ngor">Ngor</option>
            <option value="sacre-coeur">Sacré-Cœur</option>
          </select>
        </div>

        <div class="liste-filtres__group">
          <label class="liste-filtres__label">TYPE DE BIEN</label>
          <select v-model="filtres.typeBien" class="liste-filtres__select">
            <option value="">Tous les types</option>
            <option value="APPARTEMENT">Appartement</option>
            <option value="MAISON">Maison</option>
            <option value="STUDIO">Studio</option>
            <option value="VILLA">Villa</option>
          </select>
        </div>

        <div class="liste-filtres__group">
          <label class="liste-filtres__label">BUDGET (FCFA)</label>
          <select v-model="filtres.budget" class="liste-filtres__select">
            <option value="">Toute gamme</option>
            <option value="0-500000">0 – 500 000</option>
            <option value="500000-2000000">500 000 – 2 000 000</option>
            <option value="2000000-10000000">2 000 000 – 10 000 000</option>
            <option value="10000000+">10 000 000+</option>
          </select>
        </div>

        <div class="liste-filtres__group">
          <label class="liste-filtres__label">CHAMBRES</label>
          <select v-model="filtres.chambres" class="liste-filtres__select">
            <option value="">Tout</option>
            <option value="1">1</option>
            <option value="2">2</option>
            <option value="3">3</option>
            <option value="4">4+</option>
          </select>
        </div>

        <ButtonPrimary @click="fetchAnnonces">Rechercher</ButtonPrimary>
      </div>

      <!-- État de chargement -->
      <div v-if="loading" class="liste-loading">
        <div class="liste-loading__spinner"></div>
        <p>Chargement des annonces...</p>
      </div>

      <!-- Erreur -->
      <div v-else-if="error" class="liste-error">{{ error }}</div>

      <!-- Grille d'annonces -->
      <div v-else-if="paginatedAnnonces.length" class="liste-grid">
        <AnnonceCard
          v-for="annonce in paginatedAnnonces"
          :key="annonce.id"
          :id="annonce.id"
          :title="annonce.libelle"
          :prix="annonce.prix"
          :images="annonce.photos"
          :nbrChambres="annonce.nbreDePieces"
          :nbrSallesBain="annonce.nbreSallesBain || 0"
          :surface="annonce.surface"
          :adresse="annonce.adresse"
          :badge="annonce.badge || ''"
        />
      </div>

      <!-- Aucun résultat -->
      <div v-else class="liste-empty">
        <p>🏠 Aucune annonce trouvée pour ces critères.</p>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="liste-pagination">
        <button
          class="liste-pagination__btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >
          ← Précédent
        </button>

        <button
          v-for="page in pageNumbers"
          :key="page"
          class="liste-pagination__page"
          :class="{ 'liste-pagination__page--active': page === currentPage }"
          @click="goToPage(page)"
        >
          {{ page }}
        </button>

        <button
          class="liste-pagination__btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          Suivant →
        </button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.liste-page {
  background: var(--color-background);
}

.liste-main {
  max-width: 1200px;
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
  margin-bottom: 0.5rem;
}

.liste-header__subtitle {
  color: var(--color-text);
  opacity: 0.6;
}

/* Filtres */
.liste-filtres {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  background: var(--color-card);
  padding: 1.5rem;
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  margin-bottom: 2rem;
}

.liste-filtres__group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  flex: 1;
  min-width: 160px;
}

.liste-filtres__label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text);
  opacity: 0.7;
}

.liste-filtres__select {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 0.9rem;
  color: var(--color-text);
  transition: border-color 0.2s;
}

.liste-filtres__select:focus {
  border-color: var(--color-primary);
  outline: none;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

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
  text-align: center;
  padding: 4rem;
  color: var(--color-text);
  opacity: 0.5;
  font-size: 1.1rem;
}

/* Pagination */
.liste-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
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

.liste-pagination__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.liste-pagination__page {
  width: 38px;
  height: 38px;
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
  .liste-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .liste-grid {
    grid-template-columns: 1fr;
  }

  .liste-filtres {
    flex-direction: column;
  }
}
</style>
