<script setup>
defineProps({
  type: { type: String, default: 'button' },
  variant: { type: String, default: 'primary' }, // primary | accent | outline
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  fullWidth: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="['btn', `btn--${variant}`, { 'btn--full': fullWidth, 'btn--loading': loading }]"
  >
    <span v-if="loading" class="btn__spinner"></span>
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  font-weight: 600;
  transition: background 0.2s, opacity 0.2s;
}

.btn--neutral {
  background: var(--color-border);
  color: var(--color-text);
  opacity: 0.7;
}

.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn--accent {
  background: var(--color-accent);
  color: #fff;
}
.btn--accent:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.btn--outline {
  background: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
}
.btn--outline:hover:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

.btn--full {
  width: 100%;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
