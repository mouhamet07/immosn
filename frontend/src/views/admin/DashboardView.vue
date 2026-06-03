<script setup>
import { ref, computed, onMounted, defineComponent, h } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import dashboardService from '@/services/dashboardService'

const authStore = useAuthStore()

// Composant icône SVG inline — remplace les emojis
const SVGS = {
  building: `
    <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18"/>
    <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/>
    <path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/>
    <path d="M10 6h4"/><path d="M10 10h4"/><path d="M10 14h4"/><path d="M10 18h4"/>`,

  user: `
    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
    <circle cx="12" cy="8" r="4"/>
    <path d="M12 12v1"/>`,

  calendar: `
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
    <line x1="16" y1="2" x2="16" y2="6"/>
    <line x1="8" y1="2" x2="8" y2="6"/>
    <line x1="3" y1="10" x2="21" y2="10"/>
    <path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/>`,

  document: `
    <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z"/>
    <polyline points="14 2 14 8 20 8"/>
    <line x1="9" y1="13" x2="15" y2="13"/>
    <line x1="9" y1="17" x2="13" y2="17"/>`,

  target: `
    <circle cx="12" cy="12" r="10"/>
    <circle cx="12" cy="12" r="6"/>
    <circle cx="12" cy="12" r="2"/>
    <line x1="12" y1="2" x2="12" y2="4"/>
    <line x1="12" y1="20" x2="12" y2="22"/>
    <line x1="2" y1="12" x2="4" y2="12"/>
    <line x1="20" y1="12" x2="22" y2="12"/>`,

  alert: `
    <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
    <line x1="12" y1="8" x2="12" y2="12"/>
    <line x1="12" y1="16" x2="12.01" y2="16" stroke-linecap="round" stroke-width="2.5"/>`,

  chat: `
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
    <line x1="9" y1="10" x2="15" y2="10"/>
    <line x1="9" y1="14" x2="13" y2="14"/>`,

  people: `
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
    <circle cx="9" cy="7" r="4"/>
    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    <line x1="19" y1="8" x2="21" y2="10"/>`,
}
const DashIcon = defineComponent({
  props: { name: String },
  setup(props) {
    return () => h('svg', {
      viewBox: '0 0 24 24', fill: 'none',
      stroke: 'currentColor', 'stroke-width': '2',
      innerHTML: SVGS[props.name] ?? SVGS.document,
    })
  },
})

const loading = ref(true)
const error   = ref('')
const stats   = ref(null)

// Date du jour en français
const dateAujourdhui = new Date().toLocaleDateString('fr-FR', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
})

// ── Chargement ─────────────────────────────────────────────
onMounted(async () => {
  try {
    const res = await dashboardService.getStats()
    stats.value = res.data.data
  } catch {
    error.value = 'Impossible de charger les statistiques.'
  } finally {
    loading.value = false
  }
})

// ── Cards statistiques ──────────────────────────────────────
const statCards = computed(() => {
  if (!stats.value) return []
  const s = stats.value
  const cards = [
    { icon: 'building',     label: 'Annonces actives',    value: s.annoncesActives,      total: s.totalAnnonces,        color: 'primary', to: '/admin/annonces' },
    { icon: 'user',         label: 'Clients',              value: s.totalClients,         total: null,                   color: 'blue',    to: null },
    { icon: 'calendar',     label: 'Visites en attente',   value: s.visitesEnAttente,     total: s.totalVisites,         color: 'orange',  to: '/admin/visites' },
    { icon: 'document',     label: 'Contrats actifs',      value: s.contratsActifs,       total: s.totalContrats,        color: 'green',   to: '/admin/contrats' },
    { icon: 'target',       label: 'Leads en cours',       value: s.leadsEnCours,         total: s.totalLeads,           color: 'purple',  to: '/admin/leads' },
    { icon: 'alert',        label: 'Signalements ouverts', value: s.signalementsOuverts,  total: s.totalSignalements,    color: 'red',     to: '/admin/signalements' },
    { icon: 'chat', label: 'Discussions', value: s.totalDiscussions, total: null, color: 'teal', to: '/admin/messages' },
  ]
  // Carte admins visible uniquement pour SUPER_ADMIN
  if (authStore.role === 'SUPER_ADMIN') {
    cards.push({ icon: 'people', label: 'Administrateurs', value: s.totalAdmins, total: null, color: 'gray', to: '/admin/administrateurs' })
  }
  return cards
})

const recentActivities = computed(() => stats.value?.activitesRecentes ?? [])

const ACTIVITY_ICONS  = { ANNONCE: 'building', VISITE: 'calendar', CONTRAT: 'document', SIGNALEMENT: 'alert', CLIENT: 'user' }
const STATUT_COLORS   = {
  ACTIVE: 'badge--success', EN_ATTENTE: 'badge--warning', ACCEPTEE: 'badge--success',
  REFUSEE: 'badge--danger', ACTIF: 'badge--success', RESILIE: 'badge--danger',
  EXPIRE: 'badge--neutral', OUVERT: 'badge--warning', EN_COURS: 'badge--info',
  RESOLU: 'badge--success', FERME: 'badge--neutral',
}
const STATUT_LABELS   = {
  ACTIVE: 'Actif', EN_ATTENTE: 'En attente', ACCEPTEE: 'Acceptée',
  REFUSEE: 'Refusée', ACTIF: 'Actif', RESILIE: 'Résilié',
  EXPIRE: 'Expiré', OUVERT: 'Ouvert', EN_COURS: 'En cours',
  RESOLU: 'Résolu', FERME: 'Fermé',
}

function formatDate(dt) {
  if (!dt) return ''
  const d = new Date(dt), now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60)    return 'À l\'instant'
  if (diff < 3600)  return `Il y a ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}

const shortcuts = [
  { icon: 'building', label: 'Publier une annonce', to: '/admin/annonces/publier' },
  { icon: 'calendar', label: 'Gérer les visites',   to: '/admin/visites' },
  { icon: 'target',   label: 'Voir les leads',       to: '/admin/leads' },
  { icon: 'document', label: 'Voir les contrats',    to: '/admin/contrats' },
  { icon: 'alert',    label: 'Signalements SAV',     to: '/admin/signalements' },
  { icon: 'chat',     label: 'Messagerie',           to: '/admin/messages' },
]
</script>

<template>
  <div class="dash">

    <div class="dash__header">
      <div>
        <h1 class="dash__title">Bonjour, {{ authStore.user?.nomComplet?.split(' ')[0] }} 👋</h1>
        <p class="dash__sub">{{ dateAujourdhui }}</p>
      </div>
      <div v-if="stats" class="dash__today">
        <span class="dash__today-label">Visites aujourd'hui</span>
        <span class="dash__today-value">{{ stats.visitesAujourdhui }}</span>
      </div>
    </div>

    <div v-if="loading" class="dash__loading">
      <div class="spinner"></div>
      <p>Chargement des statistiques&hellip;</p>
    </div>

    <div v-else-if="error" class="dash__error">{{ error }}</div>

    <template v-else>
      <!-- Stats -->
      <div class="dash__stats">
        <component
          :is="card.to ? 'RouterLink' : 'div'"
          v-for="card in statCards"
          :key="card.label"
          :to="card.to"
          class="stat-card"
          :class="`stat-card--${card.color}`"
        >
          <div class="stat-card__icon">
            <DashIcon :name="card.icon" />
          </div>
          <div class="stat-card__body">
            <div class="stat-card__row">
              <span class="stat-card__value">{{ card.value?.toLocaleString('fr-FR') }}</span>
              <span v-if="card.total !== null" class="stat-card__total">/ {{ card.total?.toLocaleString('fr-FR') }}</span>
            </div>
            <p class="stat-card__label">{{ card.label }}</p>
          </div>
        </component>
      </div>

      <!-- Grille 2 colonnes -->
      <div class="dash__grid">

        <!-- Activités récentes -->
        <div class="dash__section">
          <div class="section-head">
            <h2 class="section-head__title">Activités récentes</h2>
          </div>
          <p v-if="!recentActivities.length" class="dash__empty">Aucune activité récente.</p>
          <ul v-else class="activity-list">
            <li v-for="(a, i) in recentActivities" :key="i" class="activity-item">
              <span class="activity-item__icon">
                <DashIcon :name="ACTIVITY_ICONS[a.type] ?? 'document'" />
              </span>
              <div class="activity-item__body">
                <p class="activity-item__title">{{ a.titre }}</p>
                <p class="activity-item__desc">{{ a.description }}</p>
              </div>
              <div class="activity-item__right">
                <span :class="['badge', STATUT_COLORS[a.statut] ?? 'badge--neutral']">
                  {{ STATUT_LABELS[a.statut] ?? a.statut }}
                </span>
                <span class="activity-item__date">{{ formatDate(a.createdAt) }}</span>
              </div>
            </li>
          </ul>
        </div>

        <!-- Raccourcis -->
        <div class="dash__shortcuts-col">
          <div class="section-head">
            <h2 class="section-head__title">Accès rapides</h2>
          </div>
          <div class="shortcuts-grid">
            <RouterLink v-for="s in shortcuts" :key="s.to" :to="s.to" class="shortcut-card">
              <span class="shortcut-card__icon">
                <DashIcon :name="s.icon" />
              </span>
              <span class="shortcut-card__label">{{ s.label }}</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 1.75rem; }

.dash__header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; }
.dash__title  { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.dash__sub    { font-size: .88rem; color: var(--color-text); opacity: .55; margin-top: .2rem; }
.dash__today  { background: var(--color-card); border-radius: var(--radius); padding: .75rem 1.25rem; box-shadow: var(--shadow-card); text-align: center; }
.dash__today-label { display: block; font-size: .72rem; text-transform: uppercase; letter-spacing: .07em; color: var(--color-text); opacity: .5; }
.dash__today-value { font-size: 1.75rem; font-weight: 800; color: var(--color-primary); }

.dash__loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 4rem; opacity: .55; }
.dash__error   { text-align: center; padding: 2rem; color: var(--color-accent); }
.dash__empty   { padding: 2rem; text-align: center; font-size: .88rem; color: var(--color-text); opacity: .45; }

/* Grille 8 stats */
.dash__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 0.75rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;

  padding: 0.9rem 1rem;
  min-height: 85px;

  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;

  text-decoration: none;
  color: inherit;

  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.stat-card__icon {
  width: 44px;
  height: 44px;
  min-width: 44px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 12px;
}

.stat-card__icon svg {
  width: 20px;
  height: 20px;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.stat-card__body {
  flex: 1;
  min-width: 0;
}

.stat-card__row {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.stat-card__value {
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1;
  color: #111827;
}

.stat-card__total {
  font-size: 0.85rem;
  font-weight: 500;
  color: #9ca3af;
}

.stat-card__label {
  margin-top: 0.2rem;
  font-size: 0.8rem;
  font-weight: 500;
  color: #6b7280;
}

/* Couleurs */

.stat-card--primary .stat-card__icon {
  background: #eef2ff;
  color: #4f46e5;
}

.stat-card--blue .stat-card__icon {
  background: #eff6ff;
  color: #2563eb;
}

.stat-card--orange .stat-card__icon {
  background: #fff7ed;
  color: #ea580c;
}

.stat-card--green .stat-card__icon {
  background: #ecfdf5;
  color: #059669;
}

.stat-card--purple .stat-card__icon {
  background: #f5f3ff;
  color: #7c3aed;
}

.stat-card--red .stat-card__icon {
  background: #fef2f2;
  color: #dc2626;
}

.stat-card--teal .stat-card__icon {
  background: #f0fdfa;
  color: #0f766e;
}

.stat-card--gray .stat-card__icon {
  background: #f3f4f6;
  color: #4b5563;
}
/* Grille 2 colonnes */
.dash__grid { display: grid; grid-template-columns: 1fr 300px; gap: 1.5rem; align-items: start; }

.dash__section { background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }
.dash__shortcuts-col { display: flex; flex-direction: column; gap: 0; background: var(--color-card); border-radius: var(--radius); box-shadow: var(--shadow-card); overflow: hidden; }

.section-head { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem .75rem; border-bottom: 1px solid var(--color-border); }
.section-head__title { font-size: .95rem; font-weight: 700; color: var(--color-text); }

/* Activités */
.activity-list { list-style: none; margin: 0; padding: 0; }
.activity-item { display: flex; align-items: center; gap: .9rem; padding: .85rem 1.25rem; border-bottom: 1px solid var(--color-border); transition: background .12s; }
.activity-item:last-child { border-bottom: none; }
.activity-item:hover { background: var(--color-background); }
.activity-item__icon { flex-shrink: 0; width: 24px; display: flex; align-items: center; justify-content: center; }
.activity-item__icon svg { width: 16px; height: 16px; stroke: var(--color-text-muted); }
.activity-item__body { flex: 1; min-width: 0; }
.activity-item__title { font-size: .88rem; font-weight: 600; color: var(--color-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.activity-item__desc  { font-size: .75rem; color: var(--color-text); opacity: .5; }
.activity-item__right { display: flex; flex-direction: column; align-items: flex-end; gap: .2rem; flex-shrink: 0; }
.activity-item__date  { font-size: .68rem; color: var(--color-text); opacity: .4; }

/* Raccourcis */
.shortcuts-grid { display: grid; grid-template-columns: 1fr 1fr; }
.shortcut-card {
  display: flex; flex-direction: column; align-items: center; gap: .4rem;
  padding: 1rem .5rem; text-decoration: none; border-right: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border); transition: background .12s;
}
.shortcut-card:nth-child(even) { border-right: none; }
.shortcut-card:nth-last-child(-n+2) { border-bottom: none; }
.shortcut-card:hover { background: var(--color-background); }
.shortcut-card__icon  { width: 2rem; height: 2rem; display: flex; align-items: center; justify-content: center; color: var(--color-primary); }
.shortcut-card__icon svg { width: 1.25rem; height: 1.25rem; }
.shortcut-card__label { font-size: .75rem; font-weight: 600; color: var(--color-text); text-align: center; line-height: 1.3; }

/* Badges */
.badge { padding: .2rem .5rem; border-radius: 10px; font-size: .68rem; font-weight: 700; }
.badge--success { background: #d1fae5; color: #059669; }
.badge--warning { background: #fef3c7; color: #d97706; }
.badge--danger  { background: #fee2e2; color: #dc2626; }
.badge--info    { background: #dbeafe; color: #2563eb; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px)  { .dash__stats { grid-template-columns: repeat(2, 1fr); } .dash__grid { grid-template-columns: 1fr; } }
@media (max-width: 560px)  { .dash__stats { grid-template-columns: 1fr 1fr; } }
</style>
