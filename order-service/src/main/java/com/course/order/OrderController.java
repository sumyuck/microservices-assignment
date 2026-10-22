package com.course.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class OrderController {
  private final OrderService service;
  public OrderController(OrderService service) { this.service=service; }
  @GetMapping("/health") Map<String,String> health() { return Map.of("status","UP", "service","order-service"); }
  @GetMapping(value="/metrics", produces="text/plain") String metrics() { return "course_service_up{service=\"order\"} 1\n"; }
  @PostMapping("/orders") ResponseEntity<?> create(@RequestBody OrderService.CreateOrder request) {
    if (request.customerId()==null || request.productId()==null || request.quantity()==null || request.quantity()<1) return ResponseEntity.badRequest().body(Map.of("error","customerId, productId and positive quantity are required"));
    try { return ResponseEntity.status(201).body(service.create(request)); }
    catch (OrderService.StockUnavailable error) { return ResponseEntity.status(503).body(Map.of("error",error.getMessage())); }
  }
  @GetMapping("/orders/{id}") ResponseEntity<?> get(@PathVariable Long id) { try { return ResponseEntity.ok(service.get(id)); } catch (Exception e) { return ResponseEntity.notFound().build(); } }
}
