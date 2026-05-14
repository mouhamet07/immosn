import { ref, computed } from 'vue'

/**
 * Composable de pagination réutilisable.
 * @param {Ref<Array>} items - Liste réactive à paginer
 * @param {number} perPage  - Nombre d'éléments par page
 */
export function usePagination(items, perPage = 5) {
  const currentPage = ref(1)

  const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / perPage)))

  const paginated = computed(() => {
    const start = (currentPage.value - 1) * perPage
    return items.value.slice(start, start + perPage)
  })

  const rangeLabel = computed(() => {
    const start = Math.min((currentPage.value - 1) * perPage + 1, items.value.length)
    const end   = Math.min(currentPage.value * perPage, items.value.length)
    return `${start}–${end} sur ${items.value.length}`
  })

  function prev() {
    if (currentPage.value > 1) currentPage.value--
  }

  function next() {
    if (currentPage.value < totalPages.value) currentPage.value++
  }

  function reset() {
    currentPage.value = 1
  }

  return { currentPage, totalPages, paginated, rangeLabel, prev, next, reset }
}
