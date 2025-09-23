package com.polarbookshop.orderservice.order.event

data class OrderAcceptedMessage(
    val orderId: Long,
)
