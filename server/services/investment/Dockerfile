FROM golang:1.24.2-alpine AS builder

WORKDIR /app

COPY ./go.mod ./go.sum ./
RUN go mod download

COPY . .

RUN go build -o investment-service ./cmd/main.go

FROM alpine:3.18

WORKDIR /app

COPY --from=builder /app/investment-service .

EXPOSE 8080

CMD [ "./investment-service" ]
