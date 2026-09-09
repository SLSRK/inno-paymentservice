package com.innowise.paymentservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateDto(

        @NotNull(message = "Order id cannot be null")
        Long orderId,

        @NotNull(message = "User id cannot be null")
        Long userId,

        @NotNull(message = "Payment amount cannot be null")
        @Positive(message = "Payment amount must be positive")
        Long paymentAmountInCents
) {
}
