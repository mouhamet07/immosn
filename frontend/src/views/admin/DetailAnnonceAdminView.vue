<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Edit, Archive, RotateCcw, MapPin, Bed, Droplet, Maximize, CheckCircle, Phone, Mail } from 'lucide-vue-next'
import annonceService from '@/services/annonceService'
import StatusBadge from '@/components/StatusBadge.vue'
import ImageGallery from '@/components/ImageGallery.vue'
import LocationMap from '@/components/LocationMap.vue'
import ProprietaireStatsModal from '@/components/admin/ProprietaireStatsModal.vue'

const route  = useRoute()
const router = useRouter()

const annonce  = ref(null)
const loading  = ref(false)
const error    = ref('')

// Confirmation archivage
const showConfirm = ref(false)
const confirming  = ref(false)

// Modal statistiques propriétaire
const showOwnerStats = ref(false)

async function fetchAnnonce() {
  loading.value = true
  try {
    const res = await annonceService.getAnnonceByIdAdmin(route.params.id)
    annonce.value = res.data.data
  } catch {
    error.value = 'Annonce introuvable.'
  } finally {
    loading.value = false
  }
}

async function handleArchive() {
  confirming.value = true
  try {
    await annonceService.archiveAnnonce(annonce.value.id)
    router.push('/admin/annonces')
  } catch (err) {
    error.value = err.response?.data?.message || "Erreur lors de l'archivage."
  } finally {
    confirming.value = false
    showConfirm.value = false
  }
}

async function handleRestore() {
  try {
    await annonceService.restoreAnnonce(annonce.value.id)
    await fetchAnnonce()
  } catch (err) {
    error.value = err.response?.data?.message || 'Erreur lors de la restauration.'
  }
}

function formatPrix(prix) {
  return new Intl.NumberFormat('fr-SN').format(prix) + ' FCFA'
}

onMounted(fetchAnnonce)
</script>

<template>
  <div class="daa-page">

    <div v-if="loading" class="daa-loading"><div class="spinner"></div></div>
    <div v-else-if="error" class="daa-error">{{ error }}</div>

    <template v-else-if="annonce">

      <!-- Header admin -->
      <div class="daa-header">
        <div>
          <button class="daa-back" @click="router.push('/admin/annonces')">← Retour aux annonces</button>
          <h1 class="daa-header__title">{{ annonce.libelle }}</h1>
          <StatusBadge :label="annonce.archived ? 'Archivée' : 'Active'" :variant="annonce.archived ? 'neutral' : 'success'" />
        </div>
        <!-- Actions admin -->
        <div class="daa-actions">
          <button class="daa-btn daa-btn--edit" @click="router.push(`/admin/annonces/${annonce.id}/modifier`)">
            <Edit :size="15" /> Modifier
          </button>
          <button v-if="!annonce.archived" class="daa-btn daa-btn--archive" @click="showConfirm = true">
            <Archive :size="15" /> Archiver
          </button>
          <button v-else class="daa-btn daa-btn--restore" @click="handleRestore">
            <RotateCcw :size="15" /> Restaurer
          </button>
        </div>
      </div>

      <!-- Galerie -->
      <div class="daa-gallery">
        <ImageGallery :images="annonce.images" :alt="annonce.libelle" height="460px" />
      </div>

      <!-- Corps -->
      <div class="daa-body">
        <div class="daa-content">

          <!-- Prix + adresse -->
          <div class="daa-meta">
            <p class="daa-prix">{{ formatPrix(annonce.prix) }}</p>
            <p class="daa-adresse"><MapPin :size="14" /> {{ annonce.adresse }}</p>
          </div>

          <!-- Stats -->
          <div class="daa-stats">
            <div class="daa-stat"><Bed :size="20" /><span>{{ annonce.nbrPieces }} pièces</span></div>
            <div class="daa-stat"><Droplet :size="20" /><span>{{ annonce.nbrSallesBain >= 1 ? `${annonce.nbrSallesBain} salle(s) de bains` : 'SDB : non renseigné' }}</span></div>
            <div class="daa-stat"><Maximize :size="20" /><span>{{ annonce.surface }} m²</span></div>
            <div class="daa-stat"><CheckCircle :size="20" /><span>{{ annonce.typeBien?.libelle }}</span></div>
          </div>

          <!-- Description -->
          <section class="daa-section">
            <h2 class="daa-section__title">Description</h2>
            <p class="daa-section__text">{{ annonce.description || 'Aucune description.' }}</p>
          </section>

          <!-- Équipements -->
          <section class="daa-section">
            <h2 class="daa-section__title">Équipements</h2>
            <ul v-if="annonce.commodites?.length" class="daa-commodites">
              <li v-for="c in annonce.commodites" :key="c.id" class="daa-commodite">
                <CheckCircle :size="13" /> {{ c.libelle }}
              </li>
            </ul>
            <p v-else class="daa-section__text">Aucun équipement renseigné.</p>
          </section>

          <!-- Emplacement -->
          <section class="daa-section">
            <h2 class="daa-section__title">Emplacement</h2>
            <LocationMap
              :latitude="annonce.latitude"
              :longitude="annonce.longitude"
              :draggable="false"
              height="280px"
              :zoom="15"
            />
          </section>
        </div>

        <!-- Sidebar infos -->
        <aside class="daa-sidebar">
          <div class="daa-info-card">
            <h3 class="daa-info-card__title">Informations</h3>
            <div class="daa-info-row"><span>ID</span><span>{{ annonce.id }}</span></div>
            <div class="daa-info-row"><span>Type</span><span>{{ annonce.typeBien?.libelle || '–' }}</span></div>
            <div class="daa-info-row"><span>Statut</span>
              <StatusBadge :label="annonce.archived ? 'Archivée' : 'Active'" :variant="annonce.archived ? 'neutral' : 'success'" />
            </div>
            <div class="daa-info-row"><span>Créée le</span><span>{{ new Date(annonce.createdAt).toLocaleDateString('fr-FR') }}</span></div>
          </div>

          <div class="daa-info-card">
            <h3 class="daa-info-card__title">Informations propriétaire</h3>
            <template v-if="annonce.owner">
              <div class="daa-info-row"><span>Nom</span><span>{{ annonce.owner.nomComplet }}</span></div>
              <div class="daa-info-row"><span>Téléphone</span><span><Phone :size="13" /> {{ annonce.owner.telephone }}</span></div>
              <div v-if="annonce.owner.email" class="daa-info-row"><span>Email</span><span><Mail :size="13" /> {{ annonce.owner.email }}</span></div>
              <button class="daa-owner-stats-btn" @click="showOwnerStats = true">Voir statistiques</button>
            </template>
            <p v-else class="daa-owner-empty">Aucun propriétaire renseigné pour ce bien.</p>
          </div>
        </aside>
      </div>
    </template>

    <ProprietaireStatsModal
      v-if="showOwnerStats && annonce?.owner"
      :proprietaire-id="annonce.owner.id"
      @close="showOwnerStats = false"
    />

    <!-- Modal confirmation archivage -->
    <Teleport to="body">
      <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
        <div class="modal-box">
          <h2 class="modal-box__title">Archiver cette annonce ?</h2>
          <p class="modal-box__desc">L'annonce ne sera plus visible par les clients.</p>
          <div class="modal-box__footer">
            <button class="modal-box__cancel" @click="showConfirm = false">Annuler</button>
            <button class="modal-box__confirm" :disabled="confirming" @click="handleArchive">
              {{ confirming ? 'Archivage…' : 'Confirmer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.daa-page { background: var(--color-background); min-height: 100%; }
.daa-loading { display: flex; justify-content: center; padding: 4rem; }
.daa-error { text-align: center; padding: 2rem; color: var(--color-accent); }

.daa-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.daa-back { background: none; border: none; color: var(--color-primary); font-size: .85rem; font-weight: 600; cursor: pointer; margin-bottom: .5rem; display: block; }
.daa-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); margin-bottom: .4rem; }

.daa-actions { display: flex; gap: .5rem; flex-wrap: wrap; }
.daa-btn {
  display: flex; align-items: center; gap: .4rem;
  padding: .5rem 1rem; border-radius: var(--radius-sm);
  font-size: .85rem; font-weight: 600; cursor: pointer; border: none; transition: opacity .15s;
}
.daa-btn--edit    { background: var(--color-primary); color: #fff; }
.daa-btn--archive { background: var(--color-accent); color: #fff; }
.daa-btn--restore { background: #e8f2ef; color: var(--color-primary); border: 1px solid var(--color-primary); }
.daa-btn:hover { opacity: .85; }

/* Galerie */
.daa-gallery { margin-bottom: 2rem; }

/* Corps */
.daa-body { display: grid; grid-template-columns: 1fr 280px; gap: 2rem; align-items: start; }
.daa-sidebar { display: flex; flex-direction: column; gap: 1.25rem; }
.daa-meta { margin-bottom: 1.25rem; }
.daa-prix { font-size: 1.5rem; font-weight: 800; color: var(--color-accent); }
.daa-adresse { display: flex; align-items: center; gap: .35rem; font-size: .88rem; color: var(--color-text-muted); margin-top: .25rem; }

.daa-stats { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
.daa-stat { display: flex; align-items: center; gap: .4rem; font-size: .88rem; color: var(--color-text); background: var(--color-card); padding: .5rem .9rem; border-radius: var(--radius-sm); border: 1px solid var(--color-border); }
.daa-stat svg { color: var(--color-primary); }

.daa-section { margin-bottom: 1.5rem; }
.daa-section__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .6rem; }
.daa-section__text { font-size: .9rem; color: var(--color-text-muted); line-height: 1.7; }

.daa-commodites { list-style: none; display: grid; grid-template-columns: repeat(2, 1fr); gap: .4rem; }
.daa-commodite { display: flex; align-items: center; gap: .35rem; font-size: .88rem; color: var(--color-text); }
.daa-commodite svg { color: var(--color-primary); flex-shrink: 0; }

/* Sidebar */
.daa-info-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.daa-info-card__title { font-size: .95rem; font-weight: 700; color: var(--color-text); margin-bottom: 1rem; padding-bottom: .6rem; border-bottom: 1px solid var(--color-border); }
.daa-info-row { display: flex; justify-content: space-between; align-items: center; padding: .5rem 0; border-bottom: 1px solid var(--color-border); font-size: .85rem; }
.daa-info-row:last-child { border-bottom: none; }
.daa-info-row span:first-child { color: var(--color-text-muted); }
.daa-info-row span:last-child { font-weight: 600; color: var(--color-text); display: flex; align-items: center; gap: .3rem; }

.daa-owner-empty { font-size: .85rem; color: var(--color-text-muted); }
.daa-owner-stats-btn {
  margin-top: 1rem; width: 100%; padding: .55rem 1rem; border-radius: var(--radius-sm);
  background: var(--color-primary); color: #fff; border: none; font-size: .85rem; font-weight: 600;
  cursor: pointer; transition: background .15s;
}
.daa-owner-stats-btn:hover { background: var(--color-primary-hover); }


/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-box { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; width: 100%; max-width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.modal-box__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .5rem; }
.modal-box__desc { font-size: .88rem; color: var(--color-text-muted); margin-bottom: 1.25rem; }
.modal-box__footer { display: flex; justify-content: flex-end; gap: .75rem; }
.modal-box__cancel { padding: .5rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; font-size: .88rem; color: var(--color-text); }
.modal-box__confirm { padding: .5rem 1.25rem; background: var(--color-accent); color: #fff; border: none; border-radius: var(--radius-sm); font-weight: 600; cursor: pointer; font-size: .88rem; }
.modal-box__confirm:disabled { opacity: .5; cursor: not-allowed; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .daa-body { grid-template-columns: 1fr; }
}
</style>
