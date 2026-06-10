import api from '@/services/api'

const BASE = '/api/v1/notifications'

export const notificationApiService = {
  getUnread:      ()             => api.get(`${BASE}/unread`),
  getUnreadCount: ()             => api.get(`${BASE}/unread/count`),
  getHistory:     (page = 0, size = 20) => api.get(BASE, { params: { page, size } }),
  markAsRead:     (id)           => api.post(`${BASE}/${id}/read`),
  markAllAsRead:  ()             => api.post(`${BASE}/read-all`),
}
