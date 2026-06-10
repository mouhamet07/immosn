<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import StatsCard from '@/components/admin/StatsCard.vue'
import ConfirmModal from '@/components/admin/ConfirmModal.vue'
import ToastNotification from '@/components/admin/ToastNotification.vue'
import { usePagination } from '@/composables/usePagination'

const router = useRouter()
const toast = ref(null)

const admins = ref([])
const loading = ref(true)
const confirmId = ref(null)

const { currentPage, totalPages, paginated, rangeLabel, prev, next } = usePagination(admins, 4)

// AuthResponseDto: id, nomComplet, email, telephone, creationDate, archived, roles (Set<String>)
// Normalise les champs pour l'affichage dans la vue
function normalize(admin) {
  return {
    ...admin,
    role:     Array.from(admin.roles ?? []).join(', '),
    statut:   admin.archived ? 'INACTIF' : 'ACTIF',
    dateAjout: admin.creationDate,
    photo:    admin.photo || null,
  }
}

const stats = computed(() => ({
  total:   admins.value.length,
  actifs:  admins.value.filter(a => a.statut === 'ACTIF').length,
  attente: admins.value.filter(a => a.statut === 'INACTIF').length,
}))

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(name) {
  if (!name) return '?'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}

onMounted(async () => {
  try {
    const response = await authService.getAdmins(0, 100)
    // PagedResponse: { data: [...], totalElements, totalPages, currentPage }
    admins.value = (response.data?.data ?? []).map(normalize)
  } catch (err) {
    toast.value?.show(err.response?.data?.message || 'Erreur lors du chargement des administrateurs.', 'error')
  } finally {
    loading.value = false
  }
})

async function revokeAccess() {
  try {
    // PATCH /auth/admins/{id}/revoke — révoque le rôle ADMIN
    await authService.revokeAdmin(confirmId.value)
    const idx = admins.value.findIndex(a => a.id === confirmId.value)
    if (idx !== -1) admins.value[idx].statut = 'INACTIF'
    toast.value.show('Accès révoqué avec succès.', 'success')
  } catch (err) {
    toast.value.show(err.userMessage || err.response?.data?.message || 'Erreur lors de la révocation.', 'error')
  } finally {
    confirmId.value = null
  }
}

async function restoreAccess(id) {
  try {
    await authService.restoreAdmin(id)
    const idx = admins.value.findIndex(a => a.id === id)
    if (idx !== -1) admins.value[idx].statut = 'ACTIF'
    toast.value.show('Accès restauré avec succès.', 'success')
  } catch (err) {
    toast.value.show(err.userMessage || err.response?.data?.message || 'Erreur lors de la restauration.', 'error')
  }
}
</script>

<template>
  <div class="admins-page">
    <ToastNotification ref="toast" />

    <ConfirmModal
      v-if="confirmId"
      title="Révoquer l'accès"
      message="Cette action supprimera définitivement l'accès de cet administrateur. Voulez-vous continuer ?"
      @confirm="revokeAccess"
      @cancel="confirmId = null"
    />

    <!-- En-tête -->
    <div class="admins-header">
      <div>
        <h1 class="admins-header__title">Administrateurs de la plateforme</h1>
        <p class="admins-header__subtitle">Gérez les accès système et les permissions de votre équipe.</p>
      </div>
      <button class="btn-add" @click="router.push('/admin/administrateurs/ajouter')">
        + Ajouter un admin
      </button>
    </div>

    <!-- Stats -->
    <div class="admins-stats">
      <StatsCard label="Total admins"    :value="stats.total"   color="primary" />
      <StatsCard label="Actifs"          :value="stats.actifs"  color="primary" />
      <StatsCard label="En attente"      :value="stats.attente" color="accent"  />
    </div>

    <!-- Skeleton loader -->
    <div v-if="loading" class="skeleton-table">
      <div v-for="i in 4" :key="i" class="skeleton-row"></div>
    </div>

    <!-- Table -->
    <div v-else class="admins-table-wrap">
      <table class="admins-table">
        <thead>
          <tr>
            <th>Administrateur</th>
            <th>Adresse e-mail</th>
            <th>Date d'ajout</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="admin in paginated" :key="admin.id" class="admins-table__row">
            <td>
              <div class="admin-identity">
                <div class="admin-avatar">
                  <img v-if="admin.photo" :src="admin.photo" :alt="admin.nomComplet" class="admin-avatar-img" />
                  <span v-else>{{ initials(admin.nomComplet) }}</span>
                </div>
                <div>
                  <div class="admin-name">{{ admin.nomComplet }}</div>
                  <div class="admin-role">{{ Array.from(admin.roles ?? []).join(', ') }}</div>
                </div>
              </div>
            </td>
            <td class="td-email">{{ admin.email }}</td>
            <td class="td-date">{{ formatDate(admin.creationDate) }}</td>
            <td>
              <span
                class="badge"
                :class="{
                  'badge--active':  admin.statut === 'ACTIF',
                  'badge--neutral': admin.statut === 'INACTIF',
                  'badge--pending': admin.statut === 'EN_ATTENTE',
                }"
              >
                {{ admin.statut === 'EN_ATTENTE' ? 'En attente' : admin.statut === 'ACTIF' ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td>
              <div class="td-actions">
                <button class="action-btn" title="Modifier" @click="router.push(`/admin/administrateurs/${admin.id}/modifier`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button v-if="admin.statut === 'INACTIF'" class="action-btn" title="Restaurer l'accès" @click="restoreAccess(admin.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-4.5"/></svg>
                </button>
                <button v-else class="action-btn action-btn--danger" title="Révoquer l'accès" @click="confirmId = admin.id">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
                </button>
              </div>
            </td>
          </tr>

          <!-- État vide -->
          <tr v-if="paginated.length === 0">
            <td colspan="5" class="empty-state">
              <div class="empty-state__content">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40" style="color:#9ca3af"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <p>Aucun administrateur trouvé.</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="table-footer">
        <span class="table-footer__info">Affichage {{ rangeLabel }} admins</span>
        <div class="pagination">
          <button class="page-btn" :disabled="currentPage === 1" @click="prev()">Précédent</button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="next()">Suivant</button>
        </div>
      </div>
    </div>

    <!-- Carte Protocoles de sécurité -->
    <div class="security-card">
      <h3 class="security-card__title">Protocoles de sécurité</h3>
      <p class="security-card__text">
        Assurez-vous que tous les administrateurs respectent les politiques d'accès et de confidentialité de la plateforme ImmoSN.
      </p>

    </div>
  </div>
</template>

<style scoped>
.admins-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

/* En-tête */
.admins-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.admins-header__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.admins-header__subtitle {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

.btn-add {
  background: var(--color-primary);
  color: #fff;
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  transition: background 0.15s;
  white-space: nowrap;
}

.btn-add:hover { background: var(--color-primary-hover); }

/* Stats */
.admins-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

/* Skeleton */
.skeleton-table { display: flex; flex-direction: column; gap: 0.75rem; }
.skeleton-row {
  height: 52px;
  background: linear-gradient(90deg, var(--color-hover-row) 25%, var(--color-border-solid) 50%, var(--color-hover-row) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: var(--radius-sm);
}
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Table */
.admins-table-wrap {
  background: var(--color-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.admins-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.admins-table thead tr {
  background: var(--color-background);
  border-bottom: 1px solid var(--color-border-solid);
}

.admins-table th {
  padding: 0.85rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.admins-table__row {
  border-bottom: 1px solid var(--color-hover-row);
  transition: background 0.15s;
}

.admins-table__row:hover { background: var(--color-hover-row); }
.admins-table__row:last-child { border-bottom: none; }

.admins-table td {
  padding: 0.9rem 1rem;
  color: var(--color-text);
  vertical-align: middle;
}

.td-email { color: var(--color-text-muted); font-size: 0.85rem; }
.td-date  { color: var(--color-text-muted); font-size: 0.85rem; }

/* Identité admin */
.admin-identity {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.admin-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.admin-name { font-weight: 600; font-size: 0.88rem; }
.admin-role { font-size: 0.78rem; color: var(--color-text-muted); }

/* Badges */
.badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge--active  { background: var(--badge-active-bg);  color: var(--badge-active-color); }
.badge--neutral { background: var(--badge-neutral-bg); color: var(--badge-neutral-color); }
.badge--pending { background: var(--badge-pending-bg); color: var(--badge-pending-color); }

/* Actions */
.td-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, border-color 0.15s;
}

.action-btn svg { width: 15px; height: 15px; stroke: var(--color-text-muted); }
.action-btn:hover { background: var(--color-hover-row); border-color: var(--color-border-solid); }

.action-btn--danger:hover { background: #FDECEA; border-color: var(--color-accent); }
.action-btn--danger:hover svg { stroke: var(--color-accent); }

/* État vide */
.empty-state { text-align: center; padding: 3rem !important; }
.empty-state__content { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; color: var(--color-text-muted); }
.empty-state__icon { font-size: 2.5rem; }

/* Pagination */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-top: 1px solid var(--color-border-solid);
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.pagination { display: flex; gap: 0.5rem; }

.page-btn {
  padding: 0.4rem 0.9rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border-solid);
  background: var(--color-card);
  font-size: 0.82rem;
  color: var(--color-text);
  transition: background 0.15s;
}

.page-btn:hover:not(:disabled) { background: var(--color-hover-row); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Carte sécurité */
.security-card {
  background: var(--badge-pending-bg);
  border: 1px solid rgba(212, 113, 74, 0.25);
  border-radius: var(--radius);
  padding: 1.5rem;
  max-width: 480px;
}

.security-card__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-accent);
  margin-bottom: 0.5rem;
}

.security-card__text {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.75;
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.security-card__link {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-accent);
  text-decoration: none;
}

.security-card__link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .admins-stats { grid-template-columns: 1fr; }
  .admins-table th:nth-child(3),
  .admins-table td:nth-child(3) { display: none; }
}
</style>
