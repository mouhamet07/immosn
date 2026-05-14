import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // Redirection racine
    { path: '/', redirect: '/connexion' },

    // Authentification (publiques)
    { path: '/inscription', name: 'inscription', component: () => import('@/views/auth/InscriptionView.vue') },
    { path: '/connexion',   name: 'connexion',   component: () => import('@/views/auth/ConnexionView.vue') },

    // Interface CLIENT
    {
      path: '/annonces',
      component: () => import('@/layouts/ClientLayout.vue'),
      children: [
        { path: '', name: 'annonces', component: () => import('@/views/annonces/ListeAnnoncesView.vue') },
        { path: '/annonces/:id', name: 'detail-annonce', component: () => import('@/views/annonces/DetailAnnonceView.vue') },
        {
          path: '/profil',
          name: 'profil',
          component: () => import('@/views/profil/ProfilView.vue'),
          meta: { requiresAuth: true, role: 'CLIENT' },
        },
      ],
    },

    // Interface ADMIN — toutes les routes imbriquées dans AdminLayout
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        { path: '',           redirect: '/admin/dashboard' },
        { path: 'dashboard',  name: 'admin-dashboard',  component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'annonces',   name: 'admin-annonces',   component: () => import('@/views/admin/AnnoncesView.vue') },
        { path: 'annonces/publier', name: 'admin-publier', component: () => import('@/views/admin/PublierAnnonceView.vue') },
        { path: 'messages',   name: 'admin-messages',   component: () => import('@/views/admin/MessagesView.vue') },
        { path: 'visites',    name: 'admin-visites',    component: () => import('@/views/admin/VisitesView.vue') },
        { path: 'contrats',   name: 'admin-contrats',   component: () => import('@/views/admin/ContratsView.vue') },
        { path: 'administrateurs',         name: 'admin-admins',        component: () => import('@/views/admin/AdministrateursView.vue') },
        { path: 'administrateurs/ajouter', name: 'admin-ajouter-admin', component: () => import('@/views/admin/AjouterAdminView.vue') },
        { path: 'profil',     name: 'admin-profil',     component: () => import('@/views/admin/ProfilAdminView.vue') },
      ],
    },
  ],
})

// Guard de navigation
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (!to.meta.requiresAuth) return true

  // Non authentifié → connexion
  if (!authStore.isAuthenticated) {
    return { name: 'connexion' }
  }

  // Route admin → vérifier le rôle
  if (to.meta.role === 'ADMIN' && authStore.role !== 'ADMIN' && authStore.role !== 'SUPER_ADMIN') {
    return { name: 'annonces' }
  }

  return true
})

export default router
