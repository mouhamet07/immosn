<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import FormStepper from '@/components/admin/FormStepper.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import annonceService from '@/services/annonceService'
import commoditeService from '@/services/commoditeService'
import typeBienService from '@/services/typeBienService'
import { uploadImages } from '@/services/cloudinaryService'

const router = useRouter()
const toast = ref(null)

const STEPS = ['Identité du bien', 'Localisation', 'Équipements', 'Photos']
const currentStep = ref(0)

const progress = computed(() => Math.round((currentStep.value / (STEPS.length - 1)) * 100))

// Champs alignés avec AnnonceCreateRequestDto
const form = reactive({
  libelle:      '',
  description:  '',
  typeBienId:   null,
  nbrPieces:    '',
  surface:      '',
  prix:         '',
  adresse:      '',
  commoditeIds: [],
  images:       [],
})

// Types de bien et commodités chargés depuis le backend
const commodites = ref([])
const typesBien = ref([])

onMounted(async () => {
  try {
    const [resCommodites, resTypesBien] = await Promise.all([
      commoditeService.getAllCommodites(),
      typeBienService.getAllTypesBien(),
    ])
    // Réponse: RestResponse<List<CommoditeResponseDto>> → { id, libelle }
    commodites.value = resCommodites.data.data
    // Réponse: RestResponse<List<TypeBienResponseDto>> → { id, libelle }
    typesBien.value = resTypesBien.data.data
  } catch {
    toast.value?.show('Erreur lors du chargement des données.', 'error')
  }
})

// Fichiers File bruts conservés pour l'upload Cloudinary
const photoFiles    = ref([])
const photoPreviews = ref([])
const uploadProgress = ref({ done: 0, total: 0 })

function toggleCommodite(id) {
  const idx = form.commoditeIds.indexOf(id)
  if (idx === -1) form.commoditeIds.push(id)
  else form.commoditeIds.splice(idx, 1)
}

function handleFileUpload(e) {
  const files = Array.from(e.target.files)
  files.forEach(file => {
    photoFiles.value.push(file)
    const reader = new FileReader()
    reader.onload = ev => photoPreviews.value.push(ev.target.result)
    reader.readAsDataURL(file)
  })
}

function removePhoto(i) {
  photoFiles.value.splice(i, 1)
  photoPreviews.value.splice(i, 1)
}

const loading = ref(false)

async function submit() {
  if (!form.typeBienId) {
    toast.value.show('Veuillez sélectionner un type de bien.', 'error')
    return
  }
  loading.value = true
  try {
    let imageUrls = []
    if (photoFiles.value.length) {
      uploadProgress.value = { done: 0, total: photoFiles.value.length }
      imageUrls = await uploadImages(photoFiles.value, (done, total) => {
        uploadProgress.value = { done, total }
      })
    }
    await annonceService.createAnnonce({
      libelle:      form.libelle,
      description:  form.description,
      nbrPieces:    parseInt(form.nbrPieces),
      surface:      parseFloat(form.surface),
      prix:         parseFloat(form.prix),
      adresse:      form.adresse,
      typeBienId:   form.typeBienId,
      commoditeIds: form.commoditeIds,
      images:       imageUrls,
    })
    toast.value.show('Annonce publiée avec succès ✓', 'success')
    setTimeout(() => router.push('/admin/annonces'), 1200)
  } catch (err) {
    toast.value.show(err.response?.data?.message || err.message || 'Données invalides. Vérifiez les champs saisis.', 'error')
  } finally {
    loading.value = false
    uploadProgress.value = { done: 0, total: 0 }
  }
}
</script>

<template>
  <div class="publier-page">
    <ToastNotification ref="toast" />

    <!-- En-tête -->
    <div class="publier-header">
      <div>
        <h1 class="publier-header__title">Publier votre annonce</h1>
        <p class="publier-header__subtitle">Renseignez les informations de votre bien immobilier.</p>
      </div>
      <div class="publier-progress">
        <span class="publier-progress__label">{{ progress }}% Finalisé</span>
        <div class="publier-progress__bar">
          <div class="publier-progress__fill" :style="{ width: progress + '%' }"></div>
        </div>
      </div>
    </div>

    <!-- Stepper -->
    <FormStepper :steps="STEPS" :current-step="currentStep" />

    <!-- Formulaire -->
    <div class="publier-form-card">

      <!-- Section 1 — Identité du bien -->
      <section v-if="currentStep === 0" class="form-section">
        <h2 class="form-section__title"><span class="form-section__num">1</span> Identité du bien</h2>

        <div class="field">
          <label class="field__label">LIBELLÉ DE L'ANNONCE <span class="req">*</span></label>
          <input v-model="form.libelle" type="text" class="field__input" placeholder="ex. Villa moderne à Almadies" />
        </div>

        <div class="field">
          <label class="field__label">DESCRIPTION <span class="req">*</span></label>
          <textarea v-model="form.description" class="field__input field__textarea" rows="4" placeholder="Décrivez le bien en détail..."></textarea>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="field__label">TYPE DE BIEN (ID) <span class="req">*</span></label>
            <select v-model="form.typeBienId" class="field__input">
              <option :value="null">Sélectionner...</option>
              <option v-for="type in typesBien" :key="type.id" :value="type.id">
                {{ type.libelle }}
              </option>
            </select>
          </div>
          <div class="field">
            <label class="field__label">PRIX (FCFA) <span class="req">*</span></label>
            <input v-model="form.prix" type="number" class="field__input" placeholder="ex. 150000000" />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="field__label">NOMBRE DE PIÈCES <span class="req">*</span></label>
            <input v-model="form.nbrPieces" type="number" class="field__input" placeholder="ex. 4" />
          </div>
          <div class="field">
            <label class="field__label">SURFACE (m²) <span class="req">*</span></label>
            <input v-model="form.surface" type="number" class="field__input" placeholder="ex. 150" />
          </div>
        </div>
      </section>

      <!-- Section 2 — Localisation -->
      <section v-else-if="currentStep === 1" class="form-section">
        <h2 class="form-section__title"><span class="form-section__num">2</span> Contexte géographique</h2>

        <div class="field">
          <label class="field__label">ADRESSE COMPLÈTE <span class="req">*</span></label>
          <input v-model="form.adresse" type="text" class="field__input" placeholder="ex. 12 Rue Carnot, Plateau, Dakar" />
        </div>

        <div class="map-placeholder">
          <span class="map-placeholder__icon">📍</span>
          <span>Cliquer pour placer l'annonce sur la carte</span>
        </div>
      </section>

      <!-- Section 3 — Équipements (commoditeIds depuis API) -->
      <section v-else-if="currentStep === 2" class="form-section">
        <h2 class="form-section__title"><span class="form-section__num">3</span> Équipements de base</h2>

        <div v-if="commodites.length" class="equipements-grid">
          <button
            v-for="eq in commodites"
            :key="eq.id"
            type="button"
            class="equip-card"
            :class="{ 'equip-card--selected': form.commoditeIds.includes(eq.id) }"
            @click="toggleCommodite(eq.id)"
          >
            <span class="equip-card__label">{{ eq.libelle }}</span>
          </button>
        </div>
        <p v-else class="equip-empty">Chargement des équipements...</p>
      </section>

      <!-- Section 4 — Photos (images) -->
      <section v-else-if="currentStep === 3" class="form-section">
        <h2 class="form-section__title"><span class="form-section__num">4</span> Portfolio visuel</h2>

        <label class="upload-zone">
          <input type="file" multiple accept="image/*" class="upload-zone__input" @change="handleFileUpload" />
          <span class="upload-zone__icon">📷</span>
          <span class="upload-zone__text">Glissez-déposez vos photos ici</span>
          <span class="upload-zone__btn">Parcourir la galerie</span>
        </label>

        <div v-if="photoPreviews.length" class="photo-previews">
          <div v-for="(src, i) in photoPreviews" :key="i" class="photo-thumb">
            <img :src="src" alt="preview" />
            <button type="button" class="photo-thumb__remove" @click="removePhoto(i)">✕</button>
          </div>
        </div>
      </section>

      <!-- Navigation bas -->
      <div class="form-nav">
        <button type="button" class="form-nav__draft" @click="router.push('/admin/annonces')">
          Sauvegarder en brouillon
        </button>
        <div class="form-nav__right">
          <button v-if="currentStep > 0" type="button" class="btn-prev" @click="currentStep--">
            Étape précédente
          </button>
          <button v-if="currentStep < STEPS.length - 1" type="button" class="btn-next" @click="currentStep++">
            Continuer vers la fiche finale
          </button>
          <button v-else type="button" class="btn-next" :disabled="loading" @click="submit">
            <template v-if="loading && uploadProgress.total > 0">
              Upload {{ uploadProgress.done }}/{{ uploadProgress.total }}...
            </template>
            <template v-else-if="loading">Publication...</template>
            <template v-else>Publier l'annonce</template>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.publier-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 860px;
}

.publier-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.publier-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.publier-header__subtitle { font-size: 0.88rem; color: #6b7280; margin-top: 0.25rem; }

.publier-progress {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
  min-width: 160px;
}

.publier-progress__label { font-size: 0.82rem; font-weight: 700; color: var(--color-accent); }

.publier-progress__bar {
  width: 160px;
  height: 6px;
  background: #e8e0d4;
  border-radius: 3px;
  overflow: hidden;
}

.publier-progress__fill {
  height: 100%;
  background: var(--color-accent);
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* Carte formulaire */
.publier-form-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2rem;
  box-shadow: var(--shadow-card);
}

/* Section */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.form-section__title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text);
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #E8E0D4;
}

.form-section__num {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }

.field__label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text);
}

.req { color: var(--color-accent); }

.field__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid #e8e0d4;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color 0.2s;
  width: 100%;
}

.field__input:focus { border-color: var(--color-primary); outline: none; }
.field__textarea { resize: vertical; min-height: 100px; }

.map-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 160px;
  background: #e8f2ef;
  border: 2px dashed var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
}

.map-placeholder__icon { font-size: 2rem; }

.equipements-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.equip-card {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.9rem 0.5rem;
  border-radius: var(--radius-sm);
  border: 1.5px solid #e8e0d4;
  background: var(--color-card);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.equip-card:hover { background: #f0ebe3; }
.equip-card--selected { border-color: var(--color-primary); background: #e8f2ef; }
.equip-card__label { font-size: 0.82rem; font-weight: 500; color: var(--color-text); text-align: center; }

.equip-empty { font-size: 0.88rem; color: #6b7280; text-align: center; padding: 2rem; }

.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  height: 160px;
  border: 2px dashed #e8e0d4;
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-zone:hover { border-color: var(--color-primary); }
.upload-zone__input { display: none; }
.upload-zone__icon { font-size: 2rem; }
.upload-zone__text { font-size: 0.88rem; color: #6b7280; }

.upload-zone__btn {
  background: var(--color-primary);
  color: #fff;
  padding: 0.45rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 600;
}

.photo-previews { display: flex; flex-wrap: wrap; gap: 0.75rem; margin-top: 0.75rem; }

.photo-thumb {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid #e8e0d4;
}

.photo-thumb img { width: 100%; height: 100%; object-fit: cover; }

.photo-thumb__remove {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(45, 55, 72, 0.7);
  color: #fff;
  font-size: 0.65rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.form-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 1.5rem;
  border-top: 1px solid #e8e0d4;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.form-nav__draft {
  background: transparent;
  border: none;
  font-size: 0.85rem;
  color: #6b7280;
  text-decoration: underline;
  cursor: pointer;
}

.form-nav__right { display: flex; gap: 0.75rem; }

.btn-prev {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-size: 0.88rem;
  font-weight: 600;
  transition: background 0.15s;
}
.btn-prev:hover { background: rgba(74, 124, 111, 0.08); }

.btn-next {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  transition: background 0.15s;
}
.btn-next:hover:not(:disabled) { background: var(--color-accent-hover); }
.btn-next:disabled { opacity: 0.6; cursor: not-allowed; }

@media (max-width: 768px) {
  .equipements-grid { grid-template-columns: repeat(2, 1fr); }
  .field-row { grid-template-columns: 1fr; }
}
</style>
