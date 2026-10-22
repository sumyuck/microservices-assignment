package com.course.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
  private final JdbcTemplate db; private final ProductStockClient product; private final RabbitTemplate rabbit; private final ObjectMapper json;
  public OrderService(JdbcTemplate db, ProductStockClient product, RabbitTemplate rabbit, ObjectMapper json) { this.db=db; this.product=product; this.rabbit=rabbit; this.json=json; }
  @Transactional
  public Map<String,Object> create(CreateOrder request) {
    var stock = product.check(request.productId(), request.quantity());
    if (!stock.available()) throw new StockUnavailable(stock.message());
    db.update("INSERT INTO orders(customer_id,product_id,quantity,status) VALUES(?,?,?,?)", request.customerId(), request.productId(), request.quantity(), "CONFIRMED");
    Long id = db.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    db.update("INSERT INTO order_summary(order_id,customer_id,product_id,quantity,status) VALUES(?,?,?,?,?)", id, request.customerId(), request.productId(), request.quantity(), "CONFIRMED");
    try { db.update("INSERT INTO outbox_events(event_type,payload,published) VALUES(?,?,false)", "order.created", json.writeValueAsString(Map.of("orderId",id,"customerId",request.customerId(),"productId",request.productId(),"quantity",request.quantity()))); }
    catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    return get(id);
  }
  public Map<String,Object> get(Long id) { return db.queryForMap("SELECT order_id AS id, customer_id AS customerId, product_id AS productId, quantity, status FROM order_summary WHERE order_id=?", id); }
  @Scheduled(fixedDelay = 2000)
  public void publishOutbox() {
    List<Map<String,Object>> events = db.queryForList("SELECT id,payload FROM outbox_events WHERE published=false ORDER BY id LIMIT 20");
    for (var event : events) { rabbit.convertAndSend("order.events", "order.created", event.get("payload").toString()); db.update("UPDATE outbox_events SET published=true WHERE id=?", event.get("id")); }
  }
  public record CreateOrder(String customerId, Long productId, Integer quantity) {}
  public static class StockUnavailable extends RuntimeException { public StockUnavailable(String message) { super(message); } }
}
