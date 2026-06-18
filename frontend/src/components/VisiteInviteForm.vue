<script setup>
import { ref } from 'vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import PhoneInput from '@/components/PhoneInput.vue'
import visiteService from '@/services/visiteService'

const props = defineProps({
  annonceId: { type: [Number, String], required: true },
})

const form = ref({
  nom: '',
  prenom: '',
  telephone: '',
  email: '',
  adresse: '',
  date: '',
  commentaire: '',
})

const sending = ref(false)
const error = ref('')
const success = ref(false)
const prospectToken = ref(null)

function validate() {
  if (!form.value.nom.trim()) return 'Le nom est obligatoire.'
  if (!form.value.telephone.trim()) return 'Le téléphone est obligatoire.'
  if (!form.value.email.trim()) return "L'email est obligatoire."
  if (!form.value.date) return 'Veuillez choisir une date.'
  return ''
}

async function submit() {
  const v = validate()
  if (v) { error.value = v; return }
  sending.value = true
  error.value = ''
  try {
    // datetime-local "2026-05-24T10:30" → "2026-05-24T10:30:00" attendu par le backend
    const dateFormatted = form.value.date.length === 16 ? form.value.date + ':00' : form.value.date
    const res = await visiteService.createInvite({
      annonceId: Number(props.annonceId),
      nom: form.value.nom.trim(),
      prenom: form.value.prenom.trim() || null,
      telephone: form.value.telephone.trim(),
      email: form.value.email.trim(),
      adresse: form.value.adresse.trim() || null,
      dateVisite: dateFormatted,
      commentaire: form.value.commentaire.trim() || null,
    })
    prospectToken.value = res.data.data.prospectToken
    success.value = true
  } catch (e) {
    error.value = e.response?.data?.message || "Erreur lors de l'envoi. Veuillez réessayer."
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="invite-form">
    <template v-if="!success">
      <div class="invite-form__grid">
        <div class="invite-form__field">
          <label>Nom *</label>
          <input v-model="form.nom" type="text" placeholder="Diallo" />
        </div>
        <div class="invite-form__field">
          <label>Prénom</label>
          <input v-model="form.prenom" type="text" placeholder="Aminata" />
        </div>
        <div class="invite-form__field">
          <label>Téléphone *</label>
          <PhoneInput v-model="form.telephone" />
        </div>
        <div class="invite-form__field">
          <label>Email *</label>
          <input v-model="form.email" type="email" placeholder="vous@email.com" />
        </div>
        <div class="invite-form__field invite-form__field--full">
          <label>Adresse</label>
          <input v-model="form.adresse" type="text" placeholder="Almadies, Dakar" />
        </div>
        <div class="invite-form__field invite-form__field--full">
          <label>Date et heure souhaitées *</label>
          <input v-model="form.date" type="datetime-local" />
        </div>
        <div class="invite-form__field invite-form__field--full">
          <label>Commentaire</label>
          <textarea v-model="form.commentaire" rows="3"
            placeholder="Ex. : Disponible en matinée, préférence pour le week-end…" />
        </div>
      </div>

      <p v-if="error" class="invite-form__err">{{ error }}</p>
      <ButtonPrimary full-width :disabled="sending" @click="submit">
        {{ sending ? 'Envoi…' : 'Envoyer la demande' }}
      </ButtonPrimary>
    </template>

    <template v-else>
      <div class="invite-form__success">
        <p class="invite-form__success-title">Demande envoyée !</p>
        <p class="invite-form__success-text">
          Votre demande de visite a été transmise à l'agence. Conservez votre référence de suivi :
        </p>
        <code class="invite-form__token">{{ prospectToken }}</code>
        <p class="invite-form__success-hint">
          Vous serez contacté au numéro et à l'email indiqués.
        </p>
        <RouterLink :to="{ name: 'suivi-visite', query: { token: prospectToken } }" class="invite-form__track-link">
          Suivre ma visite avec ce numéro
        </RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.invite-form { display: flex; flex-direction: column; gap: 1rem; }
.invite-form__grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem;
}
.invite-form__field { display: flex; flex-direction: column; gap: 0.3rem; }
.invite-form__field--full { grid-column: 1 / -1; }
.invite-form__field label { font-size: 0.78rem; font-weight: 600; opacity: 0.7; }
.invite-form__field input,
.invite-form__field textarea {
  width: 100%; padding: 0.6rem 0.75rem; box-sizing: border-box;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  resize: vertical; transition: border-color 0.2s;
}
.invite-form__field input:focus,
.invite-form__field textarea:focus { border-color: var(--color-primary); outline: none; }
.invite-form__err { font-size: 0.82rem; color: var(--color-accent); }
.invite-form__success { text-align: center; padding: 1rem 0.5rem; }
.invite-form__success-title { font-weight: 700; margin-bottom: 0.4rem; }
.invite-form__success-text { font-size: 0.88rem; opacity: 0.7; margin-bottom: 0.75rem; }
.invite-form__token {
  display: inline-block; padding: 0.4rem 0.75rem; margin-bottom: 0.75rem;
  background: var(--color-background); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); font-size: 0.8rem; word-break: break-all;
}
.invite-form__success-hint { font-size: 0.8rem; opacity: 0.6; }
.invite-form__track-link {
  display: inline-block; margin-top: 0.5rem; font-size: 0.85rem; font-weight: 600;
  color: var(--color-primary); text-decoration: underline;
}
@media (max-width: 480px) {
  .invite-form__grid { grid-template-columns: 1fr; }
}
</style>
