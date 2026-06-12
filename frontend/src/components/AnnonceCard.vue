<template>
  <div class="annonce-card" @click="goToDetail">

    <!-- Image -->
    <div class="card-image-wrapper">
      <img
        :src="photos?.[0]?.url || null"
        :alt="libelle"
        class="card-image"
      />
      <div v-if="badge" class="card-badge"
        :style="{
          background: badge.bg,
          color: badge.color
        }">
        {{ badge.label }}
      </div>
      <button
        class="card-favori"
        @click.stop="$emit('toggle-favori', id)"
      >
        <Heart :size="16"
          :fill="isFavori ? 'var(--color-accent)' : 'none'"
          :color="isFavori
            ? 'var(--color-accent)'
            : 'white'" />
      </button>
    </div>

    <!-- Body -->
    <div class="card-body">
      <p class="card-title">{{ libelle }}</p>
      <p class="card-price">
        {{ formatPrice(prix) }} FCFA
      </p>
      <div class="card-location">
        <MapPin :size="12" />
        <span>{{ adresse }}</span>
      </div>
      <div class="card-meta">
        <span class="meta-item">
          <Bed :size="13" /> {{ nbreDePieces }} pièces
        </span>
        <span class="meta-item">
          <Maximize2 :size="13" /> {{ surface }} m²
        </span>
        <span class="meta-item">
          <Home :size="13" /> {{ typeBien?.libelle }}
        </span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Heart, MapPin, Bed, Maximize2, Home
} from 'lucide-vue-next'

const props = defineProps({
  id: [Number, String],
  libelle: String,
  prix: Number,
  adresse: String,
  surface: Number,
  nbreDePieces: Number,
  typeBien: Object,
  photos: Array,
  createdAt: String,
  isExclusif: Boolean,
  isFavori: { type: Boolean, default: false },
})
defineEmits(['toggle-favori'])

const router = useRouter()

const goToDetail = () => {
  router.push('/annonces/' + props.id)
}

const formatPrice = (val) => {
  if (!val) return '—'
  return new Intl.NumberFormat('fr-SN')
    .format(val)
}

const badge = computed(() => {
  if (props.isExclusif) return {
    label: 'Exclusivité',
    bg: 'var(--color-accent)',
    color: 'white'
  }
  const days = Math.floor(
    (Date.now() - new Date(props.createdAt))
    / (1000 * 60 * 60 * 24)
  )
  if (days < 7) return {
    label: 'Nouveau',
    bg: 'var(--color-primary)',
    color: 'white'
  }
  return null
})
</script>

<style scoped>
.annonce-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 200ms ease;
}

.annonce-card:hover {
  box-shadow: 0 4px 20px rgba(45,55,72,0.12);
  transform: translateY(-2px);
}

.card-image-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: var(--color-border);
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 300ms ease;
}

.annonce-card:hover .card-image {
  transform: scale(1.03);
}

.card-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
  letter-spacing: 0.03em;
  z-index: 2;
}

.card-favori {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255,255,255,0.9);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  transition: transform 150ms ease;
}

.card-favori:hover { transform: scale(1.15); }

.card-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
}

.card-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-accent);
  margin: 0;
}

.card-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.card-location span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border);
  margin-top: 2px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
