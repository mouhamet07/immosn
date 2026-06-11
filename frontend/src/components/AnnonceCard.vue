<script setup>
import placeholderImg from '@/assets/Penthouse.png'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from './SvgIcon.vue'
import { useFavorisStore } from '@/stores/favorisStore'
import { useAuthStore }   from '@/stores/authStore'

const props = defineProps({
  id:           { type: [Number, String], required: true },
  title:        { type: String, required: true },
  prix:         { type: Number, required: true },
  images:       { type: Array,  default: () => [] },
  nbrChambres:  { type: Number, default: 0 },
  nbrSallesBain:{ type: Number, default: 0 },
  surface:      { type: Number, default: 0 },
  badge:        { type: String, default: '' },
  adresse:      { type: String, default: '' },
  isNew:        { type: Boolean, default: false },
})

const router       = useRouter()
const favorisStore = useFavorisStore()
const authStore    = useAuthStore()

const normalizedId = computed(() => Number(props.id))
const isFavori      = computed(() => favorisStore.isFavori(normalizedId.value))
const showFavBtn    = computed(() => authStore.isAuthenticated && authStore.role === 'CLIENT')

function formatPrix(prix) {
  return new Intl.NumberFormat('fr-SN').format(prix) + ' FCFA'
}

async function toggleFavoris(e) {
  e.stopPropagation()
  if (!showFavBtn.value) return
  await favorisStore.toggle(normalizedId.value)
}

function goToDetail() {
  router.push({ name: 'detail-annonce', params: { id: String(normalizedId.value) } })
}
</script>

<template>
  <article class="card" @click="goToDetail">
    <div class="card__image-wrapper">
      <img :src="images[0] || placeholderImg" :alt="title" class="card__image" />

      <!-- Badge overlay -->
      <span v-if="isNew" class="card__badge card__badge--new">Nouveau</span>
      <span v-else-if="badge" class="card__badge card__badge--exclusif">Exclusivité</span>

      <!-- Bouton favori -->
      <button
        class="card__favoris"
        :class="{ 'card__favoris--active': isFavori }"
        :title="isFavori ? 'Retirer des favoris' : 'Ajouter aux favoris'"
        @click="toggleFavoris"
      >
        <SvgIcon :name="isFavori ? 'heart-filled' : 'heart'" :size="16" />
      </button>
    </div>

    <div class="card__body">
      <h3 class="card__title">{{ title }}</h3>
      <p class="card__prix">{{ formatPrix(prix) }}</p>

      <div class="card__stats">
        <span class="card__stat">
          <SvgIcon name="bed" :size="14" />
          {{ nbrChambres }} Chambres
        </span>
        <span class="card__stat">
          <SvgIcon name="droplet" :size="14" />
          {{ nbrSallesBain }} Bains
        </span>
        <span class="card__stat">
          <SvgIcon name="maximize" :size="14" />
          {{ surface }}m²
        </span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.card {
  background: var(--color-card);
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid rgba(30, 37, 50, 0.08);
  box-shadow: 0 4px 20px rgba(45, 55, 72, 0.05);
  cursor: pointer;
  transition: box-shadow 0.25s;
}
.card:hover {
  box-shadow: 0 8px 32px rgba(45, 55, 72, 0.12);
}

/* Image aspect-ratio 3/2 comme Figma */
.card__image-wrapper {
  position: relative;
  aspect-ratio: 3 / 2;
  overflow: hidden;
}
.card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}
.card:hover .card__image {
  transform: scale(1.06);
}

/* Badges overlay */
.card__badge {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.card__badge--new {
  background: var(--color-primary);
  color: #fff;
}
.card__badge--exclusif {
  background: var(--color-accent);
  color: #fff;
}

/* Bouton favori */
.card__favoris {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  transition: background var(--transition), color var(--transition);
}
.card__favoris:hover {
  background: #fff;
  color: var(--color-accent);
}
.card__favoris--active { color: var(--color-danger); }
.card__favoris--active:hover { color: var(--color-danger); }

/* Corps */
.card__body {
  padding: 1rem;
}
.card__title {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
  margin-bottom: 0.35rem;
}
.card__prix {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--color-accent);
  margin-bottom: 0.75rem;
}

/* Stats */
.card__stats {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}
.card__stat {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
