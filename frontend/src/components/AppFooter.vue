<script setup>
const year = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="footer-logo" />
        <span class="footer-tagline">© {{ year }} ImmoSN. Immobilier Sénégalais Haut de Gamme.</span>
      </div>
      <nav class="footer-links">
        <RouterLink to="/confidentialite">Politique de confidentialité</RouterLink>
        <RouterLink to="/conditions">Conditions d'utilisation</RouterLink>
        <RouterLink to="/assistance">Assistance</RouterLink>
      </nav>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
  padding: 18px 32px;
  width: 100%;
  margin-top: auto;
  opacity: 1;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-logo { height: 26px; object-fit: contain; }

.footer-tagline {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.footer-links a {
  font-size: 12px;
  color: var(--color-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  transition: color 150ms ease;
}

.footer-links a:hover { color: var(--color-primary); }

@media (max-width: 480px) {
  .app-footer { padding: 16px; }
  .footer-inner { flex-direction: column; align-items: flex-start; }
  .footer-links { gap: 12px; }
}
</style>
