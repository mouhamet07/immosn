<script setup>
defineProps({
  label: String,
  value: [String, Number],
})
</script>

<template>
  <div class="stats-card">
    <div class="stats-card__content">
      <span class="stats-card__value">{{ value }}</span>
      <span class="stats-card__label">{{ label }}</span>
    </div>
  </div>
</template>

<style scoped>
.stats-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 1.5rem;
  min-height: 120px;

  display: flex;
  align-items: center;

  border: 1px solid #f1f3f5;

  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.04),
    0 2px 4px rgba(0, 0, 0, 0.02);

  transition: all 0.25s ease;
}

.stats-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.08),
    0 4px 8px rgba(0, 0, 0, 0.04);
}

.stats-card__content {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.stats-card__value {
  font-size: 2.2rem;
  font-weight: 800;
  color: #111827;
  line-height: 1;
}

.stats-card__label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #6b7280;
}
</style>