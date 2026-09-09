package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.model.dto.PaymentCreateDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Locale;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    @Mapping(target = "paymentAmount", source = "paymentAmountInCents")
    Payment toEntity(PaymentCreateDto paymentCreateDto);

    @Mapping(target = "paymentAmount", expression = "java(mapAmount(payment.getPaymentAmount()))")
    PaymentResponseDto toDto(Payment payment);

    default String mapAmount(Long amountInCents) {
        return String.format(Locale.US, "%.2f", amountInCents / 100.0);
    }
}
