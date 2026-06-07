# Étape 1 : Build frontend Vue
FROM node:20-alpine AS build
ARG VITE_API_URL=/api/v1
ARG VITE_CLOUDINARY_CLOUD_NAME=
ARG VITE_CLOUDINARY_UPLOAD_PRESET=
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_CLOUDINARY_CLOUD_NAME=$VITE_CLOUDINARY_CLOUD_NAME
ENV VITE_CLOUDINARY_UPLOAD_PRESET=$VITE_CLOUDINARY_UPLOAD_PRESET
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci --prefer-offline --no-audit --progress=false
COPY frontend/ ./
RUN npm run build
# Étape 2 : Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]