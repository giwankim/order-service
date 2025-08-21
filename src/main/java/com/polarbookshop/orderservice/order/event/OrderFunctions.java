package com.polarbookshop.orderservice.order.event;

import com.polarbookshop.orderservice.order.domain.OrderService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@Slf4j
public class OrderFunctions {

  @Bean
  public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
    return flux ->
        orderService
            .consumeOrderDispatchedEvent(flux)
            .doOnNext(order -> log.info("Order with id {} is dispatched", order.id()))
            .subscribe();
  }
}
