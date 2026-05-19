# ── Étape 1 : Build du frontend Vue ────────────────────────
FROM node:20-alpine AS build

# Variables Vite injectées au moment du build (baked dans le bundle JS)
ARG VITE_API_URL=/api/v1
ARG VITE_CLOUDINARY_CLOUD_NAME=
ARG VITE_CLOUDINARY_UPLOAD_PRESET=

ENV VITE_API_URL=$VITE_API_URL
ENV VITE_CLOUDINARY_CLOUD_NAME=$VITE_CLOUDINARY_CLOUD_NAME
ENV VITE_CLOUDINARY_UPLOAD_PRESET=$VITE_CLOUDINARY_UPLOAD_PRESET

WORKDIR /app

# Installer les dépendances (cache layer si package.json inchangé)
COPY frontend/package*.json ./
RUN npm ci --prefer-offline

# Copier les sources et builder
COPY frontend/ ./
RUN npm run build

# ── Étape 2 : Serveur Nginx ─────────────────────────────────
FROM nginx:alpine

# Copier le build Vite
COPY --from=build /app/dist /usr/share/nginx/html

# Copier la config Nginx (chemin depuis le build context = racine du projet)
COPY nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
