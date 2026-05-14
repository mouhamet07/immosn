<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import AppFooter from '@/components/AppFooter.vue'

const router = useRouter()

const form = reactive({
  nomComplet: '',
  email: '',
  motDePasse: '',
  acceptConditions: false,
})

const errorMessage = ref('')
const loading = ref(false)

async function handleSubmit() {
  errorMessage.value = ''
  if (!form.nomComplet.trim() || !form.email.includes('@') || form.motDePasse.length < 6) {
    errorMessage.value = 'Veuillez remplir tous les champs correctement.'
    return
  }
  loading.value = true
  try {
    await authService.register(form.nomComplet, form.email, '', form.motDePasse)
    router.push({ name: 'connexion' })
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Une erreur est survenue. Réessayez.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="inscription">
    <!-- Header pleine largeur : logo gauche + connexion droite -->
    <header class="inscription__header">
      <RouterLink to="/annonces" class="inscription__logo">
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="inscription__logo-img" />
      </RouterLink>
      <RouterLink to="/connexion" class="inscription__btn-connexion">Connexion</RouterLink>
    </header>

    <!-- Body : split gauche/droite -->
    <div class="inscription__body">
      <!-- Côté gauche : image -->
      <div class="inscription__left"></div>

      <!-- Côté droit : formulaire -->
      <div class="inscription__right">
        <div class="inscription__form-wrapper">
          <h2 class="inscription__title">Créer un compte</h2>
          <p class="inscription__subtitle">Rejoignez la plateforme ImmoSN dès aujourd'hui.</p>

          <form class="inscription__form" @submit.prevent="handleSubmit">
            <div class="inscription__field">
              <label class="inscription__label">
                NOM COMPLET <span class="inscription__required">*</span>
              </label>
              <input
                v-model="form.nomComplet"
                type="text"
                required
                class="inscription__input"
                placeholder="Abdoulaye Diop"
              />
            </div>

            <div class="inscription__field">
              <label class="inscription__label">
                ADRESSE E-MAIL <span class="inscription__required">*</span>
              </label>
              <input
                v-model="form.email"
                type="email"
                required
                class="inscription__input"
                placeholder="abdoulaye@example.sn"
              />
            </div>

            <div class="inscription__field">
              <label class="inscription__label">
                MOT DE PASSE <span class="inscription__required">*</span>
              </label>
              <input
                v-model="form.motDePasse"
                type="password"
                required
                class="inscription__input"
                placeholder="••••••••"
              />
            </div>

            <div class="inscription__checkbox-wrapper">
              <input
                id="acceptTerms"
                v-model="form.acceptConditions"
                type="checkbox"
                class="inscription__checkbox"
              />
              <label for="acceptTerms" class="inscription__checkbox-label">
                J'accepte les
                <a href="#" class="inscription__link">Conditions d'Utilisation</a>
                et la
                <a href="#" class="inscription__link">Politique de Confidentialité</a>.
              </label>
            </div>

            <p v-if="errorMessage" class="inscription__error">{{ errorMessage }}</p>

            <button
              type="submit"
              :disabled="loading"
              :class="['inscription__btn', form.acceptConditions ? 'inscription__btn--active' : 'inscription__btn--inactive']"
            >
              <span v-if="loading" class="inscription__spinner"></span>
              {{ loading ? 'Création en cours...' : 'Créer un compte' }}
            </button>
          </form>

          <p class="inscription__login-link">
            Vous avez déjà un compte ?
            <RouterLink to="/connexion" class="inscription__link">Se connecter</RouterLink>
          </p>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<style scoped>
.inscription {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.inscription__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  border-top: 4px solid var(--color-primary);
}

.inscription__logo {
  display: flex;
  align-items: center;
}

.inscription__logo-img {
  height: 40px;
  object-fit: contain;
}

.inscription__btn-connexion {
  background: var(--color-primary);
  color: #fff;
  padding: 0.5rem 1.25rem;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.inscription__btn-connexion:hover {
  background: var(--color-primary-hover);
}

.inscription__body {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  max-width: 690px;
  margin: 0 auto;
  width: 100%;
}

.inscription__left {
  position: relative;
  overflow: hidden;
}

.inscription__left::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('@/assets/Branding.png') left/contain no-repeat;
  background-color: #fff;
}

.inscription__right {
  background: var(--color-background);
  display: flex;
  flex-direction: column;
  padding: 2rem;
}

.inscription__form-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 420px;
  margin: 0 auto;
  width: 100%;
}

.inscription__title {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--color-text);
  margin-bottom: 0.4rem;
}

.inscription__subtitle {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.55;
  margin-bottom: 2rem;
}

.inscription__form {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.inscription__field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.inscription__label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text);
  opacity: 0.75;
}

.inscription__required {
  color: var(--color-accent);
}

.inscription__input {
  width: 100%;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color 0.2s;
}

.inscription__input:focus {
  border-color: var(--color-primary);
  outline: none;
}

.inscription__input::placeholder {
  color: var(--color-text);
  opacity: 0.35;
}

.inscription__checkbox-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
}

.inscription__checkbox {
  margin-top: 2px;
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  flex-shrink: 0;
  cursor: pointer;
}

.inscription__checkbox-label {
  font-size: 0.88rem;
  color: var(--color-text);
  opacity: 0.75;
  line-height: 1.5;
  cursor: pointer;
}

.inscription__link {
  color: var(--color-text);
  font-weight: 600;
  text-decoration: underline;
}

.inscription__link:hover {
  color: var(--color-primary);
}

.inscription__error {
  font-size: 0.85rem;
  color: var(--color-accent);
}

.inscription__btn {
  width: 100%;
  padding: 0.85rem;
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: background 0.2s, opacity 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.inscription__btn--active {
  background: var(--color-primary);
}

.inscription__btn--active:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.inscription__btn--inactive {
  background: #9ca3af;
  cursor: not-allowed;
}

.inscription__btn:disabled {
  opacity: 0.65;
}

.inscription__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.inscription__login-link {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.65;
}

@media (max-width: 768px) {
  .inscription__body {
    grid-template-columns: 1fr;
  }

  .inscription__left {
    min-height: 220px;
  }
}
</style>
