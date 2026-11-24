package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.Usuario;
import com.healthplus.healthplus.repositorio.MetaRepository;
import com.healthplus.healthplus.repositorio.PerfilFisicoRepository;
import com.healthplus.healthplus.repositorio.PlanEjercicioRepository;
import com.healthplus.healthplus.repositorio.UsuarioRepository;
import com.healthplus.healthplus.servicio.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Menú "Mi cuenta" dentro de /perfil: cambiar nombre, cambiar contraseña y borrar la cuenta.
 * (Los datos físicos siguen en PerfilController, así no se mezclan responsabilidades.)
 */
@Controller
public class CuentaController {

    private final UsuarioRepository usuarios;
    private final PerfilFisicoRepository perfiles;
    private final MetaRepository metas;
    private final PlanEjercicioRepository plan;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public CuentaController(UsuarioRepository usuarios, PerfilFisicoRepository perfiles,
                            MetaRepository metas, PlanEjercicioRepository plan) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.metas = metas;
        this.plan = plan;
    }

    private Optional<Usuario> actual(HttpSession session) {
        Long id = SesionUsuario.id(session);
        return id == null ? Optional.empty() : usuarios.findById(id);
    }

    @PostMapping("/perfil/cuenta")
    public String cambiarNombre(@RequestParam(name = "nombre", defaultValue = "") String nombre,
                                HttpSession session, RedirectAttributes ra) {
        Optional<Usuario> ou = actual(session);
        if (ou.isEmpty()) {
            return "redirect:/";
        }
        String n = nombre.trim();
        if (n.isEmpty() || n.length() > 100) {
            ra.addFlashAttribute("error", "Escribe un nombre válido (máximo 100 letras).");
            return "redirect:/perfil#cuenta";
        }
        Usuario u = ou.get();
        u.setNombre(n);
        usuarios.save(u);
        session.setAttribute(SesionUsuario.NOMBRE, n);   // para que el saludo cambie de una vez
        ra.addFlashAttribute("mensaje", "Nombre actualizado.");
        return "redirect:/perfil#cuenta";
    }

    @PostMapping("/perfil/contrasena")
    public String cambiarContrasena(@RequestParam(name = "actual", defaultValue = "") String actual,
                                    @RequestParam(name = "nueva", defaultValue = "") String nueva,
                                    @RequestParam(name = "confirmar", defaultValue = "") String confirmar,
                                    HttpSession session, RedirectAttributes ra) {
        Optional<Usuario> ou = actual(session);
        if (ou.isEmpty()) {
            return "redirect:/";
        }
        Usuario u = ou.get();
        if (!u.isCuentaLocal()) {
            ra.addFlashAttribute("error", "Tu cuenta usa " + u.getProveedor() + ": la contraseña se maneja allá.");
        } else if (!encoder.matches(actual, u.getContrasenaHash())) {
            ra.addFlashAttribute("error", "La contraseña actual no es correcta.");
        } else if (nueva.length() < 8 || !nueva.matches(".*[A-Za-z].*") || !nueva.matches(".*\\d.*")) {
            ra.addFlashAttribute("error", "La nueva contraseña debe tener mínimo 8 caracteres, con letras y números.");
        } else if (!nueva.equals(confirmar)) {
            ra.addFlashAttribute("error", "Las contraseñas nuevas no coinciden.");
        } else {
            u.setContrasenaHash(encoder.encode(nueva));
            usuarios.save(u);
            ra.addFlashAttribute("mensaje", "Contraseña actualizada.");
        }
        return "redirect:/perfil#seguridad";
    }

    /** Borra al usuario Y todo lo suyo (perfil físico, metas y plan), para no dejar "basura" en la BD. */
    @PostMapping("/perfil/eliminar")
    @Transactional
    public String eliminar(@RequestParam(name = "confirmacion", defaultValue = "") String confirmacion,
                           HttpSession session, RedirectAttributes ra) {
        Optional<Usuario> ou = actual(session);
        if (ou.isEmpty()) {
            return "redirect:/";
        }
        if (!"ELIMINAR".equals(confirmacion.trim())) {
            ra.addFlashAttribute("error", "Para borrar tu cuenta escribe ELIMINAR en mayúsculas.");
            return "redirect:/perfil#peligro";
        }
        Long id = ou.get().getIdUsuario();
        plan.deleteAll(plan.findByIdUsuario(id));
        metas.deleteAll(metas.findByIdUsuarioOrderByFechaFinAsc(id));
        perfiles.findByIdUsuario(id).ifPresent(perfiles::delete);
        usuarios.delete(ou.get());
        session.invalidate();
        return "redirect:/";
    }
}
