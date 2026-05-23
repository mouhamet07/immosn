<script setup>
import { computed } from 'vue'
import { ICONS } from './icons.js'

defineOptions({ name: 'SvgIcon' })

const props = defineProps({
  name:  { type: String, required: true },
  size:  { type: Number, default: 20 },
})

const path = computed(() => ICONS[props.name] || '')
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    v-html="path"
    aria-hidden="true"
  />
</template>
