import api from './api'

export default {
  getDepartements() {
    return api.get('/locations/departements')
  },
  getQuartiersByDepartement(departement) {
    return api.get('/locations/quartiers', { params: { departement } })
  },
}
