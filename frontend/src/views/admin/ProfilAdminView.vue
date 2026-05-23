<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import api from '@/services/api'

const authStore = useAuthStore()
const toast = ref(null)

const formInfo = reactive({ nomComplet: '', email: '', telephone: '', langue: 'fr' })
const formSecurite = reactive({ motDePasseActuel: '', nouveauMotDePasse: '' })
const loadingInfo = ref(false)
const loadingSecurite = ref(false)

onMounted(() => {
  if (authStore.user) {
    formInfo.nomComplet = authStore.user.nomComplet || ''
    formInfo.email      = authStore.user.email      || ''
    formInfo.telephone  = authStore.user.telephone  || ''
  }
})

async function saveInfo() {
  loadingInfo.value = true
  try {
    await api.put('/auth/profile', {
      nomComplet: formInfo.nomComplet,
      email:      formInfo.email,
      telephone:  formInfo.telephone,
    })
    await authStore.fetchProfile()
    toast.value.show('Informations mises à jour.', 'success')
  } catch (err) {
    toast.value.show(err.response?.data?.message || 'Erreur lors de la mise à jour.', 'error')
  } finally {
    loadingInfo.value = false
  }
}

async function saveSecurite() {
  if (!formSecurite.motDePasseActuel || !formSecurite.nouveauMotDePasse) {
    toast.value.show('Veuillez remplir les deux champs.', 'error')
    return
  }
  loadingSecurite.value = true
  try {
    await api.put('/auth/profile', {
      motDePasseActuel: formSecurite.motDePasseActuel,
      motDePasse:       formSecurite.nouveauMotDePasse,
    })
    formSecurite.motDePasseActuel  = ''
    formSecurite.nouveauMotDePasse = ''
    toast.value.show('Mot de passe mis à jour.', 'success')
  } catch (err) {
    toast.value.show(err.response?.data?.message || 'Erreur lors de la mise à jour.', 'error')
  } finally {
    loadingSecurite.value = false
  }
}
</script>

<template>
  <div class="profil-admin">
    <ToastNotification ref="toast" />

    <h1 class="profil-admin__title">Gestion du profil</h1>

    <div class="profil-grid">
      <!-- Colonne gauche -->
      <div class="profil-left">

        <!-- Informations de base -->
        <section class="profil-card">
          <h2 class="profil-card__title">Informations de base</h2>
          <form class="profil-card__form" @submit.prevent="saveInfo">

            <div class="field">
              <label class="field__label">NOM COMPLET</label>
              <input v-model="formInfo.nomComplet" type="text" class="field__input" placeholder="Abdoulaye Diop" />
            </div>

            <div class="field">
              <label class="field__label">ADRESSE E-MAIL</label>
              <input v-model="formInfo.email" type="email" class="field__input" />
            </div>

            <div class="field">
              <label class="field__label">TÉLÉPHONE</label>
              <input v-model="formInfo.telephone" type="text" class="field__input" placeholder="+221 77 000 00 00" />
            </div>

            <div class="field">
              <label class="field__label">LANGUE PRÉFÉRÉE</label>
              <select v-model="formInfo.langue" class="field__input">
                <option value="fr">Français (Standard)</option>
                <option value="wo">Wolof</option>
                <option value="en">English</option>
              </select>
            </div>

            <button type="submit" class="btn-save" :disabled="loadingInfo">
              {{ loadingInfo ? 'Enregistrement...' : 'Enregistrer' }}
            </button>
          </form>
        </section>

        <!-- Sécurité -->
        <section class="profil-card">
          <h2 class="profil-card__title">Sécurité du compte</h2>
          <form class="profil-card__form" @submit.prevent="saveSecurite">

            <div class="field">
              <label class="field__label">MOT DE PASSE ACTUEL</label>
              <input v-model="formSecurite.motDePasseActuel" type="password" class="field__input" placeholder="••••••••" />
            </div>

            <div class="field">
              <label class="field__label">NOUVEAU MOT DE PASSE</label>
              <input v-model="formSecurite.nouveauMotDePasse" type="password" class="field__input" placeholder="••••••••" />
            </div>

            <button type="submit" class="btn-update" :disabled="loadingSecurite">
              {{ loadingSecurite ? 'Mise à jour...' : 'Mettre à jour' }}
            </button>
          </form>
        </section>
      </div>

      <!-- Colonne droite -->
      <div class="profil-right">
        <section class="profil-card profil-card--center">
          <div class="profil-avatar">
            {{ authStore.user?.nomComplet?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <h3 class="profil-user__name">{{ authStore.user?.nomComplet }}</h3>
          <p class="profil-user__email">{{ authStore.user?.email }}</p>
          <p class="profil-user__phone">{{ authStore.user?.telephone }}</p>
          <span class="profil-user__role">{{ authStore.user?.role }}</span>
        </section>

        <section class="profil-card">
          <h2 class="profil-card__title">Besoin d'aide ?</h2>
          <ul class="profil-help">
            <li><a href="#" class="profil-help__link">💬 Contacter le support</a></li>
            <li><a href="#" class="profil-help__link">📖 Guide d'utilisation</a></li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profil-admin { display: flex; flex-direction: column; gap: 1.75rem; }

.profil-admin__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.profil-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 1.5rem;
  align-items: start;
}

.profil-left, .profil-right {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profil-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.75rem;
  box-shadow: var(--shadow-card);
}

.profil-card--center { text-align: center; }

.profil-card__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.profil-card__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field { display: flex; flex-direction: column; gap: 0.4rem; }

.field__label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text);
}

.field__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid #E8E0D4;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color 0.2s;
}

.field__input:focus { border-color: var(--color-primary); }

.btn-save {
  padding: 0.6rem 1.25rem;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  align-self: flex-start;
  transition: background 0.15s;
}
.btn-save:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-update {
  padding: 0.6rem 1.25rem;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  align-self: flex-start;
  transition: background 0.15s;
}
.btn-update:hover:not(:disabled) { background: var(--color-accent-hover); }
.btn-update:disabled { opacity: 0.6; cursor: not-allowed; }

/* Avatar */
.profil-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 1.8rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.profil-user__name { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.profil-user__email, .profil-user__phone { font-size: 0.82rem; color: #6B7280; margin-top: 0.2rem; }

.profil-user__role {
  display: inline-block;
  margin-top: 0.75rem;
  background: rgba(74, 124, 111, 0.12);
  color: var(--color-primary);
  padding: 0.2rem 0.65rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.profil-help { list-style: none; display: flex; flex-direction: column; gap: 0.75rem; }
.profil-help__link { color: var(--color-primary); font-size: 0.88rem; font-weight: 500; }
.profil-help__link:hover { text-decoration: underline; }

@media (max-width: 900px) {
  .profil-grid { grid-template-columns: 1fr; }
}
</style>
