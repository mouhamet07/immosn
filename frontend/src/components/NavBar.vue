<script setup>
import { ref, watch } from 'vue'
import { useAuthStore }  from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import { useRoute } from 'vue-router'
import { AUTH_MESSAGES } from '@/utils/messages'
import SvgIcon from '@/components/SvgIcon.vue'

const authStore  = useAuthStore()
const toastStore = useToastStore()
const route      = useRoute()
const menuOpen   = ref(false)

watch(() => route.fullPath, () => { menuOpen.value = false })

function closeMenu() { menuOpen.value = false }

async function handleLogout() {
  closeMenu()
  await authStore.logout()
  // authStore.logout() gère déjà router.push('/connexion')
  toastStore.success(AUTH_MESSAGES.logoutSuccess)
}
</script>

<template>
  <nav class="navbar">
    <!-- Logo -->
    <RouterLink to="/annonces" class="navbar__logo">
      <img src="@/assets/logo nav 1 - orange.svg" alt="ImmoSN" class="navbar__logo-img" />
    </RouterLink>

    <!-- Liens de navigation (desktop) -->
    <ul class="navbar__links">
      <li><RouterLink to="/annonces" class="navbar__link">Annonces</RouterLink></li>
      <template v-if="authStore.isAuthenticated">
        <li><RouterLink to="/mes-visites" class="navbar__link">Visites</RouterLink></li>
        <li><RouterLink to="/mes-contrats" class="navbar__link">Contrats</RouterLink></li>
        <li><RouterLink to="/discussions" class="navbar__link">Messages</RouterLink></li>
        <li><RouterLink to="/mes-signalements" class="navbar__link">Signalements</RouterLink></li>
      </template>
      <template v-else>
        <li><RouterLink to="/suivi-visite" class="navbar__link">Suivre ma visite</RouterLink></li>
      </template>
    </ul>

    <!-- Actions droite -->
    <div class="navbar__actions">
      <RouterLink to="/favoris" class="navbar__icon-btn" title="Favoris">
        <SvgIcon name="heart" :size="20" />
      </RouterLink>

      <template v-if="authStore.isAuthenticated">
        <RouterLink to="/profil" class="navbar__avatar" title="Profil">
          <span v-if="authStore.user?.nomComplet">{{ authStore.user.nomComplet.charAt(0).toUpperCase() }}</span>
          <SvgIcon v-else name="user" :size="18" />
        </RouterLink>
        <button class="navbar__btn-logout navbar__btn--desktop" @click="handleLogout">Déconnexion</button>
      </template>

      <template v-else>
        <RouterLink to="/connexion" class="navbar__btn-login navbar__btn--desktop">Connexion</RouterLink>
      </template>

      <!-- Burger button (mobile only) -->
      <button
        class="navbar__burger"
        @click="menuOpen = !menuOpen"
        :aria-expanded="menuOpen"
        aria-label="Menu de navigation"
      >
        <SvgIcon :name="menuOpen ? 'x' : 'menu'" :size="22" />
      </button>
    </div>
  </nav>

  <!-- Mobile drawer -->
  <Teleport to="body">
    <div v-if="menuOpen" class="navbar__overlay" @click="closeMenu">
      <nav class="navbar__drawer" @click.stop>
        <ul class="navbar__drawer-links">
          <li><RouterLink to="/annonces" class="navbar__drawer-link" @click="closeMenu">Annonces</RouterLink></li>
          <template v-if="authStore.isAuthenticated">
            <li><RouterLink to="/mes-visites" class="navbar__drawer-link" @click="closeMenu">Visites</RouterLink></li>
            <li><RouterLink to="/mes-contrats" class="navbar__drawer-link" @click="closeMenu">Contrats</RouterLink></li>
            <li><RouterLink to="/discussions" class="navbar__drawer-link" @click="closeMenu">Messages</RouterLink></li>
            <li><RouterLink to="/mes-signalements" class="navbar__drawer-link" @click="closeMenu">Signalements</RouterLink></li>
          </template>
          <template v-else>
            <li><RouterLink to="/suivi-visite" class="navbar__drawer-link" @click="closeMenu">Suivre ma visite</RouterLink></li>
          </template>
        </ul>

        <div class="navbar__drawer-footer">
          <template v-if="authStore.isAuthenticated">
            <button class="navbar__drawer-logout" @click="handleLogout">Déconnexion</button>
          </template>
          <template v-else>
            <RouterLink to="/connexion" class="navbar__drawer-login" @click="closeMenu">Se connecter</RouterLink>
          </template>
        </div>
      </nav>
    </div>
  </Teleport>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  height: 64px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 200;
  box-shadow: var(--shadow-card);
}

.navbar__logo {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.navbar__logo-img {
  height: 23px;
  object-fit: contain;
}

/* Liens desktop */
.navbar__links {
  display: flex;
  list-style: none;
  gap: 2rem;
}

.navbar__link {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text);
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s, color 0.2s;
  white-space: nowrap;
}

.navbar__link:hover,
.navbar__link.router-link-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* Actions */
.navbar__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.navbar__icon-btn {
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.4rem;
  border-radius: 50%;
  color: var(--color-text-muted);
  transition: background var(--transition), color var(--transition);
}

.navbar__icon-btn:hover {
  background: var(--color-border);
  color: var(--color-primary);
}

.navbar__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.navbar__btn-logout,
.navbar__btn-login {
  background: var(--color-primary);
  color: #fff;
  padding: 0.45rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 600;
  transition: background 0.2s;
  white-space: nowrap;
}

.navbar__btn-logout:hover,
.navbar__btn-login:hover {
  background: var(--color-primary-hover);
}

/* Burger */
.navbar__burger {
  display: none;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--color-text);
  padding: 0.4rem;
  border-radius: var(--radius-sm);
  transition: background var(--transition);
}

.navbar__burger:hover {
  background: var(--color-border);
}

/* Mobile overlay + drawer */
.navbar__overlay {
  position: fixed;
  inset: 0;
  top: 64px;
  background: rgba(30, 37, 50, 0.45);
  z-index: 199;
}

.navbar__drawer {
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 8px 24px rgba(30, 37, 50, 0.12);
}

.navbar__drawer-links {
  list-style: none;
}

.navbar__drawer-link {
  display: block;
  padding: 1rem 1.5rem;
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
  transition: background var(--transition), color var(--transition);
}

.navbar__drawer-link:hover,
.navbar__drawer-link.router-link-active {
  background: var(--color-hover-row);
  color: var(--color-primary);
}

.navbar__drawer-footer {
  padding: 1rem 1.5rem;
}

.navbar__drawer-logout {
  width: 100%;
  padding: 0.75rem;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  font-weight: 600;
  text-align: center;
  transition: background 0.2s;
}

.navbar__drawer-logout:hover {
  background: var(--color-primary-hover);
}

.navbar__drawer-login {
  display: block;
  width: 100%;
  padding: 0.75rem;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  font-weight: 600;
  text-align: center;
  transition: background 0.2s;
}

.navbar__drawer-login:hover {
  background: var(--color-primary-hover);
}

/* Responsive */
@media (max-width: 768px) {
  .navbar {
    padding: 0 1rem;
  }

  .navbar__links {
    display: none;
  }

  .navbar__burger {
    display: flex;
  }

  .navbar__btn--desktop {
    display: none;
  }
}

@media (min-width: 769px) {
  .navbar__overlay,
  .navbar__drawer {
    display: none;
  }
}
</style>
