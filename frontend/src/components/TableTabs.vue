<script setup>
defineProps({
  modelValue: { type: String, required: true },
  tabs: { type: Array, required: true }, // [{ value, label }]
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="table-tabs" role="tablist">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      role="tab"
      class="table-tabs__btn"
      :class="{ 'table-tabs__btn--active': modelValue === tab.value }"
      :aria-selected="modelValue === tab.value"
      @click="$emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.table-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-background);
  padding: 0 0.5rem;
}

.table-tabs__btn {
  padding: 0.75rem 1.1rem;
  font-size: 0.85rem;
  font-weight: 600;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;
}

.table-tabs__btn:hover:not(.table-tabs__btn--active) {
  color: var(--color-text);
}

.table-tabs__btn--active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}
</style>
