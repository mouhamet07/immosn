<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  options: { type: Array, required: true }, // [{ value, label }]
  label: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="filter-select">
    <label v-if="label" class="filter-select__label">{{ label }}</label>
    <div class="filter-select__wrap">
      <select
        class="filter-select__input"
        :value="modelValue"
        @change="$emit('update:modelValue', $event.target.value)"
      >
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <svg
        class="filter-select__arrow"
        viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        width="14" height="14"
        aria-hidden="true"
      >
        <polyline points="6 9 12 15 18 9"/>
      </svg>
    </div>
  </div>
</template>

<style scoped>
.filter-select {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-select__label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.filter-select__wrap {
  position: relative;
}

.filter-select__input {
  appearance: none;
  -webkit-appearance: none;
  padding: 0.42rem 2.25rem 0.42rem 0.85rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-text);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  min-width: 150px;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.filter-select__input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(74, 124, 111, 0.15);
}

.filter-select__input:hover {
  border-color: var(--color-border-hover);
}

.filter-select__arrow {
  position: absolute;
  right: 0.7rem;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: var(--color-text-muted);
}
</style>
