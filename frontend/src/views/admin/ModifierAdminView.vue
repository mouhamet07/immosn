<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RotateCcw, ShieldOff, Save } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toastStore'
import authService from '@/services/authService'
import { getErrorMessage } from '@/utils/messages'
import StatusBadge from '@/components/StatusBadge.vue'
import PhoneInput from '@/components/PhoneInput.vue'

const route  = useRoute()
const router = useRouter()
const toast  = useToastStore()

const admin   = ref(null)
const loading = ref(true)
const acting  = ref(false)
const saving  = ref(false)

// Formulaire d'édition — champs du AdminUpdateRequestDto (tous optionnels)
const form = reactive({
  nomComplet: '',
  email: '',
  telephone: '',
  nouveauMotDePasse: '',
})

// Chargement de l'admin — GET /auth/admins retourne la liste paginée
// On cherche l'admin par id dans la liste complète
onMounted(async () => {
  try {
    const res  = await authService.getAdmins(0, 200)
    const list = res.data?.content ?? res.data?.data ?? []
    admin.value = list.find(a => String(a.id) === String(route.params.id)) || null
    if (!admin.value) {
      toast.error('Administrateur introuvable.')
    } else {
      form.nomComplet = admin.value.nomComplet || ''
      form.email      = admin.value.email      || ''
      form.telephone  = admin.value.telephone  || ''
    }
  } catch {
    toast.error('Erreur lors du chargement.')
  } finally {
    loading.value = false
  }
})

// Enregistrer les modifications — PUT /auth/admins/{id}
async function handleSave() {
  // N'envoyer que les champs réellement renseignés
  const payload = {
    nomComplet: form.nomComplet.trim(),
    email:      form.email.trim(),
    telephone:  form.telephone.trim(),
  }
  if (form.nouveauMotDePasse) payload.nouveauMotDePasse = form.nouveauMotDePasse

  saving.value = true
  try {
    const res = await authService.updateAdmin(admin.value.id, payload)
    admin.value = { ...admin.value, ...(res.data?.data ?? {}) }
    form.nouveauMotDePasse = ''
    toast.success('Administrateur modifié ✓')
  } catch (err) {
    // getErrorMessage extrait les messages détaillés de validation (champ data)
    toast.error(getErrorMessage(err))
  } finally {
    saving.value = false
  }
}

// Révoquer l'accès administrateur — PATCH /auth/admins/{id}/archive
// Opération réversible : l'admin révoqué est simplement archivé (ne peut plus
// se connecter) et conserve son rôle. Son accès est réactivable via « Restaurer ».
async function handleRevoke() {
  if (!confirm('Révoquer l\'accès administrateur de cet utilisateur ? Il ne pourra plus se connecter, mais vous pourrez restaurer son accès à tout moment.')) return
  acting.value = true
  try {
    await authService.archiveAdmin(admin.value.id)
    toast.success('Accès administrateur révoqué ✓')
    router.push('/admin/administrateurs')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la révocation de l\'accès.')
  } finally {
    acting.value = false
  }
}

// Restaurer l'accès administrateur — PATCH /auth/admins/{id}/restore
async function handleRestore() {
  acting.value = true
  try {
    await authService.restoreAdmin(admin.value.id)
    toast.success('Accès administrateur restauré ✓')
    router.push('/admin/administrateurs')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la restauration de l\'accès.')
  } finally {
    acting.value = false
  }
}

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}
</script>

<template>
  <div class="ma-page">
    <!-- En-tête -->
    <div class="ma-header">
      <button class="ma-back" @click="router.push('/admin/administrateurs')">
        <ArrowLeft :size="18" />
      </button>
      <div>
        <h1 class="ma-title">Détail administrateur</h1>
        <p class="ma-sub">Consultez et gérez cet accès administrateur.</p>
      </div>
    </div>

    <div v-if="loading" class="ma-loading"><div class="spinner"></div></div>

    <div v-else-if="!admin" class="ma-error">Administrateur introuvable.</div>

    <div v-else class="ma-grid">
      <!-- Formulaire d'édition -->
      <form class="ma-card" @submit.prevent="handleSave">
        <h2 class="ma-card__title">Informations</h2>

        <div class="ma-avatar">
          {{ admin.nomComplet?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || '?' }}
        </div>

        <div class="ma-form">
          <div class="ma-form__field">
            <label class="ma-form__label">Nom complet</label>
            <input v-model="form.nomComplet" type="text" class="ma-form__input" placeholder="ex. Mamadou Diop" />
          </div>
          <div class="ma-form__field">
            <label class="ma-form__label">Adresse e-mail</label>
            <input v-model="form.email" type="email" class="ma-form__input" placeholder="mamadou@immosn.sn" />
          </div>
          <div class="ma-form__field">
            <label class="ma-form__label">Téléphone</label>
            <PhoneInput v-model="form.telephone" />
          </div>
          <div class="ma-form__field">
            <label class="ma-form__label">Nouveau mot de passe <span class="ma-form__optional">(optionnel)</span></label>
            <input v-model="form.nouveauMotDePasse" type="password" class="ma-form__input" placeholder="Laisser vide pour ne pas changer" autocomplete="new-password" />
            <span class="ma-form__hint">Renseignez ce champ uniquement pour réinitialiser le mot de passe de l'administrateur.</span>
          </div>

          <div class="ma-field">
            <span class="ma-field__label">Rôle</span>
            <span class="ma-field__value">{{ Array.from(admin.roles ?? []).join(', ') }}</span>
          </div>
          <div class="ma-field">
            <span class="ma-field__label">Date de création</span>
            <span class="ma-field__value">{{ formatDate(admin.creationDate) }}</span>
          </div>
          <div class="ma-field">
            <span class="ma-field__label">Statut</span>
            <StatusBadge :label="admin.archived ? 'Accès révoqué' : 'Actif'" :variant="admin.archived ? 'neutral' : 'success'" />
          </div>
        </div>

        <button type="submit" class="ma-btn ma-btn--save" :disabled="saving">
          <Save :size="16" />
          {{ saving ? 'Enregistrement...' : 'Enregistrer les modifications' }}
        </button>
      </form>

      <!-- Actions -->
      <div class="ma-card ma-actions-card">
        <h2 class="ma-card__title">Actions</h2>

        <!-- Restaurer si l'accès est révoqué (compte archivé) -->
        <button
          v-if="admin.archived"
          class="ma-btn ma-btn--restore"
          :disabled="acting"
          @click="handleRestore"
        >
          <RotateCcw :size="16" />
          Restaurer l'accès administrateur
        </button>
        <p v-if="admin.archived" class="ma-action-hint">
          Réactive le compte : l'administrateur pourra de nouveau se connecter et retrouve ses privilèges.
        </p>

        <!-- Révoquer l'accès si actif (réversible — logique d'archivage) -->
        <button
          v-else
          class="ma-btn ma-btn--revoke"
          :disabled="acting"
          @click="handleRevoke"
        >
          <ShieldOff :size="16" />
          Révoquer l'accès administrateur
        </button>
        <p v-if="!admin.archived" class="ma-action-hint">
          Suspend l'accès : l'administrateur ne pourra plus se connecter. Opération réversible via « Restaurer ».
        </p>

        <button class="ma-btn ma-btn--cancel" @click="router.push('/admin/administrateurs')">
          Retour à la liste
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ma-page { display: flex; flex-direction: column; gap: 1.75rem; }

.ma-header { display: flex; align-items: center; gap: 1rem; }
.ma-back {
  background: none; border: 1px solid var(--color-border);
  border-radius: 8px; padding: 0.5rem; cursor: pointer;
  display: flex; align-items: center; color: var(--color-text);
  transition: background 0.15s;
}
.ma-back:hover { background: var(--color-background); }
.ma-title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.ma-sub   { font-size: 0.85rem; color: var(--color-text-muted); margin-top: 0.2rem; }

.ma-loading {
  display: flex; justify-content: center; padding: 4rem;
}
.ma-error {
  text-align: center; padding: 3rem; color: var(--color-accent);
}

.ma-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.5rem;
  align-items: start;
}

.ma-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.75rem;
  box-shadow: var(--shadow-card);
}
.ma-card__title {
  font-size: 1rem; font-weight: 700;
  color: var(--color-text);
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

/* Avatar */
.ma-avatar {
  width: 64px; height: 64px; border-radius: 50%;
  background: var(--color-primary); color: #fff;
  font-size: 1.5rem; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 1.5rem;
}

/* Formulaire d'édition */
.ma-form { display: flex; flex-direction: column; gap: 1rem; }
.ma-form__field { display: flex; flex-direction: column; gap: 0.4rem; }
.ma-form__label {
  font-size: 0.78rem; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--color-text-muted);
}
.ma-form__optional { text-transform: none; font-weight: 500; opacity: 0.8; }
.ma-form__input {
  padding: 0.7rem 0.9rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text);
  background: var(--color-card);
  transition: border-color 0.2s;
  width: 100%; box-sizing: border-box;
}
.ma-form__input:focus { border-color: var(--color-primary); outline: none; }
.ma-form__hint { font-size: 0.75rem; color: var(--color-text-secondary); font-style: italic; }

.ma-btn--save { background: var(--color-primary); color: #fff; margin-top: 1rem; }
.ma-btn--save:hover:not(:disabled) { opacity: 0.9; }

/* Champs */
.ma-fields { display: flex; flex-direction: column; gap: 1rem; }
.ma-field {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0.65rem 0;
  border-bottom: 1px solid var(--color-border);
}
.ma-field:last-child { border-bottom: none; }
.ma-field__label {
  font-size: 0.78rem; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--color-text-muted);
}
.ma-field__value { font-size: 0.9rem; color: var(--color-text); font-weight: 500; }


/* Actions */
.ma-actions-card { display: flex; flex-direction: column; gap: 0.75rem; }
.ma-actions-note {
  font-size: 0.82rem; color: var(--color-text-muted);
  line-height: 1.5; margin-bottom: 0.5rem;
  padding: 0.75rem; background: var(--color-background);
  border-radius: 8px; border: 1px solid var(--color-border);
}

.ma-btn {
  width: 100%; padding: 0.7rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem; font-weight: 600;
  cursor: pointer; display: flex; align-items: center;
  justify-content: center; gap: 6px;
  transition: opacity 0.15s;
  border: none;
}
.ma-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.ma-btn--restore { background: var(--color-primary); color: #fff; }
.ma-btn--restore:hover:not(:disabled) { opacity: 0.85; }

.ma-btn--revoke { background: var(--color-accent); color: #fff; }
.ma-btn--revoke:hover:not(:disabled) { opacity: 0.85; }

.ma-btn--cancel {
  background: transparent; color: var(--color-accent);
  border: 1.5px solid var(--color-accent);
}
.ma-btn--cancel:hover:not(:disabled) { background: rgba(var(--color-accent-rgb), 0.1); }

.ma-action-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: -0.35rem 0 0.35rem;
}

/* Spinner */
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .ma-grid { grid-template-columns: 1fr; }
}
</style>
