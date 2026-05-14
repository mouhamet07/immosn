<script setup>
import { ref, reactive, onMounted } from 'vue'
import InputField from '@/components/InputField.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import { useAuthStore } from '@/stores/authStore'
import api from '@/services/api'

const authStore = useAuthStore()

// Formulaire informations de base
const formInfo = reactive({
  nomComplet: '',
  email: '',
  telephone: '',
  langue: 'fr',
})

// Formulaire sécurité
const formSecurite = reactive({
  motDePasseActuel: '',
  nouveauMotDePasse: '',
})

const loadingInfo = ref(false)
const loadingSecurite = ref(false)
const successInfo = ref('')
const successSecurite = ref('')
const errorInfo = ref('')
const errorSecurite = ref('')

// Pré-remplir depuis le store
onMounted(() => {
  if (authStore.user) {
    formInfo.nomComplet = authStore.user.nomComplet || ''
    formInfo.email = authStore.user.email || ''
    formInfo.telephone = authStore.user.telephone || ''
  }
})

async function saveInfo() {
  loadingInfo.value = true
  successInfo.value = ''
  errorInfo.value = ''
  try {
    await api.put(`/users/${authStore.user.id}`, {
      nomComplet: formInfo.nomComplet,
      email: formInfo.email,
      telephone: formInfo.telephone,
    })
    await authStore.fetchProfile()
    successInfo.value = 'Informations mises à jour avec succès.'
  } catch (err) {
    errorInfo.value = err.response?.data?.message || 'Erreur lors de la mise à jour.'
  } finally {
    loadingInfo.value = false
  }
}

async function saveSecurite() {
  if (!formSecurite.motDePasseActuel || !formSecurite.nouveauMotDePasse) {
    errorSecurite.value = 'Veuillez remplir les deux champs.'
    return
  }
  loadingSecurite.value = true
  successSecurite.value = ''
  errorSecurite.value = ''
  try {
    await api.put(`/users/${authStore.user.id}`, {
      motDePasseActuel: formSecurite.motDePasseActuel,
      motDePasse: formSecurite.nouveauMotDePasse,
    })
    formSecurite.motDePasseActuel = ''
    formSecurite.nouveauMotDePasse = ''
    successSecurite.value = 'Mot de passe mis à jour avec succès.'
  } catch (err) {
    errorSecurite.value = err.response?.data?.message || 'Erreur lors de la mise à jour.'
  } finally {
    loadingSecurite.value = false
  }
}
</script>

<template>
  <div class="profil-page">
    <main class="profil-main">
      <h1 class="profil-main__title">Gestion du profil</h1>

      <div class="profil-grid">
        <!-- Colonne gauche -->
        <div class="profil-left">
          <!-- Informations de base -->
          <section class="profil-card">
            <h2 class="profil-card__title">Informations de base</h2>
            <form class="profil-card__form" @submit.prevent="saveInfo">
              <InputField v-model="formInfo.nomComplet" label="Nom complet" placeholder="Abdoulaye Diop" required />
              <InputField v-model="formInfo.email" label="Adresse e-mail" type="email" icon="✉️" required />
              <InputField v-model="formInfo.telephone" label="Numéro de téléphone" placeholder="+221 77 000 00 00" icon="📞" />

              <!-- Langue préférée -->
              <div class="profil-select-wrapper">
                <label class="profil-select-label">LANGUE PRÉFÉRÉE</label>
                <select v-model="formInfo.langue" class="profil-select">
                  <option value="fr">Français (Standard)</option>
                  <option value="wo">Wolof</option>
                  <option value="en">English</option>
                </select>
              </div>

              <div v-if="successInfo" class="profil-alert profil-alert--success">{{ successInfo }}</div>
              <div v-if="errorInfo" class="profil-alert profil-alert--error">{{ errorInfo }}</div>

              <ButtonPrimary type="submit" :loading="loadingInfo">Enregistrer</ButtonPrimary>
            </form>
          </section>

          <!-- Sécurité du compte -->
          <section class="profil-card">
            <h2 class="profil-card__title">Sécurité du compte</h2>
            <form class="profil-card__form" @submit.prevent="saveSecurite">
              <InputField v-model="formSecurite.motDePasseActuel" label="Mot de passe actuel" type="password" icon="🔒" />
              <InputField v-model="formSecurite.nouveauMotDePasse" label="Nouveau mot de passe" type="password" icon="🔑" />

              <div v-if="successSecurite" class="profil-alert profil-alert--success">{{ successSecurite }}</div>
              <div v-if="errorSecurite" class="profil-alert profil-alert--error">{{ errorSecurite }}</div>

              <ButtonPrimary type="submit" variant="accent" :loading="loadingSecurite">Mettre à jour</ButtonPrimary>
            </form>
          </section>
        </div>

        <!-- Colonne droite -->
        <div class="profil-right">
          <!-- Carte avatar -->
          <section class="profil-card profil-card--center">
            <div class="profil-avatar">
              {{ authStore.user?.nomComplet?.charAt(0)?.toUpperCase() || '?' }}
            </div>
            <h3 class="profil-user__name">{{ authStore.user?.nomComplet }}</h3>
            <p class="profil-user__email">{{ authStore.user?.email }}</p>
            <p class="profil-user__phone">{{ authStore.user?.telephone }}</p>
            <span class="profil-user__role">{{ authStore.user?.role }}</span>
          </section>

          <!-- Besoin d'aide -->
          <section class="profil-card">
            <h2 class="profil-card__title">Besoin d'aide ?</h2>
            <ul class="profil-help">
              <li><a href="#" class="profil-help__link">💬 Contacter le support</a></li>
              <li><a href="#" class="profil-help__link">📖 Guide d'utilisation</a></li>
            </ul>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.profil-page {
  background: var(--color-background);
}

.profil-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.profil-main__title {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--color-text);
  margin-bottom: 2rem;
}

.profil-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  align-items: start;
}

.profil-left {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profil-right {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Carte */
.profil-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.75rem;
  box-shadow: var(--shadow-card);
}

.profil-card--center {
  text-align: center;
}

.profil-card__title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.profil-card__form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

/* Select langue */
.profil-select-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.profil-select-label {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text);
}

.profil-select {
  padding: 0.75rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 0.95rem;
  color: var(--color-text);
  transition: border-color 0.2s;
}

.profil-select:focus {
  border-color: var(--color-primary);
}

/* Alertes */
.profil-alert {
  padding: 0.75rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
}

.profil-alert--success {
  background: rgba(74, 124, 111, 0.1);
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
}

.profil-alert--error {
  background: rgba(212, 113, 74, 0.1);
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
}

/* Avatar */
.profil-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 2rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.profil-user__name {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
}

.profil-user__email,
.profil-user__phone {
  font-size: 0.88rem;
  color: var(--color-text);
  opacity: 0.6;
  margin-top: 0.25rem;
}

.profil-user__role {
  display: inline-block;
  margin-top: 0.75rem;
  background: rgba(74, 124, 111, 0.12);
  color: var(--color-primary);
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

/* Aide */
.profil-help {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.profil-help__link {
  color: var(--color-primary);
  font-size: 0.9rem;
  font-weight: 500;
  transition: opacity 0.2s;
}

.profil-help__link:hover {
  opacity: 0.75;
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 900px) {
  .profil-grid {
    grid-template-columns: 1fr;
  }
}
</style>
