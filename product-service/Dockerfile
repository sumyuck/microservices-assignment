FROM node:22-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev
COPY . .
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app
EXPOSE 3001 50051
CMD ["npm", "start"]
