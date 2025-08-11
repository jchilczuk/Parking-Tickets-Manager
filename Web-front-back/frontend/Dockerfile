# Etap budowania
FROM node:18-alpine as build

WORKDIR /app
COPY package*.json ./
COPY . .
RUN npm install
RUN npm run build

# Etap produkcyjny
FROM nginx:stable-alpine

# Skopiuj zbudowaną aplikację
COPY --from=build /app/build /usr/share/nginx/html

# Skopiuj konfigurację Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Ustaw port i uruchom Nginx
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]