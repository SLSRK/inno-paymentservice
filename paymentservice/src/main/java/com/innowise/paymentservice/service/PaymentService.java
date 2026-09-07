package com.innowise.paymentservice.service;

import com.innowise.paymentservice.model.dto.PaymentCreateDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.SumOfPaymentsResponseDto;
import com.innowise.paymentservice.model.entity.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    /**
     * Create a new payment;
     *
     * @param paymentCreateDto data of a payment to create;
     * @return returns data of the saved payment.
     */
    PaymentResponseDto createPayment(PaymentCreateDto paymentCreateDto);

    /**
     * Pay the fee(payment) by ID;
     *
     * @param id ID of a fee(payment) to pay;
     * @return returns data of the payment with the result.
     */
    PaymentResponseDto pay(String id);

    /**
     * Get payments by criteria;
     *
     * @param userId ID of a payment's payer;
     * @param orderId ID of a payment's order;
     * @param status status of a payments to get;
     * @return the payments, that match the given criteria.
     */
    List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status);

    /**
     * Get total sum of payments for date range for a user;
     *
     * @param userId ID of a payment's payer;
     * @param date payment date to filter results;
     * @return returns total sum of the payments, that match the given criteria.
     */
    SumOfPaymentsResponseDto sumForUser(Long userId, LocalDate date);

    /**
     * Get total sum of payments for date range for all users
     *
     * @param from minimum payment date and time to filter results;
     * @param to maximum payment date and time to filter results;
     * @return returns total sum of the payments, that match the given criteria.
     */
    SumOfPaymentsResponseDto sumForAll(LocalDateTime from, LocalDateTime to);
}
