<script setup>
import { useAuthStore } from '@/stores/authStore'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const route = useRoute()

const navLinks = [
  { to: '/admin/dashboard',       label: 'Dashboard',         icon: 'grid' },
  { to: '/admin/annonces',        label: 'Annonces',          icon: 'building' },
  { to: '/admin/messages',        label: 'Messages',          icon: 'chat' },
  { to: '/admin/visites',         label: 'Visites',           icon: 'calendar' },
  { to: '/admin/contrats',        label: 'Contrats',          icon: 'document' },
  { to: '/admin/administrateurs', label: 'Administrateurs',   icon: 'people' },
  { to: '/admin/profil',          label: 'Paramètres',        icon: 'gear' },
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
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="sidebar__logo-img" />
      </RouterLink>

      <!-- Navigation principale -->
      <nav class="sidebar__nav">
        <RouterLink
          v-for="link in navLinks"
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
            <!-- people -->
            <svg v-else-if="link.icon === 'people'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <!-- gear -->
            <svg v-else-if="link.icon === 'gear'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          </span>
          <span class="sidebar__link-label">{{ link.label }}</span>
        </RouterLink>
      </nav>

      <!-- Bas de sidebar -->
      <div class="sidebar__bottom">
        <a href="#" class="sidebar__bottom-link">
          <span class="sidebar__link-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </span>
          <span>Centre d'aide</span>
        </a>
        <button class="sidebar__bottom-link sidebar__logout" @click="authStore.logout()">
          <span class="sidebar__link-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </span>
          <span>Déconnexion</span>
        </button>
      </div>
    </aside>

    <!-- Contenu principal -->
    <main class="admin-main">
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
  padding: 1rem 1.25rem;
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
  transition: background 150ms ease, color 150ms ease;
}

.sidebar__link:hover {
  background: var(--color-hover-row);
}

.sidebar__link--active {
  background: var(--color-accent);
  color: #fff;
}

.sidebar__link-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar__link-icon svg {
  width: 18px;
  height: 18px;
}

.sidebar__link--active .sidebar__link-icon svg {
  stroke: #fff;
}

.sidebar__link:not(.sidebar__link--active) .sidebar__link-icon svg {
  stroke: var(--color-text-muted);
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
</style>
