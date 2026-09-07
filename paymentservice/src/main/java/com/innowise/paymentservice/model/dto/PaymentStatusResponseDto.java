package com.innowise.paymentservice.model.dto;

import lombok.Builder;

@Builder
public record PaymentStatusResponseDto(

        Long orderId,

        String status,

        Long amount
) {
}
