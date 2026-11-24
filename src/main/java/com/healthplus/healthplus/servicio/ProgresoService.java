package com.healthplus.healthplus.servicio;

import com.healthplus.healthplus.modelo.Ejercicio;
import com.healthplus.healthplus.modelo.Meta;
import com.healthplus.healthplus.modelo.PlanEjercicio;
import com.healthplus.healthplus.modelo.Rutina;
import com.healthplus.healthplus.repositorio.EjercicioRepository;
import com.healthplus.healthplus.repositorio.MetaRepository;
import com.healthplus.healthplus.repositorio.PlanEjercicioRepository;
import com.healthplus.healthplus.repositorio.RutinaEjercicioRepository;
import com.healthplus.healthplus.repositorio.RutinaRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ===== Módulo de JONATHAN: Inicio y progreso =====
 *
 * Calcula todo lo que se ve en la página de inicio (/inicio):
 *  - el entrenamiento de hoy y su porcentaje,
 *  - la racha de días seguidos entrenando,
 *  - la gráfica de la semana,
 *  - los minutos activos,
 *  - las metas con los días que faltan,
 *  - y las rutinas recomendadas según el IMC.
 *
 * No guarda nada nuevo en la base de datos: LEE lo que ya existe
 * (plan_ejercicio, ejercicios, rutinas, metas) y lo convierte en números.
 */
@Service
public class ProgresoService {

    private static final String[] LETRAS = {"L", "M", "X", "J", "V", "S", "D"};
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private static final String[] TIPS = {
            "Toma un vaso de agua apenas te levantes: tu cuerpo pasó horas sin hidratarse.",
            "Dormir 7 a 9 horas también es entrenar: el músculo se recupera mientras duermes.",
            "Calienta 5 minutos antes de empezar. Menos lesiones, mejor rendimiento.",
            "La constancia le gana a la intensidad: 20 minutos diarios valen más que 2 horas un solo día.",
            "Incluye proteína en cada comida: huevos, fríjoles, pollo, atún o lentejas.",
            "Si te duele una articulación, para. El dolor muscular leve es normal; el dolor agudo no.",
            "Estira al terminar: 20 segundos por zona, sin rebotes.",
            "Camina después de almorzar: 10 minutos ayudan a la digestión y a tu energía.",
            "Celebra lo pequeño: cada ejercicio marcado como hecho cuenta.",
            "Respira: exhala cuando haces fuerza, inhala cuando vuelves a la posición inicial."
    };

    private final PlanEjercicioRepository plan;
    private final EjercicioRepository ejercicios;
    private final RutinaRepository rutinas;
    private final RutinaEjercicioRepository detalle;
    private final MetaRepository metas;

    public ProgresoService(PlanEjercicioRepository plan, EjercicioRepository ejercicios,
                           RutinaRepository rutinas, RutinaEjercicioRepository detalle, MetaRepository metas) {
        this.plan = plan;
        this.ejercicios = ejercicios;
        this.rutinas = rutinas;
        this.detalle = detalle;
        this.metas = metas;
    }

    // =====================================================================
    //  Clases pequeñas que viajan a la vista (Thymeleaf lee sus getters)
    // =====================================================================

    /** Entrenamiento de hoy. */
    public static class Hoy {
        private final String rutina;
        private final int total;
        private final int hechos;
        private final int minutos;

        public Hoy(String rutina, int total, int hechos, int minutos) {
            this.rutina = rutina;
            this.total = total;
            this.hechos = hechos;
            this.minutos = minutos;
        }

        public String getRutina() { return rutina; }
        public int getTotal() { return total; }
        public int getHechos() { return hechos; }
        public int getMinutos() { return minutos; }
        public int getPorcentaje() { return total == 0 ? 0 : hechos * 100 / total; }
        public boolean isTerminado() { return total > 0 && hechos == total; }

        /** Para el anillo SVG: cuánto del círculo (circunferencia 2·π·52 ≈ 326.7) queda sin pintar. */
        public double getOffsetAnillo() {
            return Math.round(326.7 * (100 - getPorcentaje()) / 100.0 * 10) / 10.0;
        }
    }

    /** Una barra de la gráfica semanal. */
    public static class Barra {
        private final String letra;
        private final String dia;
        private final int porcentaje;
        private final boolean hoy;
        private final boolean conPlan;

        public Barra(String letra, String dia, int porcentaje, boolean hoy, boolean conPlan) {
            this.letra = letra;
            this.dia = dia;
            this.porcentaje = porcentaje;
            this.hoy = hoy;
            this.conPlan = conPlan;
        }

        public String getLetra() { return letra; }
        public String getDia() { return dia; }
        public int getPorcentaje() { return porcentaje; }
        public boolean isHoy() { return hoy; }
        public boolean isConPlan() { return conPlan; }
        /** Altura mínima para que se vea algo aunque sea 0 %. */
        public int getAltura() { return conPlan ? Math.max(6, porcentaje) : 4; }
    }

    /** Meta con cuenta regresiva. */
    public static class MetaVista {
        private final Meta meta;
        private final long diasRestantes;
        private final int avanceTiempo;

        public MetaVista(Meta meta, long diasRestantes, int avanceTiempo) {
            this.meta = meta;
            this.diasRestantes = diasRestantes;
            this.avanceTiempo = avanceTiempo;
        }

        public Meta getMeta() { return meta; }
        public long getDiasRestantes() { return diasRestantes; }
        public int getAvanceTiempo() { return avanceTiempo; }
        public String getIcono() {
            return switch (meta.getTipo()) {
                case "Bajar de peso" -> "bi-graph-down-arrow";
                case "Subir de peso" -> "bi-graph-up-arrow";
                default -> "bi-calendar-check";
            };
        }
        public String getUnidad() {
            return meta.getTipo().contains("peso") ? "kg" : "días/semana";
        }
    }

    /** Tarjeta de "explorar rutinas". */
    public static class Programa {
        private final String categoria;
        private final String titulo;
        private final String descripcion;
        private final String icono;
        private final String estilo;
        private final int dias;
        private final int ejercicios;
        private final List<String> enfoque;
        private final boolean recomendado;

        public Programa(String categoria, String titulo, String descripcion, String icono, String estilo,
                        int dias, int ejercicios, List<String> enfoque, boolean recomendado) {
            this.categoria = categoria;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.icono = icono;
            this.estilo = estilo;
            this.dias = dias;
            this.ejercicios = ejercicios;
            this.enfoque = enfoque;
            this.recomendado = recomendado;
        }

        public String getCategoria() { return categoria; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public String getIcono() { return icono; }
        public String getEstilo() { return estilo; }
        public int getDias() { return dias; }
        public int getEjercicios() { return ejercicios; }
        public List<String> getEnfoque() { return enfoque; }
        public boolean isRecomendado() { return recomendado; }
    }

    // =====================================================================
    //  Cálculos
    // =====================================================================

    /** Minutos de un ejercicio (si ya no existe, 0). */
    private int minutos(Long idEjercicio) {
        return ejercicios.findById(idEjercicio).map(Ejercicio::getDuracionMin).orElse(0);
    }

    public Hoy hoy(Long idUsuario) {
        List<PlanEjercicio> filas = plan.findByIdUsuarioAndFecha(idUsuario, LocalDate.now());
        int hechos = 0;
        int min = 0;
        for (PlanEjercicio p : filas) {
            if (p.isCompletado()) {
                hechos++;
            }
            min += minutos(p.getIdEjercicio());
        }
        String nombre = filas.isEmpty() ? null
                : rutinas.findById(filas.get(0).getIdRutina()).map(Rutina::getNombre).orElse("Rutina");
        return new Hoy(nombre, filas.size(), hechos, min);
    }

    /** Gráfica de lunes a domingo de la semana actual. */
    public List<Barra> semana(Long idUsuario) {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, int[]> cuenta = new HashMap<>(); // [total, hechos]
        for (PlanEjercicio p : plan.findByIdUsuarioAndFechaBetween(idUsuario, lunes, lunes.plusDays(6))) {
            int[] c = cuenta.computeIfAbsent(p.getFecha(), k -> new int[2]);
            c[0]++;
            if (p.isCompletado()) {
                c[1]++;
            }
        }
        List<Barra> barras = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate f = lunes.plusDays(i);
            int[] c = cuenta.get(f);
            int pct = (c == null || c[0] == 0) ? 0 : c[1] * 100 / c[0];
            barras.add(new Barra(LETRAS[i], DIAS[i], pct, f.equals(hoy), c != null));
        }
        return barras;
    }

    /**
     * Racha = cuántos días seguidos (hasta hoy) has completado al menos un ejercicio.
     * Si hoy todavía no has hecho nada, se cuenta desde ayer (para no "romperla" en la mañana).
     */
    public int racha(Long idUsuario) {
        Set<LocalDate> diasActivos = new HashSet<>();
        for (PlanEjercicio p : plan.findByIdUsuarioAndCompletadoTrue(idUsuario)) {
            diasActivos.add(p.getFecha());
        }
        LocalDate dia = LocalDate.now();
        if (!diasActivos.contains(dia)) {
            dia = dia.minusDays(1);
        }
        int racha = 0;
        while (diasActivos.contains(dia)) {
            racha++;
            dia = dia.minusDays(1);
        }
        return racha;
    }

    /** [ejercicios completados esta semana, minutos activos esta semana, total histórico] */
    public int[] totales(Long idUsuario) {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int semana = 0;
        int min = 0;
        List<PlanEjercicio> hechos = plan.findByIdUsuarioAndCompletadoTrue(idUsuario);
        for (PlanEjercicio p : hechos) {
            if (!p.getFecha().isBefore(lunes) && !p.getFecha().isAfter(lunes.plusDays(6))) {
                semana++;
                min += minutos(p.getIdEjercicio());
            }
        }
        return new int[]{semana, min, hechos.size()};
    }

    /** Metas que no han vencido, con días restantes y % de tiempo transcurrido. */
    public List<MetaVista> metasActivas(Long idUsuario) {
        LocalDate hoy = LocalDate.now();
        List<MetaVista> lista = new ArrayList<>();
        for (Meta m : metas.findByIdUsuarioOrderByFechaFinAsc(idUsuario)) {
            if (m.getFechaFin().isBefore(hoy)) {
                continue;
            }
            long totalDias = Math.max(1, ChronoUnit.DAYS.between(m.getFechaInicio(), m.getFechaFin()));
            long pasaron = Math.max(0, ChronoUnit.DAYS.between(m.getFechaInicio(), hoy));
            int avance = (int) Math.min(100, pasaron * 100 / totalDias);
            lista.add(new MetaVista(m, ChronoUnit.DAYS.between(hoy, m.getFechaFin()), avance));
            if (lista.size() == 3) {
                break;
            }
        }
        return lista;
    }

    /** Misma regla de RutinaController: IMC -> categoría. */
    public static String categoriaPorImc(double imc) {
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25) return "Peso normal";
        return "Sobrepeso";
    }

    public List<Programa> programas(String recomendada) {
        List<Programa> lista = new ArrayList<>();
        lista.add(programa("Bajo peso", "Fuerza y volumen",
                "Más series, menos cardio. Pensado para ganar masa muscular.", "bi-lightning-charge-fill", "p-violeta", recomendada));
        lista.add(programa("Peso normal", "Equilibrio total",
                "Fuerza, cardio y core para mantenerte en forma toda la semana.", "bi-activity", "p-menta", recomendada));
        lista.add(programa("Sobrepeso", "Quema y bajo impacto",
                "Caminatas y cardio suave que cuidan tus rodillas mientras quemas calorías.", "bi-fire", "p-ambar", recomendada));
        return lista;
    }

    private Programa programa(String categoria, String titulo, String descripcion, String icono,
                              String estilo, String recomendada) {
        List<Rutina> semana = rutinas.findByNivelOrderByDiaSemanaAsc(categoria);
        int nEjercicios = 0;
        List<String> enfoque = new ArrayList<>();
        for (Rutina r : semana) {
            nEjercicios += detalle.findByIdRutina(r.getIdRutina()).size();
            if (enfoque.size() < 3) {
                enfoque.add(r.getNombre());
            }
        }
        return new Programa(categoria, titulo, descripcion, icono, estilo, semana.size(), nEjercicios,
                enfoque, categoria.equals(recomendada));
    }

    public String tipDelDia() {
        return TIPS[LocalDate.now().getDayOfYear() % TIPS.length];
    }

    /** "Buenos días" / "Buenas tardes" / "Buenas noches" según la hora. */
    public String saludo(int hora) {
        if (hora < 12) return "Buenos días";
        if (hora < 19) return "Buenas tardes";
        return "Buenas noches";
    }
}
