<script setup>
defineProps({
  label: String,
  value: [String, Number],
  color: { type: String, default: 'primary' }, // primary | accent | neutral
})
</script>

<template>
  <div class="stats-card" :class="`stats-card--${color}`">
    <span class="stats-card__value">{{ value }}</span>
    <span class="stats-card__label">{{ label }}</span>
  </div>
</template>

<style scoped>
.stats-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  border-left: 4px solid transparent;
}

.stats-card--primary { border-left-color: var(--color-primary); }
.stats-card--accent  { border-left-color: var(--color-accent); }
.stats-card--neutral { border-left-color: #6B7280; }

.stats-card__value {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1;
}

.stats-card__label {
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #6B7280;
}
</style>
