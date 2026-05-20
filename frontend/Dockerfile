# =========================
# Build Stage
# =========================
FROM node:20-bookworm AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Clean install dependencies
RUN npm install

# Copy source code
COPY . .

# Build app
RUN npm run build

# =========================
# Production Stage
# =========================
FROM nginx:stable-alpine

# Copy built frontend
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose nginx port
EXPOSE 8080

# Configure nginx to use Cloud Run port
RUN sed -i 's/listen       80;/listen 8080;/g' /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]