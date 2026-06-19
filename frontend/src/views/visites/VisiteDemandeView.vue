<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { User, MapPin, CalendarClock, MessageSquare, CheckCircle2 } from 'lucide-vue-next'
import PhoneInput from '@/components/PhoneInput.vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import { useToastStore } from '@/stores/toastStore'
import visiteService from '@/services/visiteService'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const annonceId = computed(() => route.query.annonceId || route.params.annonceId || null)

const form = ref({
  nom: '',
  prenom: '',
  telephone: '',
  email: '',
  adresse: '',
  date: '',
  heure: '',
  commentaire: '',
})

const sending = ref(false)
const error = ref('')
const success = ref(false)
const prospectToken = ref(null)

function validate() {
  if (!annonceId.value) return "Impossible d'identifier l'annonce concernée."
  if (!form.value.nom.trim()) return 'Le nom est obligatoire.'
  if (!form.value.telephone.trim()) return 'Le téléphone est obligatoire.'
  if (!form.value.email.trim()) return "L'email est obligatoire."
  if (!form.value.date) return 'Veuillez choisir une date de visite.'
  return ''
}

async function submit() {
  const v = validate()
  if (v) { error.value = v; toast.error(v); return }
  sending.value = true
  error.value = ''
  try {
    const dateFormatted = form.value.heure
      ? `${form.value.date}T${form.value.heure}:00`
      : `${form.value.date}T00:00:00`
    const res = await visiteService.createInvite({
      annonceId: Number(annonceId.value),
      nom: form.value.nom.trim(),
      prenom: form.value.prenom.trim() || null,
      telephone: form.value.telephone.trim(),
      email: form.value.email.trim(),
      adresse: form.value.adresse.trim() || null,
      dateVisite: dateFormatted,
      heureVisite: form.value.heure || null,
      commentaire: form.value.commentaire.trim() || null,
    })
    prospectToken.value = res.data.data.prospectToken
    success.value = true
    toast.success('Demande de visite enregistrée !')
  } catch (e) {
    error.value = e.response?.data?.message || "Erreur lors de l'envoi. Veuillez réessayer."
    toast.error(error.value)
  } finally {
    sending.value = false
  }
}

function retourAnnonces() {
  router.push('/annonces')
}
</script>

<template>
  <div class="vd-page">
    <div class="vd-card">

      <template v-if="!success">
        <div class="vd-header">
          <h1 class="vd-header__title">Demander une visite</h1>
          <p class="vd-header__sub">Remplissez ce formulaire, l'agence vous contactera pour confirmer le créneau.</p>
        </div>

        <form class="vd-form" @submit.prevent="submit">
          <section class="vd-section">
            <h2 class="vd-section__title"><User :size="16" /> Informations personnelles</h2>
            <div class="vd-grid">
              <div class="vd-field">
                <label>Nom *</label>
                <input v-model="form.nom" type="text" placeholder="Diallo" />
              </div>
              <div class="vd-field">
                <label>Prénom</label>
                <input v-model="form.prenom" type="text" placeholder="Aminata" />
              </div>
              <div class="vd-field">
                <label>Téléphone *</label>
                <PhoneInput v-model="form.telephone" />
              </div>
              <div class="vd-field">
                <label>Email *</label>
                <input v-model="form.email" type="email" placeholder="vous@email.com" />
              </div>
            </div>
          </section>

          <section class="vd-section">
            <h2 class="vd-section__title"><MapPin :size="16" /> Adresse</h2>
            <div class="vd-field vd-field--full">
              <label>Adresse</label>
              <input v-model="form.adresse" type="text" placeholder="Almadies, Dakar" />
            </div>
          </section>

          <section class="vd-section">
            <h2 class="vd-section__title"><CalendarClock :size="16" /> Rendez-vous</h2>
            <div class="vd-grid">
              <div class="vd-field">
                <label>Date *</label>
                <input v-model="form.date" type="date" />
              </div>
              <div class="vd-field">
                <label>Heure</label>
                <input v-model="form.heure" type="time" />
              </div>
            </div>
          </section>

          <section class="vd-section">
            <h2 class="vd-section__title"><MessageSquare :size="16" /> Message</h2>
            <div class="vd-field vd-field--full">
              <label>Commentaire</label>
              <textarea v-model="form.commentaire" rows="3"
                placeholder="Ex. : Disponible en matinée, préférence pour le week-end…" />
            </div>
          </section>

          <p v-if="error" class="vd-err">{{ error }}</p>
          <ButtonPrimary type="submit" full-width :disabled="sending">
            {{ sending ? 'Envoi…' : 'Envoyer ma demande' }}
          </ButtonPrimary>
        </form>
      </template>

      <template v-else>
        <div class="vd-success">
          <CheckCircle2 :size="48" class="vd-success__icon" />
          <p class="vd-success__title">Demande enregistrée</p>
          <p class="vd-success__text">Vous serez contacté au numéro et à l'email indiqués.</p>
          <p class="vd-success__label">Numéro de suivi :</p>
          <code class="vd-success__token">{{ prospectToken }}</code>
          <div class="vd-success__actions">
            <RouterLink :to="{ name: 'suivi-visite', query: { token: prospectToken } }" class="vd-success__link">
              Suivre ma demande
            </RouterLink>
            <ButtonPrimary variant="outline" @click="retourAnnonces">Retour annonces</ButtonPrimary>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>

<style scoped>
.vd-page { background: var(--color-background); min-height: 100vh; padding: 2rem 1rem; display: flex; justify-content: center; }
.vd-card {
  width: 100%; max-width: 640px; background: var(--color-card);
  border-radius: var(--radius); box-shadow: var(--shadow-card);
  padding: 2rem; box-sizing: border-box;
}

.vd-header { margin-bottom: 1.5rem; }
.vd-header__title { font-size: 1.5rem; font-weight: 800; color: var(--color-text); }
.vd-header__sub { font-size: 0.88rem; color: var(--color-text); opacity: .6; margin-top: .4rem; }

.vd-form { display: flex; flex-direction: column; gap: 1.5rem; }
.vd-section { display: flex; flex-direction: column; gap: .75rem; }
.vd-section__title {
  display: flex; align-items: center; gap: .5rem;
  font-size: 0.85rem; font-weight: 700; color: var(--color-text); opacity: .75;
  text-transform: uppercase; letter-spacing: .02em;
  padding-bottom: .5rem; border-bottom: 1px solid var(--color-border);
}

.vd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.vd-field { display: flex; flex-direction: column; gap: 0.35rem; }
.vd-field--full { grid-column: 1 / -1; }
.vd-field label { font-size: 0.78rem; font-weight: 600; opacity: 0.7; }
.vd-field input,
.vd-field textarea {
  width: 100%; padding: 0.65rem 0.85rem; box-sizing: border-box;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  resize: vertical; transition: border-color 0.2s;
}
.vd-field input:focus,
.vd-field textarea:focus { border-color: var(--color-primary); outline: none; }

.vd-err { font-size: 0.85rem; color: var(--color-accent); }

.vd-success { text-align: center; padding: 1.5rem .5rem; display: flex; flex-direction: column; align-items: center; gap: .35rem; }
.vd-success__icon { color: var(--color-primary); margin-bottom: .5rem; }
.vd-success__title { font-size: 1.2rem; font-weight: 700; color: var(--color-text); }
.vd-success__text { font-size: 0.88rem; opacity: .65; margin-bottom: .5rem; }
.vd-success__label { font-size: 0.8rem; font-weight: 600; opacity: .6; margin-top: .5rem; }
.vd-success__token {
  display: inline-block; padding: 0.5rem 1rem; margin: .4rem 0 1rem;
  background: var(--color-background); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); font-size: 0.9rem; font-weight: 600; word-break: break-all;
}
.vd-success__actions { display: flex; flex-direction: column; gap: .75rem; width: 100%; max-width: 280px; margin-top: .5rem; }
.vd-success__link {
  font-size: 0.85rem; font-weight: 600; color: var(--color-primary); text-decoration: underline;
}

@media (max-width: 480px) {
  .vd-card { padding: 1.5rem 1.25rem; }
  .vd-grid { grid-template-columns: 1fr; }
}
</style>
