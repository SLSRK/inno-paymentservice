package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.exception.NotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.dto.PaymentCreateDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.SumOfPaymentsDto;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import com.innowise.paymentservice.service.kafka.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_shouldCreatePayment() {
        PaymentCreateDto paymentCreateDto = Mockito.mock(PaymentCreateDto.class);
        Payment payment = testPayment();
        Payment savedPayment = testPayment();
        PaymentResponseDto responseDto = Mockito.mock(PaymentResponseDto.class);

        when(paymentMapper.toEntity(paymentCreateDto)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(responseDto);

        PaymentResponseDto resultDto = paymentService.createPayment(paymentCreateDto);

        assertEquals(responseDto, resultDto);
        verify(paymentRepository).save(payment);
    }

    @Test
    void pay_shouldSetSuccessWhenRandomNumberIsEven() {
        Payment payment = testPayment();
        PaymentResponseDto paymentResponseDto = Mockito.mock(PaymentResponseDto.class);

        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment));
        when(externalApiClient.getRandomNumber())
                .thenReturn(2L);
        when(paymentRepository.save(payment))
                .thenReturn(payment);
        when(paymentMapper.toDto(payment))
                .thenReturn(paymentResponseDto);

        PaymentResponseDto responseDto = paymentService.pay("1");

        assertEquals(paymentResponseDto, responseDto);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(kafkaProducerService).sendPaymentEvent(any());
    }

    @Test
    void pay_shouldSetFailedWhenRandomNumberIsOdd() {
        Payment payment = testPayment();
        PaymentResponseDto paymentResponseDto = Mockito.mock(PaymentResponseDto.class);

        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment));
        when(externalApiClient.getRandomNumber())
                .thenReturn(3L);
        when(paymentRepository.save(payment))
                .thenReturn(payment);
        when(paymentMapper.toDto(payment))
                .thenReturn(paymentResponseDto);

        PaymentResponseDto responseDto = paymentService.pay("1");
        assertEquals(paymentResponseDto, responseDto);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void pay_shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> paymentService.pay("1")
        );
    }

    @Test
    void getPayments_shouldReturnPayments() {
        Payment payment = testPayment();
        PaymentResponseDto paymentResponseDto = Mockito.mock(PaymentResponseDto.class);

        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of(payment));
        when(paymentMapper.toDto(payment))
                .thenReturn(paymentResponseDto);

        List<PaymentResponseDto> resultDto =
                paymentService.getPayments(1L, 2L, PaymentStatus.SUCCESS);

        assertEquals(1, resultDto.size());
        assertEquals(paymentResponseDto, resultDto.get(0));
        verify(mongoTemplate).find(any(Query.class), Mockito.eq(Payment.class));
    }

    @Test
    void getPayments_shouldReturnEmptyListWhenNoPaymentsFound() {
        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of());

        List<PaymentResponseDto> paymentResponseDtos =
                paymentService.getPayments(1L, null, null);

        assertEquals(0, paymentResponseDtos.size());
    }

    @Test
    void sumForUser_shouldReturnSum() {
        Payment payment1 = testPayment();
        payment1.setPaymentAmount(1000L);
        Payment payment2 = testPayment();
        payment2.setPaymentAmount(2500L);

        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of(payment1, payment2));
        SumOfPaymentsDto sumOfPaymentsDto =
                paymentService.sumForUser(1L, LocalDate.of(2026, 8, 20));

        assertEquals("user with id:1", sumOfPaymentsDto.sumForUser());
        assertEquals("35.00", sumOfPaymentsDto.totalAmount());
    }

    @Test
    void sumForUser_shouldReturnZeroWhenNoPaymentsFound() {
        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of());
        SumOfPaymentsDto sumOfPaymentsDto =
                paymentService.sumForUser(1L, LocalDate.of(2026, 8, 20));

        assertEquals("user with id:1", sumOfPaymentsDto.sumForUser());
        assertEquals("0.00", sumOfPaymentsDto.totalAmount());
    }

    @Test
    void sumForAll_shouldReturnSum() {
        Payment payment1 = testPayment();
        payment1.setPaymentAmount(1000L);
        Payment payment2 = testPayment();
        payment2.setPaymentAmount(5000L);

        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of(payment1, payment2));

        SumOfPaymentsDto sumOfPaymentsDto = paymentService.sumForAll(
                LocalDateTime.of(2026, 8, 20, 0, 0),
                LocalDateTime.of(2026, 8, 20, 23, 59)
        );

        assertEquals("all", sumOfPaymentsDto.sumForUser());
        assertEquals("60.00", sumOfPaymentsDto.totalAmount());
    }

    @Test
    void sumForAll_shouldReturnZeroWhenNoPaymentsFound() {
        when(mongoTemplate.find(any(Query.class), Mockito.eq(Payment.class)))
                .thenReturn(List.of());

        SumOfPaymentsDto sumOfPaymentsDto = paymentService.sumForAll(
                LocalDateTime.of(2026, 8, 20, 0, 0),
                LocalDateTime.of(2026, 8, 20, 23, 59)
        );

        assertEquals("all", sumOfPaymentsDto.sumForUser());
        assertEquals("0.00", sumOfPaymentsDto.totalAmount());
    }

    private Payment testPayment() {
        Payment payment = new Payment();
        payment.setId("1");
        payment.setOrderId(2L);
        payment.setUserId(1L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentAmount(900L);
        return payment;
    }
}
