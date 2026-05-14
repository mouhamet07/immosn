<script setup>
defineProps({
  steps:       { type: Array,  required: true },  // ['Étape 1', 'Étape 2', ...]
  currentStep: { type: Number, default: 0 },       // index 0-based
})
</script>

<template>
  <div class="stepper">
    <div class="stepper__bar-wrap">
      <div
        class="stepper__bar-fill"
        :style="{ width: `${((currentStep) / (steps.length - 1)) * 100}%` }"
      ></div>
    </div>
    <div class="stepper__steps">
      <div
        v-for="(step, i) in steps"
        :key="i"
        class="stepper__step"
        :class="{
          'stepper__step--done':   i < currentStep,
          'stepper__step--active': i === currentStep,
        }"
      >
        <div class="stepper__dot">{{ i < currentStep ? '✓' : i + 1 }}</div>
        <span class="stepper__label">{{ step }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stepper {
  margin-bottom: 2rem;
}

.stepper__bar-wrap {
  height: 4px;
  background: #E8E0D4;
  border-radius: 2px;
  margin-bottom: 1rem;
  position: relative;
}

.stepper__bar-fill {
  height: 100%;
  background: var(--color-accent);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.stepper__steps {
  display: flex;
  justify-content: space-between;
}

.stepper__step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  flex: 1;
}

.stepper__dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #E8E0D4;
  color: #6B7280;
  font-size: 0.78rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.stepper__step--active .stepper__dot {
  background: var(--color-accent);
  color: #fff;
}

.stepper__step--done .stepper__dot {
  background: var(--color-primary);
  color: #fff;
}

.stepper__label {
  font-size: 0.75rem;
  color: #6B7280;
  text-align: center;
}

.stepper__step--active .stepper__label { color: var(--color-accent); font-weight: 600; }
.stepper__step--done  .stepper__label  { color: var(--color-primary); }
</style>
