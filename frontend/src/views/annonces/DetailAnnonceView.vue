<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import Badge from '@/components/Badge.vue'
import annonceService from '@/services/annonceService'
import placeholderImg from '@/assets/Penthouse.png'

const route = useRoute()
const router = useRouter()

const annonce = ref(null)
const loading = ref(false)
const error = ref('')
const favoris = ref(false)
const imageActive = ref(0)

onMounted(async () => {
  loading.value = true
  try {
    // Appel GET /api/v1/annonces/{id} — réponse: RestResponse<AnnonceResponseDto>
    const response = await annonceService.getAnnonceById(route.params.id)
    annonce.value = response.data.data
  } catch {
    error.value = 'Annonce introuvable ou une erreur est survenue.'
  } finally {
    loading.value = false
  }
})

function formatPrix(prix) {
  return new Intl.NumberFormat('fr-SN').format(prix) + ' FCFA'
}

function toUSD(prix) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(prix / 600)
}

// Retourner placeholder si pas d'image (dév)
function getImage(index) {
  return annonce.value?.images?.[index] || placeholderImg
}
</script>

<template>
  <div class="detail-page">
    <!-- Chargement -->
    <div v-if="loading" class="detail-loading">
      <div class="detail-loading__spinner"></div>
      <p>Chargement de l'annonce...</p>
    </div>

    <!-- Erreur -->
    <div v-else-if="error" class="detail-error">
      <p>{{ error }}</p>
      <ButtonPrimary @click="router.push({ name: 'annonces' })">← Retour aux annonces</ButtonPrimary>
    </div>

    <main v-else-if="annonce" class="detail-main">
      <!-- Fil d'Ariane -->
      <nav class="detail-breadcrumb">
        <RouterLink to="/annonces">Annonces</RouterLink>
        <span>›</span>
        <span>{{ annonce.libelle }}</span>
      </nav>

      <!-- Galerie d'images -->
      <div class="detail-gallery">
        <!-- Image principale -->
        <div class="detail-gallery__main">
          <img
            :src="getImage(imageActive)"
            :alt="annonce.libelle"
            class="detail-gallery__img"
          />
          <button class="detail-gallery__favoris" @click="favoris = !favoris">
            {{ favoris ? '❤️' : '🤍' }}
          </button>
          <Badge v-if="annonce.typeBien" :label="annonce.typeBien.libelle" class="detail-gallery__badge" />
        </div>

        <!-- Miniatures -->
        <div class="detail-gallery__thumbs">
          <div
            v-for="(photo, index) in (annonce.images || []).slice(1, 3)"
            :key="index"
            class="detail-gallery__thumb"
            @click="imageActive = index + 1"
          >
            <img :src="photo" :alt="`Photo ${index + 2}`" />
          </div>
          <div v-if="annonce.images?.length > 3" class="detail-gallery__more">
            +{{ annonce.images.length - 3 }} Plus
          </div>
        </div>
      </div>

      <!-- Corps principal -->
      <div class="detail-body">
        <!-- Infos principales -->
        <div class="detail-content">
          <!-- Titre + prix -->
          <div class="detail-header">
            <h1 class="detail-header__title">{{ annonce.libelle }}</h1>
            <div class="detail-header__prix">
              <span class="detail-header__fcfa">{{ formatPrix(annonce.prix) }}</span>
              <span class="detail-header__usd">≈ {{ toUSD(annonce.prix) }}</span>
            </div>
          </div>

          <!-- Adresse -->
          <p class="detail-adresse">📍 {{ annonce.adresse }}</p>

          <!-- Stats -->
          <div class="detail-stats">
            <div class="detail-stat">
              <span class="detail-stat__icon">🛏️</span>
              <span class="detail-stat__value">{{ annonce.nbrPieces }}</span>
              <span class="detail-stat__label">Chambres</span>
            </div>
            <div class="detail-stat">
              <span class="detail-stat__icon">📄</span>
              <span class="detail-stat__value">{{ annonce.typeBien?.libelle || '–' }}</span>
              <span class="detail-stat__label">Type de bien</span>
            </div>
            <div class="detail-stat">
              <span class="detail-stat__icon">📐</span>
              <span class="detail-stat__value">{{ annonce.surface }} m²</span>
              <span class="detail-stat__label">Surface</span>
            </div>
          </div>

          <!-- Présentation architecturale -->
          <section class="detail-section">
            <h2 class="detail-section__title">Présentation architecturale</h2>
            <p class="detail-section__text">{{ annonce.description }}</p>
          </section>

          <!-- Équipements -->
          <section class="detail-section">
            <h2 class="detail-section__title">Équipements et caractéristiques</h2>
            <ul v-if="annonce.commodites?.length" class="detail-commodites">
              <li v-for="(item, i) in annonce.commodites" :key="i" class="detail-commodite">
                ✅ {{ item.libelle }}
              </li>
            </ul>
            <p v-else class="detail-section__text">Aucun équipement renseigné.</p>
          </section>

          <!-- Emplacement -->
          <section class="detail-section">
            <h2 class="detail-section__title">Emplacement</h2>
            <div class="detail-map">
              <p class="detail-map__placeholder">🗺️ Carte non disponible</p>
              <a
                v-if="annonce.gps"
                :href="`https://www.google.com/maps?q=${annonce.gps}`"
                target="_blank"
                class="detail-map__link"
              >
                📍 Itinéraire vers {{ annonce.adresse }}
              </a>
            </div>
          </section>
        </div>

        <!-- Sidebar contact -->
        <aside class="detail-sidebar">
          <div class="detail-sidebar__card">
            <h3 class="detail-sidebar__title">Contacter l'agence</h3>

            <div class="detail-sidebar__contact">
              <p>📞 <a href="tel:+221338000000">+221 33 800 00 00</a></p>
              <p>✉️ <a href="mailto:contact@2simmo.sn">contact@2simmo.sn</a></p>
            </div>

            <div class="detail-sidebar__actions">
              <ButtonPrimary full-width>Contacter l'agent</ButtonPrimary>
              <ButtonPrimary variant="outline" full-width>Réserver une visite</ButtonPrimary>
            </div>

            <div class="detail-sidebar__links">
              <a href="#" class="detail-sidebar__link">🔗 Partager</a>
              <a href="#" class="detail-sidebar__link">📄 Brochure</a>
              <a href="#" class="detail-sidebar__link">🚩 Signaler</a>
            </div>
          </div>

          <!-- Aperçu du marché -->
          <div class="detail-sidebar__card">
            <h3 class="detail-sidebar__title">Aperçu du marché</h3>
            <p class="detail-sidebar__market-text">
              Les biens similaires dans ce quartier sont estimés entre
              <strong>{{ formatPrix(annonce.prix * 0.85) }}</strong> et
              <strong>{{ formatPrix(annonce.prix * 1.15) }}</strong>.
            </p>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
.detail-page {
  background: var(--color-background);
}

.detail-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

/* Chargement */
.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 6rem;
  color: var(--color-text);
  opacity: 0.6;
}

.detail-loading__spinner {
  width: 48px;
  height: 48px;
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
.detail-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  padding: 4rem;
  text-align: center;
  color: var(--color-accent);
}

/* Fil d'Ariane */
.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.6;
  margin-bottom: 1.5rem;
}

.detail-breadcrumb a {
  color: var(--color-primary);
}

.detail-breadcrumb a:hover {
  text-decoration: underline;
}

/* Galerie */
.detail-gallery {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0.75rem;
  margin-bottom: 2rem;
  border-radius: var(--radius);
  overflow: hidden;
  height: 420px;
}

.detail-gallery__main {
  position: relative;
  overflow: hidden;
}

.detail-gallery__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-gallery__favoris {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  transition: background 0.2s;
}

.detail-gallery__favoris:hover {
  background: #fff;
}

.detail-gallery__badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
}

.detail-gallery__thumbs {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 0.75rem;
}

.detail-gallery__thumb {
  overflow: hidden;
  cursor: pointer;
  border-radius: 4px;
}

.detail-gallery__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.detail-gallery__thumb:hover img {
  transform: scale(1.05);
}

.detail-gallery__more {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 55, 72, 0.7);
  color: #fff;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  border-radius: 4px;
}

/* Corps */
.detail-body {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 2rem;
  align-items: start;
}

/* En-tête */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.detail-header__title {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--color-text);
  flex: 1;
}

.detail-header__prix {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.detail-header__fcfa {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-accent);
}

.detail-header__usd {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.5;
}

/* Adresse */
.detail-adresse {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.65;
  margin-bottom: 1.5rem;
}

/* Stats */
.detail-stats {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
  padding: 1.25rem;
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  margin-bottom: 2rem;
}

.detail-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  flex: 1;
  min-width: 80px;
}

.detail-stat__icon {
  font-size: 1.3rem;
}

.detail-stat__value {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.detail-stat__label {
  font-size: 0.75rem;
  color: var(--color-text);
  opacity: 0.55;
  text-align: center;
}

/* Sections */
.detail-section {
  margin-bottom: 2rem;
}

.detail-section__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--color-primary);
  display: inline-block;
}

.detail-section__text {
  color: var(--color-text);
  opacity: 0.75;
  line-height: 1.7;
}

/* Commodités */
.detail-commodites {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.detail-commodite {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.8;
}

/* Carte */
.detail-map {
  background: var(--color-card);
  border-radius: var(--radius);
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  border: 1px dashed var(--color-border);
}

.detail-map__placeholder {
  color: var(--color-text);
  opacity: 0.4;
  font-size: 1rem;
}

.detail-map__link {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 0.9rem;
}

.detail-map__link:hover {
  text-decoration: underline;
}

/* Sidebar */
.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  position: sticky;
  top: 80px;
}

.detail-sidebar__card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.5rem;
  box-shadow: var(--shadow-card);
}

.detail-sidebar__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}

.detail-sidebar__contact {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  font-size: 0.9rem;
  color: var(--color-text);
}

.detail-sidebar__contact a {
  color: var(--color-primary);
  font-weight: 500;
}

.detail-sidebar__actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.detail-sidebar__links {
  display: flex;
  justify-content: space-between;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.detail-sidebar__link {
  font-size: 0.82rem;
  color: var(--color-text);
  opacity: 0.6;
  transition: opacity 0.2s;
}

.detail-sidebar__link:hover {
  opacity: 1;
  color: var(--color-primary);
}

.detail-sidebar__market-text {
  font-size: 0.88rem;
  color: var(--color-text);
  opacity: 0.75;
  line-height: 1.6;
}

/* Responsive */
@media (max-width: 900px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .detail-gallery {
    grid-template-columns: 1fr;
    height: 280px;
  }

  .detail-gallery__thumbs {
    display: none;
  }

  .detail-sidebar {
    position: static;
  }
}
</style>
