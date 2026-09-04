package com.innowise.paymentservice.security;

import com.innowise.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    public boolean isPayer(String paymentId, Long userId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> payment.getUserId().equals(userId))
                .orElse(false);
    }
}
