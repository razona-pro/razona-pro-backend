package com.razonapro.razonaprobackend.infrastructure.email;

import com.razonapro.razonaprobackend.infrastructure.config.AppProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties  appProperties;

    private static final String BRAND_COLOR = "#D41224";
    private static final String BRAND_DARK  = "#9B1F24";
    private static final String SENDER_NAME = "RazonaPro — UFPSO";

    // ── Templates ────────────────────────────────────────────────────────

    private String base(String content) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <style>
                *{margin:0;padding:0;box-sizing:border-box;}
                body{font-family:'Helvetica Neue',Arial,sans-serif;background:#F2EDED;color:#1A1A1A;}
                .wrap{max-width:580px;margin:32px auto;background:#fff;border-radius:16px;
                      overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);}
                .header{background:linear-gradient(135deg,%s,%s);padding:36px 40px;text-align:center;}
                .logo-text{color:#fff;font-size:24px;font-weight:800;letter-spacing:-0.5px;}
                .logo-text span{opacity:0.75;}
                .tagline{color:rgba(255,255,255,0.65);font-size:12px;margin-top:6px;
                         letter-spacing:0.8px;text-transform:uppercase;}
                .body{padding:40px;}
                h1{font-size:22px;font-weight:700;color:#1A1A1A;margin-bottom:12px;line-height:1.3;}
                p{font-size:15px;color:#4A4A4A;line-height:1.75;margin-bottom:16px;}
                .btn{display:inline-block;background:linear-gradient(135deg,%s,%s);
                     color:#fff;text-decoration:none;padding:14px 32px;border-radius:10px;
                     font-weight:700;font-size:14px;letter-spacing:0.3px;
                     margin:8px 0 20px;}
                .info-box{background:#F8F8F8;border-left:3px solid %s;border-radius:0 8px 8px 0;
                          padding:14px 18px;margin:16px 0;font-size:14px;color:#555;}
                .footer{background:#F8F5F5;border-top:1px solid #EEE;
                        padding:20px 40px;text-align:center;font-size:12px;color:#999;}
                .footer a{color:#999;text-decoration:none;}
                @media(max-width:600px){.body{padding:24px;}.header{padding:28px 24px;}}
              </style>
            </head>
            <body>
              <div class="wrap">
                <div class="header">
                  <div class="logo-text">Razona<span>Pro</span></div>
                  <div class="tagline">Plataforma Saber Pro · UFPSO</div>
                </div>
                <div class="body">%s</div>
                <div class="footer">
                  Universidad Francisco de Paula Santander Ocaña<br>
                  <a href="%s">razonapro.ufpso.edu.co</a>&nbsp;&nbsp;|&nbsp;&nbsp;
                  Este mensaje fue generado automáticamente, por favor no responda.
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                BRAND_COLOR, BRAND_DARK,
                BRAND_COLOR, BRAND_DARK,
                BRAND_COLOR,
                content,
                appProperties.getFrontendUrl()
        );
    }

    // ── Envío ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(new InternetAddress(appProperties.getMailFrom(), SENDER_NAME));
            h.setTo(to.toLowerCase());
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
            log.info("Correo enviado a {} [{}]", to, subject);
        } catch (Exception e) {
            log.error("Error enviando correo a {} : {}", to, e.getMessage(), e);
        }
    }

    // ── Verificación de correo ────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String toEmail, String name, String rawToken) {
        String link    = appProperties.getFrontendUrl() + "/verify-email?token=" + rawToken;
        String display = capitalize(name);
        String content = """
            <h1>Verifica tu correo electrónico</h1>
            <p>Hola, <strong>%s</strong>. Gracias por registrarte en RazonaPro.</p>
            <p>Para activar tu cuenta y comenzar tu preparación para el Saber Pro, 
            confirma tu dirección de correo haciendo clic en el botón:</p>
            <p style="text-align:center;">
              <a href="%s" class="btn">Confirmar cuenta</a>
            </p>
            <div class="info-box">
              Este enlace es válido durante <strong>24 horas</strong>. Si no realizaste 
              este registro, puedes ignorar este mensaje con total seguridad.
            </div>
            """.formatted(display, link);
        send(toEmail, "Confirma tu cuenta — RazonaPro", base(content));
    }

    // ── Bienvenida ────────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String link    = appProperties.getFrontendUrl() + "/auth";
        String display = capitalize(name);
        String content = """
            <h1>Tu cuenta está lista, %s</h1>
            <p>Tu correo ha sido verificado exitosamente. Ya puedes acceder a la plataforma 
            y comenzar tu preparación para el examen Saber Pro.</p>
            <p style="text-align:center;">
              <a href="%s" class="btn">Acceder a RazonaPro</a>
            </p>
            <div class="info-box">
              Tienes acceso a simulacros adaptativos, práctica con inteligencia artificial 
              y rankings en tiempo real. Te deseamos mucho éxito en tu preparación.
            </div>
            """.formatted(display, link);
        send(toEmail, "Bienvenido a RazonaPro", base(content));
    }

    // ── Restablecimiento de contraseña ────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String rawToken) {
        String link    = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;
        String display = capitalize(name);
        String content = """
            <h1>Solicitud de restablecimiento de contraseña</h1>
            <p>Hola, <strong>%s</strong>. Recibimos una solicitud para restablecer la 
            contraseña asociada a tu cuenta en RazonaPro.</p>
            <p style="text-align:center;">
              <a href="%s" class="btn">Crear nueva contraseña</a>
            </p>
            <div class="info-box">
              Este enlace es válido durante <strong>15 minutos</strong> y solo puede 
              usarse una vez. Si no solicitaste este cambio, tu contraseña actual 
              permanece segura y puedes ignorar este mensaje.
            </div>
            """.formatted(display, link);
        send(toEmail, "Restablecimiento de contraseña — RazonaPro", base(content));
    }

    // ── Nuevo test disponible ─────────────────────────────────────────────

    @Async
    public void sendNewTestEmail(String toEmail, String name, String testName, String competenceName) {
        String link    = appProperties.getFrontendUrl() + "/tests";
        String display = capitalize(name);
        String content = """
            <h1>Nuevo simulacro disponible</h1>
            <p>Hola, <strong>%s</strong>. Se ha publicado un nuevo simulacro en la plataforma.</p>
            <div class="info-box">
              <strong>%s</strong><br>
              Área: %s
            </div>
            <p>Practica cuando lo consideres oportuno. Cada simulacro contribuye a 
            mejorar tu posición en el ranking y a fortalecer tu preparación.</p>
            <p style="text-align:center;">
              <a href="%s" class="btn">Ver simulacros</a>
            </p>
            """.formatted(display, testName, competenceName, link);
        send(toEmail, "Nuevo simulacro disponible — RazonaPro", base(content));
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        String lower = s.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}