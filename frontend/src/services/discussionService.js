import api from './api'

export default {
  // POST /api/v1/discussions — créer ou récupérer une discussion (CLIENT)
  // Body: { annonceId, premierMessage }
  createOrGetDiscussion(annonceId, premierMessage) {
    return api.post('/discussions', { annonceId, premierMessage })
  },

  // GET /api/v1/discussions/client — discussions du client connecté
  getClientDiscussions(page = 0, size = 10) {
    return api.get('/discussions/client', { params: { page, size } })
  },

  // GET /api/v1/discussions/admin — toutes les discussions (ADMIN)
  getAllDiscussions(page = 0, size = 20) {
    return api.get('/discussions/admin', { params: { page, size } })
  },

  // GET /api/v1/discussions/{id}/messages — messages d'une discussion
  getMessages(discussionId) {
    return api.get(`/discussions/${discussionId}/messages`)
  },

  // POST /api/v1/discussions/{id}/messages — envoyer un message
  // Body: { contenu }
  sendMessage(discussionId, contenu) {
    return api.post(`/discussions/${discussionId}/messages`, { contenu })
  },
}
