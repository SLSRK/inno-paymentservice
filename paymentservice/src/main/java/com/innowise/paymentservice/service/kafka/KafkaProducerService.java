package com.innowise.paymentservice.service.kafka;

import com.innowise.paymentservice.model.dto.PaymentStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, PaymentStatusDto> kafkaTemplate;

    public void sendPaymentEvent(PaymentStatusDto message) {
        kafkaTemplate.send("payment.cdc.status-changed", message.orderId().toString(), message);
    }
}
