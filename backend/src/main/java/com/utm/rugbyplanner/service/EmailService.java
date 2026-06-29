package com.utm.rugbyplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utm.rugbyplanner.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EmailService — UC010: Send email notification to athlete
 *
 * Sends an HTML email via Resend's HTTPS API to the athlete's email whenever
 * a trainer approves or rejects their appointment.
 *
 * NOTE: this used to go through Gmail SMTP via JavaMailSender, but Render's
 * free tier blocks outbound traffic on SMTP ports (25/465/587) to fight spam,
 * which caused every send to time out in production. Resend uses a plain
 * HTTPS API call instead, so it isn't affected by that block.
 *
 * Emails are sent asynchronously so they never block the HTTP response.
 * If sending fails, the error is logged but the appointment action still succeeds.
 */
@Slf4j
@Service
public class EmailService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.api.url}")
    private String resendApiUrl;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${resend.reply-to}")
    private String replyToEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── Public API ────────────────────────────────────────────────────────────

    @Async
    public void sendAppointmentApprovedEmail(String toEmail, String athleteName,
                                              String trainerName, Appointment appt) {
        String subject = "✅ Your Appointment has been Approved — Rugby Performance Enhancement System";
        String body    = buildApprovedBody(athleteName, trainerName, appt);
        send(toEmail, subject, body);
    }

    @Async
    public void sendAppointmentRejectedEmail(String toEmail, String athleteName,
                                              String trainerName, Appointment appt) {
        String subject = "❌ Your Appointment has been Rejected — Rugby Performance Enhancement System";
        String body    = buildRejectedBody(athleteName, trainerName, appt);
        send(toEmail, subject, body);
    }

    // ── Internal send ─────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("UC010 Skipped sending email to {} — RESEND_API_KEY is not set", to);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromEmail);
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("html", htmlBody);
            if (replyToEmail != null && !replyToEmail.isBlank()) {
                payload.put("reply_to", List.of(replyToEmail));
            }

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resendApiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("UC010 Email sent to {} — subject: {}", to, subject);
            } else {
                log.error("UC010 Failed to send email to {} — Resend returned {}: {}",
                        to, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("UC010 Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Email templates ───────────────────────────────────────────────────────

    private String buildApprovedBody(String athleteName, String trainerName, Appointment appt) {
        return baseTemplate(
            "Appointment Approved",
            "#16a34a",
            "✅",
            athleteName,
            "<p style='color:#166534;font-size:16px;margin:0 0 20px;'>" +
            "Great news! Your appointment request has been <strong>approved</strong> by your trainer.</p>",
            appt,
            trainerName,
            appt.getTrainerRemarks() != null && !appt.getTrainerRemarks().isBlank()
                ? buildRemarksBlock("Trainer's Message", "#166534", "#dcfce7", appt.getTrainerRemarks())
                : "",
            "#16a34a"
        );
    }

    private String buildRejectedBody(String athleteName, String trainerName, Appointment appt) {
        return baseTemplate(
            "Appointment Rejected",
            "#dc2626",
            "❌",
            athleteName,
            "<p style='color:#991b1b;font-size:16px;margin:0 0 20px;'>" +
            "Unfortunately, your appointment request has been <strong>rejected</strong> by your trainer. " +
            "Please book a new appointment at an available time slot.</p>",
            appt,
            trainerName,
            appt.getTrainerRemarks() != null && !appt.getTrainerRemarks().isBlank()
                ? buildRemarksBlock("Reason for Rejection", "#991b1b", "#fee2e2", appt.getTrainerRemarks())
                : "",
            "#dc2626"
        );
    }

    private String baseTemplate(String statusTitle, String accentColor, String statusIcon,
                                  String athleteName, String statusMessage,
                                  Appointment appt, String trainerName,
                                  String remarksBlock, String borderColor) {
        String formattedDate = formatDate(appt.getDate());
        String serviceLabel  = serviceLabel(appt.getServiceType());
        String locationLabel = "FACE_TO_FACE".equals(appt.getLocation()) ? "🤝 Face to Face" : "💻 Online";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:32px 16px;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr>
                      <td style="background:%s;padding:28px 32px;text-align:center;">
                        <p style="font-size:36px;margin:0 0 8px;">%s</p>
                        <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0;letter-spacing:0.5px;">%s</h1>
                        <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Rugby Performance Enhancement System — UTM Pirates</p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:32px;">

                        <p style="color:#374151;font-size:16px;margin:0 0 8px;">Dear <strong>%s</strong>,</p>
                        %s

                        <!-- Appointment Details -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:#f9fafb;border:1px solid #e5e7eb;border-left:4px solid %s;border-radius:8px;margin-bottom:20px;">
                          <tr><td style="padding:20px;">
                            <p style="font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#6b7280;margin:0 0 14px;">Appointment Details</p>
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                          </td></tr>
                        </table>

                        %s

                        <!-- CTA -->
                        <p style="text-align:center;margin:24px 0 0;">
                          <a href="%s/appointments"
                             style="display:inline-block;padding:12px 28px;background:%s;color:#ffffff;
                                    text-decoration:none;border-radius:8px;font-size:14px;font-weight:600;">
                            View My Appointments
                          </a>
                        </p>

                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f9fafb;border-top:1px solid #e5e7eb;padding:20px 32px;text-align:center;">
                        <p style="color:#9ca3af;font-size:12px;margin:0;">
                          This email was sent by Rugby Performance Enhancement System — UTM Pirates.<br>
                          Please do not reply to this email.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                accentColor, statusIcon, statusTitle,
                athleteName, statusMessage,
                borderColor,
                detailRow("👨‍💼 Trainer",       trainerName),
                detailRow("🎯 Service",        serviceLabel),
                detailRow("📅 Date",           formattedDate),
                detailRow("🕐 Time",           appt.getTime()),
                detailRow("⏱ Duration",        appt.getDuration() + " minutes"),
                detailRow("📍 Location",       locationLabel),
                remarksBlock,
                frontendUrl,
                accentColor
            );
    }

    private String detailRow(String label, String value) {
        return "<table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:8px;\">" +
               "<tr><td width=\"140\" style=\"color:#6b7280;font-size:13px;\">" + label + "</td>" +
               "<td style=\"color:#111827;font-size:13px;font-weight:600;\">" + value + "</td></tr></table>";
    }

    private String buildRemarksBlock(String title, String textColor, String bgColor, String remarks) {
        return "<table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" " +
               "style=\"background:" + bgColor + ";border-radius:8px;margin-bottom:20px;\">" +
               "<tr><td style=\"padding:16px 20px;\">" +
               "<p style=\"font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:1px;" +
               "color:" + textColor + ";margin:0 0 6px;\">" + title + "</p>" +
               "<p style=\"color:#374151;font-size:14px;margin:0;\">" + remarks + "</p>" +
               "</td></tr></table>";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatDate(String dateStr) {
        if (dateStr == null) return "–";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String serviceLabel(String s) {
        if (s == null) return "–";
        return switch (s) {
            case "FITNESS_TRAINING"      -> "Fitness Training";
            case "NUTRITION_COUNSELLING" -> "Nutrition Counselling";
            case "WELLNESS_COACHING"     -> "Wellness Coaching";
            default                      -> s;
        };
    }
}
