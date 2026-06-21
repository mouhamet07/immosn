# ImmoSN API — Documentation

Plateforme immobilière sénégalaise — Spring Boot 4 · PostgreSQL · JWT · Docker

---

## Table des matières

1. [Présentation générale](#présentation-générale)
2. [Architecture technique](#architecture-technique)
3. [Authentification JWT](#authentification-jwt)
4. [Rôles et permissions](#rôles-et-permissions)
5. [Format des réponses](#format-des-réponses)
6. [Guide Swagger UI](#guide-swagger-ui)
7. [Guide Postman](#guide-postman)
8. [Liste complète des endpoints](#liste-complète-des-endpoints)
9. [Exemples de requêtes et réponses](#exemples-de-requêtes-et-réponses)
10. [Workflows utilisateur](#workflows-utilisateur)
11. [Workflows administrateur](#workflows-administrateur)
12. [Workflows super administrateur](#workflows-super-administrateur)

---

## Présentation générale

ImmoSN est une plateforme SaaS immobilière conçue pour le marché sénégalais.
Elle permet la mise en relation entre propriétaires/agences et locataires/acheteurs.

**Fonctionnalités principales :**

| Fonctionnalité | Description |
|----------------|-------------|
| Annonces | Publication et gestion de biens (vente/location) avec géolocalisation |
| Recherche | Filtres multicritères : type, prix, localisation, commodités |
| Favoris | Sauvegarde d'annonces par les clients |
| Visites | Demandes, planification et suivi de visites immobilières |
| Messagerie | Discussions client/agent par annonce |
| Contrats | Création, suivi, résiliation et prolongation |
| Signalements | Remontée et traitement de litiges contractuels |
| Leads | Pipeline commercial pour qualifier les prospects |
| Dashboard | Tableau de bord d'administration avec statistiques temps réel |

---

## Architecture technique

```
┌─────────────────────────────────────────────────┐
│                    Nginx (reverse proxy)         │
│              Port 80 / 443 (HTTPS)               │
└────────────────────┬────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌─────────▼──────────┐
│  Vue.js 3      │      │  Spring Boot 4      │
│  (frontend)    │      │  (backend API)      │
│  Port 5173     │      │  Port 8080          │
└────────────────┘      └─────────┬──────────┘
                                  │
                         ┌────────▼────────┐
                         │  PostgreSQL      │
                         │  (Neon cloud)   │
                         └─────────────────┘
```

**Stack technique :**
- Backend : Spring Boot 4.0.6, Java 21, Spring Security, Spring Data JPA
- Base de données : PostgreSQL (Neon), H2 (développement)
- Authentification : JWT (jjwt 0.11.5)
- Documentation : SpringDoc OpenAPI 2.6.0 (Swagger UI)
- Conteneurisation : Docker, Docker Compose
- CI/CD : GitHub Actions → déploiement VPS

---

## Authentification JWT

### Flux d'authentification

```
1. Client → POST /api/v1/auth/login     {"email": "...", "motDePasse": "..."}
2. Serveur → 200 OK                     {"data": {"accessToken": "eyJ...", "tokenType": "Bearer"}}
3. Client → GET /api/v1/favoris         Authorization: Bearer eyJ...
4. Serveur → 200 OK                     (réponse protégée)
```

### Caractéristiques du token

- **Algorithme :** HMAC-SHA256
- **Durée de vie :** Configurable via `application.properties`
- **Invalidation :** Via `POST /api/v1/auth/logout` (blacklist en base de données)
- **Format en-tête :** `Authorization: Bearer <token>`

### Mécanisme de blacklist

Lors de la déconnexion, le token est stocké dans la table `BlacklistedToken`.
Le filtre JWT vérifie à chaque requête que le token n'est pas blacklisté.

---

## Rôles et permissions

### Hiérarchie des rôles

```
SUPER_ADMIN
    └── ADMIN (hérite + gestion admins)
         └── CLIENT (accès restreint)
              └── PUBLIC (non authentifié)
```

### Matrice des permissions

| Ressource | PUBLIC | CLIENT | ADMIN | SUPER_ADMIN |
|-----------|--------|--------|-------|-------------|
| Voir les annonces | ✅ | ✅ | ✅ | ✅ |
| Recherche multicritère | ✅ | ✅ | ✅ | ✅ |
| Référentiels (types, commodités) | ✅ (lecture) | ✅ (lecture) | ✅ (tout) | ✅ (tout) |
| Localisation / Géocodage | ✅ | ✅ | ✅ | ✅ |
| Créer/modifier une annonce | ❌ | ❌ | ✅ | ✅ |
| Favoris | ❌ | ✅ | ❌ | ❌ |
| Demandes de visite | ❌ | ✅ (les siennes) | ✅ (toutes) | ✅ (toutes) |
| Discussions | ❌ | ✅ (les siennes) | ✅ (toutes) | ✅ (toutes) |
| Contrats | ❌ | ✅ (les siens) | ✅ (créer/modifier) | ✅ |
| Signalements | ❌ | ✅ (les siens) | ✅ (traiter) | ✅ |
| Leads | ❌ | ❌ | ✅ | ✅ |
| Dashboard | ❌ | ❌ | ✅ | ✅ |
| Gérer les admins | ❌ | ❌ | ❌ | ✅ |

---

## Format des réponses

### Réponse standard `RestResponse<T>`

```json
{
  "success": true,
  "status": 200,
  "message": "Opération réalisée avec succès",
  "data": { },
  "timestamp": "2024-01-15T10:30:00"
}
```

### Réponse paginée `PagedResponse<T>`

```json
{
  "content": [ ],
  "page": 0,
  "size": 12,
  "totalElements": 48,
  "totalPages": 4,
  "last": false
}
```

### Codes HTTP utilisés

| Code | Signification | Cas d'usage |
|------|---------------|-------------|
| 200 | OK | Lecture, mise à jour réussie |
| 201 | Created | Création d'une ressource |
| 204 | No Content | Suppression, archivage, déconnexion |
| 400 | Bad Request | Validation échouée, données invalides |
| 401 | Unauthorized | Token manquant, expiré ou invalidé |
| 403 | Forbidden | Rôle insuffisant |
| 404 | Not Found | Ressource inexistante |
| 409 | Conflict | Doublon (email, libellé unique) |
| 500 | Server Error | Erreur interne inattendue |

---

## Guide Swagger UI

### Accès

| Environnement | URL |
|---------------|-----|
| Développement | http://localhost:8080/swagger-ui.html |
| Production | https://api.immosn.sn/swagger-ui.html |
| API Docs JSON | http://localhost:8080/v3/api-docs |

### Groupes disponibles dans Swagger UI

Le menu déroulant en haut à droite de Swagger UI permet de sélectionner un groupe :

| Groupe | Périmètre |
|--------|-----------|
| AUTHENTIFICATION | `/api/v1/auth/**` |
| ANNONCES | `/api/v1/annonces/**` |
| TYPES DE BIENS | `/api/v1/types-bien/**` |
| COMMODITES | `/api/v1/commodites/**` |
| FAVORIS | `/api/v1/favoris/**` |
| VISITES | `/api/v1/visites/**` |
| DISCUSSIONS | `/api/v1/discussions/**` |
| CONTRATS | `/api/v1/contrats/**` |
| SIGNALEMENTS | `/api/v1/signalements/**` |
| LEADS | `/api/v1/leads/**` |
| ADMINISTRATION | `/api/v1/admin/**` |
| LOCALISATION | `/api/v1/locations/**` |

### S'authentifier dans Swagger UI

1. Appeler `POST /api/v1/auth/login` via Swagger
2. Copier la valeur de `data.accessToken` dans la réponse
3. Cliquer sur le bouton **Authorize** 🔒 (en haut à droite)
4. Dans le champ `bearerAuth`, saisir le token (sans le préfixe "Bearer")
5. Cliquer sur **Authorize** puis **Close**
6. Tous les endpoints protégés incluent désormais le token automatiquement

---

## Guide Postman

### Variables d'environnement recommandées

```
BASE_URL    = http://localhost:8080/api/v1
TOKEN       = (vide — à remplir après login)
```

### Script de pre-request pour auto-refresh du token

```javascript
// Dans la collection Postman, ajouter en Pre-request Script :
const loginResponse = pm.sendRequest({
    url: pm.environment.get("BASE_URL") + "/auth/login",
    method: "POST",
    header: { "Content-Type": "application/json" },
    body: {
        mode: "raw",
        raw: JSON.stringify({
            email: pm.environment.get("USER_EMAIL"),
            motDePasse: pm.environment.get("USER_PASSWORD")
        })
    }
}, (err, res) => {
    const token = res.json().data.accessToken;
    pm.environment.set("TOKEN", token);
});
```

### En-tête d'autorisation

```
Authorization: Bearer {{TOKEN}}
```

---

## Liste complète des endpoints

### AUTHENTIFICATION `/api/v1/auth`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/register` | PUBLIC | Inscription d'un nouveau client |
| POST | `/login` | PUBLIC | Connexion — retourne un token JWT |
| POST | `/logout` | PUBLIC | Déconnexion — invalide le token |
| GET | `/profile` | Authentifié | Consulter mon profil |
| PUT | `/profile` | Authentifié | Mettre à jour mon profil |
| POST | `/admin` | SUPER_ADMIN | Créer un compte administrateur |
| GET | `/admins` | SUPER_ADMIN | Lister tous les administrateurs |
| PATCH | `/admins/{id}/archive` | SUPER_ADMIN | Archiver un admin |
| PATCH | `/admins/{id}/restore` | SUPER_ADMIN | Restaurer un admin archivé |
| PATCH | `/admins/{id}/revoke` | SUPER_ADMIN | Révoquer le rôle admin |

### ANNONCES `/api/v1/annonces`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/` | PUBLIC | Lister les annonces actives (paginé) |
| GET | `/{id}` | PUBLIC | Détail d'une annonce |
| POST | `/search` | PUBLIC | Recherche multicritère |
| POST | `/` | ADMIN | Créer une annonce |
| PUT | `/{id}` | ADMIN | Modifier une annonce |
| DELETE | `/{id}` | ADMIN | Archiver une annonce |
| PATCH | `/{id}/restore` | ADMIN | Restaurer une annonce archivée |
| GET | `/admin` | ADMIN | Toutes les annonces (actives + archivées) |

### TYPES DE BIENS `/api/v1/types-bien`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/` | PUBLIC | Lister les types actifs |
| GET | `/paged` | PUBLIC | Lister tous les types (paginé) |
| GET | `/{id}` | PUBLIC | Détail d'un type de bien |
| POST | `/` | ADMIN | Créer un type de bien |
| PUT | `/{id}` | ADMIN | Modifier un type de bien |
| DELETE | `/{id}` | ADMIN | Archiver un type de bien |
| PATCH | `/{id}/restore` | ADMIN | Restaurer un type archivé |

### COMMODITES `/api/v1/commodites`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/` | PUBLIC | Lister les commodités actives |
| GET | `/paged` | PUBLIC | Lister toutes les commodités (paginé) |
| GET | `/{id}` | PUBLIC | Détail d'une commodité |
| POST | `/` | ADMIN | Créer une commodité |
| PUT | `/{id}` | ADMIN | Modifier une commodité |
| DELETE | `/{id}` | ADMIN | Archiver une commodité |
| PATCH | `/{id}/restore` | ADMIN | Restaurer une commodité archivée |

### FAVORIS `/api/v1/favoris`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/` | CLIENT | Mes annonces favorites (paginé) |
| GET | `/ids` | CLIENT | IDs de tous mes favoris |
| POST | `/{annonceId}/toggle` | CLIENT | Ajouter/Retirer un favori |
| GET | `/{annonceId}/check` | CLIENT | Vérifier si une annonce est en favoris |

### VISITES `/api/v1/visites`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/` | CLIENT | Demander une visite |
| GET | `/client` | CLIENT | Mes demandes de visite |
| DELETE | `/{id}` | CLIENT | Annuler une demande |
| GET | `/admin` | ADMIN | Toutes les demandes de visite |
| PUT | `/{id}/status` | Authentifié | Changer le statut |
| PUT | `/{id}/date` | ADMIN | Modifier la date de visite |

### DISCUSSIONS `/api/v1/discussions`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/` | CLIENT | Démarrer une discussion |
| GET | `/client` | CLIENT | Mes discussions |
| GET | `/admin` | ADMIN | Toutes les discussions |
| GET | `/{id}/messages` | Authentifié | Lire les messages d'une discussion |
| POST | `/{id}/messages` | Authentifié | Envoyer un message |

### CONTRATS `/api/v1/contrats`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/` | ADMIN | Créer un contrat |
| GET | `/admin` | ADMIN | Tous les contrats |
| PUT | `/{id}` | ADMIN | Modifier un contrat |
| GET | `/client` | CLIENT | Mes contrats |
| GET | `/{id}` | Authentifié | Détail d'un contrat |
| PUT | `/{id}/resiliation` | CLIENT | Demander la résiliation |
| PUT | `/{id}/prolongation` | CLIENT | Demander la prolongation |

### SIGNALEMENTS `/api/v1/signalements`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/` | CLIENT | Créer un signalement |
| GET | `/client` | CLIENT | Mes signalements |
| GET | `/admin` | ADMIN | Tous les signalements |
| PUT | `/{id}/status` | ADMIN | Mettre à jour le statut |
| PUT | `/{id}/read` | ADMIN | Marquer comme lu |

### LEADS `/api/v1/leads`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/` | ADMIN | Créer un lead |
| GET | `/` | ADMIN | Lister tous les leads |
| GET | `/{id}` | ADMIN | Détail d'un lead |
| PUT | `/{id}/status` | ADMIN | Mettre à jour le statut |

### ADMINISTRATION `/api/v1/admin`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/dashboard/stats` | ADMIN | Statistiques globales du tableau de bord |

### LOCALISATION `/api/v1/locations`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/departements` | PUBLIC | Lister les départements du Sénégal |
| GET | `/quartiers?departement=` | PUBLIC | Quartiers d'un département |
| GET | `/geocode?departement=&quartier=&adresse=` | PUBLIC | Convertir une adresse en GPS |

---

## Exemples de requêtes et réponses

### Inscription

**Requête**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "nomComplet": "Aminata Diallo",
  "email": "aminata@immosn.sn",
  "telephone": "+221 77 123 45 67",
  "motDePasse": "Password123!"
}
```

**Réponse 201**
```json
{
  "success": true,
  "status": 201,
  "message": "Opération réalisée avec succès",
  "data": {
    "id": 42,
    "nomComplet": "Aminata Diallo",
    "email": "aminata@immosn.sn",
    "telephone": "+221 77 123 45 67",
    "roles": ["CLIENT"],
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Connexion

**Requête**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "client@immosn.sn",
  "motDePasse": "Password123!"
}
```

**Réponse 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "nomComplet": "Moussa Ndiaye",
    "email": "client@immosn.sn",
    "roles": ["CLIENT"],
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

---

### Recherche multicritère d'annonces

**Requête**
```http
POST /api/v1/annonces/search
Content-Type: application/json

{
  "typeBienId": 1,
  "prixMax": 300000000,
  "departement": "Dakar",
  "nbrPieces": 4,
  "commoditeIds": [2, 3],
  "page": 0,
  "size": 12,
  "sortBy": "prix",
  "sortDir": "ASC"
}
```

**Réponse 200**
```json
{
  "content": [
    {
      "id": 15,
      "libelle": "Villa F4 - Mermoz",
      "prix": 250000000,
      "departement": "Dakar",
      "quartier": "Mermoz",
      "typeBien": "Villa",
      "nbrPieces": 4,
      "surface": 200.0,
      "imagePrincipale": "https://storage.immosn.sn/annonces/15/img1.jpg"
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 7,
  "totalPages": 1,
  "last": true
}
```

---

### Démarrer une discussion

**Requête** (avec token CLIENT)
```http
POST /api/v1/discussions
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "annonceId": 15,
  "premierMessage": "Bonjour, est-il possible de visiter ce bien ce weekend ?"
}
```

**Réponse 201**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 7,
    "annonceLibelle": "Villa F4 - Mermoz",
    "clientNom": "Moussa Ndiaye",
    "messages": [
      {
        "id": 1,
        "contenu": "Bonjour, est-il possible de visiter ce bien ce weekend ?",
        "senderRole": "CLIENT",
        "senderName": "Moussa Ndiaye",
        "isRead": false,
        "createdAt": "2024-01-15T11:00:00"
      }
    ],
    "unreadCount": 0
  }
}
```

---

### Créer un contrat (admin)

**Requête** (avec token ADMIN)
```http
POST /api/v1/contrats
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "clientId": 42,
  "annonceId": 15,
  "leadId": 3,
  "dateDebut": "2024-02-01",
  "dateFin": "2025-01-31",
  "montant": 350000,
  "documentUrl": "https://storage.immosn.sn/contrats/draft_8.pdf",
  "notes": "Paiement mensuel le 1er du mois. Caution 2 mois."
}
```

---

## Workflows utilisateur

### Workflow complet d'un client

```
1. Inscription          POST /auth/register
        ↓
2. Connexion            POST /auth/login         → token JWT
        ↓
3. Parcourir annonces   GET  /annonces            → liste paginée
        ↓
4. Chercher             POST /annonces/search     → filtrer par critères
        ↓
5. Voir le détail       GET  /annonces/{id}       → infos complètes
        ↓
6. Mettre en favoris    POST /favoris/{id}/toggle → isFavoris: true
        ↓
7. Demander une visite  POST /visites             → statut EN_ATTENTE
        ↓
8. Discuter             POST /discussions         → messagerie avec l'agent
        ↓
9. Signer un contrat    (créé par l'admin)
        ↓
10. Suivre son contrat  GET  /contrats/client     → voir statut
        ↓
11. Si problème         POST /signalements        → litige contractuel
```

---

## Workflows administrateur

### Workflow de gestion d'une visite

```
1. Voir les demandes    GET  /visites/admin?statut=EN_ATTENTE
        ↓
2. Accepter la visite   PUT  /visites/{id}/status  → statut ACCEPTEE
        ↓
3. Modifier la date     PUT  /visites/{id}/date     → reprogrammer si besoin
        ↓
4. Créer un lead        POST /leads                 → qualifier le prospect
        ↓
5. Après la visite      PUT  /visites/{id}/status   → statut TERMINEE
        ↓
6. Convertir en contrat POST /contrats              → contrat créé
        ↓
7. Mettre à jour lead   PUT  /leads/{id}/status     → statut CONVERTI
```

### Workflow de traitement des signalements

```
1. Voir les non lus     GET  /signalements/admin?statut=OUVERT
        ↓
2. Marquer comme lu     PUT  /signalements/{id}/read
        ↓
3. Traiter              PUT  /signalements/{id}/status  → EN_COURS
        ↓
4. Résoudre             PUT  /signalements/{id}/status  → RESOLU
                        (avec reponseAdmin dans le body)
```

---

## Workflows super administrateur

### Gestion des comptes administrateurs

```
1. Créer un admin       POST  /auth/admin
        ↓
2. Lister les admins    GET   /auth/admins
        ↓
   Si problème avec un admin :
        ↓
3a. Archiver            PATCH /auth/admins/{id}/archive  → désactivation temporaire
3b. Restaurer           PATCH /auth/admins/{id}/restore  → réactivation
3c. Révoquer            PATCH /auth/admins/{id}/revoke   → rétrogradation en CLIENT
```

---

## Énumérations

### StatutContrat
```
EN_ATTENTE              → Contrat créé, en attente de validation
ACTIF                   → Contrat en cours
EXPIRE                  → Contrat arrivé à échéance
RESILIE                 → Contrat résilié
EN_ATTENTE_RESILIATION  → Demande de résiliation soumise par le client
PROLONGATION_EN_ATTENTE → Demande de prolongation soumise par le client
```

### StatutDemandeVisite
```
EN_ATTENTE  → Demande soumise, en attente de traitement
ACCEPTEE    → Visite confirmée
REFUSEE     → Visite refusée par l'admin
ANNULEE     → Visite annulée par le client
TERMINEE    → Visite effectuée
```

### StatutSignalement
```
OUVERT    → Signalement soumis, non lu
EN_COURS  → Signalement pris en charge
RESOLU    → Problème résolu
FERME     → Dossier fermé
```

### StatutLead
```
EN_COURS   → Prospect en cours de qualification
CONVERTI   → Lead transformé en contrat
ABANDONNE  → Prospect non concluant
```

---

*Documentation crée le 2024-01-15 — ImmoSN v1*
