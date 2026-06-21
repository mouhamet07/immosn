<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Phone, Mail, MapPin, Archive } from 'lucide-vue-next'
import proprietaireService from '@/services/proprietaireService'
import StatsCard from '@/components/admin/StatsCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'

const route = useRoute()
const router = useRouter()
const toast = ref(null)

const loading = ref(true)
const error = ref('')
const proprietaire = ref(null)
const stats = ref(null)
const biens = ref([])

const showConfirm = ref(false)
const archiving = ref(false)

function formatMontant(v) {
  return v != null ? new Intl.NumberFormat('fr-SN').format(v) + ' FCFA' : '—'
}

async function fetchData() {
  loading.value = true
  try {
    const [resDetail, resBiens] = await Promise.all([
      proprietaireService.getById(route.params.id),
      proprietaireService.getBiens(route.params.id, { page: 0, size: 20 }),
    ])
    proprietaire.value = resDetail.data.data.proprietaire
    stats.value = resDetail.data.data.stats
    biens.value = resBiens.data.data ?? []
  } catch {
    error.value = 'Propriétaire introuvable.'
  } finally {
    loading.value = false
  }
}

async function handleArchive() {
  archiving.value = true
  try {
    await proprietaireService.archive(proprietaire.value.id)
    toast.value?.show('Propriétaire archivé avec succès.', 'success')
    proprietaire.value.isArchived = true
  } catch (err) {
    toast.value?.show(err.response?.data?.message || "Erreur lors de l'archivage.", 'error')
  } finally {
    archiving.value = false
    showConfirm.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="pdv-page">
    <ToastNotification ref="toast" />

    <div v-if="loading" class="pdv-loading"><div class="spinner"></div></div>
    <div v-else-if="error" class="pdv-error">{{ error }}</div>

    <template v-else-if="proprietaire">
      <!-- Header -->
      <div class="pdv-header">
        <div>
          <button class="pdv-back" @click="router.push('/admin/proprietaires')">← Retour aux propriétaires</button>
          <h1 class="pdv-header__title">{{ proprietaire.nomComplet }}</h1>
          <StatusBadge :label="proprietaire.isArchived ? 'Archivé' : 'Actif'" :variant="proprietaire.isArchived ? 'neutral' : 'success'" />
        </div>
        <div class="pdv-actions">
          <button v-if="!proprietaire.isArchived" class="pdv-btn pdv-btn--archive" @click="showConfirm = true">
            <Archive :size="15" /> Archiver
          </button>
        </div>
      </div>

      <div class="pdv-body">
        <div class="pdv-content">
          <!-- Statistiques -->
          <section class="pdv-section">
            <h2 class="pdv-section__title">Statistiques</h2>
            <div class="pdv-stats">
              <StatsCard label="Total biens" :value="stats.totalBiens" />
              <StatsCard label="Vente" :value="stats.biensVente" />
              <StatsCard label="Location" :value="stats.biensLocation" />
              <StatsCard label="Annonces actives" :value="stats.annoncesActives" />
              <StatsCard label="Visites" :value="stats.visites" />
              <StatsCard label="Contrats" :value="stats.contrats" />
            </div>
            <p v-if="stats.revenus != null" class="pdv-revenus">
              Revenus générés : <strong>{{ formatMontant(stats.revenus) }}</strong>
            </p>
          </section>

          <!-- Biens -->
          <section class="pdv-section">
            <h2 class="pdv-section__title">Biens du propriétaire</h2>
            <div v-if="biens.length" class="pdv-table-wrap">
              <table class="pdv-table">
                <thead>
                  <tr>
                    <th>Titre</th>
                    <th>Localisation</th>
                    <th>Type</th>
                    <th>Transaction</th>
                    <th>Statut</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="b in biens" :key="b.id" class="pdv-row" @click="router.push(`/admin/annonces/${b.id}`)">
                    <td class="pdv-td-title">{{ b.libelle }}</td>
                    <td class="pdv-td-muted">{{ b.adresse }}</td>
                    <td class="pdv-td-muted">{{ b.typeBien?.libelle || '–' }}</td>
                    <td>
                      <span v-if="b.typeTransaction" :class="['badge-type', b.typeTransaction === 'VENTE' ? 'badge-type--vente' : 'badge-type--location']">
                        {{ b.typeTransaction === 'VENTE' ? 'Vente' : 'Location' }}
                      </span>
                      <span v-else class="pdv-td-muted">–</span>
                    </td>
                    <td>
                      <StatusBadge :label="b.archived ? 'Archivée' : 'Active'" :variant="b.archived ? 'neutral' : 'success'" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="pdv-empty">Aucun bien associé à ce propriétaire.</p>
          </section>
        </div>

        <!-- Sidebar profil -->
        <aside class="pdv-sidebar">
          <div class="pdv-info-card">
            <h3 class="pdv-info-card__title">Profil</h3>
            <div class="pdv-info-row"><span>Nom</span><span>{{ proprietaire.nomComplet }}</span></div>
            <div class="pdv-info-row"><span>Téléphone</span><span><Phone :size="13" /> {{ proprietaire.telephone }}</span></div>
            <div v-if="proprietaire.email" class="pdv-info-row"><span>Email</span><span><Mail :size="13" /> {{ proprietaire.email }}</span></div>
            <div v-if="proprietaire.adresse" class="pdv-info-row"><span>Adresse</span><span><MapPin :size="13" /> {{ proprietaire.adresse }}</span></div>
            <div v-if="proprietaire.notes" class="pdv-info-notes">{{ proprietaire.notes }}</div>
          </div>
        </aside>
      </div>
    </template>

    <Teleport to="body">
      <ConfirmModal
        v-if="showConfirm"
        title="Archiver ce propriétaire ?"
        message="Il n'apparaîtra plus dans le select de création d'annonce, mais son historique sera conservé."
        @confirm="handleArchive"
        @cancel="showConfirm = false"
      />
    </Teleport>
  </div>
</template>

<style scoped>
.pdv-page { background: var(--color-background); min-height: 100%; }
.pdv-loading { display: flex; justify-content: center; padding: 4rem; }
.pdv-error { text-align: center; padding: 2rem; color: var(--color-accent); }

.pdv-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;
}
.pdv-back { background: none; border: none; color: var(--color-primary); font-size: .85rem; font-weight: 600; cursor: pointer; margin-bottom: .5rem; display: block; }
.pdv-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); margin-bottom: .4rem; }

.pdv-actions { display: flex; gap: .5rem; flex-wrap: wrap; }
.pdv-btn {
  display: flex; align-items: center; gap: .4rem;
  padding: .5rem 1rem; border-radius: var(--radius-sm);
  font-size: .85rem; font-weight: 600; cursor: pointer; border: none; transition: opacity .15s;
}
.pdv-btn--archive { background: var(--color-accent); color: #fff; }
.pdv-btn:hover { opacity: .85; }

.pdv-body { display: grid; grid-template-columns: 1fr 280px; gap: 2rem; align-items: start; }

.pdv-section { margin-bottom: 1.5rem; }
.pdv-section__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: .9rem; }

.pdv-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; margin-bottom: 1rem; }
.pdv-revenus { font-size: .9rem; color: var(--color-text); }

.pdv-table-wrap { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; overflow-x: auto; }
.pdv-table { width: 100%; border-collapse: collapse; font-size: .88rem; }
.pdv-table th { padding: .75rem 1rem; text-align: left; font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; color: var(--color-text); opacity: .55; background: var(--color-background); border-bottom: 1px solid var(--color-border); }
.pdv-table td { padding: .85rem 1rem; color: var(--color-text); border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.pdv-row { cursor: pointer; transition: background .15s; }
.pdv-row:hover { background: var(--color-background); }
.pdv-row:last-child td { border-bottom: none; }
.pdv-td-title { font-weight: 600; }
.pdv-td-muted { color: var(--color-text); opacity: .6; }
.badge-type { display: inline-flex; align-items: center; gap: .3rem; padding: .25rem .65rem; border-radius: 12px; font-size: .75rem; font-weight: 700; white-space: nowrap; }
.badge-type--vente { background: #fef9c3; color: #a16207; }
.badge-type--location { background: #ede9fe; color: #7c3aed; }

.pdv-empty { font-size: .88rem; color: var(--color-text); opacity: .5; padding: 1rem 0; }

/* Sidebar */
.pdv-info-card { background: var(--color-card); border-radius: var(--radius); padding: 1.25rem; box-shadow: var(--shadow-card); }
.pdv-info-card__title { font-size: .95rem; font-weight: 700; color: var(--color-text); margin-bottom: 1rem; padding-bottom: .6rem; border-bottom: 1px solid var(--color-border); }
.pdv-info-row { display: flex; justify-content: space-between; align-items: center; padding: .5rem 0; border-bottom: 1px solid var(--color-border); font-size: .85rem; gap: .5rem; }
.pdv-info-row:last-child { border-bottom: none; }
.pdv-info-row span:first-child { color: var(--color-text-muted); flex-shrink: 0; }
.pdv-info-row span:last-child { font-weight: 600; color: var(--color-text); display: flex; align-items: center; gap: .3rem; text-align: right; }
.pdv-info-notes { margin-top: .75rem; padding-top: .75rem; border-top: 1px solid var(--color-border); font-size: .82rem; color: var(--color-text); opacity: .7; line-height: 1.5; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .pdv-body { grid-template-columns: 1fr; }
  .pdv-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>
