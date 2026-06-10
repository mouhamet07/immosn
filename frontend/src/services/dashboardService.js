import api from './api'

export default {
  getStats() {
    return api.get('/admin/dashboard/stats')
  },
  getActivities(page = 0, size = 10, type = 'ALL') {
    return api.get('/admin/dashboard/activities', { params: { page, size, type } })
  },
}
