<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import PhoneInput from '@/components/PhoneInput.vue'
import authService from '@/services/authService'
import { getErrorMessage } from '@/utils/messages'

const router = useRouter()
const toast = ref(null)

const form = reactive({
  nomComplet: '',
  email:      '',
  telephone:  '',
  motDePasse: '',
})

const showPassword = ref(false)
const loading = ref(false)
const errors = reactive({ nomComplet: '', email: '', telephone: '', motDePasse: '' })

function validate() {
  let valid = true
  errors.nomComplet = ''
  errors.email      = ''
  errors.telephone  = ''
  errors.motDePasse = ''

  if (!form.nomComplet.trim()) {
    errors.nomComplet = 'Le nom complet est requis.'
    valid = false
  }
  if (!form.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
    errors.email = 'Adresse e-mail invalide.'
    valid = false
  }
  // PhoneInput émet toujours l'indicatif (ex. "+221") : on exige des chiffres saisis
  const phoneDigits = form.telephone.replace(/^\+\d+\s*/, '').replace(/\D/g, '')
  if (!phoneDigits) {
    errors.telephone = 'Le numéro de téléphone est requis.'
    valid = false
  }
  if (form.motDePasse.length < 8) {
    errors.motDePasse = 'Le mot de passe doit contenir au moins 8 caractères.'
    valid = false
  }
  return valid
}

async function handleSubmit() {
  if (!validate()) return
  loading.value = true
  try {
    await authService.createAdmin({
      nomComplet: form.nomComplet,
      email:      form.email,
      telephone:  form.telephone,
      motDePasse: form.motDePasse,
    })
    toast.value.show('Administrateur ajouté avec succès.', 'success')
    setTimeout(() => router.push('/admin/administrateurs'), 1200)
  } catch (err) {
    // getErrorMessage extrait les messages détaillés de validation (champ data)
    // au lieu du message générique "Erreur de validation des données"
    toast.value.show(getErrorMessage(err), 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ajouter-page">
    <ToastNotification ref="toast" />

    <!-- En-tête -->
    <div class="ajouter-header">
      <RouterLink to="/admin/administrateurs" class="back-link">
        ← Retour aux paramètres
      </RouterLink>
      <h1 class="ajouter-header__title">Ajouter un nouvel administrateur</h1>
      <p class="ajouter-header__subtitle">
        Créez un compte administrateur et définissez ses permissions d'accès à la plateforme.
      </p>
    </div>

    <div class="ajouter-body">
      <!-- Formulaire -->
      <form class="ajouter-form" @submit.prevent="handleSubmit">

        <!-- Nom complet -->
        <div class="field">
          <label class="field__label">NOM COMPLET <span class="required">*</span></label>
          <input
            v-model="form.nomComplet"
            type="text"
            class="field__input"
            :class="{ 'field__input--error': errors.nomComplet }"
            placeholder="ex. Mamadou Diop"
          />
          <span v-if="errors.nomComplet" class="field__error">{{ errors.nomComplet }}</span>
        </div>

        <!-- Email -->
        <div class="field">
          <label class="field__label">EMAIL PROFESSIONNEL <span class="required">*</span></label>
          <input
            v-model="form.email"
            type="email"
            class="field__input"
            :class="{ 'field__input--error': errors.email }"
            placeholder="mamadou@immosn.sn"
          />
          <span v-if="errors.email" class="field__error">{{ errors.email }}</span>
        </div>

        <!-- Téléphone — même composant que l'inscription et le profil (PhoneInput) -->
        <div class="field">
          <label class="field__label">TÉLÉPHONE <span class="required">*</span></label>
          <PhoneInput v-model="form.telephone" />
          <span v-if="errors.telephone" class="field__error">{{ errors.telephone }}</span>
        </div>

        <!-- Mot de passe -->
        <div class="field">
          <label class="field__label">MOT DE PASSE INITIAL <span class="required">*</span></label>
          <div class="field__password-wrap">
            <input
              v-model="form.motDePasse"
              :type="showPassword ? 'text' : 'password'"
              class="field__input"
              :class="{ 'field__input--error': errors.motDePasse }"
              placeholder="••••••••"
            />
            <button type="button" class="field__eye" @click="showPassword = !showPassword" :title="showPassword ? 'Masquer' : 'Afficher'">
              <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
          <span v-if="errors.motDePasse" class="field__error">{{ errors.motDePasse }}</span>
          <span class="field__hint">Mot de passe temporaire. L'utilisateur devra le modifier à la première connexion.</span>
        </div>

        <!-- Boutons -->
        <div class="ajouter-actions">
          <button type="button" class="btn-cancel" @click="router.push('/admin/administrateurs')">
            Annuler
          </button>
          <button type="submit" class="btn-confirm" :disabled="loading">
            {{ loading ? 'Création...' : 'Confirmer' }}
          </button>
        </div>
      </form>

      <!-- Info box -->
      <div class="info-box">
        <div class="info-box__header">
          <span class="info-box__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </span>
          <h3 class="info-box__title">Privilèges d'administration</h3>
        </div>
        <p class="info-box__text">
          Les administrateurs ont accès à la gestion des annonces, des visites, des contrats et des utilisateurs de la plateforme ImmoSN.
        </p>
        <p class="info-box__note">
          En créant ce compte, vous confirmez que cet utilisateur a accepté l'Accord de Confidentialité des Données ImmoSN.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ajouter-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  max-width: 900px;
}

/* En-tête */
.back-link {
  display: inline-block;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 0.75rem;
  text-decoration: none;
  transition: opacity 0.15s;
}
.back-link:hover { opacity: 0.75; }

.ajouter-header__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.ajouter-header__subtitle {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  margin-top: 0.3rem;
}

/* Body */
.ajouter-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.5rem;
  align-items: start;
}

/* Formulaire */
.ajouter-form {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2rem;
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field__label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text);
}

.required { color: var(--color-accent); }

.field__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid var(--color-border-solid);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: var(--color-card);
  transition: border-color 0.2s;
  width: 100%;
}

.field__input:focus { border-color: var(--color-primary); }
.field__input--error { border-color: var(--color-accent); }

.field__password-wrap {
  position: relative;
}

.field__password-wrap .field__input {
  padding-right: 2.5rem;
}

.field__eye {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  padding: 0;
}

.field__error {
  font-size: 0.78rem;
  color: var(--color-accent);
}

.field__hint {
  font-size: 0.78rem;
  color: var(--color-text-secondary);
  font-style: italic;
}

/* Boutons */
.ajouter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn-cancel {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-size: 0.88rem;
  font-weight: 600;
  transition: background 0.15s;
}
.btn-cancel:hover { background: rgba(74, 124, 111, 0.08); }

.btn-confirm {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  transition: background 0.15s;
}
.btn-confirm:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }

/* Info box — dans la charte (vert sauge teinté) */
.info-box {
  background: var(--color-info-bg);
  border: 1px solid var(--color-info-border);
  border-radius: var(--radius);
  padding: 1.5rem;
}

.info-box__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.info-box__icon { font-size: 1.1rem; }

.info-box__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-info-title);
}

.info-box__text {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.75;
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.info-box__note {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-style: italic;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .ajouter-body { grid-template-columns: 1fr; }
}
</style>
