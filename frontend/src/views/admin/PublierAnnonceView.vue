<script setup>
import { Star, Images, ImagePlus, Plus, X } from 'lucide-vue-next'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import FormStepper from '@/components/admin/FormStepper.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import LocationMap from '@/components/LocationMap.vue'
import annonceService from '@/services/annonceService'
import commoditeService from '@/services/commoditeService'
import typeBienService from '@/services/typeBienService'
import locationService from '@/services/locationService'
import { uploadImages } from '@/services/cloudinaryService'

const router = useRouter()
const toast = ref(null)

const STEPS = ['Identité du bien', 'Localisation', 'Équipements', 'Photos']
const currentStep = ref(0)

const progress = computed(() => Math.round((currentStep.value / (STEPS.length - 1)) * 100))

const form = reactive({
  libelle:      '',
  description:  '',
  typeBienId:   null,
  nbrPieces:    '',
  surface:      '',
  prix:         '',
  adresse:      '',
  departement:  '',
  quartier:     '',
  latitude:     null,
  longitude:    null,
  commoditeIds: [],
  images:       [],
})

const commodites       = ref([])
const typesBien        = ref([])
const departements     = ref([])
const quartiers        = ref([])
const loadingQuartiers = ref(false)
const geocoding        = ref(false)
let geocodeTimer       = null

onMounted(async () => {
  try {
    const [resCommodites, resTypesBien, resDepts] = await Promise.all([
      commoditeService.getAllCommodites(),
      typeBienService.getAllTypesBien(),
      locationService.getDepartements(),
    ])
    commodites.value   = resCommodites.data.data
    typesBien.value    = resTypesBien.data.data
    departements.value = resDepts.data.data
  } catch {
    toast.value?.show('Erreur lors du chargement des données.', 'error')
  }
})

// Département → charger quartiers et réinitialiser coords
watch(() => form.departement, async (val) => {
  form.quartier  = ''
  form.latitude  = null
  form.longitude = null
  quartiers.value = []
  if (!val) return
  loadingQuartiers.value = true
  try {
    const res = await locationService.getQuartiersByDepartement(val)
    quartiers.value = res.data.data
  } catch {
    quartiers.value = []
  } finally {
    loadingQuartiers.value = false
  }
})

// Quartier → géocoder immédiatement
watch(() => form.quartier, (val) => {
  if (val) geocodeLocation()
  else { form.latitude = null; form.longitude = null }
})

// Adresse → géocoder avec délai (seulement si quartier défini)
watch(() => form.adresse, () => {
  if (!form.quartier) return
  clearTimeout(geocodeTimer)
  geocodeTimer = setTimeout(geocodeLocation, 500)
})

async function geocodeLocation() {
  if (!form.quartier || !form.departement) return
  geocoding.value = true
  try {
    const res = await locationService.geocode(form.departement, form.quartier, form.adresse || null)
    const coords = res.data.data
    form.latitude  = coords?.latitude  ?? null
    form.longitude = coords?.longitude ?? null
  } catch {
    // Echec silencieux — la création n'est pas bloquée
  } finally {
    geocoding.value = false
  }
}

function nextStep() {
  if (currentStep.value === 1 && (!form.departement || !form.quartier)) {
    toast.value?.show('Veuillez sélectionner un département et un quartier.', 'error')
    return
  }
  currentStep.value++
}

// FIX 5 : photo principale + sous-photos séparément
const mainPhoto        = ref(null)
const mainPhotoPreview = ref(null)
const subPhotos        = ref([])  // [{ file, preview }]
const uploadProgress   = ref({ done: 0, total: 0 })

const handleMainPhoto = (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { toast.value?.show('Photo trop lourde — max 5 Mo', 'error'); return }
  mainPhoto.value = file
  mainPhotoPreview.value = URL.createObjectURL(file)
}
const handleMainDrop = (event) => {
  const file = event.dataTransfer.files[0]
  if (file) handleMainPhoto({ target: { files: [file] } })
}
const removeMainPhoto = () => { mainPhoto.value = null; mainPhotoPreview.value = null }

const handleSubPhotos = (event) => {
  const remaining = 2 - subPhotos.value.length
  Array.from(event.target.files).slice(0, remaining).forEach(file => {
    if (file.size > 5 * 1024 * 1024) { toast.value?.show(`${file.name} trop lourd — ignoré`, 'error'); return }
    subPhotos.value.push({ file, preview: URL.createObjectURL(file) })
  })
}
const removeSubPhoto = (index) => subPhotos.value.splice(index, 1)

function toggleCommodite(id) {
  const idx = form.commoditeIds.indexOf(id)
  if (idx === -1) form.commoditeIds.push(id)
  else form.commoditeIds.splice(idx, 1)
}

const loading = ref(false)

async function submit() {
  if (!form.typeBienId) {
    toast.value.show('Veuillez sélectionner un type de bien.', 'error')
    return
  }
  if (!form.departement || !form.quartier) {
    toast.value.show('Le département et le quartier sont obligatoires.', 'error')
    return
  }
  loading.value = true
  try {
    // Upload photo principale en premier (index 0 = principal)
    let imageUrls = []
    if (mainPhoto.value) {
      const [mainUrl] = await uploadImages([mainPhoto.value])
      imageUrls.push(mainUrl)
    }
    if (subPhotos.value.length) {
      uploadProgress.value = { done: 0, total: subPhotos.value.length }
      const subUrls = await uploadImages(subPhotos.value.map(p => p.file), (done, total) => {
        uploadProgress.value = { done, total }
      })
      imageUrls.push(...subUrls)
    }
    await annonceService.createAnnonce({
      libelle:      form.libelle,
      description:  form.description,
      nbrPieces:    parseInt(form.nbrPieces),
      surface:      parseFloat(form.surface),
      prix:         parseFloat(form.prix),
      adresse:      form.adresse || null,
      departement:  form.departement,
      quartier:     form.quartier,
      typeBienId:   form.typeBienId,
      commoditeIds: form.commoditeIds,
      images:       imageUrls,
    })
    toast.value.show('Annonce publiée avec succès', 'success')
    setTimeout(() => router.push('/admin/annonces'), 1200)
  } catch (err) {
    toast.value.show(err.userMessage || err.response?.data?.message || 'Données invalides. Vérifiez les champs saisis.', 'error')
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
            <label class="field__label">TYPE DE BIEN <span class="req">*</span></label>
            <select v-model="form.typeBienId" class="field__input">
              <option :value="null">Sélectionner...</option>
              <option v-for="type in typesBien" :key="type.id" :value="type.id">{{ type.libelle }}</option>
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

        <div class="field-row">
          <div class="field">
            <label class="field__label">DÉPARTEMENT <span class="req">*</span></label>
            <select v-model="form.departement" class="field__input">
              <option value="">Sélectionner un département...</option>
              <option v-for="d in departements" :key="d" :value="d">{{ d }}</option>
            </select>
          </div>
          <div class="field">
            <label class="field__label">QUARTIER <span class="req">*</span></label>
            <select v-model="form.quartier" class="field__input" :disabled="!form.departement || loadingQuartiers">
              <option value="">{{ loadingQuartiers ? 'Chargement...' : 'Sélectionner un quartier...' }}</option>
              <option v-for="q in quartiers" :key="q" :value="q">{{ q }}</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label class="field__label">ADRESSE EXACTE <span class="field__optional">(facultatif)</span></label>
          <input v-model="form.adresse" type="text" class="field__input" placeholder="ex. 12 Rue Carnot" />
        </div>

        <div class="map-section">
          <div v-if="geocoding" class="map-loading">
            <div class="map-spinner"></div>
            <span>Recherche de la localisation...</span>
          </div>
          <LocationMap
            v-else
            :latitude="form.latitude"
            :longitude="form.longitude"
            :draggable="true"
            height="280px"
            @update:latitude="v => form.latitude = v"
            @update:longitude="v => form.longitude = v"
          />
        </div>
      </section>

      <!-- Section 3 — Équipements -->
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

      <!-- Section 4 — Photos -->
      <section v-else-if="currentStep === 3" class="form-section">
        <h2 class="form-section__title"><span class="form-section__num">4</span> Portfolio visuel</h2>

        <!-- Photo principale obligatoire -->
        <div class="photo-block">
          <h4 class="photo-block-title">
            <Star :size="16" color="var(--color-accent)" />
            Photo principale
            <span class="required-badge">Obligatoire</span>
          </h4>
          <p class="photo-hint">Cette photo sera affichée en premier sur l'annonce.</p>
          <div
            v-if="!mainPhoto"
            class="upload-zone main-zone"
            @click="$refs.mainInput.click()"
            @dragover.prevent
            @drop.prevent="handleMainDrop"
          >
            <ImagePlus :size="32" color="var(--color-primary)" />
            <p>Cliquez ou glissez votre photo principale</p>
            <span class="upload-hint">JPG, PNG — max 5 Mo</span>
            <input ref="mainInput" type="file" accept="image/jpeg,image/png,image/webp" @change="handleMainPhoto" hidden />
          </div>
          <div v-else class="main-preview">
            <img :src="mainPhotoPreview" alt="Photo principale" />
            <button class="remove-photo" @click="removeMainPhoto"><X :size="16" /></button>
            <span class="main-badge">Photo principale</span>
          </div>
        </div>

        <!-- Sous-photos optionnelles -->
        <div class="photo-block">
          <h4 class="photo-block-title">
            <Images :size="16" color="var(--color-primary)" />
            Photos supplémentaires
            <span class="optional-badge">Optionnel</span>
          </h4>
          <p class="photo-hint">Ajoutez jusqu'à 2 photos supplémentaires pour mieux présenter le bien.</p>
          <div class="sub-photos-grid">
            <div v-for="(photo, i) in subPhotos" :key="i" class="sub-photo-item">
              <img :src="photo.preview" alt="photo" />
              <button class="remove-photo" @click="removeSubPhoto(i)"><X :size="14" /></button>
            </div>
            <div v-if="subPhotos.length < 2" class="upload-zone sub-zone" @click="$refs.subInput.click()">
              <Plus :size="24" color="var(--color-primary)" />
              <span>Ajouter</span>
              <input ref="subInput" type="file" accept="image/jpeg,image/png,image/webp" multiple @change="handleSubPhotos" hidden />
            </div>
          </div>
          <p class="photos-count">{{ subPhotos.length }} / 2 photos supplémentaires</p>
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
          <button v-if="currentStep < STEPS.length - 1" type="button" class="btn-next" @click="nextStep">
            Continuer
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
.publier-page { display: flex; flex-direction: column; gap: 1.5rem; max-width: 860px; }

.publier-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.publier-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.publier-header__subtitle { font-size: 0.88rem; color: var(--color-text-muted); margin-top: 0.25rem; }

.publier-progress { display: flex; flex-direction: column; align-items: flex-end; gap: 0.4rem; min-width: 160px; }
.publier-progress__label { font-size: 0.82rem; font-weight: 700; color: var(--color-accent); }
.publier-progress__bar { width: 160px; height: 6px; background: var(--color-border); border-radius: 3px; overflow: hidden; }
.publier-progress__fill { height: 100%; background: var(--color-accent); border-radius: 3px; transition: width 0.3s ease; }

.publier-form-card { background: var(--color-card); border-radius: var(--radius); padding: 2rem; box-shadow: var(--shadow-card); }

.form-section { display: flex; flex-direction: column; gap: 1.25rem; margin-bottom: 1.5rem; }
.form-section__title { display: flex; align-items: center; gap: 0.6rem; font-size: 1.05rem; font-weight: 700; color: var(--color-text); padding-bottom: 0.75rem; border-bottom: 1px solid var(--color-border); }
.form-section__num { width: 26px; height: 26px; border-radius: 50%; background: var(--color-primary); color: #fff; font-size: 0.8rem; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field__label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-text); }
.req { color: var(--color-accent); }
.field__input { padding: 0.7rem 0.9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.9rem; color: var(--color-text); background: var(--color-card); transition: border-color 0.2s; width: 100%; }
.field__input:focus { border-color: var(--color-primary); outline: none; }
select.field__input { padding-right: 2.5rem; }
.field__textarea { resize: vertical; min-height: 100px; }
.field__optional { font-weight: 400; text-transform: none; font-size: 0.72rem; color: var(--color-text-muted); letter-spacing: 0; }
.field__hint { font-size: 0.78rem; color: var(--color-text-muted); margin-top: 0.15rem; }

.map-section { display: flex; flex-direction: column; gap: 0.5rem; }
.map-loading { height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.75rem; background: #e8f2ef; border-radius: var(--radius); color: var(--color-primary); font-size: 0.88rem; }
.map-spinner { width: 28px; height: 28px; border: 3px solid rgba(74,124,111,0.25); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.equipements-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.75rem; }
.equip-card { display: flex; align-items: center; justify-content: center; padding: 0.9rem 0.5rem; border-radius: var(--radius-sm); border: 1.5px solid var(--color-border); background: var(--color-card); cursor: pointer; transition: border-color 0.15s, background 0.15s; }
.equip-card:hover { background: #f0ebe3; }
.equip-card--selected { border-color: var(--color-primary); background: #e8f2ef; }
.equip-card__label { font-size: 0.82rem; font-weight: 500; color: var(--color-text); text-align: center; }
.equip-empty { font-size: 0.88rem; color: var(--color-text-muted); text-align: center; padding: 2rem; }

/* Upload zone générique */
.upload-zone { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.6rem; cursor: pointer; }

/* Photo principale */
.photo-block { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1.5rem; }
.photo-block-title { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; font-weight: 700; color: var(--color-text); }
.photo-hint { font-size: 0.82rem; color: var(--color-text-muted); }
.required-badge { font-size: 10px; background: #FDECEA; color: #C0392B; padding: 2px 6px; border-radius: 4px; margin-left: 4px; }
.optional-badge { font-size: 10px; background: var(--color-background); color: var(--color-text-muted); padding: 2px 6px; border-radius: 4px; margin-left: 4px; }

.main-zone { border: 2px dashed var(--color-primary); border-radius: 12px; padding: 32px; text-align: center; background: #E8F2EF; transition: all 150ms ease; }
.main-zone:hover { background: #D4E8E2; }
.upload-hint { font-size: 12px; color: var(--color-text-muted); }

.main-preview { position: relative; border-radius: 12px; overflow: hidden; height: 200px; }
.main-preview img { width: 100%; height: 100%; object-fit: cover; }
.main-badge { position: absolute; bottom: 8px; left: 8px; background: var(--color-accent); color: white; font-size: 11px; padding: 2px 8px; border-radius: 4px; }

/* Sous-photos */
.sub-photos-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 10px; }
.sub-photo-item { position: relative; height: 100px; border-radius: 8px; overflow: hidden; }
.sub-photo-item img { width: 100%; height: 100%; object-fit: cover; }
.sub-zone { height: 100px; border: 1.5px dashed var(--color-border); border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; font-size: 12px; color: var(--color-text-muted); transition: all 150ms ease; }
.sub-zone:hover { border-color: var(--color-primary); color: var(--color-primary); }
.remove-photo { position: absolute; top: 6px; right: 6px; background: rgba(45,55,72,0.7); color: white; border: none; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.photos-count { font-size: 12px; color: var(--color-text-muted); }

/* Navigation */
.form-nav { display: flex; align-items: center; justify-content: space-between; padding-top: 1.5rem; border-top: 1px solid var(--color-border); flex-wrap: wrap; gap: 0.75rem; }
.form-nav__draft { background: transparent; border: none; font-size: 0.85rem; color: var(--color-text-muted); text-decoration: underline; cursor: pointer; }
.form-nav__right { display: flex; gap: 0.75rem; }
.btn-prev { padding: 0.6rem 1.25rem; border-radius: var(--radius-sm); border: 1.5px solid var(--color-primary); background: transparent; color: var(--color-primary); font-size: 0.88rem; font-weight: 600; transition: background 0.15s; }
.btn-prev:hover { background: rgba(74,124,111,0.08); }
.btn-next { padding: 0.6rem 1.25rem; border-radius: var(--radius-sm); background: var(--color-accent); color: #fff; font-size: 0.88rem; font-weight: 600; border: none; transition: background 0.15s; }
.btn-next:hover:not(:disabled) { opacity: 0.9; }
.btn-next:disabled { opacity: 0.6; cursor: not-allowed; }

@media (max-width: 768px) {
  .equipements-grid { grid-template-columns: repeat(2, 1fr); }
  .field-row { grid-template-columns: 1fr; }
}
</style>
