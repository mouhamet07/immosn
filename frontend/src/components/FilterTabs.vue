<script setup>
defineProps({
  modelValue: { type: String, required: true },
  tabs: { type: Array, required: true }, // [{ value, label }]
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="filter-tabs" role="tablist">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      role="tab"
      class="filter-tabs__btn"
      :class="{ 'filter-tabs__btn--active': modelValue === tab.value }"
      :aria-selected="modelValue === tab.value"
      @click="$emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.filter-tabs {
  display: inline-flex;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.filter-tabs__btn {
  padding: 0.35rem 0.85rem;
  font-size: 0.82rem;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
  white-space: nowrap;
}

.filter-tabs__btn:hover:not(.filter-tabs__btn--active) {
  background: var(--color-hover-row);
  color: var(--color-text);
}

.filter-tabs__btn--active {
  background: var(--color-card);
  color: var(--color-text);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}
</style>
