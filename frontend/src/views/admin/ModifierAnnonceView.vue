<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { X, Camera } from 'lucide-vue-next'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'
import commoditeService from '@/services/commoditeService'
import locationService from '@/services/locationService'
import { uploadImages } from '@/services/cloudinaryService'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import LocationMap from '@/components/LocationMap.vue'

const route  = useRoute()
const router = useRouter()
const toast  = ref(null)

const loading    = ref(false)
const submitting = ref(false)
const typesBien  = ref([])
const commodites = ref([])

const departements     = ref([])
const quartiers        = ref([])
const loadingQuartiers = ref(false)
const geocoding        = ref(false)
let geocodeTimer       = null

const existingImages   = ref([])
const newPhotoFiles    = ref([])
const newPhotoPreviews = ref([])

const form = reactive({
  libelle:        '',
  description:    '',
  typeBienId:     null,
  nbrPieces:      '',
  nbrSallesBain:  '',
  surface:        '',
  prix:           '',
  adresse:        '',
  departement:    '',
  quartier:       '',
  latitude:       null,
  longitude:      null,
  commoditeIds:   [],
  isExclusivite:  false,
})

function toggleCommodite(id) {
  const idx = form.commoditeIds.indexOf(id)
  if (idx === -1) form.commoditeIds.push(id)
  else form.commoditeIds.splice(idx, 1)
}

function removeExistingImage(index) { existingImages.value.splice(index, 1) }

function handleNewPhotos(e) {
  Array.from(e.target.files).forEach(file => {
    newPhotoFiles.value.push(file)
    const reader = new FileReader()
    reader.onload = ev => newPhotoPreviews.value.push(ev.target.result)
    reader.readAsDataURL(file)
  })
}

function removeNewPhoto(index) {
  newPhotoFiles.value.splice(index, 1)
  newPhotoPreviews.value.splice(index, 1)
}

// Département → recharger quartiers (seulement si l'utilisateur change le département)
watch(() => form.departement, async (val, old) => {
  if (!old) return // skip initial prefill
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
watch(() => form.quartier, (val, old) => {
  if (!old) return // skip initial prefill
  if (val) geocodeLocation()
  else { form.latitude = null; form.longitude = null }
})

// Adresse → géocoder avec délai
watch(() => form.adresse, (val, old) => {
  if (!old || !form.quartier) return
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
    // Echec silencieux
  } finally {
    geocoding.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [resAnnonce, resTypes, resCommodites, resDepts] = await Promise.all([
      annonceService.getAnnonceByIdAdmin(route.params.id),
      typeBienService.getAllTypesBien(),
      commoditeService.getAllCommodites(),
      locationService.getDepartements(),
    ])
    const a = resAnnonce.data.data

    form.libelle      = a.libelle      || ''
    form.description  = a.description  || ''
    form.typeBienId   = a.typeBien?.id || null
    form.nbrPieces    = a.nbrPieces    || ''
    form.nbrSallesBain = a.nbrSallesBain || ''
    form.surface      = a.surface      || ''
    form.prix         = a.prix         || ''
    form.adresse      = a.adresse      || ''
    form.departement  = a.departement  || ''
    form.quartier     = a.quartier     || ''
    form.latitude     = a.latitude     ?? null
    form.longitude    = a.longitude    ?? null
    form.commoditeIds   = a.commodites?.map(c => c.id) || []
    form.isExclusivite  = !!a.isExclusivite
    existingImages.value = a.images ? [...a.images] : []

    typesBien.value    = resTypes.data.data       || []
    commodites.value   = resCommodites.data.data  || []
    departements.value = resDepts.data.data       || []

    // Charger les quartiers du département pré-rempli
    if (form.departement) {
      const resQ = await locationService.getQuartiersByDepartement(form.departement)
      quartiers.value = resQ.data.data || []
    }
  } catch {
    toast.value?.show('Erreur lors du chargement de l\'annonce.', 'error')
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!form.libelle.trim() || !form.typeBienId || !form.prix) {
    toast.value?.show('Veuillez remplir tous les champs obligatoires.', 'error')
    return
  }
  if (!form.departement || !form.quartier) {
    toast.value?.show('Le département et le quartier sont obligatoires.', 'error')
    return
  }
  submitting.value = true
  try {
    let newUrls = []
    const cloudName    = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME
    const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET
    if (newPhotoFiles.value.length && cloudName && uploadPreset) {
      newUrls = await uploadImages(newPhotoFiles.value)
    }
    const finalImages = [...existingImages.value, ...newUrls]

    await annonceService.updateAnnonce(route.params.id, {
      libelle:       form.libelle,
      description:   form.description,
      nbrPieces:     parseInt(form.nbrPieces),
      nbrSallesBain: form.nbrSallesBain ? parseInt(form.nbrSallesBain) : null,
      surface:       parseFloat(form.surface),
      prix:          parseFloat(form.prix),
      adresse:       form.adresse || null,
      departement:   form.departement,
      quartier:      form.quartier,
      typeBienId:    form.typeBienId,
      commoditeIds:  form.commoditeIds,
      images:        finalImages,
      isExclusivite: form.isExclusivite,
    })

    toast.value?.show('Annonce modifiée avec succès ✓', 'success')
    setTimeout(() => router.push(`/admin/annonces/${route.params.id}`), 1200)
  } catch (err) {
    toast.value?.show(err.userMessage || err.response?.data?.message || 'Erreur lors de la modification.', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="ma-page">
    <ToastNotification ref="toast" />

    <!-- Header -->
    <div class="ma-header">
      <button class="ma-back" @click="router.push(`/admin/annonces/${route.params.id}`)">← Retour au détail</button>
      <h1 class="ma-header__title">Modifier l'annonce</h1>
    </div>

    <div v-if="loading" class="ma-loading"><div class="spinner"></div></div>

    <form v-else class="ma-form" @submit.prevent="handleSubmit">

      <!-- Section 1 — Identité -->
      <div class="ma-card">
        <h2 class="ma-card__title">Identité du bien</h2>

        <div class="field">
          <label class="field__label">LIBELLÉ <span class="req">*</span></label>
          <input v-model="form.libelle" type="text" class="field__input" placeholder="ex. Villa moderne à Almadies" />
        </div>

        <div class="field">
          <label class="field__label">DESCRIPTION</label>
          <textarea v-model="form.description" class="field__input field__textarea" rows="4" placeholder="Décrivez le bien..."></textarea>
        </div>

        <div class="field">
          <label class="field__label">EXCLUSIVITÉ</label>
          <div class="ma-commodites">
            <button
              type="button"
              class="ma-commodite"
              :class="{ 'ma-commodite--selected': form.isExclusivite }"
              @click="form.isExclusivite = !form.isExclusivite"
            >
              Exclusivité
            </button>
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="field__label">TYPE DE BIEN <span class="req">*</span></label>
            <select v-model="form.typeBienId" class="field__input">
              <option :value="null" disabled>Sélectionner...</option>
              <option v-for="t in typesBien" :key="t.id" :value="t.id">{{ t.libelle }}</option>
            </select>
          </div>
          <div class="field">
            <label class="field__label">PRIX (FCFA) <span class="req">*</span></label>
            <input v-model.number="form.prix" type="number" min="0" step="1" class="field__input" placeholder="ex. 50000000" @input="form.prix = Math.max(0, form.prix)" />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="field__label">NOMBRE DE PIÈCES <span class="req">*</span></label>
            <input v-model.number="form.nbrPieces" type="number" min="0" step="1" class="field__input" placeholder="ex. 4" @input="form.nbrPieces = Math.max(0, form.nbrPieces)" />
          </div>
          <div class="field">
            <label class="field__label">SALLES DE BAINS <span class="req">*</span></label>
            <input v-model.number="form.nbrSallesBain" type="number" min="1" step="1" class="field__input" placeholder="ex. 2" @input="form.nbrSallesBain = Math.max(1, form.nbrSallesBain)" />
          </div>
          <div class="field">
            <label class="field__label">SURFACE (m²) <span class="req">*</span></label>
            <input v-model.number="form.surface" type="number" min="0" step="0.01" class="field__input" placeholder="ex. 150" @input="form.surface = Math.max(0, form.surface)" />
          </div>
        </div>
      </div>

      <!-- Section 2 — Localisation -->
      <div class="ma-card">
        <h2 class="ma-card__title">Localisation</h2>

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
            <p v-if="form.departement && !loadingQuartiers && !quartiers.length" class="ma-field-hint">
              Aucun quartier trouvé pour ce département.
            </p>
          </div>
        </div>

        <div class="field">
          <label class="field__label">ADRESSE EXACTE <span class="ma-optional">(facultatif)</span></label>
          <input v-model="form.adresse" type="text" class="field__input" placeholder="ex. 12 Rue Carnot" />
        </div>

        <div class="ma-map-section">
          <div v-if="geocoding" class="ma-map-loading">
            <div class="ma-map-spinner"></div>
            <span>Recherche de la localisation...</span>
          </div>
          <LocationMap
            v-else
            :latitude="form.latitude"
            :longitude="form.longitude"
            :draggable="true"
            height="260px"
            @update:latitude="v => form.latitude = v"
            @update:longitude="v => form.longitude = v"
          />
          <p v-if="form.quartier && !geocoding && form.latitude == null" class="ma-map-hint">
            Localisation introuvable — l'annonce sera quand même enregistrée.
          </p>
        </div>
      </div>

      <!-- Section 3 — Équipements -->
      <div class="ma-card">
        <h2 class="ma-card__title">Équipements</h2>
        <div v-if="commodites.length" class="ma-commodites">
          <button
            v-for="c in commodites" :key="c.id"
            type="button"
            class="ma-commodite"
            :class="{ 'ma-commodite--selected': form.commoditeIds.includes(c.id) }"
            @click="toggleCommodite(c.id)"
          >
            {{ c.libelle }}
          </button>
        </div>
        <p v-else class="ma-empty-text">Aucune commodité disponible.</p>
      </div>

      <!-- Section 4 — Photos -->
      <div class="ma-card">
        <h2 class="ma-card__title"><Camera :size="16" /> Photos</h2>

        <!-- Images existantes -->
        <div v-if="existingImages.length" class="ma-photos-section">
          <p class="ma-photos-label">Photos actuelles</p>
          <div class="ma-photos-grid">
            <div v-for="(url, i) in existingImages" :key="i" class="ma-photo">
              <img :src="url" alt="Photo existante" />
              <button type="button" class="ma-photo__remove" @click="removeExistingImage(i)">
                <X :size="12" />
              </button>
            </div>
          </div>
        </div>

        <!-- Nouvelles photos -->
        <div class="ma-photos-section">
          <p class="ma-photos-label">Ajouter des photos</p>
          <label class="ma-upload">
            <Camera :size="24" />
            <span>Parcourir</span>
            <input type="file" multiple accept="image/*" hidden @change="handleNewPhotos" />
          </label>
          <div v-if="newPhotoPreviews.length" class="ma-photos-grid" style="margin-top:.75rem">
            <div v-for="(src, i) in newPhotoPreviews" :key="i" class="ma-photo">
              <img :src="src" alt="Nouvelle photo" />
              <button type="button" class="ma-photo__remove" @click="removeNewPhoto(i)">
                <X :size="12" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="ma-footer">
        <button type="button" class="ma-btn-cancel" @click="router.push(`/admin/annonces/${route.params.id}`)">
          Annuler
        </button>
        <button type="submit" class="ma-btn-submit" :disabled="submitting">
          {{ submitting ? 'Sauvegarde...' : 'Enregistrer les modifications' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.ma-page { max-width: 860px; }
.ma-loading { display: flex; justify-content: center; padding: 4rem; }

.ma-header { margin-bottom: 1.5rem; }
.ma-back { background: none; border: none; color: var(--color-primary); font-size: .85rem; font-weight: 600; cursor: pointer; margin-bottom: .5rem; display: block; }
.ma-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }

.ma-form { display: flex; flex-direction: column; gap: 1.25rem; }

.ma-card {
  background: var(--color-card); border-radius: var(--radius);
  padding: 1.75rem; box-shadow: var(--shadow-card);
  display: flex; flex-direction: column; gap: 1.1rem;
}
.ma-card__title {
  font-size: 1rem; font-weight: 700; color: var(--color-text);
  padding-bottom: .75rem; border-bottom: 1px solid var(--color-border);
  display: flex; align-items: center; gap: .4rem;
}

.field { display: flex; flex-direction: column; gap: .4rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field__label { font-size: .75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); }
.req { color: var(--color-accent); }
.field__input {
  padding: .7rem .9rem; border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm); font-size: .9rem; color: var(--color-text);
  background: var(--color-card); transition: border-color .2s; width: 100%;
}
.field__input:focus { border-color: var(--color-primary); outline: none; }
select.field__input { padding-right: 2.5rem; }
.field__textarea { resize: vertical; min-height: 100px; }

/* Commodités */
.ma-commodites { display: grid; grid-template-columns: repeat(4, 1fr); gap: .6rem; }
.ma-commodite {
  padding: .65rem .5rem; border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-border); background: var(--color-card);
  font-size: .82rem; font-weight: 500; color: var(--color-text);
  cursor: pointer; transition: all .15s; text-align: center;
}
.ma-commodite:hover { background: var(--color-background); }
.ma-commodite--selected { border-color: var(--color-primary); background: rgba(74,124,111,.08); color: var(--color-primary); font-weight: 700; }
.ma-empty-text { font-size: .88rem; color: var(--color-text-muted); }

/* Photos */
.ma-photos-section { display: flex; flex-direction: column; gap: .6rem; }
.ma-photos-label { font-size: .78rem; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: .05em; }
.ma-photos-grid { display: flex; flex-wrap: wrap; gap: .6rem; }
.ma-photo { position: relative; width: 90px; height: 90px; border-radius: var(--radius-sm); overflow: hidden; border: 1px solid var(--color-border); }
.ma-photo img { width: 100%; height: 100%; object-fit: cover; }
.ma-photo__remove {
  position: absolute; top: 3px; right: 3px; width: 20px; height: 20px;
  border-radius: 50%; background: rgba(45,55,72,.75); color: #fff;
  border: none; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.ma-upload {
  display: inline-flex; align-items: center; gap: .5rem;
  padding: .6rem 1.1rem; border: 2px dashed var(--color-border);
  border-radius: var(--radius-sm); cursor: pointer; font-size: .85rem;
  color: var(--color-text-muted); transition: border-color .2s;
}
.ma-upload:hover { border-color: var(--color-primary); color: var(--color-primary); }

/* Footer */
.ma-footer { display: flex; justify-content: flex-end; gap: .75rem; padding-top: .5rem; }
.ma-btn-cancel {
  padding: .6rem 1.25rem; border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm); background: none; color: var(--color-text);
  font-size: .88rem; font-weight: 600; cursor: pointer;
}
.ma-btn-submit {
  padding: .6rem 1.5rem; background: var(--color-primary); color: #fff;
  border: none; border-radius: var(--radius-sm); font-size: .88rem;
  font-weight: 600; cursor: pointer; transition: opacity .15s;
}
.ma-btn-submit:hover:not(:disabled) { opacity: .85; }
.ma-btn-submit:disabled { opacity: .6; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.ma-optional { font-weight: 400; text-transform: none; font-size: .72rem; color: var(--color-text-muted); letter-spacing: 0; }
.ma-field-hint { font-size: .78rem; color: var(--color-text-muted); margin-top: .15rem; }
.ma-map-section { display: flex; flex-direction: column; gap: .5rem; }
.ma-map-hint { font-size: .78rem; color: var(--color-text-muted); text-align: center; }
.ma-map-loading {
  height: 260px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: .75rem;
  background: #e8f2ef; border-radius: var(--radius);
  border: 1px solid #cde3dd; color: var(--color-primary);
  font-size: .88rem; font-weight: 500;
}
.ma-map-spinner {
  width: 28px; height: 28px;
  border: 3px solid rgba(74,124,111,.25);
  border-top-color: var(--color-primary);
  border-radius: 50%; animation: spin .7s linear infinite;
}

@media (max-width: 768px) {
  .field-row { grid-template-columns: 1fr; }
  .ma-commodites { grid-template-columns: repeat(2, 1fr); }
}
</style>
