<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Heart, Phone, Mail, Share2, FileText, Flag } from 'lucide-vue-next'
import ButtonPrimary from '@/components/ButtonPrimary.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import annonceService from '@/services/annonceService'
import favorisService from '@/services/favorisService'
import discussionService from '@/services/discussionService'
import { useAuthStore } from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import placeholderImg from '@/assets/Penthouse.png'
import LocationMap from '@/components/LocationMap.vue'
import logoImg from '@/assets/logo nav 1 - orange 1.png'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const toastStore = useToastStore()

const annonce = ref(null)
const loading = ref(false)
const error = ref('')
const isFavori = ref(false)
const imageActive = ref(0)

//  Modal visite 
import visiteService from '@/services/visiteService'
const showVisiteModal  = ref(false)
const visiteDate       = ref('')
const visiteComment    = ref('')
const visiteSending    = ref(false)
const visiteSuccess    = ref(false)
const visiteError      = ref('')

async function sendVisiteRequest() {
  if (!visiteDate.value) { visiteError.value = 'Veuillez choisir une date.'; return }
  visiteSending.value = true; visiteError.value = ''
  try {
    // datetime-local retourne "2026-05-24T10:30" — le backend attend "2026-05-24T10:30:00"
    const dateFormatted = visiteDate.value.length === 16 ? visiteDate.value + ':00' : visiteDate.value
    await visiteService.create(annonce.value.id, dateFormatted, visiteComment.value || null)
    visiteSuccess.value = true
  } catch (e) {
    visiteError.value = e.response?.data?.message || 'Erreur lors de l\'envoi.'
  } finally {
    visiteSending.value = false
  }
}

function closeVisiteModal() {
  showVisiteModal.value = false; visiteSuccess.value = false
  visiteDate.value = ''; visiteComment.value = ''; visiteError.value = ''
}

function handleReserverVisite() {
  if (!authStore.isAuthenticated) {
    localStorage.setItem('redirectAfterLogin', route.fullPath)
    toastStore.info('Connectez-vous pour réserver une visite')
    router.push('/connexion')
    return
  }
  showVisiteModal.value = true
  visiteSuccess.value = false
}

//  Modal contact 
const showContactModal = ref(false)
const contactMessage = ref('')
const contactSending = ref(false)
const contactError = ref('')
const contactSuccess = ref(false)
const discussionId = ref(null)
const messagesRef = ref(null)

// Messages du chat après ouverture de la discussion
const chatMessages = ref([])
const newMessage = ref('')
const sendingMessage = ref(false)

function handleContactAgent() {
  if (!authStore.isAuthenticated) {
    localStorage.setItem('redirectAfterLogin', route.fullPath)
    toastStore.info("Connectez-vous pour contacter l'agence")
    router.push('/connexion')
    return
  }
  showContactModal.value = true
  contactSuccess.value   = false
  contactError.value     = ''
}

async function sendFirstMessage() {
  if (!contactMessage.value.trim()) return
  contactSending.value = true
  contactError.value   = ''
  try {
    const res = await discussionService.createOrGetDiscussion(
      annonce.value.id,
      contactMessage.value.trim()
    )
    discussionId.value  = res.data.data.id
    chatMessages.value  = res.data.data.messages ?? []
    contactSuccess.value = true
    contactMessage.value = ''
    await nextTick()
    scrollChat()
  } catch {
    contactError.value = 'Erreur lors de l\'envoi. Veuillez réessayer.'
  } finally {
    contactSending.value = false
  }
}

async function sendChatMessage() {
  if (!newMessage.value.trim() || sendingMessage.value) return
  sendingMessage.value = true
  try {
    const res = await discussionService.sendMessage(discussionId.value, newMessage.value.trim())
    chatMessages.value.push(res.data.data)
    newMessage.value = ''
    await nextTick()
    scrollChat()
  } catch {
    // Silently fail — message not sent
  } finally {
    sendingMessage.value = false
  }
}

function scrollChat() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function closeModal() {
  showContactModal.value = false
  contactSuccess.value   = false
  contactMessage.value   = ''
  newMessage.value       = ''
  chatMessages.value     = []
  discussionId.value     = null
}

// FIX 3 : toggle favori via favorisService
const toggleFavori = async () => {
  if (!authStore.isAuthenticated) {
    localStorage.setItem('redirectAfterLogin', route.fullPath)
    router.push('/connexion')
    return
  }
  try {
    await favorisService.toggle(annonce.value.id)
    isFavori.value = !isFavori.value
    toastStore[isFavori.value ? 'success' : 'info'](
      isFavori.value ? 'Ajouté aux favoris' : 'Retiré des favoris'
    )
  } catch {
    toastStore.error('Erreur lors de la mise à jour des favoris')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const response = await annonceService.getAnnonceById(route.params.id)
    annonce.value = response.data.data
  } catch (e) {
    error.value = e.response?.data?.message || 'Annonce introuvable ou une erreur est survenue.'
    return
  } finally {
    loading.value = false
  }

  if (authStore.isAuthenticated) {
    try {
      const res = await favorisService.checkFavoris(route.params.id)
      isFavori.value = res.data?.data ?? false
    } catch {
      // Ne pas bloquer l'affichage de l'annonce si la vérification des favoris échoue.
      isFavori.value = false
    }
  }
})

function formatPrix(prix) {
  return new Intl.NumberFormat('fr-SN').format(prix) + ' FCFA'
}

function toUSD(prix) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(prix / 600)
}

// Retourner placeholder si pas d'image (dév)
function getImage(index) {
  return annonce.value?.images?.[index] || placeholderImg
}
</script>

<template>
  <div class="detail-page">
    <div v-if="loading" class="detail-loading">
      <div class="detail-loading__spinner"></div>
      <p>Chargement de l'annonce...</p>
    </div>

    <div v-else-if="error" class="detail-error">
      <p>{{ error }}</p>
      <ButtonPrimary @click="router.push({ name: 'annonces' })">← Retour aux annonces</ButtonPrimary>
    </div>

    <main v-else-if="annonce" class="detail-main">

      <!-- Galerie Figma : 2/3 gauche + 1/3 droite -->
      <section class="detail-gallery">
        <!-- Image principale -->
        <div class="detail-gallery__main">
          <img :src="getImage(imageActive)" :alt="annonce.libelle" class="detail-gallery__img" />
          <!-- FIX 3 : Heart Lucide avec état favori réel -->
          <button
            class="detail-gallery__fav"
            :class="{ 'detail-gallery__fav--active': isFavori }"
            @click="toggleFavori"
            :title="isFavori ? 'Retirer des favoris' : 'Ajouter aux favoris'"
          >
            <Heart
              :size="18"
              :fill="isFavori ? 'var(--color-accent)' : 'none'"
              :color="isFavori ? 'var(--color-accent)' : 'currentColor'"
            />
          </button>
        </div>
        <!-- Miniatures empilées -->
        <div class="detail-gallery__side">
          <div class="detail-gallery__thumb" @click="imageActive = 1">
            <img :src="getImage(1)" alt="Photo 2" />
          </div>
          <div class="detail-gallery__thumb detail-gallery__thumb--more" @click="imageActive = 2">
            <img :src="getImage(2)" alt="Photo 3" />
            <div v-if="annonce.images?.length > 3" class="detail-gallery__overlay">+{{ annonce.images.length - 3 }} Plus</div>
          </div>
        </div>
      </section>

      <!-- Corps : contenu + sidebar -->
      <div class="detail-body">
        <div class="detail-content">

          <!-- Titre + prix -->
          <div class="detail-header">
            <div>
              <h1 class="detail-header__title">{{ annonce.libelle }}</h1>
              <p class="detail-adresse">
                <SvgIcon name="map-pin" :size="14" />
                {{ annonce.adresse }}
              </p>
            </div>
            <div class="detail-header__prix">
              <span class="detail-header__fcfa">{{ formatPrix(annonce.prix) }}</span>
              <span class="detail-header__usd">≈ {{ toUSD(annonce.prix) }}</span>
            </div>
          </div>

          <!-- Bento stats -->
          <div class="detail-bento">
            <div class="detail-bento__item">
              <SvgIcon name="bed" :size="28" class="detail-bento__icon" />
              <span class="detail-bento__label">{{ annonce.nbrPieces }} Chambres</span>
            </div>
            <div class="detail-bento__item">
              <SvgIcon name="droplet" :size="28" class="detail-bento__icon" />
              <span class="detail-bento__label">Salles de bain</span>
            </div>
            <div class="detail-bento__item">
              <SvgIcon name="maximize" :size="28" class="detail-bento__icon" />
              <span class="detail-bento__label">{{ annonce.surface }} m²</span>
            </div>
            <div class="detail-bento__item">
              <SvgIcon name="check-circle" :size="28" class="detail-bento__icon" />
              <span class="detail-bento__label">{{ annonce.typeBien?.libelle || 'Bien' }}</span>
            </div>
          </div>

          <!-- Description -->
          <section class="detail-section">
            <h2 class="detail-section__title">Présentation architecturale</h2>
            <p class="detail-section__text">{{ annonce.description }}</p>
          </section>

          <!-- Équipements -->
          <section class="detail-section">
            <h2 class="detail-section__title">Équipements et caractéristiques</h2>
            <div v-if="annonce.commodites?.length" class="detail-commodites">
              <span v-for="(item, i) in annonce.commodites" :key="i" class="detail-commodite">
                <SvgIcon name="check-circle" :size="12" class="detail-commodite__icon" />
                {{ item.libelle }}
              </span>
            </div>
            <p v-else class="detail-section__text">Aucun équipement renseigné.</p>
          </section>

          <!-- Emplacement -->
          <section class="detail-section">
            <h2 class="detail-section__title">Emplacement</h2>
            <p v-if="annonce.departement || annonce.quartier" class="detail-location-label">
              <SvgIcon name="map-pin" :size="13" />
              {{ [annonce.quartier, annonce.departement].filter(Boolean).join(', ') }}
            </p>
            <LocationMap
              :latitude="annonce.latitude"
              :longitude="annonce.longitude"
              :draggable="false"
              height="260px"
              :zoom="15"
            />
          </section>
        </div>

        <!-- Sidebar agent — FIX 1 : branding ImmoSN -->
        <aside class="detail-sidebar">
          <div class="detail-sidebar__card">
            <div class="detail-agent">
              <div class="agent-logo-wrapper">
                <img :src="logoImg" alt="ImmoSN" class="agent-logo" />
              </div>
              <div>
                <p class="agent-name">ImmoSN</p>
                <p class="agent-title">Agence Immobilière Sénégalaise</p>
              </div>
            </div>

            <div class="detail-sidebar__contact">
              <p class="detail-sidebar__contact-item">
                <Phone :size="15" color="var(--color-primary)" />
                <a href="tel:+221770000000">+221 77 000 00 00</a>
              </p>
              <p class="detail-sidebar__contact-item">
                <Mail :size="15" color="var(--color-primary)" />
                <a href="mailto:contact@immosn.sn">contact@immosn.sn</a>
              </p>
            </div>

            <!-- FIX 2 : boutons avec guard auth -->
            <div class="detail-sidebar__actions">
              <ButtonPrimary full-width @click="handleContactAgent">Contacter l'agent</ButtonPrimary>
              <ButtonPrimary variant="outline" full-width @click="handleReserverVisite">Réserver une visite privée</ButtonPrimary>
            </div>

            <div class="detail-sidebar__links">
              <button class="detail-sidebar__link"><Share2 :size="13" /> Partager</button>
              <button class="detail-sidebar__link"><FileText :size="13" /> Brochure</button>
              <button class="detail-sidebar__link"><Flag :size="13" /> Signaler</button>
            </div>
          </div>

          <!-- Aperçu du marché -->
          <div class="detail-sidebar__market">
            <div class="detail-sidebar__market-header">
              <SvgIcon name="maximize" :size="16" />
              <span>Aperçu du marché</span>
            </div>
            <p class="detail-sidebar__market-text">
              Les biens similaires dans ce quartier sont estimés entre
              <strong>{{ formatPrix(annonce.prix * 0.85) }}</strong> et
              <strong>{{ formatPrix(annonce.prix * 1.15) }}</strong>.
            </p>
          </div>
        </aside>
      </div>
    </main>

    <!--  Modal Demande de Visite ─ -->
    <Teleport to="body">
      <div v-if="showVisiteModal" class="modal-overlay" @click.self="closeVisiteModal">
        <div class="modal-chat">
          <div class="modal-chat__header">
            <div>
              <p class="modal-chat__title">Demander une visite</p>
              <p class="modal-chat__sub">{{ annonce?.libelle }}</p>
            </div>
            <button class="modal-chat__close" @click="closeVisiteModal">
              <SvgIcon name="x" :size="16" />
            </button>
          </div>

          <template v-if="!visiteSuccess">
            <div class="modal-chat__compose" style="padding-top: 1.25rem;">
              <div style="display:flex;flex-direction:column;gap:.4rem;margin-bottom:.75rem;">
                <label style="font-size:.78rem;font-weight:600;opacity:.7;">Date et heure souhaitées *</label>
                <input v-model="visiteDate" type="datetime-local" class="modal-chat__textarea"
                  style="resize:none;padding:.65rem .9rem;" />
              </div>
              <div style="display:flex;flex-direction:column;gap:.4rem;margin-bottom:.75rem;">
                <label style="font-size:.78rem;font-weight:600;opacity:.7;">Commentaire (optionnel)</label>
                <textarea v-model="visiteComment" class="modal-chat__textarea" rows="3"
                  placeholder="Ex. : Disponible en matinée, préférence pour le week-end…" />
              </div>
              <p v-if="visiteError" class="modal-chat__err">{{ visiteError }}</p>
              <ButtonPrimary full-width :disabled="visiteSending || !visiteDate" @click="sendVisiteRequest">
                {{ visiteSending ? 'Envoi…' : 'Envoyer la demande' }}
              </ButtonPrimary>
            </div>
          </template>

          <template v-else>
            <div class="modal-chat__compose" style="text-align:center;padding:2rem 1.5rem;">
              <div class="modal-success__icon"><SvgIcon name="check-circle" :size="48" /></div>
              <p style="font-weight:700;margin:.75rem 0 .4rem;">Demande envoyée !</p>
              <p style="font-size:.88rem;opacity:.65;margin-bottom:1rem;">
                Votre demande de visite a été transmise à l'agence. Vous serez notifié rapidement.
              </p>
              <RouterLink to="/mes-visites" class="modal-chat__hint" @click="closeVisiteModal">
                Voir mes demandes →
              </RouterLink>
            </div>
          </template>
        </div>
      </div>
    </Teleport>

    <!--  Modal Contact Agence ─ -->
    <Teleport to="body">
      <div v-if="showContactModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-chat">
          <!-- Header -->
          <div class="modal-chat__header">
            <div>
              <p class="modal-chat__title">Contacter l'agence</p>
              <p class="modal-chat__sub">{{ annonce?.libelle }}</p>
            </div>
            <button class="modal-chat__close" @click="closeModal">
              <SvgIcon name="x" :size="16" />
            </button>
          </div>

          <!-- Phase 1 : premier message -->
          <template v-if="!contactSuccess">
            <div class="modal-chat__intro">
              <p>Bonjour ! Présentez brièvement votre demande et l'agence vous répondra rapidement.</p>
            </div>
            <div class="modal-chat__compose">
              <textarea
                v-model="contactMessage"
                class="modal-chat__textarea"
                rows="4"
                placeholder="Ex. : Je suis intéressé par ce bien, pouvez-vous me donner plus d'informations ?"
              ></textarea>
              <p v-if="contactError" class="modal-chat__err">{{ contactError }}</p>
              <ButtonPrimary
                full-width
                :disabled="contactSending || !contactMessage.trim()"
                @click="sendFirstMessage"
              >
                {{ contactSending ? 'Envoi…' : 'Envoyer le message' }}
              </ButtonPrimary>
            </div>
          </template>

          <!-- Phase 2 : fil de discussion -->
          <template v-else>
            <div class="modal-chat__messages" ref="messagesRef">
              <div
                v-for="msg in chatMessages"
                :key="msg.id"
                class="chat-bubble"
                :class="msg.senderRole === 'CLIENT' ? 'chat-bubble--right' : 'chat-bubble--left'"
              >
                <span class="chat-bubble__sender">{{ msg.senderRole === 'ADMIN' ? 'Agence' : 'Vous' }}</span>
                <div class="chat-bubble__text">{{ msg.contenu }}</div>
                <span class="chat-bubble__time">{{ new Date(msg.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}</span>
              </div>
            </div>

            <div class="modal-chat__input-row">
              <input
                v-model="newMessage"
                class="modal-chat__input"
                placeholder="Votre message…"
                @keydown.enter.prevent="sendChatMessage"
              />
              <button
                class="modal-chat__send"
                :disabled="sendingMessage || !newMessage.trim()"
                @click="sendChatMessage"
              >
                <SvgIcon name="send" :size="16" />
              </button>
            </div>

            <p class="modal-chat__hint">
              Voir toutes vos discussions dans
              <RouterLink to="/discussions" @click="closeModal">Mon espace</RouterLink>
            </p>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.detail-page { background: var(--color-background); }
.detail-main { max-width: 1280px; margin: 0 auto; padding: 1.5rem 1.5rem 4rem; }

.detail-loading {
  display: flex; flex-direction: column; align-items: center;
  gap: 1rem; padding: 6rem; color: var(--color-text-muted);
}
.detail-loading__spinner {
  width: 48px; height: 48px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.detail-error {
  display: flex; flex-direction: column; align-items: center;
  gap: 1.5rem; padding: 4rem; text-align: center; color: var(--color-accent);
}

/* Galerie */
.detail-gallery {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0.5rem;
  height: 520px;
  border-radius: var(--radius);
  overflow: hidden;
  margin-bottom: 2.5rem;
}
.detail-gallery__main {
  position: relative; overflow: hidden;
}
.detail-gallery__img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}
.detail-gallery__main:hover .detail-gallery__img { transform: scale(1.03); }
.detail-gallery__fav {
  position: absolute; top: 1rem; right: 1rem;
  width: 40px; height: 40px; border-radius: 50%;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-text-muted);
  transition: color var(--transition);
}
.detail-gallery__fav:hover { color: var(--color-accent); }
.detail-gallery__fav--active { color: var(--color-danger); }
.detail-gallery__side {
  display: grid; grid-template-rows: 1fr 1fr; gap: 0.5rem;
}
.detail-gallery__thumb {
  position: relative; overflow: hidden; cursor: pointer;
}
.detail-gallery__thumb img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.4s;
}
.detail-gallery__thumb:hover img { transform: scale(1.06); }
.detail-gallery__overlay {
  position: absolute; inset: 0;
  background: rgba(30,37,50,0.55);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.1rem; font-weight: 700;
}

/* Corps */
.detail-body {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 2.5rem;
  align-items: start;
}

/* Header */
.detail-header {
  display: flex; justify-content: space-between;
  align-items: flex-start; gap: 1rem;
  margin-bottom: 1.5rem; flex-wrap: wrap;
}
.detail-header__title {
  font-family: var(--font-serif);
  font-size: 2.2rem; font-weight: 700;
  color: var(--color-primary); line-height: 1.15;
}
.detail-header__prix { text-align: right; }
.detail-header__fcfa {
  display: block; font-size: 1.5rem; font-weight: 800;
  color: var(--color-accent);
}
.detail-header__usd {
  font-size: 0.85rem; color: var(--color-text-muted);
}
.detail-adresse {
  display: flex; align-items: center; gap: 0.35rem;
  font-size: 0.9rem; color: var(--color-text-muted);
  margin-bottom: 1.75rem;
}

/* Bento stats */
.detail-bento {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 1rem; margin-bottom: 2rem;
}
.detail-bento__item {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 1.25rem 1rem;
  display: flex; flex-direction: column;
  align-items: center; gap: 0.5rem;
  text-align: center;
}
.detail-bento__icon { color: var(--color-primary); }
.detail-bento__label {
  font-size: 0.82rem; font-weight: 600;
  color: var(--color-text);
}

/* Sections */
.detail-section { margin-bottom: 2rem; }
.detail-section__title {
  font-family: var(--font-serif);
  font-size: 1.25rem; font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 0.75rem;
}
.detail-section__text {
  color: var(--color-text-muted); line-height: 1.75; font-size: 0.95rem;
}

/* Commodités */
.detail-commodites {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.detail-commodite {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.75rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--color-text);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  white-space: nowrap;
}
.detail-commodite__icon { color: var(--color-primary); flex-shrink: 0; }

/* Localisation */
.detail-location-label {
  display: flex; align-items: center; gap: 0.35rem;
  font-size: 0.85rem; color: var(--color-text-muted);
  margin-bottom: 0.6rem;
}

/* Sidebar */
.detail-sidebar {
  display: flex; flex-direction: column; gap: 1rem;
  position: sticky; top: 80px;
}
.detail-sidebar__card {
  background: var(--color-card);
  border-radius: var(--radius);
  border: 1px solid var(--color-border);
  padding: 1.5rem;
  box-shadow: var(--shadow-card);
}

/* Agent — branding ImmoSN */
.detail-agent {
  display: flex; align-items: center; gap: 1rem;
  margin-bottom: 1.25rem;
}
.agent-logo-wrapper {
  width: 56px; height: 56px; flex-shrink: 0;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  background: var(--color-card);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  padding: 4px;
  box-sizing: border-box;
}
.agent-logo { width: 100%; height: 100%; object-fit: contain; }
.agent-name { font-size: 1rem; font-weight: 600; color: var(--color-text); }
.agent-title {
  font-size: 0.7rem; font-weight: 700;
  letter-spacing: 0.05em; text-transform: uppercase;
  color: var(--color-primary); margin-top: 0.15rem;
}
.detail-sidebar__link {
  display: flex; align-items: center; gap: 0.3rem;
  font-size: 0.82rem; color: var(--color-text-muted);
  transition: color var(--transition);
  background: none; border: none; cursor: pointer;
}
.detail-sidebar__link:hover { color: var(--color-primary); }

/* Market */
.detail-sidebar__market {
  background: rgba(var(--color-primary-rgb), 0.06);
  border: 1px solid rgba(var(--color-primary-rgb), 0.15);
  border-radius: var(--radius); padding: 1.25rem;
}
.detail-sidebar__market-header {
  display: flex; align-items: center; gap: 0.5rem;
  color: var(--color-primary); font-weight: 700;
  font-size: 0.9rem; margin-bottom: 0.6rem;
}
.detail-sidebar__market-text {
  font-size: 0.88rem; color: var(--color-text-muted); line-height: 1.6;
}

/* Modals — inchangés */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.55);
  z-index: 1000; display: flex; align-items: center;
  justify-content: center; padding: 1rem;
}
.modal-chat {
  background: var(--color-card); border-radius: var(--radius);
  width: 100%; max-width: 480px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
  display: flex; flex-direction: column;
  max-height: 90vh; overflow: hidden;
}
.modal-chat__header {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--color-border);
}
.modal-chat__title { font-size: 1rem; font-weight: 700; color: var(--color-text); }
.modal-chat__sub { font-size: 0.82rem; color: var(--color-text-muted); margin-top: 0.15rem; }
.modal-chat__close {
  background: none; cursor: pointer; color: var(--color-text-muted);
  padding: 0.2rem; display: flex; align-items: center;
  border-radius: var(--radius-xs); transition: color var(--transition);
}
.modal-chat__close:hover { color: var(--color-text); }
.modal-chat__intro { padding: 1.25rem 1.5rem 0; font-size: 0.88rem; color: var(--color-text-muted); line-height: 1.6; }
.modal-chat__compose { display: flex; flex-direction: column; gap: 0.75rem; padding: 1rem 1.5rem 1.5rem; }
.modal-chat__textarea {
  width: 100%; padding: 0.75rem;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  resize: vertical; box-sizing: border-box; transition: border-color 0.2s;
}
.modal-chat__textarea:focus { border-color: var(--color-primary); outline: none; }
.modal-chat__err { font-size: 0.82rem; color: var(--color-accent); }
.modal-chat__messages {
  flex: 1; overflow-y: auto; padding: 1rem 1.5rem;
  display: flex; flex-direction: column; gap: 0.75rem;
  min-height: 200px; max-height: 320px;
}
.chat-bubble { display: flex; flex-direction: column; max-width: 75%; }
.chat-bubble--right { align-self: flex-end; align-items: flex-end; }
.chat-bubble--left { align-self: flex-start; align-items: flex-start; }
.chat-bubble__sender { font-size: 0.72rem; font-weight: 600; color: var(--color-text-muted); margin-bottom: 0.2rem; }
.chat-bubble__text { padding: 0.6rem 0.9rem; border-radius: 12px; font-size: 0.88rem; line-height: 1.5; word-break: break-word; }
.chat-bubble--right .chat-bubble__text { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; }
.chat-bubble--left .chat-bubble__text { background: var(--color-border); color: var(--color-text); border-bottom-left-radius: 4px; }
.chat-bubble__time { font-size: 0.68rem; color: var(--color-text-muted); margin-top: 0.2rem; }
.modal-chat__input-row { display: flex; gap: 0.5rem; padding: 0.75rem 1.5rem; border-top: 1px solid var(--color-border); }
.modal-chat__input {
  flex: 1; padding: 0.6rem 0.9rem;
  border: 1.5px solid var(--color-border); border-radius: 24px;
  font-size: 0.9rem; color: var(--color-text); background: var(--color-background);
  transition: border-color 0.2s;
}
.modal-chat__input:focus { border-color: var(--color-primary); outline: none; }
.modal-success__icon { color: var(--color-primary); display: flex; justify-content: center; margin-bottom: 0.5rem; }
.modal-chat__send {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--color-primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  transition: opacity var(--transition); flex-shrink: 0;
}
.modal-chat__send:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-chat__send:hover:not(:disabled) { opacity: 0.85; }
.modal-chat__hint { text-align: center; font-size: 0.78rem; color: var(--color-text-muted); padding: 0.5rem 1.5rem 1rem; }
.modal-chat__hint a { color: var(--color-primary); font-weight: 600; }

/* Sidebar contact / actions / links — manquaient */
.detail-sidebar__contact {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border);
}
.detail-sidebar__contact-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: var(--color-text);
}
.detail-sidebar__actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}
.detail-sidebar__links {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

/* Responsive */
@media (max-width: 1024px) {
  .detail-bento { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .detail-gallery { grid-template-columns: 1fr; height: 300px; }
  .detail-gallery__side { display: none; }
  .detail-body { grid-template-columns: 1fr; }
  .detail-sidebar { position: static; }
}
@media (max-width: 600px) {
  .detail-main { padding: 1rem 1rem 3rem; }
  .detail-header { flex-direction: column; }
  .detail-header__prix { text-align: left; }
  .detail-header__title { font-size: 1.65rem; }
  .detail-bento { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .detail-gallery { height: 220px; }
  .detail-header__title { font-size: 1.4rem; }
}
</style>
