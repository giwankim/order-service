package com.polarbookshop.orderservice.order.event

data class OrderDispatchedMessage(
    val orderId: Long,
)
