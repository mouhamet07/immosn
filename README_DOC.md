# ANNEXE A — Documentation technique de la plateforme ImmoSN

## 1. Présentation générale

ImmoSN est une plateforme web de gestion immobilière conçue pour répondre aux besoins du marché sénégalais. La solution centralise l'ensemble des opérations liées à la publication d'annonces immobilières, à la gestion des prospects, à l'organisation des visites, au suivi des contrats et au traitement des litiges.

L'objectif principal du projet est de proposer un environnement numérique permettant aux utilisateurs de consulter des biens immobiliers tandis que les administrateurs disposent d'outils de gestion et de suivi des interactions avec les clients.

La plateforme adopte une architecture moderne basée sur une séparation stricte entre le frontend et le backend, facilitant la maintenance, l'évolution et le déploiement de l'application.

---

## 2. Contexte et problématique

Le secteur immobilier sénégalais repose encore largement sur des méthodes traditionnelles de diffusion des annonces et de gestion des relations clients. Cette situation engendre plusieurs difficultés :

* manque de centralisation des informations ;
* difficulté de suivi des prospects ;
* échanges dispersés entre les différentes parties ;
* gestion manuelle des visites ;
* absence d'outils de suivi des contrats ;
* faible traçabilité des réclamations et litiges.

Face à ces contraintes, ImmoSN a été développé afin de proposer une plateforme numérique capable de couvrir l'ensemble du cycle de vie d'une transaction immobilière.

---

## 3. Objectifs du projet

### Objectif principal

Développer une plateforme immobilière centralisée permettant de gérer efficacement les annonces, les prospects, les visites, les contrats et les litiges.

### Objectifs spécifiques

* Permettre la publication et la consultation d'annonces immobilières.
* Faciliter les échanges entre clients et administrateurs.
* Organiser les demandes de visite.
* Assurer le suivi des prospects commerciaux.
* Gérer les contrats de location ou de vente.
* Traiter les signalements et réclamations.
* Fournir des indicateurs statistiques à travers un tableau de bord administratif.
* Géolocaliser les biens immobiliers afin d'améliorer l'expérience utilisateur.

---

## 4. Architecture fonctionnelle

Le fonctionnement général de la plateforme s'articule autour de plusieurs modules métiers.

```text
Gestion des utilisateurs
           │
           ▼
Gestion des annonces
           │
           ▼
Gestion des prospects
           │
           ▼
Gestion des visites
           │
           ▼
Gestion des contrats
           │
           ▼
Gestion des litiges
```

Chaque module communique avec les autres afin d'assurer un suivi complet des interactions réalisées sur la plateforme.

---

## 5. Architecture technique

L'application repose sur une architecture client-serveur composée de plusieurs services spécialisés.

```text
Utilisateur
      │
      ▼
Interface Web Vue.js
      │
      ▼
Nginx Reverse Proxy
      │
      ▼
API Spring Boot
      │
      ▼
PostgreSQL
```

### Frontend

Le frontend est développé avec Vue.js 3 et constitue l'interface utilisateur de la plateforme.

Ses principales responsabilités sont :

* affichage des annonces ;
* gestion de la navigation ;
* authentification ;
* interactions utilisateur ;
* affichage des cartes et données géographiques.

### Backend

Le backend repose sur Spring Boot et expose une API REST.

Il est responsable :

* de la logique métier ;
* de la sécurité ;
* de la validation des données ;
* de l'accès à la base de données ;
* de l'intégration avec les services externes.

### Base de données

Les données sont stockées dans PostgreSQL en environnement de production et dans H2 lors des phases de développement.

### Reverse Proxy

Nginx assure :

* la distribution des fichiers statiques ;
* le routage des requêtes API ;
* la gestion du cache ;
* la compression des ressources ;
* l'application de certaines mesures de sécurité.

---

## 6. Technologies utilisées

| Couche               | Technologie                 |
| -------------------- | --------------------------- |
| Frontend             | Vue.js 3                    |
| Build Frontend       | Vite                        |
| Backend              | Spring Boot                 |
| Langage Backend      | Java 21                     |
| Sécurité             | Spring Security + JWT       |
| ORM                  | Hibernate / Spring Data JPA |
| Base de données      | PostgreSQL                  |
| Cartographie         | Leaflet                     |
| Géocodage            | OpenStreetMap Nominatim     |
| Stockage images      | Cloudinary                  |
| Conteneurisation     | Docker                      |
| Reverse Proxy        | Nginx                       |
| Intégration continue | GitHub Actions              |
| Hébergement          | VPS Ubuntu                  |

---

## 7. Gestion des utilisateurs

Le système repose sur une gestion des accès basée sur les rôles.

### Client

Le client peut :

* consulter les annonces ;
* enregistrer des favoris ;
* envoyer des messages ;
* demander des visites ;
* consulter ses contrats ;
* effectuer des signalements.

### Administrateur

L'administrateur peut :

* gérer les annonces ;
* suivre les prospects ;
* planifier les visites ;
* créer les contrats ;
* répondre aux signalements ;
* consulter les statistiques.

### Super administrateur

Le super administrateur dispose de privilèges supplémentaires lui permettant de gérer les comptes administrateurs.

---

## 8. Gestion des annonces immobilières

Le module de gestion des annonces constitue le cœur fonctionnel de la plateforme.

Chaque annonce contient notamment :

* un titre ;
* une description ;
* un prix ;
* une superficie ;
* un nombre de pièces ;
* une localisation ;
* une liste de commodités ;
* des images stockées sur Cloudinary.

Les annonces peuvent être créées, modifiées, archivées puis restaurées par les administrateurs.

---

## 9. Gestion des prospects et visites

Lorsqu'un client manifeste un intérêt pour une annonce, il peut :

* contacter l'administration ;
* demander une visite.

L'administrateur peut ensuite transformer cette interaction en prospect afin d'assurer un suivi commercial structuré.

Le prospect peut évoluer vers :

* un contrat ;
* un abandon ;
* une poursuite du processus commercial.

---

## 10. Gestion des contrats

Le système permet la création et le suivi de contrats immobiliers.

Chaque contrat possède un cycle de vie comprenant plusieurs états :

* En attente ;
* Actif ;
* Expiré ;
* Résilié ;
* En attente de résiliation ;
* En attente de prolongation.

Un mécanisme automatisé vérifie quotidiennement les contrats arrivés à échéance.

---

## 11. Gestion des litiges

Les utilisateurs peuvent signaler des problèmes liés à leurs contrats.

Les signalements sont enregistrés puis traités par les administrateurs selon plusieurs états :

* Ouvert ;
* En cours ;
* Résolu ;
* Fermé.

Cette fonctionnalité améliore la traçabilité des réclamations.

---

## 12. Géolocalisation

La plateforme intègre un système de géolocalisation basé sur OpenStreetMap.

Lors de la création d'une annonce :

1. l'administrateur sélectionne un département ;
2. un quartier est choisi ;
3. les coordonnées géographiques sont calculées ;
4. les informations sont enregistrées dans la base de données.

Les coordonnées permettent ensuite l'affichage du bien sur une carte interactive Leaflet.

---

## 13. Sécurité

La sécurité de la plateforme repose sur plusieurs mécanismes complémentaires.

### Authentification

L'authentification utilise des jetons JWT.

Après connexion :

* un jeton est généré ;
* il est transmis à chaque requête ;
* les accès sont vérifiés automatiquement.

### Gestion des rôles

Les autorisations sont contrôlées par Spring Security selon le rôle de l'utilisateur.

### Chiffrement des mots de passe

Les mots de passe sont stockés sous forme hachée grâce à l'algorithme BCrypt.

### Validation des données

Toutes les données reçues sont validées côté serveur afin de garantir leur cohérence.

---

## 14. Déploiement de la solution

Le déploiement repose sur une architecture conteneurisée.

```text
GitHub
   │
   ▼
GitHub Actions
   │
   ▼
VPS Ubuntu
   │
   ▼
Docker Compose
   │
   ▼
Nginx
   │
   ▼
Application ImmoSN
```

Cette approche garantit :

* la reproductibilité ;
* l'automatisation ;
* la facilité de maintenance ;
* la rapidité des mises à jour.

---

## 15. Pipeline CI/CD

Le projet utilise GitHub Actions pour automatiser les déploiements.

Le processus suit les étapes suivantes :

1. Développement local.
2. Validation du code.
3. Push vers la branche principale.
4. Déclenchement du pipeline GitHub Actions.
5. Construction des images Docker.
6. Déploiement automatique sur le VPS.
7. Redémarrage des conteneurs.
8. Mise à disposition de la nouvelle version.

---

## 16. Conclusion

ImmoSN constitue une plateforme immobilière complète reposant sur une architecture moderne et scalable. L'utilisation conjointe de Spring Boot, Vue.js, PostgreSQL, Docker et Nginx permet de proposer une solution robuste répondant aux exigences fonctionnelles et techniques du marché immobilier sénégalais.

L'architecture retenue facilite les évolutions futures de la plateforme tout en garantissant la maintenabilité, la sécurité et les performances de l'application.
