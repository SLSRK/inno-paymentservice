package com.innowise.paymentservice.model.dto;

import java.io.Serializable;

public record SumOfPaymentsResponseDto(

        String sumForUser,

        String totalAmount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
