package com.zipbob.zipbob_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
// 인증 메일 발송
public class EmailSendService {

    private final JavaMailSender javaMailSender;
    private final String senderEmail;

    public EmailSendService(
            JavaMailSender javaMailSender,
            @Value("${spring.mail.username}") String senderEmail
    ) {
        this.javaMailSender = javaMailSender;
        this.senderEmail = senderEmail;
    }

    // 인증번호 메일 발송
    public void sendVerificationCode(
            String email,
            String emailAdr,
            String verificationCode,
            long expiresInSeconds
    ) {
        // 분리된 이메일을 전체 주소로 조합
        String recipientEmail = email + "@" + emailAdr;

        SimpleMailMessage message = new SimpleMailMessage();

        // application.properties에 설정한 발신 계정
        message.setFrom(senderEmail);

        // 인증받을 사용자 이메일
        message.setTo(recipientEmail);

        message.setSubject("[집밥PICK] 이메일 인증번호 안내");

        message.setText("""
                안녕하세요. 집밥PICK입니다.

                요청하신 이메일 인증번호를 안내드립니다.

                인증번호: %s

                유효시간: %d초

                유효시간 내에 앱에서 인증번호를 입력해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.

                감사합니다.
                집밥PICK
                """.formatted(
                verificationCode,
                expiresInSeconds
        ));

        javaMailSender.send(message);
    }
}