<script setup>
defineOptions({ name: 'AnnonceBadge' })
defineProps({
  label: { type: String, required: true },
  variant: { type: String, default: 'accent' }, // accent | primary | neutral
})
</script>

<template>
  <span :class="['badge', `badge--${variant}`]">{{ label }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.badge--accent {
  background: var(--color-accent);
  color: #fff;
}

.badge--primary {
  background: var(--color-primary);
  color: #fff;
}

.badge--neutral {
  background: rgba(45, 55, 72, 0.1);
  color: var(--color-text);
}
</style>
