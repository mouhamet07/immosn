<script setup>
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toastStore'

const toastStore = useToastStore()
</script>

<template>
  <Teleport to="body">
    <div class="toast-container" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast" tag="div" class="toast-group">
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.type}`"
          role="alert"
          @click="toastStore.remove(toast.id)"
        >
          <span class="toast__icon">
            <CheckCircle v-if="toast.type === 'success'" :size="18" />
            <XCircle v-else-if="toast.type === 'error'" :size="18" />
            <AlertTriangle v-else-if="toast.type === 'warning'" :size="18" />
            <Info v-else :size="18" />
          </span>
          <p class="toast__message">{{ toast.message }}</p>
          <button class="toast__close" @click.stop="toastStore.remove(toast.id)" aria-label="Fermer">
            <X :size="14" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 9999;
  pointer-events: none;
}

.toast-group {
  display: flex;
  flex-direction: column;
  gap: .5rem;
  align-items: flex-end;
}

.toast {
  display: flex;
  align-items: center;
  gap: .75rem;
  padding: .75rem 1rem;
  border-radius: 10px;
  min-width: 280px;
  max-width: 420px;
  box-shadow: 0 4px 20px rgba(0,0,0,.15);
  cursor: pointer;
  pointer-events: all;
  font-size: .9rem;
  font-weight: 500;
  border-left: none;
}

.toast--success { background: #f0fdf4; color: #166534; }
.toast--error   { background: #fef2f2; color: #991b1b; }
.toast--warning { background: #fffbeb; color: #92400e; }
.toast--info    { background: #eff6ff; color: #1e40af; }

.toast__icon   { font-size: 1rem; flex-shrink: 0; }
.toast__message { flex: 1; line-height: 1.4; }
.toast__close  {
  background: none; border: none; font-size: .8rem;
  cursor: pointer; opacity: .5; flex-shrink: 0;
  transition: opacity .15s; color: currentColor;
}
.toast__close:hover { opacity: 1; }

/* Animations */
.toast-enter-active { transition: all .25s ease; }
.toast-leave-active { transition: all .2s ease; }
.toast-enter-from   { transform: translateX(100%); opacity: 0; }
.toast-leave-to     { transform: translateX(100%); opacity: 0; }

@media (max-width: 480px) {
  .toast-container { bottom: 1rem; right: 1rem; left: 1rem; }
  .toast-group { align-items: stretch; }
  .toast { min-width: 0; }
}
</style>
