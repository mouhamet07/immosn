<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import AppFooter from '@/components/AppFooter.vue'

const authStore = useAuthStore()
const form = reactive({ email: '', motDePasse: '' })
const errors = reactive({ email: '', motDePasse: '', global: '' })
const loading = ref(false)
const showPassword = ref(false)

function validate() {
  let valid = true
  errors.email = ''; errors.motDePasse = ''; errors.global = ''
  if (!form.email.includes('@')) { errors.email = 'Adresse e-mail invalide.'; valid = false }
  if (!form.motDePasse) { errors.motDePasse = 'Le mot de passe est requis.'; valid = false }
  return valid
}

async function handleSubmit() {
  if (!validate()) return
  loading.value = true
  try {
    await authStore.login(form.email, form.motDePasse)
    // FIX 2 : rediriger vers la page demandée avant la connexion
    const redirect = localStorage.getItem('redirectAfterLogin')
    if (redirect) {
      localStorage.removeItem('redirectAfterLogin')
      router.push(redirect)
    }
    // authStore.login gère déjà la redirection par rôle si pas de redirect
  } catch (err) {
    errors.global = err.response?.status === 401
      ? 'Email ou mot de passe incorrect.'
      : (err.response?.data?.message || 'Une erreur est survenue. Veuillez réessayer.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="cx-page">

    <!-- Décoration fond -->
    <div class="cx-blob cx-blob--top"></div>
    <div class="cx-blob cx-blob--bottom"></div>

    <!-- Image droite (desktop) -->
    <div class="cx-hero">
      <div class="cx-hero__overlay"></div>
      <img src="@/assets/Luxury Home.png" alt="Villa de luxe ImmoSN" class="cx-hero__img" />
    </div>

    <div class="cx-main">
      <!-- Brand -->
      <div class="cx-brand">
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="cx-brand__logo" />
        <p class="cx-brand__tagline">Curateur d'immobilier haut de gamme au Sénégal</p>
      </div>

      <!-- Card -->
      <div class="cx-card">
        <h2 class="cx-card__title">Ravi de vous revoir</h2>
        <p class="cx-card__sub">Accédez à votre portefeuille personnalisé et aux annonces exclusives.</p>

        <form class="cx-form" @submit.prevent="handleSubmit">

          <!-- Email -->
          <div class="cx-field">
            <label class="cx-label" for="email">Adresse E-mail</label>
            <div class="cx-input-wrap">
              <svg class="cx-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <input
                id="email" v-model="form.email" type="email"
                class="cx-input" :class="{ 'cx-input--error': errors.email }"
                placeholder="nom@domaine.sn"
              />
            </div>
            <span v-if="errors.email" class="cx-error">{{ errors.email }}</span>
          </div>

          <!-- Mot de passe -->
          <div class="cx-field">
            <label class="cx-label" for="password">Mot de passe</label>
            <div class="cx-input-wrap">
              <svg class="cx-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input
                id="password" v-model="form.motDePasse"
                :type="showPassword ? 'text' : 'password'"
                class="cx-input" :class="{ 'cx-input--error': errors.motDePasse }"
                placeholder="••••••••"
              />
              <button type="button" class="cx-eye" @click="showPassword = !showPassword">
                <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
            <span v-if="errors.motDePasse" class="cx-error">{{ errors.motDePasse }}</span>
          </div>

          <!-- Mot de passe oublié -->
          <div class="cx-forgot-wrap">
            <a href="#" class="cx-forgot">Mot de passe oublié ?</a>
          </div>

          <!-- Erreur globale -->
          <div v-if="errors.global" class="cx-alert">{{ errors.global }}</div>

          <!-- Submit -->
          <button type="submit" class="cx-submit" :disabled="loading">
            <span v-if="loading" class="cx-spinner"></span>
            {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
          </button>
        </form>

        <div class="cx-divider">
          <p class="cx-register-text">
            Intéressé par des propriétés exclusives ?
            <RouterLink to="/inscription" class="cx-link">Créer un compte</RouterLink>
          </p>
        </div>
      </div>

      <!-- Lien explorer supprimé -->
    </div>

    <!-- Footer -->
    <AppFooter />
  </div>
</template>

<style scoped>
.cx-page {
  min-height: 100vh;
  background: #f9faf7;
  background-image: radial-gradient(at 0% 0%, rgba(74,124,111,0.05) 0px, transparent 50%),
                    radial-gradient(at 100% 100%, rgba(154,69,34,0.05) 0px, transparent 50%);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  font-family: 'Inter', -apple-system, sans-serif;
}

/* Blobs décoratifs */
.cx-blob {
  position: absolute;
  width: 384px;
  height: 384px;
  border-radius: 50%;
  filter: blur(64px);
  pointer-events: none;
  z-index: 0;
}
.cx-blob--top    { top: -96px; right: -96px; background: rgba(74,124,111,0.05); }
.cx-blob--bottom { bottom: -96px; left: -96px; background: rgba(154,69,34,0.05); }

/* Image héro droite */
.cx-hero {
  display: none;
  position: fixed;
  top: 0; right: 0; bottom: 0;
  width: 33.333%;
  z-index: 0;
  overflow: hidden;
}
.cx-hero__overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to left, transparent, #f9faf7);
  z-index: 1;
}
.cx-hero__img {
  width: 100%; height: 100%;
  object-fit: cover;
  opacity: 0.4;
  filter: grayscale(20%);
}

/* Contenu principal */
.cx-main {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 5rem 1rem 2rem;
  gap: 1.5rem;
  max-width: 480px;
  margin: 0 auto;
  width: 100%;
}

/* Brand */
.cx-brand { text-align: center; }
.cx-brand__logo { height: 52px; object-fit: contain; display: block; margin: 0 auto; }
.cx-brand__tagline { font-size: 0.85rem; color: #404946; margin-top: 4px; }

/* Card */
.cx-card {
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(112,121,117,0.1);
  border-radius: 12px;
  padding: 2.5rem;
  width: 100%;
  box-shadow: 0 4px 20px rgba(45,55,72,0.05);
}
.cx-card__title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.5rem;
  font-weight: 500;
  color: #191c1b;
  margin-bottom: 4px;
}
.cx-card__sub {
  font-size: 0.875rem;
  color: #404946;
  margin-bottom: 1.5rem;
}

/* Form */
.cx-form { display: flex; flex-direction: column; gap: 1.25rem; }

.cx-field { display: flex; flex-direction: column; gap: 6px; }
.cx-label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #404946;
}
.cx-forgot-wrap { text-align: right; }
.cx-forgot { font-size: 0.75rem; font-weight: 600; color: var(--color-accent); text-decoration: none; }
.cx-forgot:hover { text-decoration: underline; }

.cx-input-wrap { position: relative; }
.cx-input-icon {
  position: absolute; left: 1rem; top: 50%; transform: translateY(-50%);
  width: 16px; height: 16px; stroke: #707975; pointer-events: none;
}
.cx-input {
  width: 100%;
  padding: 1rem 1rem 1rem 3rem;
  background: #fff;
  border: 1px solid rgba(192,200,196,0.3);
  border-radius: 8px;
  font-size: 1rem;
  color: #191c1b;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}
.cx-input:focus { outline: none; border-color: #316357; box-shadow: 0 0 0 1px #316357; }
.cx-input--error { border-color: #ba1a1a; }
.cx-input::placeholder { color: rgba(112,121,117,0.5); }
.cx-eye {
  position: absolute; right: 1rem; top: 50%; transform: translateY(-50%);
  background: none; border: none; cursor: pointer; color: #707975;
  display: flex; align-items: center;
}
.cx-eye:hover { color: #316357; }

.cx-error { font-size: 0.78rem; color: #ba1a1a; }

/* Alert */
.cx-alert {
  background: rgba(186,26,26,0.08);
  border: 1px solid rgba(186,26,26,0.3);
  color: #ba1a1a;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
}

/* Submit */
.cx-submit {
  width: 100%;
  padding: 1rem;
  background: #316357;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s, transform 0.1s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}
.cx-submit:hover:not(:disabled) { background: #1b4f44; }
.cx-submit:active:not(:disabled) { transform: scale(0.98); }
.cx-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.cx-spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Divider */
.cx-divider {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(192,200,196,0.2);
  text-align: center;
}
.cx-register-text { font-size: 0.875rem; color: #404946; }
.cx-link { font-weight: 600; color: #316357; margin-left: 4px; }
.cx-link:hover { text-decoration: underline; }

/* Explorer */
.cx-explore { text-align: center; }
.cx-explore-link {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 0.875rem; color: #404946; text-decoration: none;
  transition: color 0.15s;
}
.cx-explore-link:hover { color: #9a4522; }

/* Footer */
.cx-footer {
  position: relative; z-index: 1;
  padding: 1.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}
.cx-footer__copy { font-size: 0.8rem; color: #c0c8c4; }
.cx-footer__links { display: flex; gap: 1.5rem; flex-wrap: wrap; justify-content: center; }
.cx-footer__links a { font-size: 0.8rem; color: #c0c8c4; text-decoration: none; transition: color 0.15s; }
.cx-footer__links a:hover { color: #316357; }

@media (min-width: 1024px) {
  .cx-hero { display: block; }
  .cx-footer { flex-direction: row; justify-content: space-between; padding: 1.5rem 2rem; }
}
@media (min-width: 768px) {
  .cx-footer { flex-direction: row; justify-content: space-between; }
}
</style>
