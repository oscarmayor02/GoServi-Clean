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

    // ── EMAIL con HTML ────────────────────────────────────────────────────────
    private void sendEmail(NotificationRequest req) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(req.getTo());
        helper.setSubject(req.getSubject() != null ? req.getSubject() : "GoServi");

        // ✅ FIX: Map<String,String> — tipo exacto de NotificationRequest.getMetadata()
        Map<String, String> meta = req.getMetadata();
        String event = (meta != null) ? meta.getOrDefault("event", "") : "";

        String html = buildEmailHtml(event, req.getSubject(), req.getMessage(), meta);
        helper.setText(html, true);
        mailSender.send(mime);
        log.info("Email sent to {}", req.getTo());
    }

    // ── SMS ───────────────────────────────────────────────────────────────────
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
    // HTML BUILDER
    // ✅ Ambos métodos usan Map<String, String>
    // ══════════════════════════════════════════════════════════════════════════
    private String buildEmailHtml(String event, String subject, String body,
                                  Map<String, String> meta) {
        String icon    = getEventIcon(event);
        String color   = getEventColor(event);
        String content = buildContent(event, body, meta);

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "*{margin:0;padding:0;box-sizing:border-box}" +
                "body{background:#F0F4FF;font-family:'Segoe UI',Arial,sans-serif;color:#1a2640}" +
                ".wrap{max-width:560px;margin:32px auto;background:#fff;border-radius:20px;" +
                "      box-shadow:0 8px 40px rgba(79,107,237,.12);overflow:hidden}" +
                ".header{background:linear-gradient(135deg,#4F6BED,#7B5FDC);" +
                "        padding:36px 32px 32px;text-align:center}" +
                ".logo{font-size:26px;font-weight:900;color:#fff;letter-spacing:-0.5px}" +
                ".logo span{color:#A5EFDF}" +
                ".icon-wrap{width:64px;height:64px;border-radius:20px;" +
                "           background:rgba(255,255,255,.18);display:inline-flex;" +
                "           align-items:center;justify-content:center;font-size:30px;margin:20px 0 12px}" +
                ".header h1{color:#fff;font-size:20px;font-weight:700;line-height:1.3}" +
                ".body{padding:32px}" +
                ".card{background:#F8FAFF;border:1.5px solid #E8EDFF;border-radius:16px;" +
                "      padding:20px 22px;margin:20px 0}" +
                ".card p{color:#475569;font-size:14.5px;line-height:1.7}" +
                ".highlight{background:linear-gradient(135deg," + color + ");border-radius:14px;" +
                "           padding:18px 22px;margin:20px 0;text-align:center}" +
                ".highlight .label{font-size:11px;font-weight:700;text-transform:uppercase;" +
                "                  letter-spacing:1.5px;color:rgba(255,255,255,.75);margin-bottom:6px}" +
                ".highlight .code{font-size:36px;font-weight:900;color:#fff;letter-spacing:10px;" +
                "                 font-family:'Courier New',monospace}" +
                ".highlight .hint{font-size:12px;color:rgba(255,255,255,.7);margin-top:6px}" +
                ".footer{background:#F8FAFF;border-top:1px solid #EEF2FF;" +
                "        padding:24px 32px;text-align:center}" +
                ".footer p{font-size:12px;color:#94a3b8;line-height:1.7}" +
                ".footer a{color:#4F6BED;text-decoration:none;font-weight:600}" +
                ".btn{display:inline-block;background:linear-gradient(135deg,#4F6BED,#7B5FDC);" +
                "     color:#fff;font-weight:700;font-size:15px;padding:14px 32px;" +
                "     border-radius:14px;text-decoration:none;margin:8px 0}" +
                "h2{font-size:16px;font-weight:700;color:#1a2640;margin-bottom:8px}" +
                "</style></head>" +
                "<body><div class='wrap'>" +
                "<div class='header'>" +
                "  <div class='logo'>Go<span>Servi</span></div>" +
                "  <div class='icon-wrap'>" + icon + "</div>" +
                "  <h1>" + escHtml(subject) + "</h1>" +
                "</div>" +
                "<div class='body'>" + content + "</div>" +
                "<div class='footer'>" +
                "  <p>Mensaje automático de <strong>GoServi</strong>. No respondas este correo.</p>" +
                "  <p style='margin-top:10px'>¿Problemas? " +
                "  <a href='mailto:soporte@goservi.com'>soporte@goservi.com</a></p>" +
                "  <p style='margin-top:14px;font-size:11px;color:#cbd5e1'>© 2025 GoServi</p>" +
                "</div></div></body></html>";
    }

    private String buildContent(String event, String body, Map<String, String> meta) {
        return switch (event) {

            case "REGISTER" ->
                    "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                            "Estamos muy contentos de tenerte aquí. Tu cuenta ha sido creada exitosamente " +
                            "y ya puedes explorar los mejores servicios de tu ciudad.</p>" +
                            "<div class='card'><h2>¿Cómo empezar?</h2>" +
                            "<p>1. Busca profesionales cerca de ti<br>" +
                            "2. Reserva el servicio que necesitas<br>" +
                            "3. Realiza el seguimiento en tiempo real</p></div>" +
                            "<div style='text-align:center;margin-top:24px'>" +
                            "<a class='btn' href='https://goservi.com'>Explorar GoServi →</a></div>";

            case "BOOKING_REQUEST" ->
                    "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                            "Tienes una nueva solicitud esperando tu respuesta. " +
                            "Revísala cuanto antes para no perder al cliente.</p>" +
                            "<div class='card'><p>" + escHtml(body) + "</p></div>" +
                            "<p style='font-size:13px;color:#94a3b8;margin-top:16px'>" +
                            "Responder rápido mejora tu reputación en la plataforma.</p>";

            case "BOOKING_CONFIRMED" -> {
                String code = extractCode(body);
                yield "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                        "¡Tu reserva ha sido confirmada! Guarda este código: " +
                        "compártelo con el profesional cuando llegue.</p>" +
                        (code.isEmpty()
                                ? "<div class='card'><p>" + escHtml(body) + "</p></div>"
                                : "<div class='highlight'>" +
                                "<div class='label'>Código de inicio</div>" +
                                "<div class='code'>" + code + "</div>" +
                                "<div class='hint'>Compártelo SOLO con el profesional al llegar</div>" +
                                "</div>") +
                        "<div class='card'><h2>⚠️ Importante</h2>" +
                        "<p>Nunca compartas este código antes de que el profesional llegue. " +
                        "Es tu garantía de que el servicio comenzó correctamente.</p></div>";
            }

            case "PROFESSIONAL_ARRIVED" ->
                    "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                            "Tu profesional ya llegó a tu ubicación. Es momento de compartir tu código.</p>" +
                            "<div class='card'><p>" + escHtml(body) + "</p></div>" +
                            "<div class='card'><h2>📍 ¿Qué hago ahora?</h2>" +
                            "<p>Abre la app, ve al chat de tu reserva y comparte el código con el " +
                            "profesional para que el servicio comience oficialmente.</p></div>";

            case "SERVICE_COMPLETED" ->
                    "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                            "El profesional marcó tu servicio como completado. " +
                            "Recuerda realizar el pago para cerrar la reserva.</p>" +
                            "<div class='card'><h2>💳 Siguiente paso: Pago</h2>" +
                            "<p>Ingresa a la app para completar el pago de forma segura. " +
                            "Luego podrás dejar tu valoración.</p></div>";

            case "BOOKING_PAID" ->
                    "<p style='color:#475569;font-size:15px;line-height:1.8;margin-bottom:20px'>" +
                            "Hemos confirmado tu pago. ¡Gracias por usar GoServi!</p>" +
                            "<div class='highlight'>" +
                            "<div class='label'>Estado del pago</div>" +
                            "<div style='font-size:22px;font-weight:900;color:#fff;margin:8px 0'>✓ Confirmado</div>" +
                            "<div class='hint'>El profesional ya fue notificado</div></div>" +
                            "<p style='font-size:13px;color:#94a3b8;margin-top:16px;text-align:center'>" +
                            "¿Te gustó el servicio? Deja tu valoración en la app.</p>";

            default ->
                    "<div class='card'><p style='font-size:15px;line-height:1.8'>" +
                            escHtml(body) + "</p></div>";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractCode(String body) {
        if (body == null) return "";
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private String getEventIcon(String event) {
        return switch (event) {
            case "REGISTER"             -> "🎉";
            case "BOOKING_REQUEST"      -> "📋";
            case "BOOKING_CONFIRMED"    -> "🔐";
            case "PROFESSIONAL_ARRIVED" -> "📍";
            case "SERVICE_COMPLETED"    -> "✅";
            case "BOOKING_PAID"         -> "💳";
            default                     -> "🔔";
        };
    }

    private String getEventColor(String event) {
        return switch (event) {
            case "REGISTER"             -> "#4F6BED,#7B5FDC";
            case "BOOKING_REQUEST"      -> "#F59E0B,#D97706";
            case "BOOKING_CONFIRMED"    -> "#4F6BED,#3730A3";
            case "PROFESSIONAL_ARRIVED" -> "#8B5CF6,#6D28D9";
            case "SERVICE_COMPLETED"    -> "#10B981,#059669";
            case "BOOKING_PAID"         -> "#10B981,#0D9488";
            default                     -> "#4F6BED,#7B5FDC";
        };
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}