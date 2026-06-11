<script setup>
defineProps({
  label:   { type: String, required: true },
  variant: {
    type: String,
    default: 'neutral',
    validator: (v) => ['success', 'warning', 'danger', 'neutral', 'info'].includes(v)
  }
})
</script>

<template>
  <span class="status-badge" :class="`status-badge--${variant}`">{{ label }}</span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.65rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}
.status-badge--success { background: #d1fae5; color: #059669; }
.status-badge--warning { background: #fef3c7; color: #d97706; }
.status-badge--danger  { background: #fee2e2; color: #dc2626; }
.status-badge--neutral { background: #f3f4f6; color: #6b7280; }
.status-badge--info    { background: #dbeafe; color: #2563eb; }
</style>
