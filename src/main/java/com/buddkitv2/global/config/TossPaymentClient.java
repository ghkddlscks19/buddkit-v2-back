package com.buddkitv2.global.config;

import com.buddkitv2.global.exception.TossPaymentException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient {

    private final RestClient restClient;

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossPaymentClient() {
        this.restClient = RestClient.builder().build();
    }

    public TossConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        try {
            TossConfirmResponse response = restClient.post()
                    .uri("https://api.tosspayments.com/v1/payments/confirm")
                    .header("Authorization", "Basic " + encoded)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);

            if (response == null || !"DONE".equals(response.getStatus())) {
                throw new TossPaymentException();
            }

            LocalDateTime approvedAt = OffsetDateTime.parse(response.getApprovedAt())
                    .toLocalDateTime();

            return new TossConfirmResult(
                    response.getPaymentKey(),
                    response.getOrderId(),
                    response.getMethod(),
                    response.getTotalAmount(),
                    approvedAt
            );
        } catch (TossPaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new TossPaymentException();
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class TossConfirmResponse {
        private String paymentKey;
        private String orderId;
        private String method;
        private Long totalAmount;
        private String approvedAt;
        private String status;
    }

    public record TossConfirmResult(
            String paymentKey,
            String orderId,
            String method,
            Long totalAmount,
            LocalDateTime approvedAt
    ) {}
}
