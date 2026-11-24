package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.Usuario;
import com.healthplus.healthplus.repositorio.UsuarioRepository;
import com.healthplus.healthplus.servicio.CorreoService;
import com.healthplus.healthplus.servicio.RegistroService;
import com.healthplus.healthplus.servicio.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Acceso a la app: iniciar sesión, registrarse (con código al correo) y salir.
 * (Hecho al inicio por Brayan; ampliado con verificación por correo y login social.)
 */
@Controller
public class LoginController {

    private static final String CORREO_PENDIENTE = "correoPendiente";

    private final UsuarioRepository repo;
    private final RegistroService registro;
    private final CorreoService correo;
    private final ObjectProvider<ClientRegistrationRepository> social;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginController(UsuarioRepository repo, RegistroService registro, CorreoService correo,
                           ObjectProvider<ClientRegistrationRepository> social) {
        this.repo = repo;
        this.registro = registro;
        this.correo = correo;
        this.social = social;
    }

    /** ¿Está configurado Google/Facebook en secretos.properties? (para pintar los botones) */
    private boolean socialActivo(String proveedor) {
        ClientRegistrationRepository r = social.getIfAvailable();
        return r != null && r.findByRegistrationId(proveedor) != null;
    }

    private void datosSociales(Model model) {
        model.addAttribute("googleActivo", socialActivo("google"));
        model.addAttribute("facebookActivo", socialActivo("facebook"));
    }

    // ---------------- Iniciar sesión ----------------

    @GetMapping("/")
    public String inicio(@RequestParam(name = "error", required = false) String error,
                         HttpSession session, Model model) {
        if (SesionUsuario.id(session) != null) {
            return "redirect:/inicio";
        }
        if ("social".equals(error)) {
            model.addAttribute("error", "No se pudo entrar con esa red social. Intenta otra vez.");
        } else if ("sin-correo".equals(error)) {
            model.addAttribute("error", "Tu cuenta de esa red social no compartió el correo. Regístrate con tu correo.");
        } else if ("no-configurado".equals(error)) {
            model.addAttribute("error", "Ese botón todavía no está configurado (falta secretos.properties).");
        }
        datosSociales(model);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String correo,
                        @RequestParam String contrasena,
                        HttpSession session,
                        Model model) {
        Optional<Usuario> u = repo.findByCorreo(RegistroService.normalizarCorreo(correo));
        if (u.isPresent() && !u.get().isCuentaLocal()) {
            model.addAttribute("error", "Esta cuenta se creó con " + u.get().getProveedor()
                    + ". Usa el botón de " + u.get().getProveedor() + ".");
        } else if (u.isPresent() && encoder.matches(contrasena, u.get().getContrasenaHash())) {
            SesionUsuario.iniciar(session, u.get());
            return "redirect:/inicio";
        } else {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        model.addAttribute("correo", correo);
        datosSociales(model);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ---------------- Registro paso 1: formulario ----------------

    @GetMapping("/registro")
    public String registro(Model model) {
        datosSociales(model);
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String nombre,
                            @RequestParam String correo,
                            @RequestParam String contrasena,
                            @RequestParam(name = "confirmar", defaultValue = "") String confirmar,
                            HttpSession session, Model model, RedirectAttributes ra) {
        RegistroService.Resultado r = registro.iniciar(nombre, correo, contrasena, confirmar);
        if (!r.ok()) {
            model.addAttribute("error", r.mensaje());
            model.addAttribute("nombre", nombre);
            model.addAttribute("correo", correo);
            datosSociales(model);
            return "registro";
        }
        session.setAttribute(CORREO_PENDIENTE, RegistroService.normalizarCorreo(correo));
        ra.addFlashAttribute("mensaje", r.mensaje());
        ra.addFlashAttribute("codigoPrueba", r.codigoModoPrueba());
        return "redirect:/registro/verificar";
    }

    // ---------------- Registro paso 2: escribir el código ----------------

    @GetMapping("/registro/verificar")
    public String verificarForm(HttpSession session, Model model) {
        String c = (String) session.getAttribute(CORREO_PENDIENTE);
        if (c == null) {
            return "redirect:/registro";
        }
        model.addAttribute("correoOculto", registro.ocultarCorreo(c));
        model.addAttribute("modoPrueba", !correo.estaConfigurado());
        return "verificar";
    }

    @PostMapping("/registro/verificar")
    public String verificar(@RequestParam(name = "codigo", defaultValue = "") String codigo,
                            HttpSession session, Model model, RedirectAttributes ra) {
        String c = (String) session.getAttribute(CORREO_PENDIENTE);
        if (c == null) {
            return "redirect:/registro";
        }
        StringBuilder error = new StringBuilder();
        Optional<Usuario> u = registro.verificar(c, codigo, error);
        if (u.isEmpty()) {
            model.addAttribute("error", error.toString());
            model.addAttribute("correoOculto", registro.ocultarCorreo(c));
            model.addAttribute("modoPrueba", !correo.estaConfigurado());
            return "verificar";
        }
        session.removeAttribute(CORREO_PENDIENTE);
        SesionUsuario.iniciar(session, u.get());
        ra.addFlashAttribute("bienvenida", true);
        return "redirect:/inicio";
    }

    @PostMapping("/registro/reenviar")
    public String reenviar(HttpSession session, RedirectAttributes ra) {
        String c = (String) session.getAttribute(CORREO_PENDIENTE);
        if (c == null) {
            return "redirect:/registro";
        }
        RegistroService.Resultado r = registro.reenviar(c);
        ra.addFlashAttribute(r.ok() ? "mensaje" : "error", r.mensaje());
        ra.addFlashAttribute("codigoPrueba", r.codigoModoPrueba());
        return "redirect:/registro/verificar";
    }
}
