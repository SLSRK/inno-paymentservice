package com.innowise.paymentservice.model.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Field(name = "order_id")
    private Long orderId;

    @Field(name = "user_id")
    private Long userId;

    @Field(name = "status")
    PaymentStatus status;

    @Field(name = "timestamp", targetType = FieldType.STRING)
    LocalDateTime timestamp;

    @Field(name = "payment_amount")
    private Long paymentAmount;
}
