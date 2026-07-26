package com.superhumans.medicationsheet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(name = "smtpEmailService")
public class LogEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String text) {
        log.info("[EMAIL STUB] To: {}, Subject: {}, Body(length={})",
                to, subject, text != null ? text.length() : 0);
    }
}
