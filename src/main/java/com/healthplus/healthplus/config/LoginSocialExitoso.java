package com.healthplus.healthplus.config;

import com.healthplus.healthplus.modelo.Usuario;
import com.healthplus.healthplus.repositorio.UsuarioRepository;
import com.healthplus.healthplus.servicio.SesionUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Se ejecuta cuando Google o Facebook nos dicen "esta persona sí es quien dice ser".
 *
 *  1. Leemos su correo, nombre y foto (los "atributos" que manda Google/Facebook).
 *  2. Si el correo NO existe en la tabla usuarios, creamos la cuenta (¡registro con un clic!).
 *  3. Guardamos idUsuario en la sesión, igual que el login normal, y lo mandamos a /inicio.
 */
@Component
public class LoginSocialExitoso implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginSocialExitoso(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication auth) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        String proveedor = "facebook".equals(token.getAuthorizedClientRegistrationId()) ? "Facebook" : "Google";
        OAuth2User datos = token.getPrincipal();

        String correo = texto(datos.getAttribute("email"));
        if (correo.isBlank()) {
            response.sendRedirect("/?error=sin-correo");
            return;
        }
        correo = correo.trim().toLowerCase();
        String nombre = texto(datos.getAttribute("name"));
        String foto = foto(proveedor, datos.getAttributes());

        Usuario u = usuarios.findByCorreo(correo).orElse(null);
        if (u == null) {
            u = new Usuario();
            u.setCorreo(correo);
            u.setNombre(nombre.isBlank() ? correo.substring(0, correo.indexOf('@')) : nombre);
            // No tiene contraseña propia: guardamos una aleatoria cifrada (la columna no puede ir vacía)
            u.setContrasenaHash(encoder.encode(UUID.randomUUID().toString()));
            u.setProveedor(proveedor);
        }
        if (foto != null) {
            u.setFotoUrl(foto);
        }
        usuarios.save(u);

        SesionUsuario.iniciar(request.getSession(), u);
        response.sendRedirect("/inicio");
    }

    private String texto(Object o) {
        return o == null ? "" : o.toString();
    }

    @SuppressWarnings("unchecked")
    private String foto(String proveedor, Map<String, Object> a) {
        if ("Google".equals(proveedor)) {
            Object p = a.get("picture");
            return p == null ? null : p.toString();
        }
        // Facebook manda: picture -> data -> url
        try {
            Map<String, Object> pic = (Map<String, Object>) a.get("picture");
            Map<String, Object> data = (Map<String, Object>) pic.get("data");
            return data.get("url").toString();
        } catch (Exception e) {
            return null;
        }
    }
}
