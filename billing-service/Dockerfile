FROM golang:1.23-alpine AS build
WORKDIR /src
COPY go.mod .
RUN go mod download
COPY . .
RUN CGO_ENABLED=1 go build -o billing .
FROM alpine:3.20
RUN addgroup -S app && adduser -S app -G app && mkdir /data && chown app:app /data
COPY --from=build /src/billing /usr/local/bin/billing
USER app
EXPOSE 8084
CMD ["billing"]
