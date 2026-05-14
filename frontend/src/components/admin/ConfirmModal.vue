<script setup>
defineProps({
  title:   { type: String, default: 'Confirmer l\'action' },
  message: { type: String, default: 'Cette action est irréversible.' },
})
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="emit('cancel')">
      <div class="modal-card">
        <h3 class="modal-card__title">{{ title }}</h3>
        <p class="modal-card__message">{{ message }}</p>
        <div class="modal-card__actions">
          <button class="modal-btn modal-btn--cancel" @click="emit('cancel')">Annuler</button>
          <button class="modal-btn modal-btn--confirm" @click="emit('confirm')">Confirmer</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(45, 55, 72, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 2rem;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(45, 55, 72, 0.2);
}

.modal-card__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.75rem;
}

.modal-card__message {
  font-size: 0.9rem;
  color: var(--color-text);
  opacity: 0.7;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.modal-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.modal-btn {
  padding: 0.55rem 1.25rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  transition: background 0.15s, opacity 0.15s;
}

.modal-btn--cancel {
  background: transparent;
  border: 1.5px solid var(--color-primary);
  color: var(--color-primary);
}

.modal-btn--cancel:hover { background: rgba(74, 124, 111, 0.08); }

.modal-btn--confirm {
  background: var(--color-accent);
  color: #fff;
  border: none;
}

.modal-btn--confirm:hover { background: var(--color-accent-hover); }
</style>
