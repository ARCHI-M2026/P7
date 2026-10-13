# ---------- Étape 1 : compilation Angular
FROM node:24-alpine AS build
WORKDIR /app
ENV NG_CLI_ANALYTICS=false
COPY package*.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- Étape 2 : serveur web (fichiers statiques + proxy vers le backend)
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ycyw-tchat/browser /usr/share/nginx/html
EXPOSE 80
