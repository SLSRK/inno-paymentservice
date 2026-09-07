package com.innowise.paymentservice.service.kafka;

import com.innowise.paymentservice.model.dto.PaymentStatusEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, PaymentStatusEventDto> kafkaTemplate;

    public void sendPaymentEvent(PaymentStatusEventDto message) {
        kafkaTemplate.send("payment.cdc.status-changed", message.orderId().toString(), message);
    }
}
