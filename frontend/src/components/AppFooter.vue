<template>
  <footer class="footer">
    <span>ImmoSN © 2026</span>
    <div class="footer__links">
      <a href="#">Confidentialité</a>
      <a href="#">Conditions</a>
      <a href="#">Contact</a>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  background: #f9fafb;
  border-top: 1px solid var(--color-border);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  color: var(--color-text);
  opacity: 0.55;
}

.footer__links {
  display: flex;
  gap: 1.25rem;
}

.footer a {
  color: inherit;
}

.footer a:hover {
  text-decoration: underline;
}
</style>
