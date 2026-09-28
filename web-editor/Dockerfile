FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --omit=dev
COPY public ./public
COPY server.mjs ./server.mjs
ENV NODE_ENV=production
EXPOSE 8787
CMD ["npm", "start"]
