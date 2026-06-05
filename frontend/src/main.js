import './assets/main.css'
import 'leaflet/dist/leaflet.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Initialiser le store auth (vérifier token existant)
import { useAuthStore } from './stores/authStore'
const authStore = useAuthStore()
authStore.init().then(() => {
  app.mount('#app')
})
