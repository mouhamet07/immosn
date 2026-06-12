<script setup>
import { useAuthStore } from '@/stores/authStore'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const route = useRoute()

// FIX 7 : initiales du nom de l'utilisateur connecté
function getInitials(name) {
  if (!name) return 'A'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}

const navLinks = [
  { to: '/admin/dashboard',       label: 'Dashboard',      icon: 'grid' },
  { to: '/admin/annonces',        label: 'Annonces',       icon: 'building' },
  { to: '/admin/messages',        label: 'Messages',       icon: 'chat' },
  { to: '/admin/visites',         label: 'Visites',        icon: 'calendar' },
  { to: '/admin/leads',           label: 'Leads',          icon: 'target' },
  { to: '/admin/contrats',        label: 'Contrats',       icon: 'document' },
  { to: '/admin/signalements',    label: 'Signalements',   icon: 'alert' },
  { to: '/admin/types-biens',     label: 'Types de biens', icon: 'tag' },
  { to: '/admin/commodites',      label: 'Commodités',     icon: 'list' },
  { to: '/admin/administrateurs', label: 'Admins',         icon: 'people', superAdminOnly: true },
  { to: '/admin/profil',          label: 'Compte',         icon: 'user' },
]

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <!-- Logo -->
      <RouterLink to="/admin/dashboard" class="sidebar__logo">
        <img src="@/assets/logo nav 1 - orange.svg" alt="ImmoSN" class="sidebar__logo-img" />
      </RouterLink>

      <!-- L'affichage de l'utilisateur connecté a été déplacé vers le header -->
      <nav class="sidebar__nav">
        <RouterLink
          v-for="link in navLinks"
          v-show="!link.superAdminOnly || authStore.role === 'SUPER_ADMIN'"
          :key="link.to"
          :to="link.to"
          class="sidebar__link"
          :class="{ 'sidebar__link--active': isActive(link.to) }"
        >
          <span class="sidebar__link-icon">
            <!-- grid -->
            <svg v-if="link.icon === 'grid'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
            <!-- building -->
            <svg v-else-if="link.icon === 'building'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-4h6v4"/></svg>
            <!-- chat -->
            <svg v-else-if="link.icon === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <!-- calendar -->
            <svg v-else-if="link.icon === 'calendar'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            <!-- document -->
            <svg v-else-if="link.icon === 'document'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <!-- target (leads) -->
            <svg v-else-if="link.icon === 'target'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>
            <!-- alert (signalements) -->
            <svg v-else-if="link.icon === 'alert'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <!-- tag (types de biens) -->
            <svg v-else-if="link.icon === 'tag'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
            <!-- list (commodités) -->
            <svg v-else-if="link.icon === 'list'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            <!-- people -->
            <svg v-else-if="link.icon === 'people'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <!-- user -->
            <svg v-else-if="link.icon === 'user'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <!-- gear -->
            <svg v-else-if="link.icon === 'gear'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          </span>
          <span class="sidebar__link-label">{{ link.label }}</span>
        </RouterLink>
      </nav>

      <!-- Bas de sidebar -->
      <div class="sidebar__bottom">
        <button class="sidebar__bottom-link sidebar__logout" @click="authStore.logout()" title="Déconnexion">
          <span class="sidebar__link-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </span>
          <span>Déconnexion</span>
        </button>
      </div>
    </aside>

    <!-- Contenu principal -->
    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-inner">
          <!-- Titre de la page courante -->
          <div class="admin-header-page">
            <span class="admin-header-page-label">
              {{ navLinks.find(l => isActive(l.to))?.label ?? 'Dashboard' }}
            </span>
          </div>

          <!-- Profil utilisateur -->
          <RouterLink to="/admin/profil" class="admin-header-user" title="Profil">
            <div class="admin-header-user-info">
              <p class="sidebar-user-name">{{ authStore.user?.nomComplet }}</p>
              <span class="sidebar-user-role">
                {{ authStore.user?.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Administrateur' }}
              </span>
            </div>

            <div class="sidebar-avatar">
              <img
                v-if="authStore.user?.photo"
                :src="authStore.user.photo"
                :alt="authStore.user.nomComplet"
                class="sidebar-avatar-img"
              />
              <div v-else class="sidebar-avatar-initials">
                {{ getInitials(authStore.user?.nomComplet) }}
              </div>
            </div>
          </RouterLink>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-background);
}

/* ── Sidebar ── */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: var(--color-card);
  border-right: 1px solid var(--color-border-solid);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 50;
  overflow-y: auto;
}

.sidebar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.75rem 1.25rem 1.25rem;
  text-decoration: none;
  border-bottom: 1px solid var(--color-border-solid);
  margin-bottom: 0.5rem;
}

.sidebar__logo-img {
  width: 100%;
  max-width: 160px;
  height: auto;
  object-fit: contain;
  display: block;
}

/* FIX 7 : info utilisateur connecté */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-solid);
  margin-bottom: 4px;
}
.sidebar-avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--color-border);
}
.sidebar-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.sidebar-avatar-initials {
  width: 100%; height: 100%;
  background: var(--color-primary);
  color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600;
}
.sidebar-user-info { min-width: 0; }
.sidebar-user-name {
  font-size: 13px; font-weight: 600;
  color: var(--color-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 130px;
}
.sidebar-user-role {
  font-size: 10px;
  color: var(--color-primary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

/* Navigation */
.sidebar__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0.5rem 0.75rem;
}

.sidebar__link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  transition: all 150ms ease;
}

.sidebar__link:hover:not(.sidebar__link--active) {
  color: var(--color-accent);
  background: #FEF3EE;
}

.sidebar__link--active {
  color: white;
  background: var(--color-accent);
}

.sidebar__link-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* FIX 8 : icônes vertes par défaut, terracotta au hover/actif */
.sidebar__link-icon svg {
  width: 18px; height: 18px;
  stroke: var(--color-primary);
  transition: stroke 150ms ease;
}

.sidebar__link:hover:not(.sidebar__link--active) .sidebar__link-icon svg {
  stroke: var(--color-accent);
}

.sidebar__link--active .sidebar__link-icon svg {
  stroke: white;
}

/* Bas */
.sidebar__bottom {
  padding: 0.75rem;
  border-top: 1px solid var(--color-border-solid);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar__bottom-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.55rem 0.75rem;
  border-radius: 8px;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  text-decoration: none;
  background: transparent;
  font-family: inherit;
  transition: background 150ms ease;
}

.sidebar__bottom-link:hover {
  background: var(--color-hover-row);
}

.sidebar__logout {
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;
}

.sidebar__bottom-link .sidebar__link-icon svg {
  stroke: var(--color-text-muted);
}

/* Contenu principal */
.admin-main {
  margin-left: 220px;
  flex: 1;
  padding: 2rem;
  background: var(--color-background);
  min-height: 100vh;
}

/* Responsive — sidebar icônes seulement sur tablette */
@media (max-width: 900px) {
  .sidebar {
    width: 64px;
    min-width: 64px;
  }

  .sidebar__link-label,
  .sidebar__bottom-link span:last-child {
    display: none;
  }

  .sidebar__logo {
    justify-content: center;
    padding: 1rem 0;
  }

  .sidebar__logo-img {
    height: 28px;
  }

  .admin-main {
    margin-left: 64px;
    padding: 1.5rem 1rem;
  }
}

/* ── Header amélioré ── */
.admin-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: var(--color-card);
  margin: -2rem -2rem 1.5rem -2rem;
  padding: 0 2rem;
  border-bottom: 1px solid var(--color-border-solid);
  box-shadow: 0 2px 12px rgba(16, 24, 40, 0.06);
}

.admin-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

/* Titre page courante */
.admin-header-page-label {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.01em;
}

/* Profil utilisateur — sans bordure ni ombre */
.admin-header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  text-decoration: none;
  color: inherit;
}

.admin-header-user:hover {
  opacity: 0.9;
}

.admin-header-user-info {
  text-align: right;
  min-width: 0;
}

.admin-header-user .sidebar-user-name {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.admin-header-user .sidebar-user-role {
  font-size: 10px;
  color: var(--color-primary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: block;
}

.admin-header-user .sidebar-avatar {
  width: 36px;
  height: 36px;
  border-width: 2px;
  border-color: var(--color-primary);
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .admin-header { display: none; }
}
</style>