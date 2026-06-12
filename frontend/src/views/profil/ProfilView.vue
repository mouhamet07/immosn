<script setup>
import { ref, reactive, onMounted } from 'vue'

import { Camera, Eye, EyeOff } from 'lucide-vue-next'
import InputField from '@/components/InputField.vue'
import PhoneInput from '@/components/PhoneInput.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import { useAuthStore } from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import { uploadImage } from '@/services/cloudinaryService'
import api from '@/services/api'

const authStore = useAuthStore()
const toast = useToastStore()

// Avatar
const previewUrl = ref(null)
const avatarFile = ref(null)

function getInitials(name) {
  if (!name) return '?'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}

const handleAvatarChange = (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    toast.error('Photo trop lourde — max 2 Mo')
    return
  }
  avatarFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

const uploadAvatar = async () => {
  if (!avatarFile.value) return
  try {
    // Upload Cloudinary depuis le frontend
    const url = await uploadImage(avatarFile.value)
    // Sauvegarder l'URL via PUT /auth/profile — champ photo de UpdateProfileRequestDto
    await api.put('/auth/profile', { photo: url })
    authStore.user.photo = url
    localStorage.setItem('user', JSON.stringify(authStore.user))
  } catch {
    toast.error('Erreur upload photo')
  }
}

// Formulaire informations de base
const formInfo = reactive({
  nomComplet: '',
  email: '',
  telephone: '',
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

// Toggle visibilité mot de passe
const showCurrentPwd = ref(false)
const showNewPwd = ref(false)

// Pré-remplir depuis le store
onMounted(() => {
  if (authStore.user) {
    formInfo.nomComplet = authStore.user.nomComplet || ''
    formInfo.email      = authStore.user.email      || ''
    formInfo.telephone  = authStore.user.telephone  || ''
  }
})

async function saveInfo() {
  loadingInfo.value = true
  successInfo.value = ''
  errorInfo.value   = ''
  try {
    // Upload avatar si un nouveau fichier est sélectionné
    await uploadAvatar()
    // PUT /api/v1/auth/profile — champs exacts de UpdateProfileRequestDto
    const res = await api.put('/auth/profile', {
      nomComplet: formInfo.nomComplet,
      email:      formInfo.email,
      telephone:  formInfo.telephone,
    })
    const updated = res.data.data
    authStore.user = { ...authStore.user, ...updated }
    localStorage.setItem('user', JSON.stringify(authStore.user))
    toast.success('Informations mises à jour avec succès.')
    successInfo.value = 'Informations mises à jour avec succès.'
  } catch (err) {
    const msg = err.response?.data?.message || 'Erreur lors de la mise à jour.'
    errorInfo.value = msg
    toast.error(msg)
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
  errorSecurite.value   = ''
  try {
    // PUT /api/v1/auth/profile — champs mot de passe de UpdateProfileRequestDto
    await api.put('/auth/profile', {
      motDePasseActuel:  formSecurite.motDePasseActuel,
      nouveauMotDePasse: formSecurite.nouveauMotDePasse,
    })
    formSecurite.motDePasseActuel  = ''
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
              <InputField v-model="formInfo.email" label="Adresse e-mail" type="email" required />
              
              <div class="profil-field">
                <label class="profil-field__label">Numéro de téléphone</label>
                <PhoneInput v-model="formInfo.telephone" />
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
              <div class="profil-field">
                <label class="profil-field__label">Mot de passe actuel</label>
                <div class="pwd-field">
                  <input
                    :type="showCurrentPwd ? 'text' : 'password'"
                    v-model="formSecurite.motDePasseActuel"
                    placeholder="Mot de passe actuel"
                    class="profil-pwd-input"
                    autocomplete="current-password"
                  />
                  <button
                    type="button"
                    class="pwd-toggle"
                    @click="showCurrentPwd = !showCurrentPwd"
                    :aria-label="showCurrentPwd ? 'Masquer' : 'Afficher'"
                  >
                    <EyeOff v-if="showCurrentPwd" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </div>
              </div>

              <div class="profil-field">
                <label class="profil-field__label">Nouveau mot de passe</label>
                <div class="pwd-field">
                  <input
                    :type="showNewPwd ? 'text' : 'password'"
                    v-model="formSecurite.nouveauMotDePasse"
                    placeholder="Nouveau mot de passe"
                    class="profil-pwd-input"
                    autocomplete="new-password"
                  />
                  <button
                    type="button"
                    class="pwd-toggle"
                    @click="showNewPwd = !showNewPwd"
                    :aria-label="showNewPwd ? 'Masquer' : 'Afficher'"
                  >
                    <EyeOff v-if="showNewPwd" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </div>
              </div>

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
            <div class="avatar-container">
              <div class="avatar-wrapper">
                <img
                  v-if="previewUrl || authStore.user?.photo"
                  :src="previewUrl || authStore.user.photo"
                  alt="Photo de profil"
                  class="avatar-img"
                />
                <div v-else class="avatar-initials">
                  {{ getInitials(authStore.user?.nomComplet) }}
                </div>
                <label class="avatar-overlay" title="Changer la photo">
                  <Camera :size="18" />
                  <span>Modifier</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    @change="handleAvatarChange"
                    hidden
                  />
                </label>
              </div>
              <div class="avatar-info">
                <p class="avatar-name">{{ authStore.user?.nomComplet }}</p>

              </div>
            </div>
            <p class="profil-user__email">{{ authStore.user?.email }}</p>
            <p class="profil-user__phone">{{ authStore.user?.telephone }}</p>
            <span class="profil-user__role">{{ authStore.user?.role }}</span>
          </section>

          <!-- Besoin d'aide -->
          <section class="profil-card">
            <h2 class="profil-card__title">Besoin d'aide ?</h2>
            <ul class="profil-help">
              <li><a href="#" class="profil-help__link">Contacter le support</a></li>
              <li><a href="#" class="profil-help__link">Guide d'utilisation</a></li>
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

.profil-field { display: flex; flex-direction: column; gap: 0.4rem; }
.profil-field__label { font-size: 0.8rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text); }

/* Toggle mot de passe */
.pwd-field {
  position: relative;
  display: flex;
  align-items: center;
}
.profil-pwd-input {
  width: 100%;
  padding: 0.75rem 2.75rem 0.75rem 0.9rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--color-text);
  background: #fff;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.profil-pwd-input:focus { border-color: var(--color-primary); outline: none; }
.pwd-toggle {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  transition: color 150ms ease;
}
.pwd-toggle:hover { color: var(--color-primary); }

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
.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
}

.avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-initials {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 1.8rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 0.65rem;
  font-weight: 600;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay { opacity: 1; }

.avatar-info { text-align: center; }
.avatar-name { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.avatar-hint { font-size: 0.75rem; color: var(--color-text-secondary); margin-top: 2px; }

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
