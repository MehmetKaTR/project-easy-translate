package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.exception.MailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.provider:smtp}")
    private String mailProvider;

    @Value("${app.mail.from:no-reply@talemind.app}")
    private String fromAddress;

    @Value("${app.auth.email-verification-code-ttl-minutes:15}")
    private int verifyCodeTtlMinutes;

    @Value("${app.auth.password-reset-code-ttl-minutes:15}")
    private int resetCodeTtlMinutes;

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.base-url:https://api.resend.com}")
    private String resendBaseUrl;

    private final RestClient restClient = RestClient.create();

    public void sendVerificationCode(String email, String code) {
        String subject = "TaleMind - Verify your email";
        String body = "Your TaleMind verification code: " + code + "\n\n"
                + "This code expires in " + verifyCodeTtlMinutes + " minutes.";
        send(email, subject, body, code, "EMAIL_VERIFY");
    }

    public void sendPasswordResetCode(String email, String code) {
        String subject = "TaleMind - Reset your password";
        String body = "Your TaleMind password reset code: " + code + "\n\n"
                + "This code expires in " + resetCodeTtlMinutes + " minutes.";
        send(email, subject, body, code, "PASSWORD_RESET");
    }

    private void send(String to, String subject, String body, String code, String flow) {
        if (!mailEnabled) {
            log.warn("{} mail is disabled. code={}, email={}", flow, code, to);
            return;
        }

        try {
            if ("resend".equalsIgnoreCase(mailProvider)) {
                sendWithResend(to, subject, body);
                return;
            }
            sendWithSmtp(to, subject, body);
        } catch (Exception exception) {
            throw new MailDeliveryException("Could not send verification/reset email. Please try again.", exception);
        }
    }

    private void sendWithSmtp(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendWithResend(String to, String subject, String body) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new MailDeliveryException("RESEND_API_KEY is missing while MAIL_PROVIDER=resend");
        }

        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to", List.of(to),
                "subject", subject,
                "text", body
        );

        restClient.post()
                .uri(resendBaseUrl + "/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
