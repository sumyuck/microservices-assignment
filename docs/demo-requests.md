# Five-minute live demonstration

Start the application first with `docker compose up --build`, then get a token:

```bash
curl -X POST http://localhost:8080/auth/demo-token -H 'Content-Type: application/json' -d '{"role":"SALES"}'
```

Copy the returned value into `TOKEN`. Use it in the next calls:

```bash
curl -X POST http://localhost:8080/products -H "Authorization: Bearer TOKEN" -H 'Content-Type: application/json' -d '{"name":"Demo headset","price":75,"stock":4}'
curl -X POST http://localhost:8080/customers -H "Authorization: Bearer TOKEN" -H 'Content-Type: application/json' -d '{"name":"Asha Shah","email":"asha@example.com","phone":"9999999999"}'
curl -X POST http://localhost:8080/orders -H "Authorization: Bearer TOKEN" -H 'Content-Type: application/json' -d '{"customerId":"CUSTOMER_ID","productId":2,"quantity":1}'
```

Use the ID returned from order creation:

```bash
curl http://localhost:8080/orders/ORDER_ID -H "Authorization: Bearer TOKEN"
curl http://localhost:8080/billing/invoices -H "Authorization: Bearer TOKEN"
curl http://localhost:8080/notifications -H "Authorization: Bearer TOKEN"
```

Explain the sequence as follows: REST reaches the gateway, Order synchronously verifies stock using gRPC, the order and outbox save atomically in MySQL, and RabbitMQ triggers the independent Billing and Notification updates.

For discovery and monitoring, open Consul at `http://localhost:8500` and Prometheus at `http://localhost:9090/targets`.
