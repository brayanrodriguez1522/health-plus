package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.PerfilFisico;
import com.healthplus.healthplus.modelo.Usuario;
import com.healthplus.healthplus.repositorio.PerfilFisicoRepository;
import com.healthplus.healthplus.repositorio.UsuarioRepository;
import com.healthplus.healthplus.servicio.ProgresoService;
import com.healthplus.healthplus.servicio.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * ===== Módulo de JONATHAN: Página de inicio =====
 *
 * Ruta: GET /inicio  (a donde llega la persona después de iniciar sesión).
 * El controlador NO calcula: le pide los números a ProgresoService y los
 * pone en el "Model" para que inicio.html los pinte con th:text, th:each, etc.
 */
@Controller
public class InicioController {

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private final ProgresoService progreso;
    private final UsuarioRepository usuarios;
    private final PerfilFisicoRepository perfiles;

    public InicioController(ProgresoService progreso, UsuarioRepository usuarios,
                            PerfilFisicoRepository perfiles) {
        this.progreso = progreso;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
    }

    @GetMapping("/inicio")
    public String inicio(HttpSession session, Model model) {
        Long id = SesionUsuario.id(session);
        if (id == null) {
            return "redirect:/";
        }
        Optional<Usuario> ou = usuarios.findById(id);
        if (ou.isEmpty()) {           // la cuenta fue borrada (por ejemplo desde la consola H2)
            session.invalidate();
            return "redirect:/";
        }
        Usuario u = ou.get();
        String primerNombre = u.getNombre().trim().split("\\s+")[0];

        // ---- Datos físicos (SCRUM-16) ----
        Optional<PerfilFisico> perfil = perfiles.findByIdUsuario(id);
        String recomendada = perfil.map(p -> ProgresoService.categoriaPorImc(p.getImc())).orElse(null);

        // ---- Encabezado ----
        LocalDate hoy = LocalDate.now();
        String fecha = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + ", "
                + hoy.format(DateTimeFormatter.ofPattern("d 'de' MMMM", ES));
        model.addAttribute("usuario", u);
        model.addAttribute("primerNombre", primerNombre);
        model.addAttribute("saludo", progreso.saludo(LocalTime.now().getHour()));
        model.addAttribute("fechaHoy", fecha.substring(0, 1).toUpperCase() + fecha.substring(1));

        // ---- Progreso ----
        int[] t = progreso.totales(id);
        model.addAttribute("hoy", progreso.hoy(id));
        model.addAttribute("semana", progreso.semana(id));
        model.addAttribute("racha", progreso.racha(id));
        model.addAttribute("hechosSemana", t[0]);
        model.addAttribute("minutosSemana", t[1]);
        model.addAttribute("hechosTotal", t[2]);

        // ---- Perfil, metas y rutinas ----
        model.addAttribute("perfil", perfil.orElse(null));
        model.addAttribute("categoriaImc", recomendada);
        model.addAttribute("metas", progreso.metasActivas(id));
        model.addAttribute("programas", progreso.programas(recomendada));
        model.addAttribute("tip", progreso.tipDelDia());
        return "inicio";
    }
}
