<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  icon: { type: String, default: '' }, // emoji ou caractère
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

// Gestion du toggle mot de passe
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
      <span v-if="icon" class="field__icon">{{ icon }}</span>
      <input
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        class="field__input"
        :class="{ 'field__input--icon': icon }"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <!-- Toggle visibilité mot de passe -->
      <button
        v-if="type === 'password'"
        type="button"
        class="field__toggle"
        @click="togglePassword"
      >
        {{ showPassword ? '🙈' : '👁️' }}
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
  font-size: 1rem;
  color: var(--color-text);
  opacity: 0.5;
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
  font-size: 1rem;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.field__toggle:hover {
  opacity: 1;
}

.field__error {
  font-size: 0.8rem;
  color: var(--color-accent);
}
</style>
