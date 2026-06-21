<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ChevronDown } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const countries = [
  { code: 'SN', name: 'Sénégal',       dialCode: '+221', flag: 'https://flagcdn.com/w20/sn.png', placeholder: '77 123 45 67',   maxDigits: 9,  formatGroups: [2, 3, 2, 2] },
  { code: 'CI', name: "Côte d'Ivoire", dialCode: '+225', flag: 'https://flagcdn.com/w20/ci.png', placeholder: '07 12 34 56 78', maxDigits: 10, formatGroups: [2, 2, 2, 2, 2] },
  { code: 'ML', name: 'Mali',           dialCode: '+223', flag: 'https://flagcdn.com/w20/ml.png', placeholder: '70 12 34 56',    maxDigits: 8,  formatGroups: [2, 2, 2, 2] },
  { code: 'GN', name: 'Guinée',         dialCode: '+224', flag: 'https://flagcdn.com/w20/gn.png', placeholder: '621 23 45 67',   maxDigits: 9,  formatGroups: [3, 2, 2, 2] },
  { code: 'MR', name: 'Mauritanie',     dialCode: '+222', flag: 'https://flagcdn.com/w20/mr.png', placeholder: '22 12 34 56',    maxDigits: 8,  formatGroups: [2, 2, 2, 2] },
  { code: 'GW', name: 'Guinée-Bissau',  dialCode: '+245', flag: 'https://flagcdn.com/w20/gw.png', placeholder: '955 12 34 56',   maxDigits: 9,  formatGroups: [3, 2, 2, 2] },
  { code: 'FR', name: 'France',         dialCode: '+33',  flag: 'https://flagcdn.com/w20/fr.png', placeholder: '06 12 34 56 78', maxDigits: 10, formatGroups: [2, 2, 2, 2, 2] },
]

const selectedCountry = ref(countries[0])
const rawNumber       = ref('')
const open            = ref(false)
const search          = ref('')

const filteredCountries = computed(() => {
  if (!search.value) return countries
  const q = search.value.toLowerCase()
  return countries.filter(c => c.name.toLowerCase().includes(q) || c.dialCode.includes(q))
})

const toggleDropdown = () => { open.value = !open.value }

const selectCountry = (country) => {
  selectedCountry.value = country
  rawNumber.value = ''
  open.value   = false
  search.value = ''
  emitValue()
}

const formatWithGroups = (digits, groups) => {
  let result = '', pos = 0
  for (let i = 0; i < groups.length; i++) {
    const chunk = digits.slice(pos, pos + groups[i])
    if (!chunk) break
    result += (i > 0 ? ' ' : '') + chunk
    pos += groups[i]
  }
  return result
}

const handleInput = () => {
  const digits = rawNumber.value.replace(/\D/g, '').slice(0, selectedCountry.value.maxDigits)
  rawNumber.value = formatWithGroups(digits, selectedCountry.value.formatGroups)
  emitValue()
}

const emitValue = () => {
  emit('update:modelValue', (selectedCountry.value.dialCode + ' ' + rawNumber.value).trim())
}

// Initialise le composant depuis une valeur existante (ex. édition de profil) :
// détecte le pays via l'indicatif puis reformate les chiffres restants.
const hydrateFromModel = (value) => {
  if (!value) return
  const trimmed = value.trim()
  // Pays par indicatif le plus long d'abord (+225 avant +22…) pour éviter les collisions
  const match = [...countries]
    .sort((a, b) => b.dialCode.length - a.dialCode.length)
    .find(c => trimmed.startsWith(c.dialCode))
  if (match) {
    selectedCountry.value = match
    const digits = trimmed.slice(match.dialCode.length).replace(/\D/g, '').slice(0, match.maxDigits)
    rawNumber.value = formatWithGroups(digits, match.formatGroups)
  } else {
    // Valeur sans indicatif connu : on conserve les chiffres pour le pays par défaut
    const digits = trimmed.replace(/\D/g, '').slice(0, selectedCountry.value.maxDigits)
    rawNumber.value = formatWithGroups(digits, selectedCountry.value.formatGroups)
  }
}

const handleOutsideClick = (e) => {
  if (!e.target.closest('.phone-input-wrapper')) open.value = false
}
onMounted(() => {
  hydrateFromModel(props.modelValue)
  document.addEventListener('click', handleOutsideClick)
})
onUnmounted(() => document.removeEventListener('click', handleOutsideClick))

// Si la valeur parente arrive de façon asynchrone (chargement du profil), réhydrater
// tant que l'utilisateur n'a pas encore saisi de numéro.
watch(() => props.modelValue, (val) => {
  if (!rawNumber.value && val) hydrateFromModel(val)
})
</script>

<template>
  <div class="phone-input-wrapper">
    <div class="country-selector" @click="toggleDropdown">
      <img :src="selectedCountry.flag" :alt="selectedCountry.name" class="flag-img" />
      <span class="dial-code">{{ selectedCountry.dialCode }}</span>
      <ChevronDown :size="14" />
      <div v-if="open" class="country-dropdown" @click.stop>
        <input v-model="search" placeholder="Rechercher..." class="country-search" autofocus />
        <div
          v-for="country in filteredCountries"
          :key="country.code"
          class="country-option"
          @click="selectCountry(country)"
        >
          <img :src="country.flag" class="flag-img" />
          <span class="country-name">{{ country.name }}</span>
          <span class="country-code">{{ country.dialCode }}</span>
        </div>
      </div>
    </div>
    <input
      v-model="rawNumber"
      type="tel"
      :placeholder="selectedCountry.placeholder"
      :maxlength="selectedCountry.maxDigits + 4"
      class="number-input"
      @input="handleInput"
    />
  </div>
</template>

<style scoped>
.phone-input-wrapper {
  display: flex;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  height: 44px;
  background: var(--color-card);
  position: relative;
  transition: border-color 150ms ease;
}
.phone-input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(74,124,111,0.1);
}
.country-selector {
  display: flex; align-items: center; gap: 5px;
  padding: 0 10px;
  border-right: 1px solid var(--color-border);
  cursor: pointer; min-width: 90px;
  background: var(--color-background);
  border-radius: 8px 0 0 8px;
  user-select: none; position: relative;
}
.flag-img { width: 20px; height: 14px; object-fit: cover; border-radius: 2px; }
.dial-code { font-size: 13px; font-weight: 500; color: var(--color-text); }
.country-dropdown {
  position: absolute; top: calc(100% + 6px); left: 0;
  width: min(280px, calc(100vw - 2rem)); background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: 10px;
  box-shadow: 0 8px 24px rgba(45,55,72,0.12);
  z-index: 999; max-height: 260px; overflow-y: auto;
}
.country-search {
  width: 100%; padding: 10px 14px;
  border: none; border-bottom: 1px solid var(--color-border);
  font-size: 13px; outline: none;
  background: var(--color-background);
  border-radius: 10px 10px 0 0; box-sizing: border-box;
}
.country-option {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 14px; cursor: pointer;
  transition: background 100ms ease;
}
.country-option:hover { background: var(--color-background); }
.country-name { font-size: 13px; color: var(--color-text); flex: 1; }
.country-code { font-size: 12px; color: var(--color-text-secondary); }
.number-input {
  flex: 1; border: none; outline: none;
  padding: 0 14px; font-size: 14px;
  background: transparent; color: var(--color-text);
  border-radius: 0 8px 8px 0;
}
.number-input::placeholder { color: var(--color-text-secondary); }
</style>
