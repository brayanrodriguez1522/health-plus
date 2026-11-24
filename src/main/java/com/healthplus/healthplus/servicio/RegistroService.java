package com.healthplus.healthplus.servicio;

import com.healthplus.healthplus.modelo.Usuario;
import com.healthplus.healthplus.modelo.VerificacionCorreo;
import com.healthplus.healthplus.repositorio.UsuarioRepository;
import com.healthplus.healthplus.repositorio.VerificacionCorreoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica del registro en 2 pasos:
 *   1) iniciar(): valida el formulario, genera un código de 6 dígitos y lo manda al correo.
 *   2) verificar(): compara el código; si es correcto crea el Usuario en la tabla usuarios.
 *
 * Los controladores solo reciben el formulario y muestran páginas;
 * las reglas de negocio viven aquí (así es más fácil de explicar y de probar).
 */
@Service
public class RegistroService {

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int SEGUNDOS_ENTRE_REENVIOS = 60;

    private final UsuarioRepository usuarios;
    private final VerificacionCorreoRepository pendientes;
    private final CorreoService correo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom azar = new SecureRandom();
    private final int minutosValidez;
    private final int maxIntentos;

    public RegistroService(UsuarioRepository usuarios, VerificacionCorreoRepository pendientes,
                           CorreoService correo,
                           @Value("${healthplus.codigo.minutos-validez:10}") int minutosValidez,
                           @Value("${healthplus.codigo.max-intentos:5}") int maxIntentos) {
        this.usuarios = usuarios;
        this.pendientes = pendientes;
        this.correo = correo;
        this.minutosValidez = minutosValidez;
        this.maxIntentos = maxIntentos;
    }

    /** Resultado de un paso: si salió bien y qué mensaje mostrar. */
    public record Resultado(boolean ok, String mensaje, String codigoModoPrueba) {
        static Resultado error(String m) {
            return new Resultado(false, m, null);
        }
    }

    public static String normalizarCorreo(String c) {
        return c == null ? "" : c.trim().toLowerCase();
    }

    /** Paso 1: validar formulario y enviar el código. */
    @Transactional
    public Resultado iniciar(String nombre, String correoIn, String contrasena, String confirmar) {
        String c = normalizarCorreo(correoIn);
        String n = nombre == null ? "" : nombre.trim();

        if (n.isEmpty() || c.isEmpty() || contrasena == null || contrasena.isEmpty()) {
            return Resultado.error("Completa todos los campos.");
        }
        if (n.length() > 100) {
            return Resultado.error("El nombre es demasiado largo.");
        }
        if (!CORREO.matcher(c).matches()) {
            return Resultado.error("Escribe un correo válido, por ejemplo nombre@gmail.com.");
        }
        if (contrasena.length() < 8 || !contrasena.matches(".*[A-Za-z].*") || !contrasena.matches(".*\\d.*")) {
            return Resultado.error("La contraseña debe tener mínimo 8 caracteres, con letras y números.");
        }
        if (!contrasena.equals(confirmar)) {
            return Resultado.error("Las contraseñas no coinciden.");
        }
        if (usuarios.findByCorreo(c).isPresent()) {
            return Resultado.error("Ese correo ya tiene una cuenta. Inicia sesión.");
        }

        VerificacionCorreo v = pendientes.findByCorreo(c).orElse(new VerificacionCorreo());
        v.setCorreo(c);
        v.setNombre(n);
        v.setContrasenaHash(encoder.encode(contrasena));
        return enviarNuevoCodigo(v);
    }

    /** Botón "reenviar código". */
    @Transactional
    public Resultado reenviar(String correoIn) {
        Optional<VerificacionCorreo> ov = pendientes.findByCorreo(normalizarCorreo(correoIn));
        if (ov.isEmpty()) {
            return Resultado.error("Tu registro expiró. Llena el formulario otra vez.");
        }
        VerificacionCorreo v = ov.get();
        long pasaron = Duration.between(v.getEnviadoEn(), LocalDateTime.now()).toSeconds();
        if (pasaron < SEGUNDOS_ENTRE_REENVIOS) {
            return Resultado.error("Espera " + (SEGUNDOS_ENTRE_REENVIOS - pasaron) + " segundos para pedir otro código.");
        }
        return enviarNuevoCodigo(v);
    }

    private Resultado enviarNuevoCodigo(VerificacionCorreo v) {
        // SecureRandom: números aleatorios "de verdad" (Random normal se puede adivinar)
        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        v.setCodigoHash(encoder.encode(codigo));
        v.setIntentos(0);
        v.setEnviadoEn(LocalDateTime.now());
        v.setExpiraEn(LocalDateTime.now().plusMinutes(minutosValidez));
        pendientes.save(v);

        boolean enviado = correo.enviarCodigo(v.getCorreo(), v.getNombre(), codigo, minutosValidez);
        if (enviado) {
            return new Resultado(true, "Te enviamos un código de 6 dígitos a " + v.getCorreo() + ".", null);
        }
        // Modo prueba: no hay correo configurado (o falló), mostramos el código para poder seguir probando
        return new Resultado(true, "Modo prueba: el correo no está configurado.", codigo);
    }

    /** Paso 2: comprobar el código. Si es correcto, devuelve el usuario ya creado. */
    @Transactional
    public Optional<Usuario> verificar(String correoIn, String codigo, StringBuilder error) {
        Optional<VerificacionCorreo> ov = pendientes.findByCorreo(normalizarCorreo(correoIn));
        if (ov.isEmpty()) {
            error.append("Tu registro expiró. Llena el formulario otra vez.");
            return Optional.empty();
        }
        VerificacionCorreo v = ov.get();
        if (LocalDateTime.now().isAfter(v.getExpiraEn())) {
            error.append("El código venció. Pide uno nuevo.");
            return Optional.empty();
        }
        if (v.getIntentos() >= maxIntentos) {
            error.append("Demasiados intentos. Pide un código nuevo.");
            return Optional.empty();
        }
        String limpio = codigo == null ? "" : codigo.replaceAll("\\D", "");
        if (limpio.length() != 6 || !encoder.matches(limpio, v.getCodigoHash())) {
            v.setIntentos(v.getIntentos() + 1);
            pendientes.save(v);
            int quedan = maxIntentos - v.getIntentos();
            error.append("Código incorrecto. Te quedan ").append(quedan).append(quedan == 1 ? " intento." : " intentos.");
            return Optional.empty();
        }

        // ¡Código correcto! Ahora sí se guarda el usuario en la base de datos.
        Usuario u = new Usuario();
        u.setNombre(v.getNombre());
        u.setCorreo(v.getCorreo());
        u.setContrasenaHash(v.getContrasenaHash());
        u.setProveedor("Correo");
        usuarios.save(u);
        pendientes.delete(v);
        return Optional.of(u);
    }

    public String ocultarCorreo(String c) {
        int arroba = c.indexOf('@');
        if (arroba <= 2) {
            return c;
        }
        return c.substring(0, 2) + "•".repeat(Math.min(6, arroba - 2)) + c.substring(arroba);
    }
}
