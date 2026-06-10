import api from './api'

export default {
  // GET /api/v1/admin/dashboard/stats
  // Retourne DashboardStatsDto : compteurs globaux + activités récentes
  getStats() {
    return api.get('/admin/dashboard/stats')
  },
}
