
const CLOUD_NAME    = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME
const UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET

if (!CLOUD_NAME || !UPLOAD_PRESET) {
  console.warn(
    '[cloudinaryService] Variables manquantes : VITE_CLOUDINARY_CLOUD_NAME ou VITE_CLOUDINARY_UPLOAD_PRESET. ' +
    'Définissez-les dans votre fichier .env'
  )
}

/**
 * Upload un seul fichier image vers Cloudinary.
 *
 * @param {File} file - Fichier image sélectionné par l'utilisateur
 * @returns {Promise<string>} URL sécurisée Cloudinary (secure_url)
 * @throws {Error} Si l'upload échoue ou si les variables d'env sont manquantes
 */
export async function uploadImage(file) {
  if (!CLOUD_NAME || !UPLOAD_PRESET) {
    throw new Error(
      'Configuration Cloudinary manquante. ' +
      'Vérifiez VITE_CLOUDINARY_CLOUD_NAME et VITE_CLOUDINARY_UPLOAD_PRESET dans votre .env'
    )
  }

  const formData = new FormData()
  formData.append('file',           file)
  formData.append('upload_preset',  UPLOAD_PRESET)
  formData.append('folder',         'immosn/annonces')

  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`,
    { method: 'POST', body: formData }
  )

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.error?.message ?? `Erreur Cloudinary : ${response.status}`)
  }

  const data = await response.json()
  return data.secure_url  // Ex: https://res.cloudinary.com/immosn/image/upload/v.../img.jpg
}

/**
 * Upload plusieurs fichiers image vers Cloudinary en parallèle.
 *
 * @param {File[]} files - Tableau de fichiers
 * @param {function(number, number): void} onProgress - Callback(uploaded, total)
 * @returns {Promise<string[]>} Tableau d'URLs sécurisées
 */
export async function uploadImages(files, onProgress = null) {
  if (!files?.length) return []

  let uploaded = 0
  const urls = await Promise.all(
    files.map(async (file) => {
      const url = await uploadImage(file)
      uploaded++
      if (onProgress) onProgress(uploaded, files.length)
      return url
    })
  )
  return urls
}

/**
 * Upload un fichier PDF (ou tout fichier brut) vers Cloudinary via l'endpoint /raw/upload.
 *
 * @param {File} file - Fichier PDF sélectionné par l'utilisateur
 * @returns {Promise<string>} URL sécurisée Cloudinary (secure_url)
 */
export async function uploadPdf(file) {
  if (!CLOUD_NAME || !UPLOAD_PRESET) {
    throw new Error(
      'Configuration Cloudinary manquante. ' +
      'Vérifiez VITE_CLOUDINARY_CLOUD_NAME et VITE_CLOUDINARY_UPLOAD_PRESET dans votre .env'
    )
  }

  const formData = new FormData()
  formData.append('file',          file)
  formData.append('upload_preset', UPLOAD_PRESET)
  formData.append('folder',        'immosn/contrats')

  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/raw/upload`,
    { method: 'POST', body: formData }
  )

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.error?.message ?? `Erreur Cloudinary : ${response.status}`)
  }

  const data = await response.json()
  return data.secure_url
}

/**
 * Valide qu'une URL provient bien de Cloudinary.
 * Utilisé côté frontend avant d'envoyer au backend.
 *
 * @param {string} url
 * @returns {boolean}
 */
export function isCloudinaryUrl(url) {
  return /^https:\/\/res\.cloudinary\.com\/.+/.test(url)
}

export default { uploadImage, uploadImages, uploadPdf, isCloudinaryUrl }
