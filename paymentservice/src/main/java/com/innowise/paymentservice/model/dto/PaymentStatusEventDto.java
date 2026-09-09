package com.innowise.paymentservice.model.dto;

import lombok.Builder;

@Builder
public record PaymentStatusEventDto(

        Long orderId,

        Long amount
) {
}
