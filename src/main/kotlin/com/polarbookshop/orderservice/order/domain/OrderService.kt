package com.polarbookshop.orderservice.order.domain

import com.polarbookshop.orderservice.book.BookClient
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Service
class OrderService(
    private val bookClient: BookClient,
    private val orderRepository: OrderRepository,
    private val streamBridge: StreamBridge,
) {
    fun getAllOrders(): Flux<Order> = orderRepository.findAll()

    @Transactional
    fun submitOrder(
        isbn: String,
        quantity: Int,
    ): Mono<Order> =
        bookClient
            .getBookByIsbn(isbn)
            .map { book -> Order.accepted(book, quantity) }
            .defaultIfEmpty(Order.rejected(isbn, quantity))
            .flatMap(orderRepository::save)
            .doOnNext(::publishOrderAcceptedEvent)

    private fun publishOrderAcceptedEvent(order: Order) {
        val message =
            order
                .takeIf { it.status == OrderStatus.ACCEPTED }
                ?.let { OrderAcceptedMessage(it.id) }
                ?: return

        logger.info { "Sending order accepted event with id: ${order.id}" }
        streamBridge.send("acceptOrder-out-0", message).also { result ->
            logger.info { "Result of sending data for order with id ${order.id}: $result" }
        }
    }

    fun consumeOrderDispatchedEvent(messages: Flux<OrderDispatchedMessage>): Flux<Order> =
        messages
            .flatMap { message -> orderRepository.findById(message.orderId) }
            .map(Order::dispatched)
            .flatMap(orderRepository::save)
}

fun Order.dispatched(): Order =
    Order(
        bookIsbn = bookIsbn,
        bookName = bookName,
        bookPrice = bookPrice,
        quantity = quantity,
        status = OrderStatus.DISPATCHED,
        id = id,
        createdDate = createdDate,
        lastModifiedDate = lastModifiedDate,
        version = version,
    )
