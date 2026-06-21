<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
defineEmits(['update:currentPage'])
</script>

<template>
  <div v-if="totalPages > 1" class="tpg-pagination">
    <button class="pagination-nav" :disabled="currentPage === 1" @click="$emit('update:currentPage', currentPage - 1)">
      <ChevronLeft :size="16" />
      <span>Précédent</span>
    </button>

    <div class="pagination-pages">
      <button
        v-for="p in totalPages" :key="p"
        class="pagination-page" :class="{ 'pagination-page--active': p === currentPage }"
        @click="$emit('update:currentPage', p)"
      >{{ p }}</button>
    </div>

    <button class="pagination-nav" :disabled="currentPage === totalPages" @click="$emit('update:currentPage', currentPage + 1)">
      <span>Suivant</span>
      <ChevronRight :size="16" />
    </button>
  </div>
</template>

<style scoped>
.tpg-pagination { display: flex; align-items: center; justify-content: center; gap: 0.5rem; padding: 1rem; border-top: 1px solid var(--color-border); flex-wrap: wrap; }
.tpg-pagination .pagination-nav {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.tpg-pagination .pagination-nav:hover:not(:disabled) {
  background: var(--color-border);
  color: var(--color-text);
}
.tpg-pagination .pagination-nav:disabled { opacity: 0.35; cursor: not-allowed; }
.tpg-pagination .pagination-pages { display: flex; gap: 0.25rem; }
.tpg-pagination .pagination-page {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.tpg-pagination .pagination-page:hover { background: var(--color-border); color: var(--color-text); }
.tpg-pagination .pagination-page--active {
  background: var(--color-primary);
  color: #fff;
}
</style>
