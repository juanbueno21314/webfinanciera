package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String remitente;

    @Transactional
    public void enviarEnlaceRecuperacion(String email) {
        log.info("[Reset] Solicitud de recuperación para: {}", email);

        Optional<Usuario> opt = usuarioRepo.findByEmail(email);
        if (opt.isEmpty()) {
            log.warn("[Reset] No existe usuario con email: {}", email);
            return;
        }

        Usuario usuario = opt.get();
        log.info("[Reset] Usuario encontrado: {} | proveedor: {}", usuario.getEmail(), usuario.getProveedor());

        if ("GOOGLE".equalsIgnoreCase(usuario.getProveedor())) {
            log.warn("[Reset] La cuenta {} usa Google OAuth — no se puede restablecer contraseña por email", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        usuarioRepo.save(usuario);
        log.info("[Reset] Token guardado en BD para {}", email);

        String enlace = baseUrl + "/restablecer-contrasena?token=" + token;
        log.info("[Reset] Enlace generado: {}", enlace);
        enviarCorreo(usuario, enlace);
    }

    public boolean esTokenValido(String token) {
        return usuarioRepo.findByResetToken(token)
                .filter(u -> u.getResetTokenExpiry() != null &&
                             u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public boolean restablecerContrasena(String token, String nuevaContrasena) {
        Optional<Usuario> opt = usuarioRepo.findByResetToken(token)
                .filter(u -> u.getResetTokenExpiry() != null &&
                             u.getResetTokenExpiry().isAfter(LocalDateTime.now()));
        if (opt.isEmpty()) return false;

        Usuario usuario = opt.get();
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        usuarioRepo.save(usuario);
        return true;
    }

    private void enviarCorreo(Usuario usuario, String enlace) {
        if (mailSender == null) {
            log.error("[Reset] JavaMailSender es null — Spring Mail no está configurado correctamente. Enlace: {}", enlace);
            return;
        }
        if (remitente.isBlank()) {
            log.error("[Reset] spring.mail.username está vacío. Enlace: {}", enlace);
            return;
        }
        log.info("[Reset] Intentando enviar correo desde {} a {}", remitente, usuario.getEmail());
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente, "Zenfi");
            helper.setTo(usuario.getEmail());
            helper.setSubject("Restablecer tu contraseña — Zenfi");
            helper.setText(construirHtmlCorreo(usuario.getNombre(), enlace), true);
            mailSender.send(msg);
            log.info("[Reset] ✅ Correo enviado exitosamente a {}", usuario.getEmail());
        } catch (Exception e) {
            log.error("[Reset] ❌ Error enviando correo a {}: {} — {}", usuario.getEmail(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String construirHtmlCorreo(String nombre, String enlace) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <body style="margin:0;padding:0;background:#f8fafc;font-family:'Segoe UI',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 16px;">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(15,23,42,0.10);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#0d6efd,#084298);padding:32px 40px;text-align:center;">
                        <p style="color:rgba(255,255,255,0.85);margin:0 0 8px;font-size:13px;letter-spacing:0.06em;text-transform:uppercase;">Zenfi · Finanzas personales</p>
                        <h1 style="color:#fff;margin:0;font-size:22px;font-weight:700;">Restablecer contraseña</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 40px;">
                        <p style="color:#0f172a;font-size:16px;margin:0 0 12px;">Hola, <strong>%s</strong> 👋</p>
                        <p style="color:#475569;font-size:14px;line-height:1.7;margin:0 0 28px;">
                          Recibimos una solicitud para restablecer la contraseña de tu cuenta Zenfi.
                          Haz clic en el botón para crear una nueva contraseña.
                        </p>
                        <div style="text-align:center;margin-bottom:28px;">
                          <a href="%s"
                             style="display:inline-block;background:#0d6efd;color:#fff;padding:14px 36px;border-radius:14px;text-decoration:none;font-weight:700;font-size:15px;box-shadow:0 6px 16px rgba(13,110,253,0.30);">
                            Restablecer contraseña →
                          </a>
                        </div>
                        <p style="color:#94a3b8;font-size:13px;text-align:center;margin:0;">
                          Este enlace es válido por <strong>30 minutos</strong>.<br>
                          Si no solicitaste este cambio, puedes ignorar este correo.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;padding:18px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                        <p style="color:#94a3b8;font-size:12px;margin:0;">© 2026 Zenfi · zenfi.app</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nombre != null ? nombre : "usuario", enlace);
    }
}
