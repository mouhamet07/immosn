<script setup>
import NavBar from '@/components/NavBar.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<template>
  <div class="client-layout">
    <NavBar />
    <main class="client-main">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.client-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
}

.client-main {
  flex: 1;
}
</style>
