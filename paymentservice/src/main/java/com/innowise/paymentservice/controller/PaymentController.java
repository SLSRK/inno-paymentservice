package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.model.dto.PaymentCreateDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.SumOfPaymentsResponseDto;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN') or #paymentCreateDto.userId == authentication.principal")
    public PaymentResponseDto createPayment(@RequestBody @Valid PaymentCreateDto paymentCreateDto) {
        return paymentService.createPayment(paymentCreateDto);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isPayer(#id, authentication.principal)")
    public PaymentResponseDto pay(@PathVariable String id) {
        return paymentService.pay(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.principal")
    public List<PaymentResponseDto> getPayments(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Long orderId,
                                                @RequestParam(required = false) PaymentStatus status) {
        return paymentService.getPayments(userId, orderId, status);
    }

    @GetMapping("/sum/{id}/{date}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN') or #id == authentication.principal")
    public SumOfPaymentsResponseDto sumOfPaymentsForUser(@PathVariable Long id,
                                                         @PathVariable LocalDate date) {
        return paymentService.sumForUser(id, date);
    }

    @GetMapping("/sum/all/{from}/{to}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public SumOfPaymentsResponseDto sumOfPaymentsForAll(@PathVariable LocalDateTime from,
                                                        @PathVariable LocalDateTime to) {
        return paymentService.sumForAll(from, to);
    }
}
