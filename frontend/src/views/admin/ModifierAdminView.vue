<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RotateCcw, UserX } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toastStore'
import authService from '@/services/authService'

const route  = useRoute()
const router = useRouter()
const toast  = useToastStore()

const admin   = ref(null)
const loading = ref(true)
const acting  = ref(false)

// Chargement de l'admin — GET /auth/admins retourne la liste paginée
// On cherche l'admin par id dans la liste complète
onMounted(async () => {
  try {
    const res  = await authService.getAdmins(0, 200)
    const list = res.data?.content ?? res.data?.data ?? []
    admin.value = list.find(a => String(a.id) === String(route.params.id)) || null
    if (!admin.value) toast.error('Administrateur introuvable.')
  } catch {
    toast.error('Erreur lors du chargement.')
  } finally {
    loading.value = false
  }
})

// Archiver — PATCH /auth/admins/{id}/archive
async function handleArchive() {
  if (!confirm('Archiver cet administrateur ?')) return
  acting.value = true
  try {
    await authService.archiveAdmin(admin.value.id)
    toast.success('Administrateur archivé ✓')
    router.push('/admin/administrateurs')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de l\'archivage.')
  } finally {
    acting.value = false
  }
}

// Restaurer — PATCH /auth/admins/{id}/restore
async function handleRestore() {
  acting.value = true
  try {
    await authService.restoreAdmin(admin.value.id)
    toast.success('Administrateur restauré ✓')
    router.push('/admin/administrateurs')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la restauration.')
  } finally {
    acting.value = false
  }
}

// Révoquer — PATCH /auth/admins/{id}/revoke
async function handleRevoke() {
  if (!confirm('Révoquer le rôle admin de cet utilisateur ?')) return
  acting.value = true
  try {
    await authService.revokeAdmin(admin.value.id)
    toast.success('Rôle admin révoqué ✓')
    router.push('/admin/administrateurs')
  } catch (err) {
    toast.error(err.response?.data?.message || 'Erreur lors de la révocation.')
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
      <!-- Fiche info -->
      <div class="ma-card">
        <h2 class="ma-card__title">Informations</h2>

        <div class="ma-avatar">
          {{ admin.nomComplet?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || '?' }}
        </div>

        <div class="ma-fields">
          <div class="ma-field">
            <span class="ma-field__label">Nom complet</span>
            <span class="ma-field__value">{{ admin.nomComplet }}</span>
          </div>
          <div class="ma-field">
            <span class="ma-field__label">Adresse e-mail</span>
            <span class="ma-field__value">{{ admin.email }}</span>
          </div>
          <div class="ma-field">
            <span class="ma-field__label">Téléphone</span>
            <span class="ma-field__value">{{ admin.telephone || '—' }}</span>
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
            <span class="badge" :class="admin.archived ? 'badge--neutral' : 'badge--active'">
              {{ admin.archived ? 'Archivé' : 'Actif' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="ma-card ma-actions-card">
        <h2 class="ma-card__title">Actions</h2>
        <p class="ma-actions-note">
          La modification directe d'un profil admin n'est pas disponible.<br/>
          L'administrateur peut modifier son propre profil depuis ses paramètres.
        </p>

        <!-- Restaurer si archivé -->
        <button
          v-if="admin.archived"
          class="ma-btn ma-btn--restore"
          :disabled="acting"
          @click="handleRestore"
        >
          <RotateCcw :size="16" />
          Restaurer l'accès
        </button>

        <!-- Archiver si actif -->
        <button
          v-else
          class="ma-btn ma-btn--archive"
          :disabled="acting"
          @click="handleArchive"
        >
          <UserX :size="16" />
          Archiver l'accès
        </button>

        <!-- Révoquer le rôle admin -->
        <button
          v-if="!admin.archived"
          class="ma-btn ma-btn--revoke"
          :disabled="acting"
          @click="handleRevoke"
        >
          <UserX :size="16" />
          Révoquer le rôle Admin
        </button>

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

/* Badges */
.badge { padding: 0.25rem 0.65rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; }
.badge--active  { background: #d1fae5; color: #059669; }
.badge--neutral { background: #f3f4f6; color: #6b7280; }

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

.ma-btn--archive { background: var(--color-accent); color: #fff; }
.ma-btn--archive:hover:not(:disabled) { opacity: 0.85; }

.ma-btn--revoke {
  background: transparent; color: #dc2626;
  border: 1.5px solid #dc2626;
}
.ma-btn--revoke:hover:not(:disabled) { background: #fee2e2; }

.ma-btn--cancel {
  background: transparent; color: var(--color-text);
  border: 1px solid var(--color-border);
}
.ma-btn--cancel:hover { background: var(--color-background); }

/* Spinner */
.spinner { width: 36px; height: 36px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .ma-grid { grid-template-columns: 1fr; }
}
</style>
