package com.course.order;

import com.course.product.grpc.ProductStockGrpc;
import com.course.product.grpc.StockReply;
import com.course.product.grpc.StockRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductStockClient {
  private final String host;
  public ProductStockClient(@Value("${PRODUCT_GRPC_HOST:localhost}") String host) { this.host = host; }
  @CircuitBreaker(name = "product", fallbackMethod = "unavailable")
  public StockResult check(Long productId, Integer quantity) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, 50051).usePlaintext().build();
    try {
      StockReply reply = ProductStockGrpc.newBlockingStub(channel).withDeadlineAfter(2, TimeUnit.SECONDS).checkStock(StockRequest.newBuilder().setProductId(productId).setQuantity(quantity).build());
      return new StockResult(reply.getAvailable(), reply.getMessage());
    } finally { channel.shutdownNow(); }
  }
  public StockResult unavailable(Long productId, Integer quantity, Throwable error) { return new StockResult(false, "Product service unavailable (circuit breaker fallback)"); }
  public record StockResult(boolean available, String message) {}
}
