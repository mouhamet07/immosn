<template>
  <div class="empty-page">
    <div class="empty-page__content">
      <span class="empty-page__icon">📄</span>
      <h1 class="empty-page__title">Contrats</h1>
      <p class="empty-page__text">Cette section sera disponible dans un prochain sprint.</p>
    </div>
  </div>
</template>

<style scoped>
.empty-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.empty-page__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  text-align: center;
}

.empty-page__icon { font-size: 3rem; }

.empty-page__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.empty-page__text {
  font-size: 0.9rem;
  color: var(--color-text-muted);
}
</style>
