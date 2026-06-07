<script setup>
// Année courante pour le copyright
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-brand">
        <img src="@/assets/logo nav 1 - orange 1.png" alt="ImmoSN" class="footer-logo" />
        <p class="footer-tagline">© {{ currentYear }} ImmoSN. Immobilier Sénégalais Haut de Gamme.</p>
      </div>
      <div class="footer-links">
        <a href="#">Politique de confidentialité</a>
        <a href="#">Conditions d'utilisation</a>
        <a href="#">Assistance</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
  padding: 20px 32px;
  margin-top: auto;
}

@media (max-width: 480px) {
  .app-footer { padding: 16px; }
  .footer-content { flex-direction: column; align-items: flex-start; }
  .footer-links { gap: 12px; }
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-logo { height: 28px; object-fit: contain; }
.footer-tagline { font-size: 12px; color: var(--color-text-secondary, #6B7280); margin-top: 4px; }

.footer-links { display: flex; gap: 20px; flex-wrap: wrap; }
.footer-links a {
  font-size: 12px;
  color: var(--color-text-secondary, #6B7280);
  text-decoration: none;
  transition: color 150ms ease;
}
.footer-links a:hover { color: var(--color-primary); }
</style>
