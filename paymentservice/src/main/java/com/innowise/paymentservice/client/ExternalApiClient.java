package com.innowise.paymentservice.client;

import com.innowise.paymentservice.exception.ForeignServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${random.number.api.url}")
    private String randomNumberApiUrl;

    public Long getRandomNumber() {
        ResponseEntity<String> externalResponse = restTemplate.getForEntity(randomNumberApiUrl, String.class);

        if (!externalResponse.getStatusCode().is2xxSuccessful() || externalResponse.getBody() == null) {
            throw new ForeignServiceException("External API failed");
        }

        return Long.valueOf(externalResponse.getBody().trim());
    }
}
