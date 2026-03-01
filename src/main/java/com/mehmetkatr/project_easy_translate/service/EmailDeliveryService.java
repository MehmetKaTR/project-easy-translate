package com.mehmetkatr.project_easy_translate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@talemind.app}")
    private String fromAddress;

    @Value("${app.auth.email-verification-code-ttl-minutes:15}")
    private int verifyCodeTtlMinutes;

    @Value("${app.auth.password-reset-code-ttl-minutes:15}")
    private int resetCodeTtlMinutes;

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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
