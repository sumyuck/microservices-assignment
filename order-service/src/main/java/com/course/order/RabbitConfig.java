package com.course.order;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  @Bean DirectExchange orderEvents() { return new DirectExchange("order.events", true, false); }
}
