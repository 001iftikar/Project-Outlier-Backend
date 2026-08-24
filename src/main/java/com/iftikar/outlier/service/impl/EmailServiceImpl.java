package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.config.BrevoProperties;
import com.iftikar.outlier.service.api.EmailService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {
    private final RestClient restClient;
    private final BrevoProperties brevoProperties;

    public EmailServiceImpl(BrevoProperties brevoProperties) {
        this.brevoProperties = brevoProperties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com")
                .defaultHeader("api-key", brevoProperties.apiKey())
                .defaultHeader("accept", "application/json")
                .build();
    }

    @Override
    public void sendVerificationOtp(String recipientEmail, String otp) {
        BrevoEmailRequest request = new BrevoEmailRequest(
                new Sender(
                        brevoProperties.senderEmail(),
                        brevoProperties.senderName()
                ),
                List.of(
                        new Recipient(recipientEmail)
                ),
                "Outlier verification code",
                "Your Outlier verification code is: "
                        + otp
                        + "\n\nThis code expires in 5 minutes."
        );

        restClient.post()
                .uri("/v3/smtp/username")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private record BrevoEmailRequest(
            Sender sender,
            List<Recipient> to,
            String subject,
            String textContent
    ) {
    }

    private record Sender(
            String email,
            String name
    ) {
    }

    private record Recipient(
            String email
    ) {
    }
}
