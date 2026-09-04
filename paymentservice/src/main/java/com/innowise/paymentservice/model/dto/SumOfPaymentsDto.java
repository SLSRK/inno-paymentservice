package com.innowise.paymentservice.model.dto;

import java.io.Serializable;

public record SumOfPaymentsDto(

        String sumForUser,

        String totalAmount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
