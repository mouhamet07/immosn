<script setup>
import { ref, computed } from 'vue'

const PLACEHOLDER = 'data:image/svg+xml,%3Csvg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 width%3D%22400%22 height%3D%22300%22%3E%3Crect width%3D%22400%22 height%3D%22300%22 fill%3D%22%23e5e7eb%22%2F%3E%3C%2Fsvg%3E'

const props = defineProps({
  images: { type: Array,  default: () => [] },
  alt:    { type: String, default: 'Image' },
  height: { type: String, default: '480px' },
  /**
   * Ratio largeur/hauteur de l'image principale (ex. '16 / 9', '4 / 3').
   * Quand il est fourni, il PRIME sur `height` : l'image reste proportionnelle
   * à toutes les largeurs au lieu d'être écrasée par une hauteur fixe.
   * `maxHeight` borne la hauteur sur très grand écran.
   */
  aspect:    { type: String, default: null },
  maxHeight: { type: String, default: '520px' },
})

const activeIndex = ref(0)
const imgs = computed(() => props.images?.length ? props.images : [PLACEHOLDER])

/**
 * Optimisation Cloudinary à l'affichage (aucune modif d'upload/stockage).
 * Insère f_auto (WebP/AVIF), q_auto (qualité optimale), c_limit + largeur
 * cible : sert une variante nette sans upscaler une source trop petite.
 * Toute URL non-Cloudinary (placeholder, etc.) est renvoyée telle quelle.
 */
function cldOptimize(url, width) {
  if (typeof url !== 'string') return url
  const marker = '/image/upload/'
  const i = url.indexOf(marker)
  if (i === -1) return url
  // Évite la double transformation si l'URL en contient déjà une.
  const after = url.slice(i + marker.length)
  if (/^(f_|q_|w_|c_|dpr_)/.test(after)) return url
  const t = `f_auto,q_auto,c_limit,w_${width},dpr_auto`
  return `${url.slice(0, i + marker.length)}${t}/${after}`
}

const mainImg  = computed(() => cldOptimize(imgs.value[activeIndex.value], 1280))
const thumbSrc = (src) => cldOptimize(src, 200)

function setActive(i) { activeIndex.value = i }
function prev() { activeIndex.value = (activeIndex.value - 1 + imgs.value.length) % imgs.value.length }
function next() { activeIndex.value = (activeIndex.value + 1) % imgs.value.length }

let touchStartX = 0
function onTouchStart(e) { touchStartX = e.touches[0].clientX }
function onTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - touchStartX
  if (Math.abs(dx) > 40) { dx < 0 ? next() : prev() }
}
</script>

<template>
  <div class="ig-root">
    <!-- Main image -->
    <div
      class="ig-main"
      :class="{ 'ig-main--ratio': aspect }"
      :style="{ '--ig-h': height, '--ig-ratio': aspect, '--ig-max-h': maxHeight }"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <img
        :src="mainImg"
        :alt="`${alt} — photo ${activeIndex + 1}`"
        class="ig-main__img"
        loading="lazy"
        decoding="async"
      />

      <!-- Nav arrows (> 1 image) -->
      <template v-if="imgs.length > 1">
        <button class="ig-arrow ig-arrow--prev" @click.stop="prev" aria-label="Image précédente">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <button class="ig-arrow ig-arrow--next" @click.stop="next" aria-label="Image suivante">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </template>

      <!-- Counter -->
      <span v-if="imgs.length > 1" class="ig-counter">{{ activeIndex + 1 }} / {{ imgs.length }}</span>

      <!-- Slot: overlay content (e.g. fav button) -->
      <slot name="overlay" />
    </div>

    <!-- Thumbnail strip (desktop/tablet) -->
    <div v-if="imgs.length > 1" class="ig-thumbs">
      <button
        v-for="(src, i) in imgs"
        :key="i"
        class="ig-thumb"
        :class="{ 'ig-thumb--active': i === activeIndex }"
        @click="setActive(i)"
        :aria-label="`Voir photo ${i + 1}`"
      >
        <img :src="thumbSrc(src)" :alt="`${alt} miniature ${i + 1}`" loading="lazy" decoding="async" />
      </button>
    </div>

    <!-- Dot indicators (mobile) -->
    <div v-if="imgs.length > 1" class="ig-dots" aria-hidden="true">
      <span
        v-for="(_, i) in imgs"
        :key="i"
        class="ig-dot"
        :class="{ 'ig-dot--active': i === activeIndex }"
      />
    </div>
  </div>
</template>

<style scoped>
.ig-root { width: 100%; }

.ig-main {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: var(--radius);
  background: #e5e7eb;
  /* Desktop : compact (500–600px) pour éviter l'upscale et le vide vertical. */
  height: min(var(--ig-h, 480px), 560px);
}

/*
 * Variante ratio : l'image principale garde une proportion constante
 * (ex. 16/9) au lieu d'une hauteur fixe qui l'écrase en largeur.
 * On borne par max-height pour ne pas devenir géante sur grand écran.
 */
.ig-main--ratio {
  height: auto;
  aspect-ratio: var(--ig-ratio, 16 / 9);
  max-height: var(--ig-max-h, 520px);
}

.ig-main__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Nav arrows */
.ig-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text);
  border: none;
  cursor: pointer;
  z-index: 2;
  transition: background 0.15s;
}
.ig-arrow:hover { background: #fff; }
.ig-arrow svg { width: 18px; height: 18px; }
.ig-arrow--prev { left: 0.75rem; }
.ig-arrow--next { right: 0.75rem; }

/* Counter */
.ig-counter {
  position: absolute;
  bottom: 0.75rem;
  right: 0.75rem;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.2rem 0.6rem;
  border-radius: 20px;
  backdrop-filter: blur(4px);
  pointer-events: none;
}

/* Thumbnail strip */
.ig-thumbs {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 2px;
}
.ig-thumbs::-webkit-scrollbar { display: none; }

.ig-thumb {
  flex-shrink: 0;
  width: 80px;
  height: 56px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s;
  padding: 0;
  background: #e5e7eb;
}
.ig-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.ig-thumb--active { border-color: var(--color-primary); }
.ig-thumb:hover:not(.ig-thumb--active) { border-color: var(--color-border); }

/* Dot indicators — mobile only */
.ig-dots {
  display: none;
  justify-content: center;
  gap: 6px;
  margin-top: 0.75rem;
}
.ig-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-border);
  transition: background 0.2s, transform 0.2s;
}
.ig-dot--active {
  background: var(--color-primary);
  transform: scale(1.3);
}

/*
 * Responsive height caps — uniquement pour la variante hauteur fixe.
 * La variante ratio (.ig-main--ratio) reste proportionnelle à toutes
 * les largeurs : pas de hauteur imposée, juste son max-height de base.
 */
/* Tablette (768–1199px) : 420–500px */
@media (max-width: 1199px) {
  .ig-main:not(.ig-main--ratio) { height: min(var(--ig-h, 480px), 480px); }
}
@media (max-width: 768px) {
  .ig-thumbs { display: none; }
  .ig-dots   { display: flex; }
  .ig-main:not(.ig-main--ratio) { height: min(var(--ig-h, 480px), 360px); }
}
/* Mobile (≤480px) : 250–320px */
@media (max-width: 480px) {
  .ig-main:not(.ig-main--ratio) { height: min(var(--ig-h, 480px), 300px); }
}
</style>
