<script setup>
import { ref, nextTick, onMounted } from 'vue'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import discussionService from '@/services/discussionService'

const props = defineProps({
  annonceId: { type: [Number, String], required: true },
})

const STORAGE_KEY = 'guestDiscussionToken'

const form = ref({ nom: '', prenom: '', telephone: '', email: '', adresse: '' })
const premierMessage = ref('')
const sending = ref(false)
const error = ref('')

const guestToken = ref(null)
const messages = ref([])
const newMessage = ref('')
const sendingMessage = ref(false)
const messagesRef = ref(null)

function scrollChat() {
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

function validate() {
  if (!form.value.nom.trim()) return 'Le nom est obligatoire.'
  if (!form.value.telephone.trim()) return 'Le téléphone est obligatoire.'
  if (!form.value.email.trim()) return "L'email est obligatoire."
  if (!premierMessage.value.trim()) return 'Veuillez saisir un message.'
  return ''
}

async function start() {
  const v = validate()
  if (v) { error.value = v; return }
  sending.value = true
  error.value = ''
  try {
    const res = await discussionService.createInvite({
      annonceId: Number(props.annonceId),
      nom: form.value.nom.trim(),
      prenom: form.value.prenom.trim() || null,
      telephone: form.value.telephone.trim(),
      email: form.value.email.trim(),
      adresse: form.value.adresse.trim() || null,
      premierMessage: premierMessage.value.trim(),
    })
    guestToken.value = res.data.data.guestToken
    messages.value = res.data.data.messages ?? []
    localStorage.setItem(STORAGE_KEY, guestToken.value)
    premierMessage.value = ''
    await nextTick()
    scrollChat()
  } catch (e) {
    error.value = e.response?.data?.message || "Erreur lors de l'envoi. Veuillez réessayer."
  } finally {
    sending.value = false
  }
}

async function sendChatMessage() {
  if (!newMessage.value.trim() || sendingMessage.value) return
  sendingMessage.value = true
  try {
    const res = await discussionService.sendInviteMessage(guestToken.value, newMessage.value.trim())
    messages.value.push(res.data.data)
    newMessage.value = ''
    await nextTick()
    scrollChat()
  } catch {
    // Échec silencieux : le message n'est pas ajouté
  } finally {
    sendingMessage.value = false
  }
}

async function refresh() {
  if (!guestToken.value) return
  try {
    const res = await discussionService.getByToken(guestToken.value)
    messages.value = res.data.data.messages ?? []
    await nextTick()
    scrollChat()
  } catch {
    // Token invalide : on repart d'un formulaire vierge
    localStorage.removeItem(STORAGE_KEY)
    guestToken.value = null
  }
}

// Reprise d'une conversation existante si un token est déjà mémorisé
onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    guestToken.value = saved
    refresh()
  }
})
</script>

<template>
  <div class="contact-invite">
    <!-- Phase 1 : identité + premier message -->
    <template v-if="!guestToken">
      <div class="contact-invite__grid">
        <div class="contact-invite__field">
          <label>Nom *</label>
          <input v-model="form.nom" type="text" placeholder="Diallo" />
        </div>
        <div class="contact-invite__field">
          <label>Prénom</label>
          <input v-model="form.prenom" type="text" placeholder="Aminata" />
        </div>
        <div class="contact-invite__field">
          <label>Téléphone *</label>
          <input v-model="form.telephone" type="tel" placeholder="+221 77 000 00 00" />
        </div>
        <div class="contact-invite__field">
          <label>Email *</label>
          <input v-model="form.email" type="email" placeholder="vous@email.com" />
        </div>
      </div>
      <div class="contact-invite__field">
        <label>Votre message *</label>
        <textarea v-model="premierMessage" rows="3"
          placeholder="Ex. : Je suis intéressé par ce bien, pouvez-vous me donner plus d'informations ?" />
      </div>
      <p v-if="error" class="contact-invite__err">{{ error }}</p>
      <ButtonPrimary full-width :disabled="sending" @click="start">
        {{ sending ? 'Envoi…' : 'Envoyer le message' }}
      </ButtonPrimary>
    </template>

    <!-- Phase 2 : fil de discussion -->
    <template v-else>
      <div class="contact-invite__messages" ref="messagesRef">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="chat-bubble"
          :class="msg.senderRole === 'ADMIN' ? 'chat-bubble--left' : 'chat-bubble--right'"
        >
          <span class="chat-bubble__sender">{{ msg.senderRole === 'ADMIN' ? 'Agence' : 'Vous' }}</span>
          <div class="chat-bubble__text">{{ msg.contenu }}</div>
          <span class="chat-bubble__time">
            {{ new Date(msg.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}
          </span>
        </div>
      </div>

      <div class="contact-invite__input-row">
        <input
          v-model="newMessage"
          class="contact-invite__input"
          placeholder="Votre message…"
          @keydown.enter.prevent="sendChatMessage"
        />
        <button class="contact-invite__send" :disabled="sendingMessage || !newMessage.trim()" @click="sendChatMessage">
          <SvgIcon name="send" :size="16" />
        </button>
      </div>
      <p class="contact-invite__hint">
        <button class="contact-invite__refresh" type="button" @click="refresh">Actualiser</button>
        — votre conversation est mémorisée sur cet appareil.
      </p>
    </template>
  </div>
</template>

<style scoped>
.contact-invite { display: flex; flex-direction: column; gap: 0.85rem; }
.contact-invite__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.contact-invite__field { display: flex; flex-direction: column; gap: 0.3rem; }
.contact-invite__field label { font-size: 0.78rem; font-weight: 600; opacity: 0.7; }
.contact-invite__field input,
.contact-invite__field textarea {
  width: 100%; padding: 0.6rem 0.75rem; box-sizing: border-box;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  resize: vertical; transition: border-color 0.2s;
}
.contact-invite__field input:focus,
.contact-invite__field textarea:focus { border-color: var(--color-primary); outline: none; }
.contact-invite__err { font-size: 0.82rem; color: var(--color-accent); }

.contact-invite__messages {
  overflow-y: auto; padding: 0.5rem 0;
  display: flex; flex-direction: column; gap: 0.75rem;
  min-height: 180px; max-height: 300px;
}
.chat-bubble { display: flex; flex-direction: column; max-width: 75%; }
.chat-bubble--right { align-self: flex-end; align-items: flex-end; }
.chat-bubble--left { align-self: flex-start; align-items: flex-start; }
.chat-bubble__sender { font-size: 0.72rem; font-weight: 600; color: var(--color-text-muted); margin-bottom: 0.2rem; }
.chat-bubble__text { padding: 0.6rem 0.9rem; border-radius: 12px; font-size: 0.88rem; line-height: 1.5; word-break: break-word; }
.chat-bubble--right .chat-bubble__text { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; }
.chat-bubble--left .chat-bubble__text { background: var(--color-border); color: var(--color-text); border-bottom-left-radius: 4px; }
.chat-bubble__time { font-size: 0.68rem; color: var(--color-text-muted); margin-top: 0.2rem; }

.contact-invite__input-row { display: flex; gap: 0.5rem; align-items: center; }
.contact-invite__input {
  flex: 1; padding: 0.6rem 0.9rem;
  border: 1.5px solid var(--color-border); border-radius: 24px;
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  transition: border-color 0.2s;
}
.contact-invite__input:focus { border-color: var(--color-primary); outline: none; }
.contact-invite__send {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: opacity var(--transition);
  background: var(--color-primary); color: #fff;
}
.contact-invite__send:disabled { opacity: 0.4; cursor: not-allowed; }
.contact-invite__send:hover:not(:disabled) { opacity: 0.85; }
.contact-invite__hint { text-align: center; font-size: 0.76rem; color: var(--color-text-muted); }
.contact-invite__refresh {
  background: none; border: none; cursor: pointer; padding: 0;
  color: var(--color-primary); font-weight: 600; font-size: 0.76rem;
}

@media (max-width: 480px) {
  .contact-invite__grid { grid-template-columns: 1fr; }
}
</style>
