package com.healthplus.healthplus.servicio;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envía el código de verificación al correo real de la persona.
 *
 * Spring crea el objeto JavaMailSender solo si en secretos.properties existe
 * "spring.mail.host". Por eso lo pedimos con ObjectProvider que es como decir "dámelo si existe".
 *  - Si existe: se manda un correo de verdad por Gmail (SMTP).
 *  - Si no existe: estamos en modo prueba, el código se imprime en la consola de IntelliJ.
 *
 * EN INTERNET (Render gratis) los puertos de correo SMTP están bloqueados, así que Gmail no funciona por ahí.
 * Para eso existe la opción BREVO: si hay "healthplus.brevo.api-key", el correo se manda por la página web
 * de Brevo (https, que nunca se bloquea) usando el mismo Gmail como remitente. Tiene prioridad sobre SMTP.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;
    private final String brevoKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${spring.mail.username:}") String remitente,
                         @Value("${healthplus.brevo.api-key:}") String brevoKey) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.brevoKey = brevoKey;
    }

    private boolean usaBrevo() {
        return !brevoKey.isBlank() && !remitente.isBlank();
    }

    /** true si hay un correo configurado en secretos.properties (Gmail por SMTP o Brevo). */
    public boolean estaConfigurado() {
        return usaBrevo() || (mailSender.getIfAvailable() != null && !remitente.isBlank());
    }

    /**
     * @return true si el correo salió; false si estamos en modo prueba o si falló el envío.
     */
    public boolean enviarCodigo(String para, String nombre, String codigo, int minutos) {
        if (usaBrevo()) {
            return enviarConBrevo(para, nombre, codigo, minutos);
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || remitente.isBlank()) {
            log.warn("""

                    ============================================================
                      HEALTH+  MODO PRUEBA (no hay correo en secretos.properties)
                      Código de verificación para {}: {}
                    ============================================================
                    """, para, codigo);
            return false;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mensaje, true, "UTF-8");
            h.setFrom(remitente, "HEALTH+");
            h.setTo(para);
            h.setSubject(codigo + " es tu código de HEALTH+");
            h.setText(textoPlano(nombre, codigo, minutos), html(nombre, codigo, minutos));
            sender.send(mensaje);
            log.info("Código de verificación enviado a {}", para);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
            log.warn("Código de verificación (respaldo en consola) para {}: {}", para, codigo);
            return false;
        }
    }

    /** Envía el mismo correo por la API web de Brevo (para servidores que bloquean SMTP). */
    private boolean enviarConBrevo(String para, String nombre, String codigo, int minutos) {
        String json = "{"
                + "\"sender\":{\"name\":\"HEALTH+\",\"email\":" + j(remitente) + "},"
                + "\"to\":[{\"email\":" + j(para) + "}],"
                + "\"subject\":" + j(codigo + " es tu código de HEALTH+") + ","
                + "\"htmlContent\":" + j(html(nombre, codigo, minutos)) + ","
                + "\"textContent\":" + j(textoPlano(nombre, codigo, minutos))
                + "}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", brevoKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 == 2) {
                log.info("Código de verificación enviado a {} (Brevo)", para);
                return true;
            }
            log.error("Brevo no envió el correo a {}: {} {}", para, r.statusCode(), r.body());
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {} con Brevo: {}", para, e.getMessage());
        }
        log.warn("Código de verificación (respaldo en consola) para {}: {}", para, codigo);
        return false;
    }

    /** Convierte un texto en texto JSON entre comillas (escapando comillas, barras y saltos de línea). */
    private String j(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }

    private String textoPlano(String nombre, String codigo, int minutos) {
        return "Hola " + nombre + ",\n\nTu código para crear la cuenta en HEALTH+ es: " + codigo
                + "\n\nVence en " + minutos + " minutos. Si no fuiste tú, ignora este correo.\n\nEquipo HEALTH+";
    }

    // Los correos usan estilos "en línea" (style="...") porque Gmail ignora los archivos .css
    private String html(String nombre, String codigo, int minutos) {
        String digitos = String.join("&nbsp;", codigo.split(""));
        return """
                <div style="background:#0d1013;padding:32px 12px;font-family:Arial,Helvetica,sans-serif">
                  <div style="max-width:460px;margin:0 auto;background:#161b20;border-radius:20px;
                              padding:32px;border:1px solid #263039">
                    <div style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-.5px">
                      HEALTH<span style="color:#3ee6be">+</span></div>
                    <p style="color:#c9d3db;font-size:15px;margin:22px 0 6px">Hola <b style="color:#fff">%s</b>,</p>
                    <p style="color:#8b98a5;font-size:14px;margin:0 0 22px">
                      Este es tu código para terminar de crear tu cuenta:</p>
                    <div style="background:#0d1013;border:1px solid #2b3640;border-radius:14px;padding:18px;
                                text-align:center;font-size:34px;font-weight:800;color:#3ee6be;letter-spacing:6px">%s</div>
                    <p style="color:#8b98a5;font-size:13px;margin:22px 0 0">
                      Vence en %d minutos. Si tú no pediste este código, ignora este mensaje.</p>
                  </div>
                  <p style="text-align:center;color:#56636e;font-size:12px;margin-top:16px">
                    HEALTH+ · Tu salud, un paso más cada día</p>
                </div>
                """.formatted(escapar(nombre), digitos, minutos);
    }

    private String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
