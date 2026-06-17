<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import authService from '@/services/authService'
import ButtonPrimary from '@/components/ButtonPrimary.vue'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const nouveau = ref('')
const confirmation = ref('')
const saving = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (nouveau.value.length < 8) {
    error.value = 'Le mot de passe doit contenir au moins 8 caractères.'
    return
  }
  if (nouveau.value !== confirmation.value) {
    error.value = 'La confirmation ne correspond pas.'
    return
  }
  saving.value = true
  try {
    // Compte à mot de passe temporaire : pas besoin du mot de passe actuel
    await authService.changePasswordForced(nouveau.value, confirmation.value)
    // Recharge le profil → mustChangePassword repasse à false
    await authStore.fetchProfile()
    toastStore.success('Mot de passe mis à jour. Bienvenue !')
    if (authStore.role === 'ADMIN' || authStore.role === 'SUPER_ADMIN') {
      router.push('/admin/dashboard')
    } else {
      router.push('/annonces')
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Erreur lors du changement de mot de passe.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="fl-page">
    <div class="fl-card">
      <h1 class="fl-title">Première connexion</h1>
      <p class="fl-sub">
        Votre compte a été créé par l'agence avec un mot de passe temporaire.
        Pour des raisons de sécurité, veuillez définir un nouveau mot de passe.
      </p>

      <div class="fl-field">
        <label>Nouveau mot de passe</label>
        <input v-model="nouveau" type="password" placeholder="Au moins 8 caractères" @keydown.enter="submit" />
      </div>
      <div class="fl-field">
        <label>Confirmer le mot de passe</label>
        <input v-model="confirmation" type="password" @keydown.enter="submit" />
      </div>

      <p v-if="error" class="fl-err">{{ error }}</p>

      <ButtonPrimary full-width :disabled="saving || !nouveau || !confirmation" @click="submit">
        {{ saving ? 'Enregistrement…' : 'Définir mon mot de passe' }}
      </ButtonPrimary>
    </div>
  </div>
</template>

<style scoped>
.fl-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  padding: 1.5rem; background: var(--color-background);
}
.fl-card {
  background: var(--color-card); border-radius: var(--radius);
  border: 1px solid var(--color-border); box-shadow: var(--shadow-card);
  padding: 2rem; width: 100%; max-width: 420px;
}
.fl-title { font-size: 1.4rem; font-weight: 800; color: var(--color-primary); margin-bottom: .5rem; }
.fl-sub { font-size: .88rem; color: var(--color-text-muted); line-height: 1.6; margin-bottom: 1.5rem; }
.fl-field { display: flex; flex-direction: column; gap: .35rem; margin-bottom: 1rem; }
.fl-field label { font-size: .78rem; font-weight: 600; opacity: .7; }
.fl-field input {
  width: 100%; box-sizing: border-box; padding: .65rem .8rem;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: .9rem; background: var(--color-background); color: var(--color-text);
}
.fl-field input:focus { border-color: var(--color-primary); outline: none; }
.fl-err { font-size: .82rem; color: var(--color-accent); margin-bottom: .75rem; }
</style>
