package com.innowise.paymentservice.model.dto;

public record PaymentStatusDto(

        Long orderId,

        String status
) {
}
