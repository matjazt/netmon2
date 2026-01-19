package com.matjazt.netmon2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration for JavaMailSender using AlerterProperties.
 *
 * <p>This creates the JavaMailSender bean that AlerterService uses to send emails.
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(AlerterProperties properties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(properties.getSmtpHost());
        mailSender.setPort(properties.getSmtpPort());
        mailSender.setUsername(properties.getSmtpUsername());
        mailSender.setPassword(properties.getSmtpPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", properties.isSmtpAuth());
        props.put("mail.smtp.starttls.enable", properties.isSmtpStartTls());
        props.put("mail.debug", "false"); // Set to "true" for debugging SMTP issues

        return mailSender;
    }
}
