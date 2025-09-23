package com.polarbookshop.orderservice.order.domain

import com.polarbookshop.orderservice.book.BookClient
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Service
class OrderService(
    private val bookClient: BookClient,
    private val orderRepository: OrderRepository,
) {
    fun getAllOrders(): Flux<Order> = orderRepository.findAll()

    fun submitOrder(
        isbn: String,
        quantity: Int,
    ): Mono<Order> =
        bookClient
            .getBookByIsbn(isbn)
            .map { Order.accepted(it, quantity) }
            .defaultIfEmpty(Order.rejected(isbn, quantity))
            .flatMap(orderRepository::save)

    fun consumerOrderDispatchedEvent(messages: Flux<OrderDispatchedMessage>): Flux<Order> =
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
