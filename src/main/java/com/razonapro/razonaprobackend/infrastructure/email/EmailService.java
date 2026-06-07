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
    private static final String SENDER_NAME = "RazonaPro";

    // Estilos inline para el botón CTA (CSS en <style> lo ignoran Gmail/Outlook)
    private static final String BTN_STYLE =
        "display:inline-block;background-color:#D41224;color:#ffffff;text-decoration:none;" +
        "padding:14px 32px;border-radius:10px;font-weight:700;font-size:14px;" +
        "letter-spacing:0.3px;margin:8px 0 20px;font-family:Georgia,'Times New Roman',serif;";

    // ── Templates ────────────────────────────────────────────────────────

    private String base(String content) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <!--[if mso]>
              <noscript><xml><o:OfficeDocumentSettings><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml></noscript>
              <![endif]-->
            </head>
            <body style="margin:0;padding:0;background-color:#F2EDED;font-family:Georgia,'Times New Roman',serif;color:#1A1A1A;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#F2EDED;">
                <tr><td align="center" style="padding:32px 16px;">
                  <table width="580" cellpadding="0" cellspacing="0" border="0"
                         style="max-width:580px;width:100%%;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <!-- HEADER -->
                    <tr>
                      <td style="background-color:#D41224;padding:36px 40px;text-align:center;">
                        <p style="margin:0;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:-0.5px;font-family:Georgia,'Times New Roman',serif;">
                          Razona<span style="opacity:0.75;">Pro</span>
                        </p>
                        <p style="margin:6px 0 0;color:rgba(255,255,255,0.65);font-size:11px;letter-spacing:0.8px;text-transform:uppercase;font-family:Georgia,'Times New Roman',serif;">
                          Plataforma de evaluación
                        </p>
                      </td>
                    </tr>
                    <!-- BODY -->
                    <tr>
                      <td style="padding:40px;font-family:Georgia,'Times New Roman',serif;">
                        %s
                      </td>
                    </tr>
                    <!-- FOOTER -->
                    <tr>
                      <td style="background-color:#F8F5F5;border-top:1px solid #EEEEEE;padding:20px 40px;text-align:center;font-size:12px;color:#999999;font-family:Georgia,'Times New Roman',serif;">
                        <a href="%s" style="color:#999999;text-decoration:none;">razonapro.edu.co</a>
                        &nbsp;&nbsp;|&nbsp;&nbsp;
                        Mensaje generado automáticamente, por favor no responda.
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(content, appProperties.getFrontendUrl());
    }

    // Helpers para generar HTML con estilos inline
    private String h1(String text) {
        return "<h1 style=\"margin:0 0 12px;font-size:22px;font-weight:700;color:#1A1A1A;line-height:1.3;font-family:Georgia,'Times New Roman',serif;\">" + text + "</h1>";
    }
    private String p(String html) {
        return "<p style=\"margin:0 0 16px;font-size:15px;color:#4A4A4A;line-height:1.75;font-family:Georgia,'Times New Roman',serif;\">" + html + "</p>";
    }
    private String btn(String href, String label) {
        return "<p style=\"text-align:center;margin:8px 0 20px;\"><a href=\"" + href + "\" style=\"" + BTN_STYLE + "\">" + label + "</a></p>";
    }
    private String infoBox(String html) {
        return "<div style=\"background-color:#F8F8F8;border-left:3px solid #D41224;border-radius:0 8px 8px 0;padding:14px 18px;margin:16px 0;font-size:14px;color:#555555;font-family:Georgia,'Times New Roman',serif;\">" + html + "</div>";
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
        String content =
            h1("Verifica tu correo electrónico") +
            p("Hola, <strong>" + display + "</strong>. Gracias por registrarte en RazonaPro.") +
            p("Para activar tu cuenta confirma tu dirección de correo haciendo clic en el botón:") +
            btn(link, "Confirmar cuenta") +
            infoBox("Este enlace es válido durante <strong>24 horas</strong>. Si no realizaste " +
                    "este registro, puedes ignorar este mensaje con total seguridad.");
        send(toEmail, "Confirma tu cuenta - RazonaPro", base(content));
    }

    // ── Bienvenida ────────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String link    = appProperties.getFrontendUrl() + "/auth";
        String display = capitalize(name);
        String content =
            h1("Tu cuenta está lista, " + display) +
            p("Tu correo ha sido verificado exitosamente. Ya puedes acceder a la plataforma.") +
            btn(link, "Acceder a RazonaPro") +
            infoBox("Tienes acceso a simulacros adaptativos, práctica con inteligencia " +
                    "artificial y rankings en tiempo real. ¡Mucho éxito en tu preparación!");
        send(toEmail, "Bienvenido a RazonaPro", base(content));
    }

    // ── Credenciales de administrador (cuenta creada por otro admin) ──────

    @Async
    public void sendAdminCredentialsEmail(String toEmail, String name, String adminCode, String rawPassword) {
        String link    = appProperties.getFrontendUrl() + "/auth";
        String display = capitalize(name);
        String content =
            h1("Tu cuenta de administrador está lista") +
            p("Hola, <strong>" + display + "</strong>. Se creó una cuenta de administrador en " +
              "RazonaPro asociada a este correo. Estas son tus credenciales de acceso:") +
            infoBox("<strong>Código de administrador:</strong> " + adminCode + "<br>" +
                    "<strong>Correo:</strong> " + toEmail + "<br>" +
                    "<strong>Contraseña temporal:</strong> " + rawPassword) +
            btn(link, "Iniciar sesión") +
            p("Necesitas el <strong>código de administrador</strong> junto con tu correo y contraseña para ingresar. " +
              "Por seguridad, te recomendamos cambiar la contraseña después de iniciar sesión.");
        send(toEmail, "Acceso de administrador - RazonaPro", base(content));
    }

    // ── Restablecimiento de contraseña ────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String rawToken) {
        String link    = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;
        String display = capitalize(name);
        String content =
            h1("Restablecimiento de contraseña") +
            p("Hola, <strong>" + display + "</strong>. Recibimos una solicitud para " +
              "restablecer la contraseña de tu cuenta en RazonaPro.") +
            btn(link, "Crear nueva contraseña") +
            infoBox("Este enlace es válido durante <strong>15 minutos</strong> y solo puede " +
                    "usarse una vez. Si no solicitaste este cambio, tu contraseña actual " +
                    "permanece segura y puedes ignorar este mensaje.");
        send(toEmail, "Restablecimiento de contraseña - RazonaPro", base(content));
    }

    // ── Nuevo test disponible ─────────────────────────────────────────────

    @Async
    public void sendNewTestEmail(String toEmail, String name, String testName, String competenceName) {
        String link    = appProperties.getFrontendUrl() + "/tests";
        String display = capitalize(name);
        String content =
            h1("Nuevo simulacro disponible") +
            p("Hola, <strong>" + display + "</strong>. Se ha publicado un nuevo simulacro " +
              "en la plataforma.") +
            infoBox("<strong>" + testName + "</strong><br>Área: " + competenceName) +
            p("Practica cuando lo consideres oportuno. Cada simulacro contribuye a mejorar " +
              "tu posición en el ranking y fortalecer tu preparación.") +
            btn(link, "Ver simulacros");
        send(toEmail, "Nuevo simulacro disponible - RazonaPro", base(content));
    }

    // ── Código de verificación (acción sensible: cambio de contraseña) ─────

    @Async
    public void sendVerificationCodeEmail(String toEmail, String name, String code) {
        String display = capitalize(name);
        String content =
            h1("Código de verificación") +
            p("Hola, <strong>" + display + "</strong>. Para continuar con el cambio de contraseña, " +
              "usa el siguiente código de verificación:") +
            "<p style=\"text-align:center;margin:8px 0 20px\"><span style=\"display:inline-block;font-family:Georgia,serif;" +
              "font-size:30px;font-weight:800;letter-spacing:8px;color:#D41224;background:#F8F8F8;border:1px solid #EEE;" +
              "border-radius:10px;padding:14px 26px\">" + code + "</span></p>" +
            infoBox("Este código vence en <strong>10 minutos</strong>. Si no solicitaste este cambio, " +
                    "ignora este mensaje y tu contraseña seguirá igual.");
        send(toEmail, "Código de verificación - RazonaPro", base(content));
    }

    // ── Apelaciones ───────────────────────────────────────────────────────

    /** A los admins: un estudiante envió una apelación. */
    @Async
    public void sendAppealReceivedAdminEmail(String toEmail, String adminName,
                                             String studentName, String studentId, String reason, String message) {
        String link = appProperties.getFrontendUrl() + "/admin/appeals";
        String reasonTxt = "FRAUD".equals(reason) ? "plagio en un examen" : "decisión administrativa";
        String content =
            h1("Nueva apelación de estudiante") +
            p("Hola, <strong>" + capitalize(adminName) + "</strong>. El estudiante " +
              "<strong>" + studentName + "</strong> (" + studentId + "), cuya cuenta fue desactivada por " +
              reasonTxt + ", envió una apelación de reactivación.") +
            infoBox("<strong>Mensaje del estudiante:</strong><br>" + escapeHtml(message)) +
            btn(link, "Revisar apelación");
        send(toEmail, "Nueva apelación de estudiante - RazonaPro", base(content));
    }

    /** Al estudiante: resultado de su apelación (aprobada/rechazada). */
    @Async
    public void sendAppealResolvedEmail(String toEmail, String name, boolean approved, String adminResponse) {
        String link = appProperties.getFrontendUrl() + "/auth";
        String content;
        if (approved) {
            content =
                h1("Tu apelación fue aprobada") +
                p("Hola, <strong>" + capitalize(name) + "</strong>. Tu apelación fue revisada y " +
                  "<strong>aprobada</strong>. Tu cuenta ha sido reactivada y ya puedes iniciar sesión.") +
                (adminResponse != null && !adminResponse.isBlank()
                    ? infoBox("<strong>Nota del administrador:</strong><br>" + escapeHtml(adminResponse)) : "") +
                btn(link, "Iniciar sesión");
        } else {
            content =
                h1("Tu apelación fue rechazada") +
                p("Hola, <strong>" + capitalize(name) + "</strong>. Tu apelación fue revisada y " +
                  "<strong>rechazada</strong>. Tu cuenta permanece desactivada.") +
                (adminResponse != null && !adminResponse.isBlank()
                    ? infoBox("<strong>Motivo:</strong><br>" + escapeHtml(adminResponse)) : "") +
                p("Si consideras que hubo un error, puedes enviar una nueva apelación desde la pantalla de acceso.");
        }
        send(toEmail, approved ? "Apelación aprobada - RazonaPro" : "Apelación rechazada - RazonaPro", base(content));
    }

    /** A los admins: un estudiante fue anulado por plagio y su cuenta desactivada. */
    @Async
    public void sendFraudAdminEmail(String toEmail, String adminName, String studentName,
                                    String studentId, String testName) {
        String link = appProperties.getFrontendUrl() + "/admin/appeals";
        String content =
            h1("Estudiante anulado por plagio") +
            p("Hola, <strong>" + capitalize(adminName) + "</strong>. El estudiante " +
              "<strong>" + studentName + "</strong> (" + studentId + ") fue anulado por plagio en el examen " +
              "<strong>" + escapeHtml(testName) + "</strong> y su cuenta quedó desactivada automáticamente.") +
            infoBox("El estudiante puede enviar una apelación para solicitar la reactivación.") +
            btn(link, "Ver apelaciones");
        send(toEmail, "Alerta de plagio - RazonaPro", base(content));
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }


    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        String lower = s.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}