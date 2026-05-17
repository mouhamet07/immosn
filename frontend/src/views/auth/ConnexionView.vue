<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import InputField from '@/components/InputField.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import AppFooter from '@/components/AppFooter.vue'

const authStore = useAuthStore()
const form = reactive({ email: '', motDePasse: '' })
const errors = reactive({ email: '', motDePasse: '', global: '' })
const loading = ref(false)

function validate() {
  let valid = true
  errors.email = ''
  errors.motDePasse = ''
  errors.global = ''

  if (!form.email.includes('@')) {
    errors.email = 'Adresse e-mail invalide.'
    valid = false
  }
  if (!form.motDePasse) {
    errors.motDePasse = 'Le mot de passe est requis.'
    valid = false
  }
  return valid
}

async function handleSubmit() {
  if (!validate()) return
  loading.value = true
  try {
    // POST /api/v1/auth/login — champs exacts: email, motDePasse (AuthLoginRequestDto)
    await authStore.login(form.email, form.motDePasse)
  } catch (err) {
    // Extraire le message depuis RestResponse<AuthResponseDto>
    errors.global = err.response?.data?.message || err.response?.data?.data?.message || 'Email ou mot de passe incorrect.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="connexion">
    <!-- Fond image -->
    <div class="connexion__bg"></div>

    <!-- Contenu centré -->
    <div class="connexion__content">
      <!-- Logo + sous-titre -->
      <div class="connexion__header">
        <RouterLink to="/annonces" class="connexion__logo">
          <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="connexion__logo-img" />
        </RouterLink>
        <p class="connexion__tagline">Curateur d'Immobilier haut de gamme au Sénégal</p>
      </div>

      <!-- Carte de connexion -->
      <div class="connexion__card">
        <h2 class="connexion__title">Ravi de vous revoir</h2>
        <p class="connexion__subtitle">Accédez à notre catalogue d'annonces exclusives.</p>

        <form class="connexion__form" @submit.prevent="handleSubmit">
          <InputField
            v-model="form.email"
            label="Adresse e-mail"
            type="email"
            placeholder="abdoulaye@example.sn"
            icon="✉️"
            :error="errors.email"
            required
          />
          <InputField
            v-model="form.motDePasse"
            label="Mot de passe"
            type="password"
            placeholder="••••••••"
            icon="🔒"
            :error="errors.motDePasse"
            required
          />

          <!-- Erreur globale -->
          <div v-if="errors.global" class="connexion__alert">{{ errors.global }}</div>

          <ButtonPrimary type="submit" :loading="loading" full-width>
            Se connecter →
          </ButtonPrimary>
        </form>

        <p class="connexion__register-link">
          Vous n'avez pas encore de compte ?
          <RouterLink to="/inscription" class="connexion__link">Créer un compte</RouterLink>
        </p>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<style scoped>
.connexion {
  min-height: 100vh;
  background: var(--color-background);
  position: relative;
  display: flex;
  flex-direction: column;
}

/* Fond image avec overlay */
.connexion__bg {
  position: fixed;
  top: 0;
  right: 0;
  width: 45%;
  height: 100%;
  background: url('@/assets/Luxury Home.png') center/cover no-repeat;
  opacity: 0.15;
  z-index: 0;
}

.connexion__content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

/* Logo */
.connexion__header {
  text-align: center;
}

.connexion__logo {
  display: inline-flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.connexion__logo-img {
  height: 52px;
  object-fit: contain;
}

.logo-immo {
  color: var(--color-primary);
}

.logo-sn {
  color: var(--color-accent);
}

.connexion__tagline {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.65;
  font-style: italic;
}

/* Carte */
.connexion__card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2.5rem;
  width: 100%;
  box-shadow: var(--shadow-card-hover);
}

.connexion__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
  margin-bottom: 0.4rem;
}

.connexion__subtitle {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.6;
  margin-bottom: 1.75rem;
}

.connexion__form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.connexion__alert {
  background: rgba(212, 113, 74, 0.1);
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
  padding: 0.75rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
}

.connexion__register-link {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.7;
}

.connexion__link {
  color: var(--color-primary);
  font-weight: 600;
}

.connexion__link:hover {
  text-decoration: underline;
}

</style>
