package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.innowise.paymentservice.PaymentserviceApplication;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.hamcrest.Matchers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        PaymentserviceApplication.class,
        PaymentIntegrationTest.TestCacheConfig.class
},
        properties = "jwt.secret=jwt-secret-for-test-JzdWIiOiI1Iiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3ODU3NTM1MjAsImV4cC")
@AutoConfigureMockMvc
public class PaymentIntegrationTest {

    private static final String URI = "/api/v1/payments";
    private static final String URI_W_ID = "/api/v1/payments/{id}";
    private static final String URI_SUM_USER = "/api/v1/payments/sum/{id}/{date}";
    private static final String URI_SUM_ALL = "/api/v1/payments/sum/all/{from}/{to}";
    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long AMOUNT_IN_CENTS = 1_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    static final MongoDBContainer mongoDBContainer;
    static final KafkaContainer kafkaContainer;
    static final WireMockServer wireMockServer;

    static {
        mongoDBContainer = new MongoDBContainer("mongo:7");
        mongoDBContainer.start();

        kafkaContainer = new KafkaContainer("apache/kafka:3.7.0");
        kafkaContainer.start();

        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("random-number.api.url", wireMockServer::baseUrl);
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void createPayment_shouldReturnCreatedPayment() throws Exception {
        String body = """
                {
                  "userId": %d,
                  "orderId": %d,
                  "paymentAmountInCents": %d
                }
                """.formatted(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(post(URI)
                        .with(user(USER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID));
    }

    @Test
    void createPayment_shouldReturnForbidden_whenNotOwnerAndNotAdmin() throws Exception {
        String body = """
                {
                  "userId": %d,
                  "orderId": %d,
                  "paymentAmountInCents": %d
                }
                """.formatted(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(post(URI)
                        .with(user(2L))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void pay_shouldSetStatusSuccess_whenExternalApiReturnsEvenNumber() throws Exception {
        stubRandomNumber(4L);
        String id = createPayment(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(patch(URI_W_ID, id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()));
    }

    @Test
    void pay_shouldSetStatusFailed_whenExternalApiReturnsOddNumber() throws Exception {
        stubRandomNumber(3L);
        String id = createPayment(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(patch(URI_W_ID, id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PaymentStatus.FAILED.name()));
    }

    @Test
    void pay_shouldPublishKafkaEvent_whenPaymentSucceeds() throws Exception {
        stubRandomNumber(4L);
        String id = createPayment(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(patch(URI_W_ID, id).with(admin()))
                .andExpect(status().isOk());

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
            consumer.subscribe(Pattern.compile(".*"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            boolean matched = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.value() != null && record.value().contains(String.valueOf(ORDER_ID))) {
                    matched = true;
                }
            }
            org.assertj.core.api.Assertions.assertThat(matched).isTrue();
        }
    }

    @Test
    void pay_shouldReturnNotFound_whenPaymentDoesNotExist() throws Exception {
        mockMvc.perform(patch(URI_W_ID, "does-not-exist").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayments_shouldReturnPaymentsFilteredByUserId() throws Exception {
        stubRandomNumber(4L);
        createPayment(USER_ID, ORDER_ID, AMOUNT_IN_CENTS);
        createPayment(2L, ORDER_ID, AMOUNT_IN_CENTS);

        mockMvc.perform(get(URI)
                        .with(admin())
                        .param("userId", String.valueOf(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
    }

    @Test
    void getPayments_shouldReturnForbidden_whenUserRequestsAnotherUsersPayments() throws Exception {
        mockMvc.perform(get(URI)
                        .with(user(2L))
                        .param("userId", String.valueOf(USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sumOfPaymentsForUser_shouldReturnTotalAmount() throws Exception {
        stubRandomNumber(4L);
        createPayment(USER_ID, ORDER_ID, 1_000L);
        createPayment(USER_ID, ORDER_ID + 1, 2_500L);

        mockMvc.perform(get(URI_SUM_USER, USER_ID, LocalDate.now())
                        .with(user(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("35.00")));
    }

    @Test
    void sumOfPaymentsForAll_shouldReturnTotalAmount_whenAdmin() throws Exception {
        stubRandomNumber(4L);
        createPayment(USER_ID, ORDER_ID, 1_000L);
        createPayment(2L, ORDER_ID + 1, 500L);

        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        mockMvc.perform(get(URI_SUM_ALL, from, to).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("15.00")));
    }

    @Test
    void sumOfPaymentsForAll_shouldReturnForbidden_whenNotAdmin() throws Exception {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        mockMvc.perform(get(URI_SUM_ALL, from, to).with(user(USER_ID)))
                .andExpect(status().isForbidden());
    }

    private String createPayment(Long userId, Long orderId, Long amountInCents) throws Exception {
        String body = """
                {
                  "userId": %d,
                  "orderId": %d,
                  "paymentAmountInCents": %d
                }
                """.formatted(userId, orderId, amountInCents);

        String response = mockMvc.perform(post(URI)
                        .with(admin())
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private void stubRandomNumber(Long value) {
        wireMockServer.stubFor(WireMock.get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.valueOf(value))));
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.nanoTime());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private RequestPostProcessor admin() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        1L, null, List.of(new SimpleGrantedAuthority("ADMIN"))
                )
        );
    }

    private RequestPostProcessor user(Long userId) {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("USER"))
                )
        );
    }

    @TestConfiguration
    static class TestCacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
