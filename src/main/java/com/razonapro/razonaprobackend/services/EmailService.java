package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Async
    public void sendVerificationEmail(String toEmail, String studentName, String token) {
        String link = appProperties.getFrontendUrl() + "/verify-email?token=" + token;
        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 32px;">
              <h2 style="color: #4F46E5;">¡Bienvenido a RazonaPro, %s! 🎯</h2>
              <p>Gracias por registrarte. Verifica tu correo electrónico haciendo clic en el botón:</p>
              <a href="%s"
                 style="display:inline-block;background:#4F46E5;color:#fff;padding:14px 28px;
                        border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0;">
                 Verificar mi correo
              </a>
              <p style="color:#888;font-size:13px;">Este enlace expira en 24 horas.<br>
              Si no te registraste en RazonaPro, ignora este correo.</p>
            </div>
            """.formatted(studentName, link);
        send(toEmail, "Verifica tu correo — RazonaPro", html);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String studentName) {
        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 32px;">
              <h2 style="color: #4F46E5;">¡Tu cuenta está activa, %s! ✅</h2>
              <p>Ya puedes iniciar sesión en <a href="%s">RazonaPro</a> y empezar a practicar.</p>
              <p style="color:#888;font-size:13px;">¡Mucho éxito!</p>
            </div>
            """.formatted(studentName, appProperties.getFrontendUrl());
        send(toEmail, "¡Cuenta verificada! — RazonaPro", html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        String link = appProperties.getFrontendUrl() + "/reset-password?token=" + token;
        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 32px;">
              <h2 style="color: #4F46E5;">Restablece tu contraseña, %s</h2>
              <p>Recibimos una solicitud para restablecer tu contraseña:</p>
              <a href="%s"
                 style="display:inline-block;background:#4F46E5;color:#fff;padding:14px 28px;
                        border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0;">
                 Restablecer contraseña
              </a>
              <p style="color:#888;font-size:13px;">Expira en 24 horas. Si no lo solicitaste, ignora este correo.</p>
            </div>
            """.formatted(name, link);
        send(toEmail, "Restablecer contraseña — RazonaPro", html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("Email enviado a {}", to);
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
        }
    }
}
