<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff } from 'lucide-vue-next'
import authService from '@/services/authService'
import { MESSAGES, getErrorMessage } from '@/utils/messages'
import { useToastStore } from '@/stores/toastStore'
import PhoneInput from '@/components/PhoneInput.vue'
import AppFooter from '@/components/AppFooter.vue'

const router     = useRouter()
const toastStore = useToastStore()

// Champs exacts du AuthRegisterRequestDto: nomComplet, email, telephone, motDePasse
const form = reactive({
  nomComplet: '',
  email: '',
  telephone: '',
  motDePasse: '',
  acceptConditions: false,
})

const errorMessage = ref('')
const loading = ref(false)
const showPassword = ref(false)

// Indicateur de force du mot de passe
const passwordStrength = computed(() => {
  const p = form.motDePasse
  if (!p) return null
  if (p.length < 6) return 'faible'
  if (p.length < 10 || !/[A-Z]/.test(p) || !/[0-9]/.test(p)) return 'moyen'
  return 'fort'
})

async function handleSubmit() {
  errorMessage.value = ''
  if (!form.nomComplet.trim()) { errorMessage.value = 'Le nom complet est requis.'; return }
  if (!form.email.includes('@')) { errorMessage.value = 'Adresse e-mail invalide.'; return }
  if (!form.telephone.trim()) { errorMessage.value = 'Le numéro de téléphone est requis.'; return }
  if (form.motDePasse.length < 8) { errorMessage.value = 'Le mot de passe doit contenir au moins 8 caractères.'; return }
  loading.value = true
  try {
    await authService.register(form.nomComplet, form.email, form.telephone, form.motDePasse)
    toastStore.success(MESSAGES.registerSuccess)
    router.push({ name: 'connexion' })
  } catch (err) {
    errorMessage.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ins-page">

    <!-- Header fixe -->
    <header class="ins-header">
      <div class="ins-header__inner">
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="ins-header__logo" />
        <div class="ins-header__actions">
          <RouterLink to="/connexion" class="ins-header__login">Connexion</RouterLink>
        </div>
      </div>
    </header>

    <!-- Décoration fond -->
    <div class="ins-deco ins-deco--right"></div>
    <div class="ins-deco ins-deco--left"></div>

    <!-- Contenu principal -->
    <main class="ins-main">
      <div class="ins-card">

        <!-- Côté gauche : image + texte -->
        <div class="ins-card__left">
          <img src="@/assets/Luxury Home.png" alt="Villa ImmoSN" class="ins-card__img" />
          <div class="ins-card__overlay">
            <h2 class="ins-card__overlay-title">Sécurisez Votre Futur au Sénégal</h2>
            <p class="ins-card__overlay-text">Rejoignez l'écosystème immobilier le plus fiable d'Afrique de l'Ouest, où la beauté architecturale rencontre la fiabilité moderne.</p>
          </div>
        </div>

        <!-- Côté droit : formulaire -->
        <div class="ins-card__right">
          <h1 class="ins-title">Rejoindre ImmoSN</h1>
          <p class="ins-subtitle">Créez votre compte pour accéder aux meilleures opportunités immobilières du Sénégal.</p>

          <form class="ins-form" @submit.prevent="handleSubmit">

            <div class="ins-field">
              <label class="ins-label" for="full_name">Nom Complet</label>
              <input id="full_name" v-model="form.nomComplet" type="text" class="ins-input" placeholder="Abdoulaye Diop" required />
            </div>

            <div class="ins-field">
              <label class="ins-label" for="email">Adresse E-mail</label>
              <input id="email" v-model="form.email" type="email" class="ins-input" placeholder="abdoulaye@example.sn" required />
            </div>

            <div class="ins-field">
              <label class="ins-label" for="telephone">Téléphone</label>
              <PhoneInput v-model="form.telephone" />
            </div>

            <div class="ins-field">
              <label class="ins-label" for="password">Mot de Passe</label>
              <div class="ins-input-wrap">
                <input
                  id="password"
                  v-model="form.motDePasse"
                  :type="showPassword ? 'text' : 'password'"
                  class="ins-input ins-input--pw"
                  placeholder="••••••••"
                  required
                />
                <button type="button" class="ins-eye" @click="showPassword = !showPassword">
                  <EyeOff v-if="showPassword" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
              <!-- Indicateur de force -->
              <div v-if="form.motDePasse" class="ins-strength">
                <div class="ins-strength__bars">
                  <span
                    class="ins-strength__bar"
                    :class="{ '--active': passwordStrength }"
                    :style="{ background: passwordStrength === 'faible' ? '#ef4444' : passwordStrength === 'moyen' ? '#f59e0b' : '#22c55e' }"
                  ></span>
                  <span
                    class="ins-strength__bar"
                    :class="{ '--active': passwordStrength === 'moyen' || passwordStrength === 'fort' }"
                    :style="{ background: passwordStrength === 'moyen' ? '#f59e0b' : passwordStrength === 'fort' ? '#22c55e' : '' }"
                  ></span>
                  <span
                    class="ins-strength__bar"
                    :class="{ '--active': passwordStrength === 'fort' }"
                    :style="{ background: passwordStrength === 'fort' ? '#22c55e' : '' }"
                  ></span>
                </div>
                <span class="ins-strength__label" :class="`ins-strength__label--${passwordStrength}`">
                  {{ passwordStrength === 'faible' ? 'Faible' : passwordStrength === 'moyen' ? 'Moyen' : 'Fort' }}
                </span>
              </div>
            </div>

            <!-- Conditions -->
            <div class="ins-terms">
              <input id="terms" v-model="form.acceptConditions" type="checkbox" class="ins-checkbox" />
              <label for="terms" class="ins-terms-label">
                J'accepte les <a href="#" class="ins-link">Conditions d'Utilisation</a> et la <a href="#" class="ins-link">Politique de Confidentialité</a>.
              </label>
            </div>

            <p v-if="errorMessage" class="ins-error">{{ errorMessage }}</p>

            <button
              type="submit"
              :disabled="loading || !form.acceptConditions"
              class="ins-submit"
              :class="{ 'ins-submit--active': form.acceptConditions }"
            >
              <span v-if="loading" class="ins-spinner"></span>
              {{ loading ? 'Création en cours...' : 'Créer un compte' }}
            </button>
          </form>

          <div class="ins-footer-link">
            <p>Vous avez déjà un compte ? <RouterLink to="/connexion" class="ins-link">Se connecter</RouterLink></p>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <AppFooter />
  </div>
</template>

<style scoped>
.ins-page {
  min-height: 100vh;
  background: #f9faf7;
  display: flex;
  flex-direction: column;
  font-family: 'Inter', -apple-system, sans-serif;
  position: relative;
  overflow: hidden;
}

/* Header */
.ins-header {
  position: fixed; top: 0; width: 100%; z-index: 50;
  background: rgba(249,250,247,0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(192,200,196,0.2);
  box-shadow: 0 1px 4px rgba(45,55,72,0.05);
}
.ins-header__inner {
  max-width: 1280px; margin: 0 auto;
  padding: 1rem 1.5rem;
  display: flex; justify-content: space-between; align-items: center;
}
.ins-header__logo { height: 40px; object-fit: contain; }
.ins-header__actions { display: flex; align-items: center; gap: 1.5rem; }
.ins-header__login {
  font-size: 0.9rem; font-weight: 500;
  color: #404946; text-decoration: none; transition: color 0.15s;
}
.ins-header__login:hover { color: #316357; }
.ins-header__support {
  background: #316357; color: #fff;
  padding: 0.5rem 1.25rem; border-radius: 12px;
  font-size: 0.875rem; font-weight: 600;
  text-decoration: none; transition: background 0.15s;
}
.ins-header__support:hover { background: #1b4f44; }

/* Décorations */
.ins-deco {
  position: absolute; z-index: 0; pointer-events: none;
}
.ins-deco--right {
  top: 0; right: 0;
  width: 33%; height: 100%;
  background: rgba(74,124,111,0.1);
  transform: skewX(-12deg) translateX(50%);
}
.ins-deco--left {
  bottom: 0; left: 0;
  width: 25%; height: 50%;
  background: rgba(253,145,104,0.05);
  border-radius: 50%;
  filter: blur(48px);
}

/* Main */
.ins-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 7rem 1rem 3rem;
  position: relative; z-index: 1;
}

/* Card split */
.ins-card {
  width: 100%;
  max-width: 1000px;
  display: grid;
  grid-template-columns: 1fr;
  background: #f9faf7;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(192,200,196,0.2);
  box-shadow: 0 4px 20px rgba(45,55,72,0.05);
}

/* Côté gauche image */
.ins-card__left {
  display: none;
  position: relative;
  min-height: 600px;
}
.ins-card__img {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
}
.ins-card__overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(49,99,87,0.8), transparent);
  display: flex; flex-direction: column;
  justify-content: flex-end;
  padding: 2.5rem;
  color: #fff;
}
.ins-card__overlay-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.5rem; font-weight: 500;
  margin-bottom: 0.75rem;
}
.ins-card__overlay-text { font-size: 1rem; opacity: 0.9; line-height: 1.6; }

/* Côté droit formulaire */
.ins-card__right {
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
}

.ins-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2rem; font-weight: 600;
  color: #191c1b; margin-bottom: 0.5rem;
}
.ins-subtitle { font-size: 1rem; color: #404946; margin-bottom: 1.75rem; line-height: 1.5; }

/* Form */
.ins-form { display: flex; flex-direction: column; gap: 1rem; }
.ins-field { display: flex; flex-direction: column; gap: 6px; }
.ins-label {
  font-size: 0.75rem; font-weight: 600;
  letter-spacing: 0.08em; text-transform: uppercase;
  color: #404946;
}
.ins-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid rgba(192,200,196,0.4);
  background: #f3f4f2;
  font-size: 1rem; color: #191c1b;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.ins-input:focus { outline: none; border-color: #316357; }
.ins-input::placeholder { color: #707975; }

/* Champ mot de passe avec toggle */
.ins-input-wrap { position: relative; }
.ins-input--pw { padding-right: 2.75rem; }
.ins-eye {
  position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%);
  background: none; border: none; cursor: pointer; color: #707975;
  display: flex; align-items: center; transition: color 0.15s;
}
.ins-eye:hover { color: #316357; }

/* Indicateur de force du mot de passe */
.ins-strength { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.ins-strength__bars { display: flex; gap: 4px; }
.ins-strength__bar {
  width: 36px; height: 4px; border-radius: 2px;
  background: #e5e7eb; transition: background 0.2s;
}
.ins-strength__label { font-size: 0.75rem; font-weight: 600; }
.ins-strength__label--faible { color: #ef4444; }
.ins-strength__label--moyen  { color: #f59e0b; }
.ins-strength__label--fort   { color: #22c55e; }

/* Champ téléphone avec préfixe +221 */
.ins-phone-wrap {
  display: flex;
  border-radius: 8px;
  border: 1px solid rgba(192,200,196,0.4);
  overflow: hidden;
  background: #f3f4f2;
}
.ins-phone-prefix {
  display: flex; align-items: center; gap: 4px;
  padding: 0 0.75rem;
  background: var(--color-border);
  color: var(--color-text);
  font-size: 0.875rem; font-weight: 600;
  flex-shrink: 0;
  border-right: 1px solid rgba(192,200,196,0.4);
}
.ins-prefix-text { font-size: 0.875rem; }
.ins-phone-input {
  flex: 1; padding: 0.75rem 1rem;
  border: none; background: transparent;
  font-size: 1rem; color: #191c1b; outline: none;
}
.ins-phone-input::placeholder { color: #707975; }
.ins-phone-wrap:focus-within { border-color: var(--color-primary); }

/* Terms */
.ins-terms { display: flex; align-items: flex-start; gap: 0.75rem; padding-top: 4px; }
.ins-checkbox { margin-top: 2px; width: 16px; height: 16px; accent-color: #316357; cursor: pointer; flex-shrink: 0; }
.ins-terms-label { font-size: 0.875rem; color: #404946; line-height: 1.5; cursor: pointer; }
.ins-link { color: #316357; text-decoration: none; }
.ins-link:hover { text-decoration: underline; }

.ins-error { font-size: 0.85rem; color: #ba1a1a; }

/* Submit */
.ins-submit {
  width: 100%;
  padding: 1rem;
  border-radius: 12px;
  border: none;
  font-size: 1rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 0.5rem;
  background: #9ca3af;
  color: #fff;
}
.ins-submit--active { background: #4a7c6f; color: #e7fff6; }
.ins-submit--active:hover:not(:disabled) { filter: brightness(1.1); }
.ins-submit:active:not(:disabled) { transform: scale(0.98); }
.ins-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.ins-spinner {
  display: inline-block;
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  margin-right: 6px;
}
@keyframes spin { to { transform: rotate(360deg); } }

.ins-footer-link {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(192,200,196,0.2);
  text-align: center;
  font-size: 0.875rem;
  color: #707975;
}

/* Footer */
.ins-footer {
  position: relative; z-index: 1;
  background: #e1e3e0;
  border-top: 1px solid rgba(192,200,196,0.3);
  margin-top: 3rem;
}
.ins-footer__inner {
  max-width: 1280px; margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}
.ins-footer__brand {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.25rem; font-weight: 500;
  color: #191c1b; margin-bottom: 4px;
}
.ins-footer__copy { font-size: 0.8rem; color: #404946; }
.ins-footer__links { display: flex; flex-wrap: wrap; justify-content: center; gap: 1rem; }
.ins-footer__links a {
  font-size: 0.8rem; color: #404946; text-decoration: none;
  transition: color 0.15s, transform 0.15s;
}
.ins-footer__links a:hover { color: #316357; transform: translateX(2px); }

@media (min-width: 768px) {
  .ins-card { grid-template-columns: 1fr 1fr; }
  .ins-card__left { display: block; }
  .ins-footer__inner { flex-direction: row; justify-content: space-between; align-items: flex-start; }
}
</style>
