package com.superhumans.service.impl;

import com.superhumans.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    @SneakyThrows
    public void sendEmail(String to, String subject, String text) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom("medicinelistnotifications@superhumans.com");
        helper.setTo(to);
        helper.setSubject(subject);

        helper.setText(text, true);

        mailSender.send(mimeMessage);
    }
}
