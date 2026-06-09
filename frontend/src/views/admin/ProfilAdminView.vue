<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { Camera, Eye, EyeOff } from 'lucide-vue-next'
import PhoneInput from '@/components/PhoneInput.vue'
import { useAuthStore } from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import { uploadImage } from '@/services/cloudinaryService'
import api from '@/services/api'

const authStore = useAuthStore()
const toast     = useToastStore()

const lastConnexionRaw = computed(() => authStore.user?.dernierConnexion ?? authStore.user?.lastLogin)
const sessionsActives = computed(() => authStore.user?.sessionsActives ?? authStore.user?.activeSessions ?? 0)

function formatLastConnexion(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

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
  if (file.size > 2 * 1024 * 1024) { toast.error('Photo trop lourde — max 2 Mo'); return }
  avatarFile.value  = file
  previewUrl.value  = URL.createObjectURL(file)
}

const uploadAvatar = async () => {
  if (!avatarFile.value) return
  try {
    const url = await uploadImage(avatarFile.value)
    await api.put('/auth/profile', { photo: url })
    authStore.user.photo = url
    localStorage.setItem('user', JSON.stringify(authStore.user))
  } catch {
    toast.error('Erreur upload photo')
  }
}

const formInfo     = reactive({ nomComplet: '', email: '', telephone: '' })
const formSecurite = reactive({ motDePasseActuel: '', nouveauMotDePasse: '', confirmationNouveauMotDePasse: '' })
const loadingInfo      = ref(false)
const loadingSecurite  = ref(false)

// Toggle visibilité mot de passe
const showCurrentPwd = ref(false)
const showNewPwd     = ref(false)

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
    await uploadAvatar()
    await api.put('/auth/profile', {
      nomComplet: formInfo.nomComplet,
      email:      formInfo.email,
      telephone:  formInfo.telephone,
    })
    await authStore.fetchProfile()
    toast.success('Informations mises à jour.')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la mise à jour.')
  } finally {
    loadingInfo.value = false
  }
}

async function saveSecurite() {
  if (!formSecurite.motDePasseActuel || !formSecurite.nouveauMotDePasse || !formSecurite.confirmationNouveauMotDePasse) {
    toast.error('Veuillez remplir tous les champs du formulaire.')
    return
  }

  if (formSecurite.nouveauMotDePasse !== formSecurite.confirmationNouveauMotDePasse) {
    toast.error('Les deux nouveaux mots de passe ne correspondent pas.')
    return
  }

  loadingSecurite.value = true
  try {
    await api.put('/auth/profile', {
      motDePasseActuel:  formSecurite.motDePasseActuel,
      nouveauMotDePasse: formSecurite.nouveauMotDePasse,
    })
    formSecurite.motDePasseActuel = ''
    formSecurite.nouveauMotDePasse = ''
    formSecurite.confirmationNouveauMotDePasse = ''
    toast.success('Mot de passe mis à jour.')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la mise à jour.')
  } finally {
    loadingSecurite.value = false
  }
}
</script>

<template>
  <div class="profil-admin">
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
            <!-- FIX 1 : téléphone avec sélecteur de pays -->
            <div class="field">
              <label class="field__label">TÉLÉPHONE</label>
              <PhoneInput v-model="formInfo.telephone" />
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
            <!-- FIX 4 : toggle visibilité mot de passe -->
            <div class="field">
              <label class="field__label">MOT DE PASSE ACTUEL</label>
              <div class="pwd-field">
                <input
                  :type="showCurrentPwd ? 'text' : 'password'"
                  v-model="formSecurite.motDePasseActuel"
                  class="field__input"
                  placeholder="••••••••"
                  autocomplete="current-password"
                />
                <button type="button" class="pwd-toggle" @click="showCurrentPwd = !showCurrentPwd">
                  <EyeOff v-if="showCurrentPwd" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
            </div>
            <div class="field">
              <label class="field__label">NOUVEAU MOT DE PASSE</label>
              <div class="pwd-field">
                <input
                  :type="showNewPwd ? 'text' : 'password'"
                  v-model="formSecurite.nouveauMotDePasse"
                  class="field__input"
                  placeholder="••••••••"
                  autocomplete="new-password"
                />
                <button type="button" class="pwd-toggle" @click="showNewPwd = !showNewPwd">
                  <EyeOff v-if="showNewPwd" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
            </div>
            <div class="field">
              <label class="field__label">CONFIRMATION DU NOUVEAU MOT DE PASSE</label>
              <div class="pwd-field">
                <input
                  :type="showNewPwd ? 'text' : 'password'"
                  v-model="formSecurite.confirmationNouveauMotDePasse"
                  class="field__input"
                  placeholder="••••••••"
                  autocomplete="new-password"
                />
                <button type="button" class="pwd-toggle" @click="showNewPwd = !showNewPwd">
                  <EyeOff v-if="showNewPwd" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
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
                <input type="file" accept="image/jpeg,image/png,image/webp" @change="handleAvatarChange" hidden />
              </label>
            </div>
            <div class="avatar-info">
              <p class="avatar-name">{{ authStore.user?.nomComplet }}</p>
              
            </div>
          </div>
          <p class="profil-user__email">{{ authStore.user?.email }}</p>
          <p class="profil-user__phone">{{ authStore.user?.telephone }}</p>
          <span class="profil-user__role">{{ authStore.user?.roles?.[0] }}</span>
        </section>

        <section class="profil-card">
          <h2 class="profil-card__title">Activité du compte</h2>
          <div class="profil-activity">
            <div class="profil-activity__item">
              <span class="profil-activity__label">Dernière connexion</span>
              <span class="profil-activity__value">{{ formatLastConnexion(lastConnexionRaw) ?? 'Non disponible' }}</span>
            </div>
            <div class="profil-activity__item">
              <span class="profil-activity__label">Sessions actives</span>
              <span class="profil-activity__value">{{ sessionsActives }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profil-admin { display: flex; flex-direction: column; gap: 1.75rem; }
.profil-admin__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.profil-grid { display: grid; grid-template-columns: 1fr 300px; gap: 1.5rem; align-items: start; }
.profil-left, .profil-right { display: flex; flex-direction: column; gap: 1.5rem; }
.profil-card { background: var(--color-card); border-radius: var(--radius); padding: 1.75rem; box-shadow: var(--shadow-card); }
.profil-card--center { text-align: center; }
.profil-card__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin-bottom: 1.25rem; padding-bottom: 0.75rem; border-bottom: 1px solid var(--color-border); }
.profil-card__form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field__label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-text); }
.field__input { padding: 0.7rem 0.9rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.9rem; color: var(--color-text); background: #fff; transition: border-color 0.2s; width: 100%; box-sizing: border-box; }
.field__input:focus { border-color: var(--color-primary); outline: none; }

/* Toggle mot de passe */
.pwd-field { position: relative; display: flex; align-items: center; }
.pwd-field .field__input { padding-right: 2.75rem; }
.pwd-toggle { position: absolute; right: 0.75rem; background: none; border: none; cursor: pointer; color: var(--color-text-secondary); display: flex; align-items: center; transition: color 150ms ease; }
.pwd-toggle:hover { color: var(--color-primary); }

.btn-save { padding: 0.6rem 1.25rem; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-sm); font-size: 0.88rem; font-weight: 600; align-self: flex-start; transition: background 0.15s; }
.btn-save:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-update { padding: 0.6rem 1.25rem; background: var(--color-accent); color: #fff; border: none; border-radius: var(--radius-sm); font-size: 0.88rem; font-weight: 600; align-self: flex-start; transition: background 0.15s; }
.btn-update:hover:not(:disabled) { background: var(--color-accent-hover); }
.btn-update:disabled { opacity: 0.6; cursor: not-allowed; }

/* Avatar */
.avatar-container { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
.avatar-wrapper { position: relative; width: 72px; height: 72px; }
.avatar-img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; }
.avatar-initials { width: 72px; height: 72px; border-radius: 50%; background: var(--color-primary); color: #fff; font-size: 1.8rem; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.avatar-overlay { position: absolute; inset: 0; border-radius: 50%; background: rgba(0,0,0,0.45); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 0.65rem; font-weight: 600; cursor: pointer; opacity: 0; transition: opacity 0.2s; }
.avatar-wrapper:hover .avatar-overlay { opacity: 1; }
.avatar-info { text-align: center; }
.avatar-name { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.avatar-hint { font-size: 0.72rem; color: var(--color-text-secondary); margin-top: 2px; }
.profil-user__email, .profil-user__phone { font-size: 0.82rem; color: var(--color-text-secondary); margin-top: 0.2rem; }
.profil-user__role { display: inline-block; margin-top: 0.75rem; background: rgba(74,124,111,0.12); color: var(--color-primary); padding: 0.2rem 0.65rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
.profil-help { list-style: none; display: flex; flex-direction: column; gap: 0.75rem; }
.profil-help__link { color: var(--color-primary); font-size: 0.88rem; font-weight: 500; display: flex; align-items: center; gap: 0.4rem; transition: opacity 0.15s; }
.profil-help__link:hover { text-decoration: underline; }

.profil-activity { display: grid; gap: 1rem; }
.profil-activity__item { display: flex; justify-content: space-between; align-items: center; padding: 0.85rem 1rem; background: rgba(245, 247, 249, 0.95); border: 1px solid rgba(0, 0, 0, 0.06); border-radius: 12px; }
.profil-activity__label { color: #4f4f4f; font-size: 0.92rem; font-weight: 600; }
.profil-activity__value { color: var(--color-primary); font-size: 0.92rem; font-weight: 700; white-space: nowrap; }

@media (max-width: 900px) { .profil-grid { grid-template-columns: 1fr; } }
</style>
