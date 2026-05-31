package com.razonapro.razonaprobackend.infrastructure.email;

import com.razonapro.razonaprobackend.infrastructure.config.AppProperties;
import jakarta.mail.MessagingException;
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

    private static final String STYLE = """
        <style>
          @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700;900&display=swap');
          * { margin:0; padding:0; box-sizing:border-box; }
          body { font-family:'Poppins',Arial,sans-serif; background:#F5EEEE; }
          .wrap { max-width:560px; margin:32px auto; background:#fff;
                  border-radius:20px; overflow:hidden;
                  box-shadow:0 8px 32px rgba(212,18,36,.10); }
          .header { background:linear-gradient(135deg,#D41224,#9B1F24);
                    padding:32px 40px 28px; text-align:center; }
          .header h1 { color:#fff; font-size:22px; font-weight:900;
                       letter-spacing:-0.5px; }
          .header p  { color:rgba(255,255,255,.75); font-size:13px; margin-top:4px; }
          .body { padding:36px 40px; }
          .body h2 { font-size:20px; font-weight:800; color:#1A1A1A;
                     margin-bottom:10px; }
          .body p  { font-size:14px; color:#555; line-height:1.75;
                     margin-bottom:16px; }
          .btn { display:inline-block; background:linear-gradient(135deg,#D41224,#9B1F24);
                 color:#fff!important; text-decoration:none;
                 padding:14px 32px; border-radius:12px; font-weight:700;
                 font-size:14px; margin:8px 0 20px;
                 box-shadow:0 4px 16px rgba(212,18,36,.35); }
          .note { font-size:12px!important; color:#999!important; }
          .footer { background:#F9F9F9; border-top:1px solid #F0E8E8;
                    padding:20px 40px; text-align:center;
                    font-size:12px; color:#AAA; }
          .badge { display:inline-block; background:rgba(212,18,36,.08);
                   color:#D41224; font-weight:700; font-size:12px;
                   padding:4px 12px; border-radius:20px; margin-bottom:20px; }
        </style>
        """;

    private String baseTemplate(String headerTitle, String headerSub, String body) {
        return """
            <!DOCTYPE html><html lang="es"><head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            """ + STYLE + """
            </head><body>
            <div class="wrap">
              <div class="header">
                <h1>🎓 RazonaPro</h1>
                <p>Plataforma Saber Pro · UFPSO</p>
              </div>
              <div class="body">
                """ + body + """
              </div>
              <div class="footer">
                © 2025 RazonaPro · Universidad Francisco de Paula Santander Ocaña<br>
                Este es un correo automático, no respondas a este mensaje.
              </div>
            </div>
            </body></html>
            """;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String name, String rawToken) {
        String link = appProperties.getFrontendUrl() + "/verify-email?token=" + rawToken;
        String body = """
            <div class="badge">✉ Verificación de correo</div>
            <h2>¡Hola, %s!</h2>
            <p>Gracias por registrarte en <strong>RazonaPro</strong>. 
            Para activar tu cuenta y comenzar a prepararte para el Saber Pro, 
            verifica tu correo haciendo clic en el botón:</p>
            <center><a href="%s" class="btn">Verificar mi correo →</a></center>
            <p class="note">⏳ Este enlace expira en <strong>24 horas</strong>. 
            Si no te registraste en RazonaPro, ignora este mensaje.</p>
            """.formatted(capitalize(name), link);
        send(toEmail, "Verifica tu correo — RazonaPro", baseTemplate("", "", body));
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String link = appProperties.getFrontendUrl() + "/auth";
        String body = """
            <div class="badge">🎉 ¡Cuenta activada!</div>
            <h2>¡Bienvenido, %s!</h2>
            <p>Tu cuenta en <strong>RazonaPro</strong> está lista. 
            Ya puedes iniciar sesión y comenzar tu preparación para el 
            <strong>Saber Pro UFPSO</strong>.</p>
            <center><a href="%s" class="btn">Iniciar sesión →</a></center>
            <p>Tienes acceso a simulacros adaptativos, práctica con IA y 
            rankings en tiempo real. ¡Mucho éxito! 🚀</p>
            """.formatted(capitalize(name), link);
        send(toEmail, "¡Bienvenido a RazonaPro! — Cuenta activada", baseTemplate("", "", body));
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String rawToken) {
        String link = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;
        String body = """
            <div class="badge">🔐 Restablecer contraseña</div>
            <h2>Hola, %s</h2>
            <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta 
            en <strong>RazonaPro</strong>. Haz clic en el botón para crear una nueva:</p>
            <center><a href="%s" class="btn">Restablecer contraseña →</a></center>
            <p class="note">⏳ Este enlace expira en <strong>15 minutos</strong>. 
            Si no solicitaste esto, ignora este correo; tu contraseña no cambiará.</p>
            """.formatted(capitalize(name), link);
        send(toEmail, "Restablecer contraseña — RazonaPro", baseTemplate("", "", body));
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        String lower = s.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
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

    @Async
    public void sendNewTestEmail(String toEmail, String name, String testName, String competenceName) {
        String link = appProperties.getFrontendUrl() + "/tests";
        String body = """
        <div class="badge">📝 Nuevo test</div>
        <h2>¡Hola, %s!</h2>
        <p>Se publicó un nuevo test en <strong>RazonaPro</strong>:
        <strong>%s</strong> (%s). Ya puedes practicar.</p>
        <center><a href="%s" class="btn">Ir a los tests →</a></center>
        """.formatted(capitalize(name), testName, competenceName, link);
        send(toEmail, "Nuevo test disponible — RazonaPro", baseTemplate("", "", body));
    }
}