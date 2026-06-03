import api from './api'

export default {
  getDepartements() {
    return api.get('/locations/departements')
  },
  getQuartiersByDepartement(departement) {
    return api.get('/locations/quartiers', { params: { departement } })
  },
  geocode(departement, quartier, adresse) {
    const params = { departement, quartier }
    if (adresse) params.adresse = adresse
    return api.get('/locations/geocode', { params })
  },
}
