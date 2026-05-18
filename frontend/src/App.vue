<script setup>
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
import ToastContainer from '@/components/ToastContainer.vue'
import { useAuthStore }   from '@/stores/authStore'
import { useFavorisStore } from '@/stores/favorisStore'

const authStore    = useAuthStore()
const favorisStore = useFavorisStore()

onMounted(async () => {
  await authStore.init()
  // Charger les favoris si le client est connecté
  if (authStore.isAuthenticated && authStore.role === 'CLIENT') {
    favorisStore.loadFavoris()
  }
})
</script>

<template>
  <RouterView />
  <ToastContainer />
</template>
