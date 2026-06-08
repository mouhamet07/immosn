import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // Redirection racine
    { path: '/', redirect: '/annonces' },

    // Authentification (publiques)
    { path: '/inscription', name: 'inscription', component: () => import('@/views/auth/InscriptionView.vue') },
    { path: '/connexion',   name: 'connexion',   component: () => import('@/views/auth/ConnexionView.vue') },

    // Interface CLIENT
    {
      path: '/',
      component: () => import('@/layouts/ClientLayout.vue'),
      children: [
        { path: 'annonces',          name: 'annonces',          component: () => import('@/views/annonces/ListeAnnoncesView.vue') },
        { path: 'annonces/:id',      name: 'detail-annonce',    component: () => import('@/views/annonces/DetailAnnonceView.vue') },
        { path: 'profil',            name: 'profil',            component: () => import('@/views/profil/ProfilView.vue'),                   meta: { requiresAuth: true, role: 'CLIENT' } },
        { path: 'discussions',       name: 'discussions',       component: () => import('@/views/discussions/DiscussionsView.vue'),         meta: { requiresAuth: true, role: 'CLIENT' } },
        { path: 'mes-visites',       name: 'mes-visites',       component: () => import('@/views/visites/MesVisitesView.vue'),              meta: { requiresAuth: true, role: 'CLIENT' } },
        { path: 'mes-contrats',      name: 'mes-contrats',      component: () => import('@/views/contrats/MesContratsView.vue'),            meta: { requiresAuth: true, role: 'CLIENT' } },
        { path: 'mes-signalements',  name: 'mes-signalements',  component: () => import('@/views/signalements/MesSignalementsView.vue'),   meta: { requiresAuth: true, role: 'CLIENT' } },
        { path: 'favoris',           name: 'favoris',           component: () => import('@/views/favoris/FavorisView.vue'),                 meta: { requiresAuth: true, role: 'CLIENT' } },
      ],
    },

    // Interface ADMIN
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        { path: '',                    redirect: '/admin/dashboard' },
        { path: 'dashboard',           name: 'admin-dashboard',      component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'annonces',            name: 'admin-annonces',        component: () => import('@/views/admin/AnnoncesView.vue') },
        { path: 'annonces/publier',    name: 'admin-publier',         component: () => import('@/views/admin/PublierAnnonceView.vue') },
        { path: 'annonces/:id',        name: 'admin-detail-annonce',  component: () => import('@/views/admin/DetailAnnonceAdminView.vue') },
        { path: 'annonces/:id/modifier', name: 'admin-modifier-annonce', component: () => import('@/views/admin/ModifierAnnonceView.vue') },
        { path: 'messages',            name: 'admin-messages',       component: () => import('@/views/admin/DiscussionsAdminView.vue') },
        { path: 'visites',             name: 'admin-visites',        component: () => import('@/views/admin/VisitesAdminView.vue') },
        { path: 'leads',               name: 'admin-leads',          component: () => import('@/views/admin/LeadsAdminView.vue') },
        { path: 'contrats',            name: 'admin-contrats',       component: () => import('@/views/admin/ContratsAdminView.vue') },
        { path: 'signalements',        name: 'admin-signalements',   component: () => import('@/views/admin/SignalementsAdminView.vue') },
        { path: 'types-biens',         name: 'admin-types-biens',    component: () => import('@/views/admin/TypesBienView.vue') },
        { path: 'commodites',          name: 'admin-commodites',     component: () => import('@/views/admin/CommoditesView.vue') },
        { path: 'administrateurs',         name: 'admin-admins',        component: () => import('@/views/admin/AdministrateursView.vue') },
        { path: 'administrateurs/ajouter', name: 'admin-ajouter-admin', component: () => import('@/views/admin/AjouterAdminView.vue') },
        { path: 'administrateurs/:id/modifier', name: 'admin-modifier-admin', component: () => import('@/views/admin/ModifierAdminView.vue') },
        { path: 'profil',              name: 'admin-profil',         component: () => import('@/views/admin/ProfilAdminView.vue') },
      ],
    },

    // Catch-all — rediriger vers annonces
    { path: '/:pathMatch(.*)*', redirect: '/annonces' },
  ],
})

// Guard de navigation strict
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const role = authStore.user?.role
  const isAdmin = role === 'ADMIN' || role === 'SUPER_ADMIN'

  // Admin/SuperAdmin redirigé vers dashboard s'il accède aux pages auth
  if (authStore.isAuthenticated && (to.path === '/connexion' || to.path === '/inscription')) {
    return next(isAdmin ? '/admin/dashboard' : '/annonces')
  }

  // Route nécessite authentification
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    localStorage.setItem('redirectAfterLogin', to.fullPath)
    return next('/connexion')
  }

  // Route réservée au client — bloquer les admins
  if (to.meta.role === 'CLIENT' && isAdmin) {
    return next('/admin/dashboard')
  }

  // Route réservée admin — bloquer les clients
  if (to.meta.role === 'ADMIN' && !isAdmin) {
    return next('/annonces')
  }

  // Bloquer accès direct /admin/* pour les non-admins
  if (to.path.startsWith('/admin') && !isAdmin && authStore.isAuthenticated) {
    return next('/annonces')
  }

  next()
})

export default router
