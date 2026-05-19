<script setup>
import { ref, useSlots } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
const slots = useSlots()

const showPassword = ref(false)
const inputType = ref(props.type)

function togglePassword() {
  showPassword.value = !showPassword.value
  inputType.value = showPassword.value ? 'text' : 'password'
}
</script>

<template>
  <div class="field">
    <label v-if="label" class="field__label">
      {{ label }} <span v-if="required" class="field__required">*</span>
    </label>
    <div class="field__wrapper" :class="{ 'field__wrapper--error': error }">
      <span v-if="slots.icon" class="field__icon">
        <slot name="icon" />
      </span>
      <input
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        class="field__input"
        :class="{ 'field__input--icon': slots.icon }"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <!-- Toggle visibilité mot de passe -->
      <button
        v-if="type === 'password'"
        type="button"
        class="field__toggle"
        :aria-label="showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
        @click="togglePassword"
      >
        <!-- Icône œil ouvert -->
        <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        <!-- Icône œil barré -->
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
          <line x1="1" y1="1" x2="23" y2="23"/>
        </svg>
      </button>
    </div>
    <span v-if="error" class="field__error">{{ error }}</span>
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field__label {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text);
}

.field__required {
  color: var(--color-accent);
}

.field__wrapper {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  transition: border-color 0.2s;
}

.field__wrapper:focus-within {
  border-color: var(--color-primary);
}

.field__wrapper--error {
  border-color: var(--color-accent);
}

.field__icon {
  padding: 0 0.75rem;
  display: flex;
  align-items: center;
  color: var(--color-text);
  opacity: 0.45;
}

.field__icon svg {
  width: 16px;
  height: 16px;
}

.field__input {
  flex: 1;
  padding: 0.75rem 0.75rem 0.75rem 0;
  border: none;
  background: transparent;
  font-size: 0.95rem;
  color: var(--color-text);
}

.field__input--icon {
  padding-left: 0;
}

.field__input::placeholder {
  color: var(--color-text);
  opacity: 0.4;
}

.field__toggle {
  padding: 0 0.75rem;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: var(--color-text);
  opacity: 0.5;
  transition: opacity 0.2s;
}

.field__toggle:hover {
  opacity: 1;
}

.field__toggle svg {
  width: 16px;
  height: 16px;
}

.field__error {
  font-size: 0.8rem;
  color: var(--color-accent);
}
</style>
