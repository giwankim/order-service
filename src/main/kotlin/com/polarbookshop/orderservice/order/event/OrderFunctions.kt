package com.polarbookshop.orderservice.order.event

import com.polarbookshop.orderservice.order.domain.OrderService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}

@Configuration
class OrderFunctions {
    @Bean
    fun dispatchOrder(orderService: OrderService): Consumer<Flux<OrderDispatchedMessage>> =
        Consumer { messages ->
            orderService
                .consumeOrderDispatchedEvent(messages)
                .doOnNext { order ->
                    logger.info { "The order with id $order is dispatched" }
                }.subscribe()
        }
}
