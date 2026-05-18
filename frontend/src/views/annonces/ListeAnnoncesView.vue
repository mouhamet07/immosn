<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import AnnonceCard from '@/components/AnnonceCard.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'

const annonces = ref([])
const loading = ref(false)
const error = ref('')
const typesBien = ref([])

// Pagination backend (PagedResponse)
const currentPage = ref(0)
const totalPages = ref(1)
const pageNumbers = computed(() => Array.from({ length: totalPages.value }, (_, i) => i + 1))

// Filtres — typeBienId envoie l'ID, pas le libellé
const filtres = reactive({
  typeBienId: '',
  adresse: '',
})

async function fetchAnnonces(page = 0) {
  loading.value = true
  error.value = ''
  try {
    const response = await annonceService.getAllAnnonces({ page, size: 9 })
    // Structure PagedResponse: { data, totalElements, totalPages, currentPage, ... }
    const paged = response.data
    annonces.value = paged.data
    totalPages.value = paged.totalPages
    currentPage.value = paged.currentPage
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
  // Charger les types de bien depuis l'API pour le dropdown
  try {
    const res = await typeBienService.getAllTypesBien()
    typesBien.value = res.data.data
  } catch {
    typesBien.value = []
  }
  fetchAnnonces(0)
})
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
          <label class="liste-filtres__label">TYPE DE BIEN</label>
          <select v-model="filtres.typeBienId" class="liste-filtres__select">
            <option value="">Tous les types</option>
            <option v-for="type in typesBien" :key="type.id" :value="type.id">
              {{ type.libelle }}
            </option>
          </select>
        </div>

        <div class="liste-filtres__group">
          <label class="liste-filtres__label">ADRESSE / QUARTIER</label>
          <input v-model="filtres.adresse" type="text" class="liste-filtres__select" placeholder="ex. Almadies" />
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

      <!-- Aucun résultat -->
      <div v-else class="liste-empty">
        <p>🏠 Aucune annonce trouvée pour ces critères.</p>
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
