<script setup>
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'connexion' })
}
</script>

<template>
  <nav class="navbar">
    <!-- Logo -->
    <RouterLink to="/annonces" class="navbar__logo">
      <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="navbar__logo-img" />
    </RouterLink>

    <!-- Liens de navigation -->
    <ul class="navbar__links">
      <li><RouterLink to="/annonces" class="navbar__link">Annonces</RouterLink></li>
      <li><RouterLink to="/visites" class="navbar__link">Visites</RouterLink></li>
      <li><RouterLink to="/contrats" class="navbar__link">Contrats</RouterLink></li>
      <li><RouterLink to="/signalements" class="navbar__link">Signalements</RouterLink></li>
    </ul>

    <!-- Actions droite -->
    <div class="navbar__actions">
      <button class="navbar__icon-btn" title="Favoris">❤️</button>
      <button class="navbar__icon-btn" title="Notifications">🔔</button>

      <template v-if="authStore.isAuthenticated">
        <RouterLink to="/profil" class="navbar__avatar" title="Profil">
          {{ authStore.user?.nomComplet?.charAt(0)?.toUpperCase() || '👤' }}
        </RouterLink>
        <button class="navbar__btn-logout" @click="handleLogout">Déconnexion</button>
      </template>

      <template v-else>
        <RouterLink to="/connexion" class="navbar__btn-login">Connexion</RouterLink>
      </template>
    </div>
  </nav>
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
  z-index: 100;
  box-shadow: var(--shadow-card);
}

.navbar__logo {
  display: flex;
  align-items: center;
}

.navbar__logo-img {
  height: 40px;
  object-fit: contain;
}

/* Liens */
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
  font-size: 1.1rem;
  padding: 0.3rem;
  border-radius: 50%;
  transition: background 0.2s;
}

.navbar__icon-btn:hover {
  background: var(--color-border);
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
}

.navbar__btn-logout {
  background: var(--color-primary);
  color: #fff;
  padding: 0.45rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 600;
  transition: background 0.2s;
}

.navbar__btn-logout:hover {
  background: var(--color-primary-hover);
}

.navbar__btn-login {
  background: var(--color-primary);
  color: #fff;
  padding: 0.45rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 600;
  transition: background 0.2s;
}

.navbar__btn-login:hover {
  background: var(--color-primary-hover);
}

/* Responsive */
@media (max-width: 768px) {
  .navbar__links {
    display: none;
  }

  .navbar {
    padding: 0 1rem;
  }
}
</style>
