package com.innowise.paymentservice.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PaymentResponseDto(

        String id,

        Long orderId,

        Long userId,

        String status,

        LocalDateTime timestamp,

        String paymentAmount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
