<script setup>
import placeholderImg from '@/assets/Penthouse.png'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Badge from './Badge.vue'
import { useFavorisStore } from '@/stores/favorisStore'
import { useAuthStore }   from '@/stores/authStore'

const props = defineProps({
  id:           { type: Number, required: true },
  title:        { type: String, required: true },
  prix:         { type: Number, required: true },
  images:       { type: Array,  default: () => [] },
  nbrChambres:  { type: Number, default: 0 },
  nbrSallesBain:{ type: Number, default: 0 },
  surface:      { type: Number, default: 0 },
  badge:        { type: String, default: '' },
  adresse:      { type: String, default: '' },
})

const router       = useRouter()
const favorisStore = useFavorisStore()
const authStore    = useAuthStore()

const isFavori = computed(() => favorisStore.isFavori(props.id))
const showFavBtn = computed(() => authStore.isAuthenticated && authStore.role === 'CLIENT')

function formatPrix(prix) {
  return new Intl.NumberFormat('fr-SN').format(prix) + ' FCFA'
}

async function toggleFavoris(e) {
  e.stopPropagation()
  if (!showFavBtn.value) return
  await favorisStore.toggle(props.id)
}

function goToDetail() {
  router.push({ name: 'detail-annonce', params: { id: props.id } })
}
</script>

<template>
  <article class="card" @click="goToDetail">
    <!-- Image avec badge et favoris -->
    <div class="card__image-wrapper">
      <img
        :src="images[0] || placeholderImg"
        :alt="title"
        class="card__image"
      />
      <Badge v-if="badge" :label="badge" class="card__badge" />
      <button
        v-if="showFavBtn"
        class="card__favoris"
        :class="{ 'card__favoris--active': isFavori }"
        :title="isFavori ? 'Retirer des favoris' : 'Ajouter aux favoris'"
        @click="toggleFavoris"
      >
        {{ isFavori ? '❤️' : '🤍' }}
      </button>
    </div>

    <!-- Contenu -->
    <div class="card__body">
      <h3 class="card__title">{{ title }}</h3>
      <p v-if="adresse" class="card__adresse">📍 {{ adresse }}</p>
      <p class="card__prix">{{ formatPrix(prix) }}</p>

      <!-- Statistiques -->
      <div class="card__stats">
        <span class="card__stat">🛏️ {{ nbrChambres }} ch.</span>
        <span class="card__stat">🚿 {{ nbrSallesBain }} sdb.</span>
        <span class="card__stat">📐 {{ surface }} m²</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.card {
  background: var(--color-card);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

.card__image-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: var(--color-background);
}

.card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s;
}

.card:hover .card__image {
  transform: scale(1.04);
}

.card__badge {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
}

.card__favoris {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  transition: background 0.2s;
}

.card__favoris:hover {
  background: #fff;
}

.card__body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.card__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.3;
}

.card__adresse {
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.6;
}

.card__prix {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--color-accent);
}

.card__stats {
  display: flex;
  gap: 1rem;
  margin-top: 0.25rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.card__stat {
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.7;
}
</style>
