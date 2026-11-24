package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.*;
import com.healthplus.healthplus.repositorio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class PlanController {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    public static class DiaCal {
        private final LocalDate fecha;
        private final String dia;
        private final String fechaCorta;
        private final String rutina;
        private final int total;
        private final int hechos;
        private final boolean hoy;

        public DiaCal(LocalDate fecha, String dia, String rutina, int total, int hechos, boolean hoy) {
            this.fecha = fecha;
            this.dia = dia;
            this.fechaCorta = fecha.format(DateTimeFormatter.ofPattern("dd/MM"));
            this.rutina = rutina;
            this.total = total;
            this.hechos = hechos;
            this.hoy = hoy;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getDia() {
            return dia;
        }

        public String getFechaCorta() {
            return fechaCorta;
        }

        public String getRutina() {
            return rutina;
        }

        public int getTotal() {
            return total;
        }

        public int getHechos() {
            return hechos;
        }

        public boolean isHoy() {
            return hoy;
        }

        public int getPorcentaje() {
            return total == 0 ? 0 : hechos * 100 / total;
        }
    }

    public static class ItemPlan {
        private final Long idPlan;
        private final Long idEjercicio;
        private final String nombre;
        private final String grupo;
        private final String detalle;
        private final boolean completado;

        public ItemPlan(Long idPlan, Long idEjercicio, String nombre, String grupo,
                        String detalle, boolean completado) {
            this.idPlan = idPlan;
            this.idEjercicio = idEjercicio;
            this.nombre = nombre;
            this.grupo = grupo;
            this.detalle = detalle;
            this.completado = completado;
        }

        public Long getIdPlan() {
            return idPlan;
        }

        public Long getIdEjercicio() {
            return idEjercicio;
        }

        public String getNombre() {
            return nombre;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getDetalle() {
            return detalle;
        }

        public boolean isCompletado() {
            return completado;
        }
    }

    private final PlanEjercicioRepository plan;
    private final RutinaRepository rutinas;
    private final EjercicioRepository ejercicios;
    private final RutinaEjercicioRepository detalle;

    public PlanController(PlanEjercicioRepository plan, RutinaRepository rutinas,
                          EjercicioRepository ejercicios, RutinaEjercicioRepository detalle) {
        this.plan = plan;
        this.rutinas = rutinas;
        this.ejercicios = ejercicios;
        this.detalle = detalle;
    }

    private Long usuario(HttpSession session) {
        return (Long) session.getAttribute("idUsuario");
    }

    private Optional<PlanEjercicio> delUsuario(Long idPlan, Long idUsuario) {
        return plan.findById(idPlan).filter(p -> p.getIdUsuario().equals(idUsuario));
    }

    // ---------- Calendario ----------

    @GetMapping("/calendario")
    public String calendario(@RequestParam(name = "semana", defaultValue = "0") int semana,
                             HttpSession session, Model model) {
        Long idUsuario = usuario(session);
        if (idUsuario == null) {
            return "redirect:/";
        }
        int offset = Math.max(-4, Math.min(4, semana));
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset);
        LocalDate domingo = lunes.plusDays(6);

        Map<LocalDate, List<PlanEjercicio>> porFecha = new HashMap<>();
        for (PlanEjercicio p : plan.findByIdUsuarioAndFechaBetween(idUsuario, lunes, domingo)) {
            porFecha.computeIfAbsent(p.getFecha(), k -> new ArrayList<>()).add(p);
        }

        List<DiaCal> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate f = lunes.plusDays(i);
            List<PlanEjercicio> delDia = porFecha.getOrDefault(f, new ArrayList<>());
            String nombreRutina = null;
            int hechos = 0;
            if (!delDia.isEmpty()) {
                nombreRutina = rutinas.findById(delDia.get(0).getIdRutina())
                        .map(Rutina::getNombre).orElse("Rutina");
                for (PlanEjercicio p : delDia) {
                    if (p.isCompletado()) {
                        hechos++;
                    }
                }
            }
            dias.add(new DiaCal(f, DIAS[i], nombreRutina, delDia.size(), hechos, f.equals(hoy)));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        model.addAttribute("nombre", session.getAttribute("nombre"));
        model.addAttribute("dias", dias);
        model.addAttribute("semana", offset);
        model.addAttribute("rango", lunes.format(fmt) + " al " + domingo.format(fmt));
        return "calendario";
    }

    // ---------- Plan de un día ----------

    @GetMapping("/plan/hoy")
    public String hoy(HttpSession session) {
        if (usuario(session) == null) {
            return "redirect:/";
        }
        return "redirect:/plan/" + LocalDate.now();
    }

    @GetMapping("/plan/{fecha}")
    public String dia(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                      HttpSession session, Model model) {
        Long idUsuario = usuario(session);
        if (idUsuario == null) {
            return "redirect:/";
        }

        List<PlanEjercicio> filas = new ArrayList<>(plan.findByIdUsuarioAndFecha(idUsuario, fecha));
        filas.sort(Comparator.comparing(PlanEjercicio::getIdPlan));

        List<ItemPlan> items = new ArrayList<>();
        int hechos = 0;
        for (PlanEjercicio p : filas) {
            Optional<Ejercicio> oe = ejercicios.findById(p.getIdEjercicio());
            if (oe.isEmpty()) {
                continue;
            }
            Ejercicio e = oe.get();
            String texto = e.getDuracionMin() + " min";
            Optional<RutinaEjercicio> re =
                    detalle.findById(new RutinaEjercicio.Clave(p.getIdRutina(), p.getIdEjercicio()));
            if (re.isPresent() && re.get().getSeries() != null && re.get().getRepeticiones() != null) {
                texto = re.get().getSeries() + " series x " + re.get().getRepeticiones() + " repeticiones";
            }
            items.add(new ItemPlan(p.getIdPlan(), e.getIdEjercicio(), e.getNombre(),
                    e.getGrupoMuscular(), texto, p.isCompletado()));
            if (p.isCompletado()) {
                hechos++;
            }
        }

        String nombreRutina = null;
        if (!filas.isEmpty()) {
            nombreRutina = rutinas.findById(filas.get(0).getIdRutina()).map(Rutina::getNombre).orElse(null);
        }

        model.addAttribute("nombre", session.getAttribute("nombre"));
        model.addAttribute("fechaIso", fecha.toString());
        model.addAttribute("fechaTexto", DIAS[fecha.getDayOfWeek().getValue() - 1] + " "
                + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("esHoy", fecha.equals(LocalDate.now()));
        model.addAttribute("nombreRutina", nombreRutina);
        model.addAttribute("items", items);
        model.addAttribute("hechos", hechos);
        model.addAttribute("total", items.size());
        model.addAttribute("porcentaje", items.isEmpty() ? 0 : hechos * 100 / items.size());
        return "plan";
    }

    // ---------- Marcar como completado (RF-13) ----------

    @PostMapping("/plan/{idPlan}/completar")
    public String completar(@PathVariable Long idPlan, HttpSession session, RedirectAttributes ra) {
        Long idUsuario = usuario(session);
        if (idUsuario == null) {
            return "redirect:/";
        }
        Optional<PlanEjercicio> op = delUsuario(idPlan, idUsuario);
        if (op.isEmpty()) {
            ra.addFlashAttribute("mensaje", "El ejercicio no se encontró");
            return "redirect:/calendario";
        }
        PlanEjercicio p = op.get();
        try {
            p.setCompletado(!p.isCompletado());
            plan.save(p);
        } catch (Exception ex) {
            ra.addFlashAttribute("mensaje", "No se pudo guardar. Intenta de nuevo");
        }
        return "redirect:/plan/" + p.getFecha();
    }

    // ---------- Cambiar un ejercicio por otro del mismo grupo ----------

    @PostMapping("/plan/{idPlan}/cambiar")
    public String cambiar(@PathVariable Long idPlan, HttpSession session, RedirectAttributes ra) {
        Long idUsuario = usuario(session);
        if (idUsuario == null) {
            return "redirect:/";
        }
        Optional<PlanEjercicio> op = delUsuario(idPlan, idUsuario);
        if (op.isEmpty()) {
            ra.addFlashAttribute("mensaje", "El ejercicio no se encontró");
            return "redirect:/calendario";
        }
        PlanEjercicio p = op.get();
        Optional<Ejercicio> actual = ejercicios.findById(p.getIdEjercicio());
        if (actual.isEmpty()) {
            ra.addFlashAttribute("mensaje", "El ejercicio no se encontró");
            return "redirect:/plan/" + p.getFecha();
        }

        Set<Long> yaEnElDia = new HashSet<>();
        for (PlanEjercicio x : plan.findByIdUsuarioAndFecha(idUsuario, p.getFecha())) {
            yaEnElDia.add(x.getIdEjercicio());
        }
        List<Ejercicio> candidatos = new ArrayList<>();
        for (Ejercicio c : ejercicios.findByGrupoMuscular(actual.get().getGrupoMuscular())) {
            if (!yaEnElDia.contains(c.getIdEjercicio())) {
                candidatos.add(c);
            }
        }
        if (candidatos.isEmpty()) {
            ra.addFlashAttribute("mensaje", "No hay otro ejercicio de ese grupo para cambiar");
            return "redirect:/plan/" + p.getFecha();
        }

        Ejercicio nuevo = candidatos.get(0);
        for (Ejercicio c : candidatos) {
            if (c.getIdEjercicio() > p.getIdEjercicio()) {
                nuevo = c;
                break;
            }
        }
        p.setIdEjercicio(nuevo.getIdEjercicio());
        p.setCompletado(false);
        plan.save(p);
        ra.addFlashAttribute("mensaje", "Cambiaste por: " + nuevo.getNombre());
        return "redirect:/plan/" + p.getFecha();
    }

    // ---------- Saltar un día ----------

    @PostMapping("/plan/{fecha}/descanso")
    public String descanso(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                           HttpSession session, RedirectAttributes ra) {
        Long idUsuario = usuario(session);
        if (idUsuario == null) {
            return "redirect:/";
        }
        plan.deleteAll(plan.findByIdUsuarioAndFecha(idUsuario, fecha));
        ra.addFlashAttribute("mensaje", "Listo, ese día queda de descanso");
        return "redirect:/calendario";
    }

    // ---------- Detalle del ejercicio (RF-12) ----------

    @GetMapping("/ejercicio/{id}")
    public String ejercicio(@PathVariable Long id,
                            @RequestParam(name = "volver", required = false) String volver,
                            HttpSession session, Model model) {
        if (usuario(session) == null) {
            return "redirect:/";
        }
        Optional<Ejercicio> oe = ejercicios.findById(id);
        if (oe.isEmpty()) {
            model.addAttribute("mensaje", "El ejercicio no se encontró");
        } else {
            model.addAttribute("ejercicio", oe.get());
        }
        String destino = (volver != null && volver.startsWith("/plan/")) ? volver : "/calendario";
        model.addAttribute("volver", destino);
        return "ejercicio";
    }
}