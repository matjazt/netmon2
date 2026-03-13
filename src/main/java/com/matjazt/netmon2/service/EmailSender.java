package com.matjazt.netmon2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailSender {

    @Value("${email-delivery.smtp-host}")
    private String smtpHost;

    @Value("${email-delivery.smtp-port:587}")
    private int smtpPort;

    @Value("${email-delivery.smtp-username}")
    private String smtpUsername;

    @Value("${email-delivery.smtp-password}")
    private String smtpPassword;

    @Value("${email-delivery.smtp-starttls:true}")
    private boolean smtpStartTls;

    @Value("${email-delivery.smtp-auth:true}")
    private boolean smtpAuth;

    @Bean
    public JavaMailSender javaMailSender() {
        var mailSender = new JavaMailSenderImpl();

        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(smtpUsername);
        mailSender.setPassword(smtpPassword);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", smtpStartTls);
        props.put("mail.debug", "false"); // Set to "true" for debugging SMTP issues

        return mailSender;
    }
}
