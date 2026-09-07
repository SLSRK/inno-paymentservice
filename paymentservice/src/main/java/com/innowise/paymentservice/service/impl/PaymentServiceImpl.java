package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.exception.DataException;
import com.innowise.paymentservice.exception.NotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.dto.PaymentCreateDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.PaymentStatusResponseDto;
import com.innowise.paymentservice.model.dto.SumOfPaymentsDto;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final KafkaProducerService kafkaProducerService;

    private final ExternalApiClient externalApiClient;
    private final MongoTemplate mongoTemplate;

    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponseDto createPayment(PaymentCreateDto paymentCreateDto) {
        log.debug("Creating a new payment for user={}, order={} and amount={}",
                paymentCreateDto.userId(),
                paymentCreateDto.orderId(),
                paymentCreateDto.paymentAmountInCents());
        Payment payment = paymentMapper.toEntity(paymentCreateDto);

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentResponseDto pay(String id) {
        Payment payment = paymentRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Payment not found"));
        if(payment.getStatus().equals(PaymentStatus.SUCCESS)) {
            throw new DataException("Payment was already paid");
        }
        if(externalApiClient.getRandomNumber() % 2 == 0){
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            kafkaProducerService.sendPaymentEvent(
                    PaymentStatusResponseDto.builder()
                            .orderId(payment.getOrderId())
                            .status(String.valueOf(PaymentStatus.SUCCESS))
                            .amount(payment.getPaymentAmount())
                            .build());
        }
        else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
        log.debug("Paying the payment with the id={}, with status={}",
                id,
                payment.getStatus());
        return paymentMapper.toDto(payment);
    }

    @Cacheable(
            value = "payments",
            key = "'user:' + #userId + ':order:' + #orderId + ':status:' + #status"
    )
    public List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status) {
        Query query = new Query();

        if(userId != null){
            query.addCriteria(Criteria.where("user_id").is(userId));
        }
        if(orderId != null){
            query.addCriteria(Criteria.where("order_id").is(orderId));
        }
        if(status != null){
            query.addCriteria(Criteria.where("status").is(status));
        }

        return mongoTemplate.find(query,Payment.class).stream()
                .map(payment -> paymentMapper.toDto(payment))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(
            value = "payments",
            key = "'user-sum:' + #userId + ':' + #date"
    )
    public SumOfPaymentsDto sumForUser(Long userId, LocalDate date) {
        return new SumOfPaymentsDto(
                "user with id:" + userId,
                sumOfPayments(userId, date.atStartOfDay(), date.atTime(LocalTime.MAX)));
    }

    @Cacheable(
            value = "payments",
            key = "'all-sum:' + #from + ':' + #to"
    )
    public SumOfPaymentsDto sumForAll(LocalDateTime from, LocalDateTime to) {
        return new SumOfPaymentsDto(
                "all",
                sumOfPayments(null, from, to));
    }

    private String sumOfPayments(Long userId, LocalDateTime from, LocalDateTime to) {
        Query query = new Query();

        query.addCriteria(Criteria.where("timestamp").gte(from).lte(to));
        if(userId != null) {
            query.addCriteria(Criteria.where("user_id").is(userId));
        }

        Long amount = mongoTemplate.find(query, Payment.class).stream()
                .mapToLong(Payment :: getPaymentAmount)
                .sum();

        return String.format(Locale.US, "%.2f", amount / 100.0);
    }
}
