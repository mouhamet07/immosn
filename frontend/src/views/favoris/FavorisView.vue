<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useFavorisStore } from '@/stores/favorisStore'
import AnnonceCard from '@/components/AnnonceCard.vue'

const router       = useRouter()
const favorisStore = useFavorisStore()

const favoris     = ref([])
const loading     = ref(false)
const error       = ref('')
const currentPage = ref(0)
const totalPages  = ref(1)
const totalItems  = ref(0)
const PAGE_SIZE   = 12

async function fetchFavoris(page = 0) {
  loading.value = true
  error.value   = ''
  try {
    // Utilise le store pour le toggle UI, mais l'API pour récupérer les détails paginés
    const { default: favorisService } = await import('@/services/favorisService')
    const res = await favorisService.getClientFavoris(page, PAGE_SIZE)
    favoris.value     = res.data.data ?? []
    currentPage.value = res.data.currentPage
    totalPages.value  = res.data.totalPages
    totalItems.value  = res.data.totalElements
  } catch {
    error.value = 'Impossible de charger vos favoris.'
  } finally {
    loading.value = false
  }
}

async function removeFavori(annonceId) {
  await favorisStore.toggle(annonceId)
  await fetchFavoris(currentPage.value)
}

onMounted(() => fetchFavoris(0))
</script>

<template>
  <div class="fav-page">
    <div class="fav-container">

      <!-- Header -->
      <div class="fav-header">
        <div>
          <h1 class="fav-header__title">Mes favoris</h1>
          <p class="fav-header__sub">
            {{ totalItems }} bien{{ totalItems !== 1 ? 's' : '' }} sauvegardé{{ totalItems !== 1 ? 's' : '' }}
          </p>
        </div>
        <RouterLink to="/annonces" class="fav-header__btn">
          Explorer les annonces
        </RouterLink>
      </div>

      <!-- Chargement -->
      <div v-if="loading" class="fav-loading"><div class="spinner"></div></div>

      <!-- Erreur -->
      <div v-else-if="error" class="fav-error">{{ error }}</div>

      <!-- Vide -->
      <div v-else-if="!favoris.length" class="fav-empty">
        <p class="fav-empty__icon">❤️</p>
        <p class="fav-empty__title">Aucun favori pour le moment</p>
        <p class="fav-empty__sub">Cliquez sur ❤️ sur une annonce pour l'ajouter à vos favoris.</p>
        <RouterLink to="/annonces" class="fav-empty__link">Parcourir les annonces →</RouterLink>
      </div>

      <!-- Grille -->
      <div v-else>
        <div class="fav-grid">
          <div v-for="f in favoris" :key="f.annonceId" class="fav-item">
            <AnnonceCard
              :id="f.annonceId"
              :title="f.libelle"
              :prix="f.prix"
              :images="f.imagePrincipale ? [f.imagePrincipale] : []"
              :nbrChambres="f.nbrPieces"
              :nbrSallesBain="0"
              :surface="f.surface"
              :adresse="f.adresse"
              :badge="f.typeBien || ''"
            />
            <button class="fav-item__remove" @click="removeFavori(f.annonceId)" title="Retirer des favoris">
              Retirer
            </button>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="fav-pager">
          <button :disabled="currentPage === 0" @click="fetchFavoris(currentPage - 1)">← Précédent</button>
          <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button :disabled="currentPage === totalPages - 1" @click="fetchFavoris(currentPage + 1)">Suivant →</button>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.fav-page      { background: var(--color-background); min-height: 100vh; }
.fav-container { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }

.fav-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;
}
.fav-header__title { font-size: 1.8rem; font-weight: 800; color: var(--color-text); }
.fav-header__sub   { font-size: .88rem; color: var(--color-text); opacity: .55; margin-top: .25rem; }
.fav-header__btn {
  padding: .6rem 1.25rem; background: var(--color-primary); color: #fff;
  border-radius: var(--radius-sm); font-weight: 600; font-size: .9rem;
  text-decoration: none; transition: opacity .2s;
}
.fav-header__btn:hover { opacity: .85; }

.fav-loading { display: flex; justify-content: center; padding: 5rem; }
.fav-error   { text-align: center; padding: 2rem; color: var(--color-accent); }

.fav-empty {
  display: flex; flex-direction: column; align-items: center; gap: .75rem;
  padding: 5rem 2rem; text-align: center;
}
.fav-empty__icon  { font-size: 3.5rem; opacity: .25; }
.fav-empty__title { font-size: 1.2rem; font-weight: 700; color: var(--color-text); }
.fav-empty__sub   { font-size: .9rem; color: var(--color-text); opacity: .55; max-width: 340px; }
.fav-empty__link  { color: var(--color-primary); font-weight: 600; font-size: .9rem; }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

/* Wrapper carte + bouton retirer */
.fav-item { position: relative; }
.fav-item__remove {
  position: absolute; bottom: .75rem; right: .75rem;
  padding: .25rem .65rem; background: rgba(239,68,68,.9); color: #fff;
  border: none; border-radius: 6px; font-size: .72rem; font-weight: 700;
  cursor: pointer; opacity: 0; transition: opacity .2s;
}
.fav-item:hover .fav-item__remove { opacity: 1; }

.fav-pager {
  display: flex; justify-content: center; align-items: center; gap: 1rem;
  font-size: .88rem; color: var(--color-text); opacity: .7;
}
.fav-pager button {
  padding: .4rem .9rem; border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm); background: var(--color-card); cursor: pointer;
}
.fav-pager button:disabled { opacity: .4; cursor: not-allowed; }

.spinner {
  width: 36px; height: 36px; border: 3px solid var(--color-border);
  border-top-color: var(--color-primary); border-radius: 50%;
  animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1024px) { .fav-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px)  { .fav-grid { grid-template-columns: 1fr; } }
</style>
