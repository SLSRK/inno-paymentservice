package com.innowise.paymentservice.model.dto;

import lombok.Builder;

@Builder
public record PaymentStatusDto(

        Long orderId,

        String status,

        Long amount
) {
}
