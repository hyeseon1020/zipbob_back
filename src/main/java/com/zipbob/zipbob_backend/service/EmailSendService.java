package com.zipbob.zipbob_backend.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class EmailSendService {

    private final boolean production;
    private final JavaMailSender smtpSender;
    private final RestClient brevoClient;
    private final String senderEmail;

    public EmailSendService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Environment environment
    ) {
        production = environment.acceptsProfiles(Profiles.of("prod"));

        if (production) {
            smtpSender = null;
            senderEmail = environment.getRequiredProperty("app.mail.from");
            String apiKey =
                    environment.getRequiredProperty("app.mail.brevo-api-key");

            if (senderEmail.isBlank() || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "운영 메일 발신 주소와 API 키를 설정해 주세요."
                );
            }

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            JdkClientHttpRequestFactory requestFactory =
                    new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(10));

            brevoClient = RestClient.builder()
                    .baseUrl("https://api.brevo.com/v3")
                    .requestFactory(requestFactory)
                    .defaultHeader("api-key", apiKey)
                    .build();
        } else {
            smtpSender = mailSenderProvider.getObject();
            senderEmail =
                    environment.getRequiredProperty("spring.mail.username");
            brevoClient = null;
        }
    }

    public void sendVerificationCode(
            String email,
            String emailAdr,
            String verificationCode,
            long expiresInSeconds
    ) {
        String recipientEmail = email + "@" + emailAdr;
        String subject = "[집밥PICK] 이메일 인증번호 안내";

        String content = """
                안녕하세요. 집밥PICK입니다.

                요청하신 이메일 인증번호를 안내드립니다.

                인증번호: %s

                유효시간: %d초

                유효시간 내에 앱에서 인증번호를 입력해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.

                감사합니다.
                집밥PICK
                """.formatted(verificationCode, expiresInSeconds);

        if (production) {
            sendWithBrevo(recipientEmail, subject, content);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(content);

        smtpSender.send(message);
    }

    private void sendWithBrevo(
            String recipientEmail,
            String subject,
            String content
    ) {
        Map<String, Object> request = Map.of(
                "sender", Map.of(
                        "name", "집밥PICK",
                        "email", senderEmail
                ),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", subject,
                "textContent", content
        );

        try {
            brevoClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            // 외부 응답 원문이나 인증번호를 오류 메시지에 포함하지 않음
            // 중복 발송을 피하기 위해 자동 재시도하지 않음
            throw new MailSendException(
                    "인증 메일 발송 요청에 실패했습니다."
            );
        }
    }
}