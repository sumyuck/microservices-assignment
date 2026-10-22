# RetailFlow — Microservices Architecture Assignment

This is a deliberately small, runnable demonstration of a retail order workflow. It is designed for a course assignment, not production use.

## What it demonstrates

| Requirement | Evidence in this project |
| --- | --- |
| Five services, five languages | Product: Node.js; Customer: Python; Order: Java; Billing: Go; Notification: C# |
| Database per service | PostgreSQL, MongoDB, MySQL, SQLite, Redis respectively |
| API Gateway | `api-gateway` validates demo JWTs and reverse-proxies all public calls |
| Discovery Service | Consul service catalogue and health checks at port 8500 |
| Circuit breaker | Resilience4j protects the Order → Product gRPC stock check |
| Sync + async communication | REST via gateway, gRPC Order → Product, RabbitMQ events |
| ACID / BASE | MySQL transaction plus outbox; asynchronous invoice/notification processing |
| Transactional outbox / CQRS | `orders`, `outbox_events`, and denormalized `order_summary` tables |
| Monitoring and logging | `/health`, `/metrics`, JSON logs, Prometheus at port 9090 |
| Security | Gateway JWT validation, Docker non-root users, secret environment variables, Kubernetes Secret/NetworkPolicy |
| Kubernetes | Plain manifests in `infra/k8s` |

The gateway is infrastructure, not one of the five assessed business services; Node.js is therefore intentionally used both for the gateway and Product Service.

## Run on a laptop

Your friend needs Docker Desktop (or Docker Engine) and Compose. Copy `.env.example` to `.env`, change the values, then run:

```bash
docker compose up --build
```

Useful pages: gateway `http://localhost:8080`, Consul `http://localhost:8500`, RabbitMQ `http://localhost:15672` (`course` / `course`), Prometheus `http://localhost:9090`.

## Manual demo

1. Get a short demo token: `POST /auth/demo-token` with `{ "role": "SALES" }`.
2. Send `Authorization: Bearer <token>` on the following requests.
3. Create a product with `POST /products` (for example `{ "name":"Keyboard", "price":40, "stock":5 }`).
4. Create a customer with `POST /customers`.
5. Create an order using `POST /orders` with `{ "customerId":"...", "productId":1, "quantity":2 }`.
6. `GET /orders/{id}` immediately shows the CQRS read model. Within a few seconds, `GET /billing/invoices` and `GET /notifications` show the asynchronous results.
7. Stop Product Service and create another order to demonstrate the circuit-breaker fallback.

See [architecture notes](docs/architecture.md), the [friend's run guide](docs/RUN_GUIDE.md), and `infra/k8s/README.md` for the presentation explanation, local run steps, and Kubernetes instructions.

## Put this in a new GitHub repository

Do not push this folder to the existing DealFlow360 repository. Create an empty GitHub repository first (for example, `RetailFlow-Microservices-Assignment`), then run these commands **inside this folder**:

```bash
git init -b main
git add .
git commit -m "Initial microservices architecture assignment"
git remote add origin https://github.com/YOUR-USERNAME/RetailFlow-Microservices-Assignment.git
git push -u origin main
```

No GitHub Actions or deployment pipeline is included; that is intentional for this course prototype.
