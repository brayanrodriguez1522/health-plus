package com.healthplus.healthplus.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security.
 *
 * Spring Security llegó al proyecto porque lo necesita el login con Google/Facebook (OAuth2).
 * Por defecto bloquearía TODAS las páginas, así que aquí le decimos:
 *  - Deja pasar todo: cada controlador ya revisa si hay sesión (idUsuario).
 *  - Protege los formularios con un token CSRF (Thymeleaf lo agrega solo en th:action).
 *  - Activa el login social SOLO si hay llaves de Google/Facebook en secretos.properties.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http,
                                      ObjectProvider<ClientRegistrationRepository> registros,
                                      LoginSocialExitoso exito) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())                       // usamos nuestro propio /logout
                .csrf(c -> c.ignoringRequestMatchers("/h2-console/**"))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())); // la consola H2 usa <frame>

        if (registros.getIfAvailable() != null) {
            http.oauth2Login(o -> o
                    .loginPage("/")
                    .successHandler(exito)
                    .failureHandler((req, res, ex) -> res.sendRedirect("/?error=social")));
        }
        return http.build();
    }
}
