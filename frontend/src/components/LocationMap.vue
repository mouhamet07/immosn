<script setup>
import { computed } from 'vue'
import { LMap, LTileLayer, LMarker } from '@vue-leaflet/vue-leaflet'
import { Icon } from 'leaflet'
import { MapPin } from 'lucide-vue-next'

// Fix icônes Leaflet dans les builds Vite
delete Icon.Default.prototype._getIconUrl
Icon.Default.mergeOptions({
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
})

const props = defineProps({
  latitude:  { type: Number, default: null },
  longitude: { type: Number, default: null },
  draggable: { type: Boolean, default: false },
  height:    { type: String,  default: '280px' },
  zoom:      { type: Number,  default: 15 },
})

const emit = defineEmits(['update:latitude', 'update:longitude'])

const hasCoords = computed(() => props.latitude != null && props.longitude != null)
const center    = computed(() =>
  hasCoords.value ? [props.latitude, props.longitude] : [14.6928, -17.4467]
)
const mapZoom   = computed(() => hasCoords.value ? props.zoom : 12)

function onMarkerMoved(e) {
  const { lat, lng } = e.target.getLatLng()
  emit('update:latitude', lat)
  emit('update:longitude', lng)
}
</script>

<template>
  <div class="lmap-wrapper" :style="{ height }">

    <template v-if="hasCoords">
      <l-map
        :zoom="mapZoom"
        :center="center"
        :options="{ zoomControl: true, scrollWheelZoom: false }"
        style="height: 100%; width: 100%; z-index: 1;"
      >
        <l-tile-layer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a>"
          layer-type="base"
          name="OpenStreetMap"
        />
        <l-marker
          :lat-lng="[latitude, longitude]"
          :draggable="draggable"
          @moveend="onMarkerMoved"
        />
      </l-map>
    </template>

    <div v-else class="lmap-empty">
      <MapPin :size="22" class="lmap-empty__icon" />
      <span>Localisation non disponible</span>
    </div>

  </div>
</template>

<style scoped>
.lmap-wrapper {
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid #e8e0d4;
  position: relative;
}

.lmap-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: #e8f2ef;
  color: var(--color-primary);
  font-size: 0.88rem;
  font-weight: 500;
}

.lmap-empty__icon { opacity: 0.7; }
</style>
