# Friend's Run Guide

Use Docker Compose for the easiest course-demo setup.

## 1. Install prerequisites

- Git
- Docker Desktop, with at least 8 GB of memory allocated

Start Docker Desktop before continuing.

## 2. Clone the project

```bash
git clone https://github.com/sumyuck/microservices-assignment.git
cd microservices-assignment
```

## 3. Create local environment settings

macOS/Linux:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

The default values are safe for this local course demonstration.

## 4. Build and start every service

```bash
docker compose up --build
```

The first run downloads database, message-broker, and language build images, so it may take several minutes. Keep this terminal open.

After services have started, open these pages:

| Component | Address | Login |
| --- | --- | --- |
| API Gateway | http://localhost:8080/health | — |
| Consul discovery | http://localhost:8500 | — |
| RabbitMQ | http://localhost:15672 | `course` / `course` |
| Prometheus targets | http://localhost:9090/targets | — |

If a service is still starting, check it with:

```bash
docker compose ps
docker compose logs -f order-service
```

## 5. Get a demo JWT

Open a second terminal. The API Gateway protects all business endpoints with a JWT.

```bash
curl -X POST http://localhost:8080/auth/demo-token \
  -H "Content-Type: application/json" \
  -d '{"role":"SALES"}'
```

Copy the returned `token` value. Replace `TOKEN` with it in each following command.

## 6. Create a product

```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo headset","price":75,"stock":4}'
```

Copy the returned product `id`. A default product is seeded with ID `1`, so this new product normally has ID `2`.

## 7. Create a customer

```bash
curl -X POST http://localhost:8080/customers \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha Shah","email":"asha@example.com","phone":"9999999999"}'
```

Copy the returned customer `id`.

## 8. Create an order

Replace `CUSTOMER_ID` and `PRODUCT_ID` using the IDs returned above.

```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUSTOMER_ID","productId":PRODUCT_ID,"quantity":1}'
```

Copy the returned order `id`.

## 9. Demonstrate the architecture

Show the Order Service CQRS read model immediately:

```bash
curl http://localhost:8080/orders/ORDER_ID \
  -H "Authorization: Bearer TOKEN"
```

Wait 5–10 seconds for RabbitMQ consumers, then show asynchronous results:

```bash
curl http://localhost:8080/billing/invoices \
  -H "Authorization: Bearer TOKEN"

curl http://localhost:8080/notifications \
  -H "Authorization: Bearer TOKEN"
```

Use this explanation during the demo:

1. The API Gateway validates the JWT.
2. Order Service calls Product Service via gRPC to check stock.
3. The order, CQRS read-model record, and outbox event are saved atomically in MySQL.
4. RabbitMQ publishes `order.created` asynchronously.
5. Billing Service creates an invoice in SQLite.
6. Notification Service stores a notification in Redis.
7. Consul displays service discovery and Prometheus displays health metrics.

## 10. Stop the application

Stop containers while retaining the demonstration data:

```bash
docker compose down
```

Remove containers and every local database volume when you want a completely fresh demo:

```bash
docker compose down -v
```
