# Architecture explanation for viva

```text
Client -> API Gateway --REST--> Customer / Product / Order
                                  Order --gRPC--> Product
                                  Order --outbox publisher--> RabbitMQ
RabbitMQ --order.created--> Billing (SQLite) and Notification (Redis)
```

Order creation is **ACID**: the order, read-model row, and `outbox_events` row are committed by one MySQL transaction. The small publisher reads only committed, unpublished outbox rows and sends `order.created` to RabbitMQ. Billing and Notification acknowledge events only after writing their own local data; their results are therefore **BASE/eventually consistent**.

CQRS is intentionally small: the write model is `orders`; query endpoints read the separate `order_summary` projection. The two are updated in the same transaction for a compact demo. In a larger system the projection would be updated by a consumer.

There are no distributed transactions and no shared application databases. The five stores are owned by their named services only. RabbitMQ is shared infrastructure, not a service database.

The Order Service performs a synchronous gRPC stock check. Resilience4j gives it a short timeout and fallback response, so an unavailable Product Service does not crash it. Consul proves discovery through registered health checks; Docker/Kubernetes DNS performs actual service routing in this compact deployment.
