package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.*;
import com.healthplus.healthplus.repositorio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class RutinaController {

    private static final List<String> CATEGORIAS = List.of("Bajo peso", "Peso normal", "Sobrepeso");
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    public static class DiaPlan {
        private final String dia;
        private final String rutina;
        private final List<String> ejercicios;

        public DiaPlan(String dia, String rutina, List<String> ejercicios) {
            this.dia = dia;
            this.rutina = rutina;
            this.ejercicios = ejercicios;
        }

        public String getDia() {
            return dia;
        }

        public String getRutina() {
            return rutina;
        }

        public List<String> getEjercicios() {
            return ejercicios;
        }
    }

    private final RutinaRepository rutinas;
    private final EjercicioRepository ejercicios;
    private final RutinaEjercicioRepository detalle;
    private final PlanEjercicioRepository plan;
    private final PerfilFisicoRepository perfiles;

    public RutinaController(RutinaRepository rutinas, EjercicioRepository ejercicios,
                            RutinaEjercicioRepository detalle, PlanEjercicioRepository plan,
                            PerfilFisicoRepository perfiles) {
        this.rutinas = rutinas;
        this.ejercicios = ejercicios;
        this.detalle = detalle;
        this.plan = plan;
        this.perfiles = perfiles;
    }

    private String categoriaPorImc(double imc) {
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25) return "Peso normal";
        return "Sobrepeso";
    }

    private void cargar(Long idUsuario, String categoria, HttpSession session, Model model) {
        model.addAttribute("nombre", session.getAttribute("nombre"));

        String sugerida = null;
        Optional<PerfilFisico> perfil = perfiles.findByIdUsuario(idUsuario);
        if (perfil.isPresent()) {
            double imc = perfil.get().getImc();
            sugerida = categoriaPorImc(imc);
            model.addAttribute("imc", imc);
        }

        String activa;
        if (categoria != null && CATEGORIAS.contains(categoria)) {
            activa = categoria;
        } else if (sugerida != null) {
            activa = sugerida;
        } else {
            activa = "Peso normal";
        }

        Map<Integer, Rutina> porDia = new HashMap<>();
        for (Rutina r : rutinas.findByNivelOrderByDiaSemanaAsc(activa)) {
            if (r.getDiaSemana() != null) {
                porDia.put(r.getDiaSemana(), r);
            }
        }

        List<DiaPlan> semana = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            Rutina r = porDia.get(d);
            if (r == null) {
                semana.add(new DiaPlan(DIAS[d - 1], null, new ArrayList<>()));
                continue;
            }
            List<String> textos = new ArrayList<>();
            for (RutinaEjercicio re : detalle.findByIdRutina(r.getIdRutina())) {
                ejercicios.findById(re.getIdEjercicio()).ifPresent(e -> {
                    if (re.getSeries() != null && re.getRepeticiones() != null) {
                        textos.add(e.getNombre() + ": " + re.getSeries() + " x " + re.getRepeticiones());
                    } else {
                        textos.add(e.getNombre() + ": " + e.getDuracionMin() + " min");
                    }
                });
            }
            semana.add(new DiaPlan(DIAS[d - 1], r.getNombre(), textos));
        }

        model.addAttribute("categorias", CATEGORIAS);
        model.addAttribute("activa", activa);
        model.addAttribute("sugerida", sugerida);
        model.addAttribute("semana", semana);
    }

    @GetMapping("/rutinas")
    public String ver(@RequestParam(name = "categoria", required = false) String categoria,
                      HttpSession session, Model model) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        cargar(idUsuario, categoria, session, model);
        return "rutinas";
    }

    @PostMapping("/rutinas/elegir")
    public String elegir(@RequestParam(name = "categoria") String categoria,
                         HttpSession session, Model model, RedirectAttributes ra) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }

        Map<Integer, Rutina> porDia = new HashMap<>();
        for (Rutina r : rutinas.findByNivelOrderByDiaSemanaAsc(categoria)) {
            if (r.getDiaSemana() != null) {
                porDia.put(r.getDiaSemana(), r);
            }
        }
        if (porDia.isEmpty()) {
            model.addAttribute("mensaje", "Ese plan no está disponible. Elige otro");
            cargar(idUsuario, null, session, model);
            return "rutinas";
        }

        LocalDate hoy = LocalDate.now();
        List<PlanEjercicio> futuros = plan.findByIdUsuarioAndFechaGreaterThanEqual(idUsuario, hoy);

        Set<LocalDate> conProgreso = new HashSet<>();
        for (PlanEjercicio p : futuros) {
            if (p.isCompletado()) {
                conProgreso.add(p.getFecha());
            }
        }
        List<PlanEjercicio> borrar = new ArrayList<>();
        for (PlanEjercicio p : futuros) {
            if (!conProgreso.contains(p.getFecha())) {
                borrar.add(p);
            }
        }
        plan.deleteAll(borrar);

        for (int i = 0; i < 28; i++) {
            LocalDate fecha = hoy.plusDays(i);
            if (conProgreso.contains(fecha)) {
                continue;
            }
            Rutina r = porDia.get(fecha.getDayOfWeek().getValue());
            if (r == null) {
                continue;
            }
            for (RutinaEjercicio re : detalle.findByIdRutina(r.getIdRutina())) {
                PlanEjercicio p = new PlanEjercicio();
                p.setIdUsuario(idUsuario);
                p.setIdRutina(r.getIdRutina());
                p.setIdEjercicio(re.getIdEjercicio());
                p.setFecha(fecha);
                plan.save(p);
            }
        }

        ra.addFlashAttribute("mensaje", "Plan elegido: " + categoria + ". Ya está en tu calendario");
        return "redirect:/calendario";
    }
}