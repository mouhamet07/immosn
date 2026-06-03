<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { X, Camera, MapPin } from 'lucide-vue-next'
import annonceService from '@/services/annonceService'
import typeBienService from '@/services/typeBienService'
import commoditeService from '@/services/commoditeService'
import { uploadImages } from '@/services/cloudinaryService'
import ToastNotification from '@/components/admin/ToastNotification.vue'

const route  = useRoute()
const router = useRouter()
const toast  = ref(null)

const loading     = ref(false)
const submitting  = ref(false)
const typesBien   = ref([])
const commodites  = ref([])

// Images existantes (URLs Cloudinary) — on peut en retirer
const existingImages = ref([])
// Nouveaux fichiers à uploader
const newPhotoFiles    = ref([])
const newPhotoPreviews = ref([])

const form = reactive({
  libelle:      '',
  description:  '',
  typeBienId:   null,
  nbrPieces:    '',
  surface:      '',
  prix:         '',
  adresse:      '',
  commoditeIds: [],
})

function toggleCommodite(id) {
  const idx = form.commoditeIds.indexOf(id)
  if (idx === -1) form.commoditeIds.push(id)
  else form.commoditeIds.splice(idx, 1)
}

function removeExistingImage(index) {
  existingImages.value.splice(index, 1)
}

function handleNewPhotos(e) {
  const files = Array.from(e.target.files)
  files.forEach(file => {
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

onMounted(async () => {
  loading.value = true
  try {
    const [resAnnonce, resTypes, resCommodites] = await Promise.all([
      annonceService.getAnnonceById(route.params.id),
      typeBienService.getAllTypesBien(),
      commoditeService.getAllCommodites(),
    ])
    const a = resAnnonce.data.data
    // Pré-remplir le formulaire avec les champs exacts de AnnonceResponseDto
    form.libelle      = a.libelle      || ''
    form.description  = a.description  || ''
    form.typeBienId   = a.typeBien?.id || null
    form.nbrPieces    = a.nbrPieces    || ''
    form.surface      = a.surface      || ''
    form.prix         = a.prix         || ''
    form.adresse      = a.adresse      || ''
    form.commoditeIds = a.commodites?.map(c => c.id) || []
    // Images existantes — List<String> URLs Cloudinary
    existingImages.value = a.images ? [...a.images] : []

    typesBien.value  = resTypes.data.data    || []
    commodites.value = resCommodites.data.data || []
  } catch {
    toast.value?.show('Erreur lors du chargement de l\'annonce.', 'error')
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!form.libelle.trim() || !form.typeBienId || !form.prix || !form.adresse.trim()) {
    toast.value?.show('Veuillez remplir tous les champs obligatoires.', 'error')
    return
  }
  submitting.value = true
  try {
    // 1. Uploader les nouvelles photos sur Cloudinary si présentes
    let newUrls = []
    const cloudName   = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME
    const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET
    if (newPhotoFiles.value.length && cloudName && uploadPreset) {
      newUrls = await uploadImages(newPhotoFiles.value)
    }

    // 2. Construire la liste finale d'images :
    //    images existantes conservées + nouvelles URLs uploadées
    const finalImages = [...existingImages.value, ...newUrls]

    // 3. Envoyer PUT /api/v1/annonces/{id} avec AnnonceUpdateRequestDto
    await annonceService.updateAnnonce(route.params.id, {
      libelle:      form.libelle,
      description:  form.description,
      nbrPieces:    parseInt(form.nbrPieces),
      surface:      parseFloat(form.surface),
      prix:         parseFloat(form.prix),
      adresse:      form.adresse,
      typeBienId:   form.typeBienId,
      commoditeIds: form.commoditeIds,
      images:       finalImages,
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
            <input v-model="form.prix" type="number" class="field__input" placeholder="ex. 50000000" />
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
      </div>

      <!-- Section 2 — Localisation -->
      <div class="ma-card">
        <h2 class="ma-card__title"><MapPin :size="16" /> Localisation</h2>
        <div class="field">
          <label class="field__label">ADRESSE COMPLÈTE <span class="req">*</span></label>
          <input v-model="form.adresse" type="text" class="field__input" placeholder="ex. 12 Rue Carnot, Plateau, Dakar" />
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
  background: #fff; transition: border-color .2s; width: 100%;
}
.field__input:focus { border-color: var(--color-primary); outline: none; }
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

@media (max-width: 768px) {
  .field-row { grid-template-columns: 1fr; }
  .ma-commodites { grid-template-columns: repeat(2, 1fr); }
}
</style>
