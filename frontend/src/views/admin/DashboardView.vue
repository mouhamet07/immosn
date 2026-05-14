<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import StatsCard from '@/components/admin/StatsCard.vue'

const router = useRouter()

// Données mockées — à remplacer par des appels API
const stats = ref([
  { label: 'Annonces actives',  value: 6,  color: 'primary' },
  { label: 'Visites ce mois',   value: 0,  color: 'neutral' },
  { label: 'Contrats signés',   value: 0,  color: 'neutral' },
  { label: 'Administrateurs',   value: 6,  color: 'primary' },
])

const recentAnnonces = ref([
  { id: 1, titre: 'Villa moderne Almadies',   quartier: 'Almadies',  prix: '150 000 000', statut: 'ACTIF' },
  { id: 2, titre: 'Appartement Plateau',      quartier: 'Plateau',   prix: '45 000 000',  statut: 'ACTIF' },
  { id: 3, titre: 'Bureau Mermoz',            quartier: 'Mermoz',    prix: '30 000 000',  statut: 'EN_ATTENTE' },
  { id: 4, titre: 'Penthouse Ngor',           quartier: 'Ngor',      prix: '200 000 000', statut: 'ACTIF' },
])
</script>

<template>
  <div class="dashboard">
    <!-- En-tête -->
    <div class="dashboard__header">
      <div>
        <h1 class="dashboard__title">Tableau de bord</h1>
        <p class="dashboard__subtitle">Bienvenue sur l'interface d'administration ImmoSN.</p>
      </div>
    </div>

    <!-- Stats -->
    <div class="dashboard__stats">
      <StatsCard
        v-for="stat in stats"
        :key="stat.label"
        :label="stat.label"
        :value="stat.value"
        :color="stat.color"
      />
    </div>

    <!-- Annonces récentes -->
    <div class="dashboard__section">
      <div class="section-header">
        <h2 class="section-header__title">Annonces récentes</h2>
        <RouterLink to="/admin/annonces" class="section-header__link">Voir tout →</RouterLink>
      </div>

      <div class="recent-table-wrap">
        <table class="recent-table">
          <thead>
            <tr>
              <th>Titre</th>
              <th>Quartier</th>
              <th>Prix (FCFA)</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="a in recentAnnonces"
              :key="a.id"
              class="recent-table__row"
              @click="router.push(`/admin/annonces`)"
            >
              <td class="td-title">{{ a.titre }}</td>
              <td class="td-muted">{{ a.quartier }}</td>
              <td class="td-muted">{{ a.prix }}</td>
              <td>
                <span
                  class="badge"
                  :class="{
                    'badge--active':  a.statut === 'ACTIF',
                    'badge--pending': a.statut === 'EN_ATTENTE',
                  }"
                >
                  {{ a.statut === 'EN_ATTENTE' ? 'En attente' : 'Actif' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Raccourcis -->
    <div class="dashboard__shortcuts">
      <RouterLink to="/admin/annonces/publier" class="shortcut-card">
        <span class="shortcut-card__icon">🏠</span>
        <span class="shortcut-card__label">Publier une annonce</span>
      </RouterLink>
      <RouterLink to="/admin/visites" class="shortcut-card">
        <span class="shortcut-card__icon">📅</span>
        <span class="shortcut-card__label">Gérer les visites</span>
      </RouterLink>
      <RouterLink to="/admin/contrats" class="shortcut-card">
        <span class="shortcut-card__icon">📄</span>
        <span class="shortcut-card__label">Voir les contrats</span>
      </RouterLink>
      <RouterLink to="/admin/administrateurs" class="shortcut-card">
        <span class="shortcut-card__icon">👥</span>
        <span class="shortcut-card__label">Administrateurs</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 2rem; }

.dashboard__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.dashboard__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.dashboard__subtitle {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

.btn-primary { display: none; }

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

/* Section */
.dashboard__section {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.25rem 0.75rem;
  border-bottom: 1px solid #E8E0D4;
}

.section-header__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.section-header__link {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
}
.section-header__link:hover { text-decoration: underline; }

/* Table */
.recent-table-wrap { overflow-x: auto; }

.recent-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.recent-table th {
  padding: 0.75rem 1.25rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  background: var(--color-background);
}

.recent-table__row {
  border-top: 1px solid var(--color-hover-row);
  cursor: pointer;
  transition: background 0.15s;
}
.recent-table__row:hover { background: var(--color-hover-row); }

.recent-table td { padding: 0.85rem 1.25rem; vertical-align: middle; }
.td-title { font-weight: 600; color: var(--color-text); }
.td-muted { color: var(--color-text-muted); }

.badge {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge--active  { background: var(--badge-active-bg);  color: var(--badge-active-color); }
.badge--pending { background: var(--badge-pending-bg); color: var(--badge-pending-color); }

/* Raccourcis */
.dashboard__shortcuts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.shortcut-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  text-decoration: none;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.15s, transform 0.15s;
  border: 1px solid #E8E0D4;
}
.shortcut-card:hover { box-shadow: var(--shadow-card-hover); transform: translateY(-2px); }

.shortcut-card__icon { font-size: 1.75rem; }
.shortcut-card__label { font-size: 0.82rem; font-weight: 600; color: var(--color-text); text-align: center; }

@media (max-width: 900px) {
  .dashboard__stats     { grid-template-columns: repeat(2, 1fr); }
  .dashboard__shortcuts { grid-template-columns: repeat(2, 1fr); }
}
</style>
