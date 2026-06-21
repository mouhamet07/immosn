<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Phone, Mail, MapPin } from 'lucide-vue-next'
import proprietaireService from '@/services/proprietaireService'
import StatsCard from '@/components/admin/StatsCard.vue'

const props = defineProps({
  proprietaireId: { type: [Number, String], required: true },
})
const emit = defineEmits(['close'])
const router = useRouter()

const loading = ref(true)
const error = ref('')
const proprietaire = ref(null)
const stats = ref(null)
const biens = ref([])

function formatMontant(v) {
  return v != null ? new Intl.NumberFormat('fr-SN').format(v) + ' FCFA' : '—'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [resDetail, resBiens] = await Promise.all([
      proprietaireService.getById(props.proprietaireId),
      proprietaireService.getBiens(props.proprietaireId, 0, 10),
    ])
    proprietaire.value = resDetail.data.data.proprietaire
    stats.value = resDetail.data.data.stats
    biens.value = resBiens.data.data ?? []
  } catch {
    error.value = 'Impossible de charger le profil du propriétaire.'
  } finally {
    loading.value = false
  }
}

watch(() => props.proprietaireId, load, { immediate: true })

function goToBien(id) {
  emit('close')
  router.push(`/admin/annonces/${id}`)
}
</script>

<template>
  <Teleport to="body">
    <div class="psm-overlay" @click.self="$emit('close')">
      <div class="psm-modal">
        <button class="psm-close" aria-label="Fermer" @click="$emit('close')">×</button>

        <div v-if="loading" class="psm-loading"><div class="spinner"></div></div>
        <div v-else-if="error" class="psm-error">{{ error }}</div>

        <template v-else-if="proprietaire">
          <h2 class="psm-title">Profil propriétaire</h2>

          <div class="psm-profile">
            <p class="psm-name">{{ proprietaire.nomComplet }}</p>
            <p class="psm-contact"><Phone :size="14" /> {{ proprietaire.telephone }}</p>
            <p v-if="proprietaire.email" class="psm-contact"><Mail :size="14" /> {{ proprietaire.email }}</p>
            <p v-if="proprietaire.adresse" class="psm-contact"><MapPin :size="14" /> {{ proprietaire.adresse }}</p>
          </div>

          <div class="psm-stats">
            <StatsCard label="Total biens" :value="stats.totalBiens" />
            <StatsCard label="Vente" :value="stats.biensVente" />
            <StatsCard label="Location" :value="stats.biensLocation" />
            <StatsCard label="Annonces actives" :value="stats.annoncesActives" />
            <StatsCard label="Visites" :value="stats.visites" />
            <StatsCard label="Contrats" :value="stats.contrats" />
          </div>

          <p v-if="stats.revenus != null" class="psm-revenus">
            Revenus générés : <strong>{{ formatMontant(stats.revenus) }}</strong>
          </p>

          <h3 class="psm-subtitle">Biens du propriétaire</h3>
          <ul v-if="biens.length" class="psm-biens">
            <li v-for="b in biens" :key="b.id" class="psm-bien" @click="goToBien(b.id)">
              <span class="psm-bien__libelle">{{ b.libelle }}</span>
              <span class="psm-bien__adresse">{{ b.adresse }}</span>
            </li>
          </ul>
          <p v-else class="psm-empty">Aucun bien associé.</p>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.psm-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.psm-modal { background: var(--color-card); border-radius: var(--radius); padding: 2rem; width: 100%; max-width: 640px; max-height: 85vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,.25); position: relative; }
.psm-close { position: absolute; top: 1rem; right: 1.25rem; background: none; border: none; font-size: 1.5rem; line-height: 1; color: var(--color-text); opacity: .5; cursor: pointer; }
.psm-close:hover { opacity: 1; }

.psm-loading, .psm-error { display: flex; align-items: center; justify-content: center; padding: 3rem; color: var(--color-text); opacity: .6; }
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.psm-title { font-size: 1.2rem; font-weight: 800; color: var(--color-text); margin-bottom: 1.25rem; }

.psm-profile { margin-bottom: 1.5rem; padding-bottom: 1.25rem; border-bottom: 1px solid var(--color-border); }
.psm-name { font-size: 1.05rem; font-weight: 700; color: var(--color-text); margin-bottom: .4rem; }
.psm-contact { display: flex; align-items: center; gap: .4rem; font-size: .85rem; color: var(--color-text); opacity: .7; margin-top: .25rem; }

.psm-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; margin-bottom: 1.25rem; }

.psm-revenus { font-size: .9rem; color: var(--color-text); margin-bottom: 1.5rem; }

.psm-subtitle { font-size: .95rem; font-weight: 700; color: var(--color-text); margin-bottom: .75rem; }
.psm-biens { list-style: none; display: flex; flex-direction: column; gap: .5rem; }
.psm-bien {
  display: flex; justify-content: space-between; align-items: center; gap: 1rem;
  padding: .65rem .9rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm);
  cursor: pointer; transition: background .15s;
}
.psm-bien:hover { background: var(--color-background); }
.psm-bien__libelle { font-weight: 600; color: var(--color-text); font-size: .88rem; }
.psm-bien__adresse { font-size: .8rem; color: var(--color-text); opacity: .6; }
.psm-empty { font-size: .85rem; color: var(--color-text); opacity: .5; }

@media (max-width: 600px) {
  .psm-stats { grid-template-columns: repeat(2, 1fr); }
  .psm-bien { flex-direction: column; align-items: flex-start; gap: .25rem; }
}
</style>
