package com.goservi.notification.service.impl;

import com.goservi.common.dto.NotificationRequest;
import com.goservi.common.dto.NotificationType;
import com.goservi.notification.entity.NotificationLog;
import com.goservi.notification.repository.NotificationLogRepository;
import com.goservi.notification.service.NotificationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepo;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${twilio.account-sid:}")
    private String twilioSid;

    @Value("${twilio.auth-token:}")
    private String twilioToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhone;

    public NotificationServiceImpl(JavaMailSender mailSender, NotificationLogRepository logRepo) {
        this.mailSender = mailSender;
        this.logRepo = logRepo;
    }

    @Override
    @Async
    public void send(NotificationRequest req) {
        boolean success = false;
        String error = null;

        try {
            if (req.getType() == NotificationType.EMAIL) {
                sendEmail(req);
                success = true;
            } else if (req.getType() == NotificationType.SMS) {
                sendSms(req);
                success = true;
            } else {
                log.info("PUSH notification to {}: {}", req.getTo(), req.getMessage());
                success = true;
            }
        } catch (Exception e) {
            error = e.getMessage();
            log.error("Notification error [{}] to {}: {}", req.getType(), req.getTo(), e.getMessage());
        }

        try {
            logRepo.save(NotificationLog.builder()
                    .recipient(req.getTo())
                    .type(req.getType())
                    .subject(req.getSubject())
                    .message(req.getMessage())
                    .success(success)
                    .errorMessage(error)
                    .build());
        } catch (Exception e) {
            log.warn("Could not save notification log: {}", e.getMessage());
        }
    }

    private void sendEmail(NotificationRequest req) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(req.getTo());
        helper.setSubject(req.getSubject() != null ? req.getSubject() : "GoServi");

        Map<String, String> meta = req.getMetadata();
        String event = (meta != null) ? meta.getOrDefault("event", "") : "";

        String html = buildEmailHtml(event, req.getSubject(), req.getMessage(), meta);
        helper.setText(html, true);
        mailSender.send(mime);
        log.info("Email sent to {}", req.getTo());
    }

    private void sendSms(NotificationRequest req) {
        if (twilioSid == null || twilioSid.isBlank()) {
            log.warn("Twilio not configured, skipping SMS to {}", req.getTo());
            return;
        }
        Twilio.init(twilioSid, twilioToken);
        Message.creator(
                new PhoneNumber(req.getTo()),
                new PhoneNumber(twilioPhone),
                req.getMessage()
        ).create();
        log.info("SMS sent to {}", req.getTo());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GoServi Email Template — Blue/White Professional Design
    // ══════════════════════════════════════════════════════════════════════════

    private String buildEmailHtml(String event, String subject, String body,
                                  Map<String, String> meta) {
        String icon    = getEventIcon(event);
        String accent  = getEventAccent(event);
        String content = buildContent(event, body, meta);

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "*{margin:0;padding:0;box-sizing:border-box}" +
                "body{background:#F0F4FA;font-family:-apple-system,'Segoe UI','Helvetica Neue',Arial,sans-serif;color:#1a2b4a;-webkit-font-smoothing:antialiased}" +
                // Wrapper
                ".gs-wrap{max-width:540px;margin:28px auto;background:#FFFFFF;border-radius:24px;" +
                "box-shadow:0 4px 24px rgba(0,80,200,0.08);overflow:hidden;border:1px solid #E8F0FE}" +
                // Header
                ".gs-header{background:linear-gradient(135deg,#0066DD 0%,#0077FF 40%,#00A3FF 100%);" +
                "padding:32px 28px 28px;text-align:center;position:relative;overflow:hidden}" +
                ".gs-header::before{content:'';position:absolute;top:-40px;right:-30px;width:140px;height:140px;" +
                "background:rgba(255,255,255,0.06);border-radius:50%}" +
                ".gs-header::after{content:'';position:absolute;bottom:-20px;left:-20px;width:100px;height:100px;" +
                "background:rgba(255,255,255,0.04);border-radius:50%}" +
                ".gs-logo{font-size:22px;font-weight:800;color:#FFFFFF;letter-spacing:-0.5px;margin-bottom:18px;position:relative;z-index:1}" +
                ".gs-logo span{color:#A5E8FF}" +
                ".gs-icon{width:56px;height:56px;border-radius:16px;background:rgba(255,255,255,0.15);" +
                "display:inline-flex;align-items:center;justify-content:center;font-size:26px;margin-bottom:14px;" +
                "backdrop-filter:blur(8px);position:relative;z-index:1}" +
                ".gs-header h1{color:#FFFFFF;font-size:18px;font-weight:700;line-height:1.35;position:relative;z-index:1;" +
                "max-width:380px;margin:0 auto}" +
                // Body
                ".gs-body{padding:28px 24px}" +
                ".gs-text{color:#4A5B75;font-size:14.5px;line-height:1.75;margin-bottom:18px}" +
                // Card
                ".gs-card{background:#F8FAFF;border:1.5px solid #E3EDFF;border-radius:16px;" +
                "padding:18px 20px;margin:16px 0}" +
                ".gs-card h3{font-size:14px;font-weight:700;color:#1a2b4a;margin-bottom:8px;display:flex;align-items:center;gap:8px}" +
                ".gs-card p{color:#4A5B75;font-size:13.5px;line-height:1.7}" +
                // Highlight (for codes, amounts, statuses)
                ".gs-highlight{background:linear-gradient(135deg," + accent + ");border-radius:16px;" +
                "padding:20px;margin:18px 0;text-align:center;position:relative;overflow:hidden}" +
                ".gs-highlight::before{content:'';position:absolute;top:-15px;right:-15px;width:60px;height:60px;" +
                "background:rgba(255,255,255,0.08);border-radius:50%}" +
                ".gs-hl-label{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:2px;" +
                "color:rgba(255,255,255,0.7);margin-bottom:8px}" +
                ".gs-hl-value{font-size:32px;font-weight:900;color:#FFFFFF;letter-spacing:8px;" +
                "font-family:'Courier New',monospace;position:relative;z-index:1}" +
                ".gs-hl-hint{font-size:11.5px;color:rgba(255,255,255,0.65);margin-top:8px}" +
                // Status badge
                ".gs-status{display:inline-flex;align-items:center;gap:6px;background:rgba(255,255,255,0.18);" +
                "border-radius:10px;padding:8px 16px;font-size:14px;font-weight:700;color:#FFFFFF}" +
                // Button
                ".gs-btn{display:inline-block;background:linear-gradient(135deg,#0066DD,#0088FF);" +
                "color:#FFFFFF;font-weight:700;font-size:14px;padding:13px 28px;border-radius:14px;" +
                "text-decoration:none;box-shadow:0 4px 16px rgba(0,102,221,0.25)}" +
                // Steps
                ".gs-steps{counter-reset:step;margin:0;padding:0;list-style:none}" +
                ".gs-steps li{counter-increment:step;display:flex;align-items:flex-start;gap:12px;padding:8px 0;" +
                "font-size:13.5px;color:#4A5B75;line-height:1.6}" +
                ".gs-steps li::before{content:counter(step);flex-shrink:0;width:28px;height:28px;border-radius:10px;" +
                "background:#E8F1FF;color:#0077FF;font-weight:700;font-size:12px;display:flex;" +
                "align-items:center;justify-content:center}" +
                // Divider
                ".gs-divider{height:1px;background:linear-gradient(90deg,transparent,#E3EDFF,transparent);margin:20px 0}" +
                // Warning
                ".gs-warn{background:#FFF8ED;border:1.5px solid #FFE4B5;border-radius:14px;padding:14px 16px;" +
                "margin:16px 0;display:flex;align-items:flex-start;gap:10px;font-size:13px;color:#92400E;line-height:1.6}" +
                // Success
                ".gs-success{background:#ECFDF5;border:1.5px solid #A7F3D0;border-radius:14px;padding:14px 16px;" +
                "margin:16px 0;display:flex;align-items:flex-start;gap:10px;font-size:13px;color:#065F46;line-height:1.6}" +
                // Cash payment
                ".gs-cash{background:#FFFBEB;border:1.5px solid #FDE68A;border-radius:16px;padding:18px 20px;margin:16px 0}" +
                ".gs-cash-title{font-size:14px;font-weight:700;color:#92400E;margin-bottom:10px;display:flex;align-items:center;gap:8px}" +
                ".gs-cash-amount{background:#FEF3C7;border-radius:12px;padding:12px 16px;text-align:center;margin:12px 0}" +
                ".gs-cash-amount .label{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;color:#B45309}" +
                ".gs-cash-amount .value{font-size:24px;font-weight:900;color:#D97706;margin-top:4px}" +
                ".gs-cash-accounts{background:#FFFFFF;border:1px solid #FDE68A;border-radius:12px;padding:12px 16px;margin-top:10px}" +
                ".gs-cash-accounts p{font-size:13px;color:#78350F;line-height:1.8;margin:0}" +
                // Footer
                ".gs-footer{background:#F8FAFF;border-top:1px solid #E8F0FE;padding:22px 24px;text-align:center}" +
                ".gs-footer p{font-size:11.5px;color:#94A3B8;line-height:1.7}" +
                ".gs-footer a{color:#0077FF;text-decoration:none;font-weight:600}" +
                ".gs-footer-brand{font-size:10px;color:#CBD5E1;margin-top:12px;letter-spacing:0.5px}" +
                // Shield
                ".gs-shield{display:flex;align-items:center;gap:8px;background:#F0F7FF;border:1.5px solid #D6E4FF;" +
                "border-radius:12px;padding:10px 14px;margin:16px 0;font-size:12px;font-weight:600;color:#0066DD}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='gs-wrap'>" +
                // Header
                "<div class='gs-header'>" +
                "<div class='gs-logo'>Go<span>Servi</span></div>" +
                "<div class='gs-icon'>" + icon + "</div>" +
                "<h1>" + esc(subject) + "</h1>" +
                "</div>" +
                // Body
                "<div class='gs-body'>" + content + "</div>" +
                // Footer
                "<div class='gs-footer'>" +
                "<div class='gs-shield'>&#x1F6E1; Tu información está protegida con GoServi</div>" +
                "<p>Este es un mensaje automático. No respondas a este correo.</p>" +
                "<p style='margin-top:8px'>¿Necesitas ayuda? <a href='mailto:soporte@goservi.com'>soporte@goservi.com</a></p>" +
                "<p class='gs-footer-brand'>© 2025 GoServi — Servicios profesionales a domicilio</p>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Content per event type
    // ══════════════════════════════════════════════════════════════════════════

    private String buildContent(String event, String body, Map<String, String> meta) {
        return switch (event) {

            // ── REGISTRO ──────────────────────────────────────────────────
            case "REGISTER" ->
                    "<p class='gs-text'>¡Bienvenido a GoServi! Tu cuenta ha sido creada " +
                            "exitosamente. Ya puedes encontrar los mejores profesionales cerca de ti.</p>" +
                            "<div class='gs-card'>" +
                            "<h3>&#x1F680; ¿Cómo empezar?</h3>" +
                            "<ol class='gs-steps'>" +
                            "<li>Busca profesionales de confianza en tu zona</li>" +
                            "<li>Reserva el servicio que necesitas con un toque</li>" +
                            "<li>Sigue el estado en tiempo real desde la app</li>" +
                            "</ol></div>" +
                            "<div style='text-align:center;margin-top:20px'>" +
                            "<a class='gs-btn' href='https://goservi.com'>Explorar GoServi &#x2192;</a></div>";

            // ── SOLICITUD DE RESERVA (al profesional) ─────────────────────
            case "BOOKING_REQUEST" ->
                    "<p class='gs-text'>Tienes una nueva solicitud de servicio. " +
                            "Revísala y responde lo antes posible para mantener tu tasa de respuesta alta.</p>" +
                            "<div class='gs-card'><p>" + esc(body) + "</p></div>" +
                            "<div class='gs-warn'>&#x23F1; Responder rápido mejora tu posicionamiento " +
                            "y genera más confianza en los clientes.</div>";

            // ── RESERVA CONFIRMADA ────────────────────────────────────────
            case "BOOKING_CONFIRMED" -> {
                String code = extractCode(body);
                yield "<p class='gs-text'>Tu reserva ha sido confirmada. Guarda este código de " +
                        "seguridad — lo necesitarás cuando el profesional llegue.</p>" +
                        (code.isEmpty()
                                ? "<div class='gs-card'><p>" + esc(body) + "</p></div>"
                                : "<div class='gs-highlight'>" +
                                "<div class='gs-hl-label'>Código de verificación</div>" +
                                "<div class='gs-hl-value'>" + code + "</div>" +
                                "<div class='gs-hl-hint'>Compártelo SOLO cuando el profesional llegue</div>" +
                                "</div>") +
                        "<div class='gs-warn'>&#x1F512; <div><strong>Importante:</strong> Nunca compartas " +
                        "este código antes de que el profesional llegue. Es tu garantía de que " +
                        "el servicio comenzó correctamente.</div></div>";
            }

            // ── PROFESIONAL LLEGÓ ─────────────────────────────────────────
            case "PROFESSIONAL_ARRIVED" ->
                    "<p class='gs-text'>Tu profesional ya está en tu ubicación. " +
                            "Es momento de compartir tu código de verificación.</p>" +
                            "<div class='gs-card'><p>" + esc(body) + "</p></div>" +
                            "<div class='gs-card'><h3>&#x1F4CD; ¿Qué hacer ahora?</h3>" +
                            "<ol class='gs-steps'>" +
                            "<li>Abre la app y ve a tu reserva activa</li>" +
                            "<li>Comparte el código con el profesional</li>" +
                            "<li>El servicio comenzará oficialmente</li>" +
                            "</ol></div>";

            // ── SERVICIO COMPLETADO ────────────────────────────────────────
            case "SERVICE_COMPLETED" ->
                    "<p class='gs-text'>El profesional marcó tu servicio como completado. " +
                            "Solo falta el pago para cerrar la reserva.</p>" +
                            "<div class='gs-card'><h3>&#x1F4B3; Siguiente paso</h3>" +
                            "<p>Ingresa a la app para completar el pago de forma segura. " +
                            "Puedes pagar con Wompi o en efectivo.</p></div>" +
                            "<div style='text-align:center;margin-top:16px'>" +
                            "<a class='gs-btn' href='https://goservi.com'>Ir a pagar &#x2192;</a></div>";

            // ── PAGO CONFIRMADO ───────────────────────────────────────────
            case "BOOKING_PAID" ->
                    "<p class='gs-text'>Tu pago ha sido confirmado exitosamente. " +
                            "¡Gracias por confiar en GoServi!</p>" +
                            "<div class='gs-highlight'>" +
                            "<div class='gs-hl-label'>Estado del pago</div>" +
                            "<div class='gs-status'>&#x2714; Pago confirmado</div>" +
                            "<div class='gs-hl-hint'>El profesional ha sido notificado</div>" +
                            "</div>" +
                            "<div class='gs-success'>&#x2B50; <div>¿Te gustó el servicio? " +
                            "Deja tu valoración en la app — ayuda a otros clientes a elegir mejor.</div></div>";

            // ── PAGO EN EFECTIVO — AL PROFESIONAL ─────────────────────────
            case "CASH_PAYMENT_PENDING" -> {
                String fee = (meta != null) ? meta.getOrDefault("platformFee", "") : "";
                yield "<p class='gs-text'>Un cliente te pagó en efectivo. Para seguir recibiendo " +
                        "servicios, debes transferir la comisión de GoServi.</p>" +
                        "<div class='gs-cash'>" +
                        "<div class='gs-cash-title'>&#x1F4B5; Comisión pendiente</div>" +
                        (fee.isEmpty() ? ""
                                : "<div class='gs-cash-amount'>" +
                                "<div class='label'>Monto a transferir</div>" +
                                "<div class='value'>$" + fee + "</div></div>") +
                        "<div class='gs-cash-accounts'>" +
                        "<p><strong>Transfiere a:</strong></p>" +
                        "<p>&#x1F4F1; Nequi: 300XXXXXXX</p>" +
                        "<p>&#x1F3E6; Bancolombia: 000-000000-00</p>" +
                        "</div></div>" +
                        "<div class='gs-warn'>&#x26A0; <div>No podrás aceptar nuevos servicios hasta que " +
                        "GoServi verifique tu transferencia. Este proceso puede tomar hasta 24 horas.</div></div>";
            }

            // ── PAGO EN EFECTIVO — AL ADMIN ───────────────────────────────
            case "ADMIN_CASH_PENDING" ->
                    "<p class='gs-text'>Se registró un nuevo pago en efectivo que requiere " +
                            "verificación de la comisión.</p>" +
                            "<div class='gs-card'><p>" + esc(body) + "</p></div>" +
                            "<div style='text-align:center;margin-top:16px'>" +
                            "<a class='gs-btn' href='https://admin.goservi.com'>Ir al panel admin &#x2192;</a></div>";

            // ── COMISIÓN APROBADA ─────────────────────────────────────────
            case "CASH_APPROVED" ->
                    "<p class='gs-text'>Tu comisión fue verificada exitosamente. " +
                            "Ya puedes aceptar nuevos servicios.</p>" +
                            "<div class='gs-highlight'>" +
                            "<div class='gs-hl-label'>Estado</div>" +
                            "<div class='gs-status'>&#x2714; Comisión verificada</div>" +
                            "<div class='gs-hl-hint'>Ya puedes aceptar nuevos servicios</div>" +
                            "</div>" +
                            "<div class='gs-success'>&#x1F680; <div>¡Sigue adelante! Tu perfil está activo " +
                            "y los clientes pueden reservar contigo nuevamente.</div></div>";

            // ── COMISIÓN RECHAZADA ────────────────────────────────────────
            case "CASH_REJECTED" ->
                    "<p class='gs-text'>No pudimos verificar tu transferencia de comisión.</p>" +
                            "<div class='gs-card'><p>" + esc(body) + "</p></div>" +
                            "<div class='gs-warn'>&#x26A0; <div>Si crees que es un error, contacta a soporte " +
                            "con el comprobante de tu transferencia.</div></div>" +
                            "<div style='text-align:center;margin-top:16px'>" +
                            "<a class='gs-btn' href='mailto:soporte@goservi.com'>Contactar soporte</a></div>";

            // ── DEFAULT ───────────────────────────────────────────────────
            default ->
                    "<div class='gs-card'><p style='font-size:14.5px;line-height:1.75'>" +
                            esc(body) + "</p></div>";
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private String extractCode(String body) {
        if (body == null) return "";
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private String getEventIcon(String event) {
        return switch (event) {
            case "REGISTER"              -> "&#x1F44B;";  // 👋
            case "BOOKING_REQUEST"       -> "&#x1F4CB;";  // 📋
            case "BOOKING_CONFIRMED"     -> "&#x1F512;";  // 🔒
            case "PROFESSIONAL_ARRIVED"  -> "&#x1F4CD;";  // 📍
            case "SERVICE_COMPLETED"     -> "&#x2705;";   // ✅
            case "BOOKING_PAID"          -> "&#x1F4B3;";  // 💳
            case "CASH_PAYMENT_PENDING"  -> "&#x1F4B5;";  // 💵
            case "ADMIN_CASH_PENDING"    -> "&#x1F514;";  // 🔔
            case "CASH_APPROVED"         -> "&#x2714;";   // ✔
            case "CASH_REJECTED"         -> "&#x26A0;";   // ⚠
            default                      -> "&#x1F514;";  // 🔔
        };
    }

    /** Accent gradient for the highlight block */
    private String getEventAccent(String event) {
        return switch (event) {
            case "REGISTER"              -> "#0066DD,#00A3FF";
            case "BOOKING_REQUEST"       -> "#0066DD,#0088FF";
            case "BOOKING_CONFIRMED"     -> "#0055BB,#0077FF";
            case "PROFESSIONAL_ARRIVED"  -> "#0066DD,#00B0FF";
            case "SERVICE_COMPLETED"     -> "#059669,#10B981";
            case "BOOKING_PAID"          -> "#059669,#0D9488";
            case "CASH_PAYMENT_PENDING"  -> "#D97706,#F59E0B";
            case "CASH_APPROVED"         -> "#059669,#10B981";
            case "CASH_REJECTED"         -> "#DC2626,#EF4444";
            default                      -> "#0066DD,#0088FF";
        };
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }
}