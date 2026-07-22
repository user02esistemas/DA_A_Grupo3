package com.arteymetal.ArteyMetal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        String subject = "Codigo de recuperacion de contrasena - ARTE Y METAL";
        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #2a2419;">Recuperacion de contrasena</h2>
                <p style="color: #6e6758;">Has solicitado restablecer tu contrasena. Usa el siguiente codigo de 6 digitos para continuar:</p>
                <div style="background: #fffdf7; border: 2px solid #d1be8a; border-radius: 12px; padding: 20px; text-align: center; margin: 20px 0;">
                    <span style="font-size: 32px; font-weight: bold; color: #b9943d; letter-spacing: 8px;">%s</span>
                </div>
                <p style="color: #6e6758;">Este codigo expira en 15 minutos.</p>
                <p style="color: #999; font-size: 12px;">Si no solicitaste este cambio, puedes ignorar este correo.</p>
                <p style="color: #6e6758; margin-top: 20px;">Saludos,<br><strong>ARTE Y METAL</strong></p>
            </div>
            """.formatted(code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@arteymetal.com");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de recuperacion: " + e.getMessage(), e);
        }
    }
}
