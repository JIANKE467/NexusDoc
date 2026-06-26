FROM node:20-alpine AS build

WORKDIR /app

COPY package.json pnpm-lock.yaml* ./
# Use npm to install dependencies in the build image to avoid pnpm lockfile incompat
# issues inside the container environment
RUN npm set progress=false && npm install --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:1.26-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
