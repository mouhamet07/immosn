<script setup>
import { ref } from 'vue'
import { Check, X } from 'lucide-vue-next'

const toasts = ref([])
let nextId = 0

function show(message, type = 'success') {
  const id = nextId++
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3000)
}

defineExpose({ show })
</script>

<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.type}`"
        >
          <span class="toast__icon">
            <Check v-if="toast.type === 'success'" :size="16" />
            <X v-else :size="16" />
          </span>
          <span>{{ toast.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: 300;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.toast {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 500;
  box-shadow: var(--shadow-card-hover);
  min-width: 260px;
}

.toast--success {
  background: #E8F2EF;
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
}

.toast--error {
  background: #FDECEA;
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
}

.toast__icon {
  font-weight: 800;
  font-size: 0.9rem;
}

/* Transition */
.toast-enter-active, .toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(20px); }
.toast-leave-to   { opacity: 0; transform: translateX(20px); }
</style>
