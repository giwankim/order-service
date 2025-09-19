package com.polarbookshop.orderservice.order.domain

import com.polarbookshop.orderservice.book.Book
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("ORDERS")
class Order(
    val bookIsbn: String,
    val bookName: String,
    val bookPrice: Double,
    val quantity: Int,
    val status: OrderStatus,
) {
    @Id
    var id: Long = 0L

    @CreatedDate
    var createdDate: Instant? = null

    @LastModifiedDate
    var lastModifiedDate: Instant? = null

    @Version
    var version: Int = 0

    companion object {
        fun accepted(
            book: Book,
            quantity: Int,
        ): Order =
            Order(
                bookIsbn = book.isbn,
                bookName = "${book.title} - ${book.author}",
                bookPrice = book.price,
                quantity = quantity,
                status = OrderStatus.ACCEPTED,
            )

        fun rejected(
            isbn: String,
            quantity: Int,
        ): Order =
            Order(
                bookIsbn = isbn,
                bookName = "",
                bookPrice = 0.0,
                quantity = quantity,
                status = OrderStatus.REJECTED,
            )
    }
}
