package com.familyfinance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String username, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("✅ Confirma contul tău — Family Budget");
            helper.setText("""
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="background: #0d6efd; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                            <h2 style="margin:0;">💰 Family Budget App</h2>
                            <p style="margin:5px 0 0 0;">Confirmare cont nou</p>
                        </div>
                        <div style="background: #f8f9fa; padding: 30px; border: 1px solid #dee2e6;">
                            <p>Bună, <strong>%s</strong>!</p>
                            <p>Ai creat un cont nou pe Family Budget App. Apasă butonul de mai jos pentru a confirma adresa de email:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s"
                                   style="background: #0d6efd; color: white; padding: 14px 28px;
                                          text-decoration: none; border-radius: 6px; font-size: 16px;">
                                    ✅ Confirmă contul
                                </a>
                            </div>
                            <p style="color: #6c757d; font-size: 13px;">
                                Link-ul este valabil 24 de ore. Dacă nu ai creat tu acest cont, ignoră acest email.
                            </p>
                        </div>
                        <div style="background: #6c757d; color: white; padding: 10px 20px;
                                    border-radius: 0 0 8px 8px; font-size: 12px;">
                            Family Budget App — email automat
                        </div>
                    </body>
                    </html>
                    """.formatted(username, verificationLink), true);

            mailSender.send(message);
            log.info("Email confirmare trimis catre: {}", toEmail);

        } catch (Exception ex) {
            log.error("Eroare trimitere email confirmare: {}", ex.getMessage(), ex);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Resetare parolă — Family Budget");
            helper.setText("""
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="background: #dc3545; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                            <h2 style="margin:0;">🔐 Resetare Parolă</h2>
                            <p style="margin:5px 0 0 0;">Family Budget App</p>
                        </div>
                        <div style="background: #f8f9fa; padding: 30px; border: 1px solid #dee2e6;">
                            <p>Bună, <strong>%s</strong>!</p>
                            <p>Ai solicitat resetarea parolei. Apasă butonul de mai jos:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s"
                                   style="background: #dc3545; color: white; padding: 14px 28px;
                                          text-decoration: none; border-radius: 6px; font-size: 16px;">
                                    🔐 Resetează parola
                                </a>
                            </div>
                            <p style="color: #6c757d; font-size: 13px;">
                                Link-ul este valabil <strong>24 de ore</strong>.
                                Dacă nu ai solicitat tu resetarea, ignoră acest email — contul tău este în siguranță.
                            </p>
                        </div>
                        <div style="background: #6c757d; color: white; padding: 10px 20px;
                                    border-radius: 0 0 8px 8px; font-size: 12px;">
                            Family Budget App — email automat
                        </div>
                    </body>
                    </html>
                    """.formatted(username, resetLink), true);

            mailSender.send(message);
            log.info("Email resetare parola trimis catre: {}", toEmail);

        } catch (Exception ex) {
            log.error("Eroare trimitere email resetare parola: {}", ex.getMessage(), ex);
        }
    }

    public void sendAlertEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email alert trimis catre: {}", toEmail);
        } catch (Exception ex) {
            log.error("Eroare trimitere email alert: {}", ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }
}