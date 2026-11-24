package com.healthplus.healthplus.config;

import com.healthplus.healthplus.modelo.*;
import com.healthplus.healthplus.repositorio.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CargaInicial implements CommandLineRunner {

    private final RutinaRepository rutinas;
    private final EjercicioRepository ejercicios;
    private final RutinaEjercicioRepository detalle;
    private final PlanEjercicioRepository plan;
    private final Map<String, Ejercicio> ej = new HashMap<>();

    public CargaInicial(RutinaRepository rutinas, EjercicioRepository ejercicios,
                        RutinaEjercicioRepository detalle, PlanEjercicioRepository plan) {
        this.rutinas = rutinas;
        this.ejercicios = ejercicios;
        this.detalle = detalle;
        this.plan = plan;
    }

    @Override
    public void run(String... args) {
        if (rutinas.count() > 0) {
            boolean yaEsSemanal = rutinas.findAll().stream().anyMatch(r -> r.getDiaSemana() != null);
            if (yaEsSemanal) {
                return;
            }
            plan.deleteAll();
            detalle.deleteAll();
            rutinas.deleteAll();
            ejercicios.deleteAll();
        }

        ejercicio("flexiones", "Flexiones de pecho", "Pecho", 10,
                "Manos bajo los hombros y cuerpo recto. Baja el pecho casi al piso y sube. Puedes apoyar las rodillas si te cuesta.");
        ejercicio("flexionesJuntas", "Flexiones con manos juntas", "Pecho", 8,
                "Igual que una flexión normal, pero con las manos juntas bajo el pecho. Trabaja más los tríceps.");
        ejercicio("fondos", "Fondos en silla", "Brazos", 8,
                "Apoya las manos en el borde de una silla firme y baja y sube el cuerpo doblando los codos.");
        ejercicio("curl", "Curl de brazos con mochila", "Brazos", 8,
                "De pie, sostén una mochila con peso y sube hacia los hombros doblando los codos, sin balancear el cuerpo.");
        ejercicio("remo", "Remo con mochila", "Espalda", 10,
                "Inclinado hacia adelante con la espalda recta, sube una mochila con peso hacia el abdomen.");
        ejercicio("remoUna", "Remo a una mano", "Espalda", 10,
                "Apoya una mano en una silla y sube la mochila con la otra hacia la cadera. Cambia de lado.");
        ejercicio("superman", "Superman", "Espalda", 6,
                "Acostado boca abajo, levanta brazos y piernas a la vez, aguanta 2 segundos y baja despacio.");
        ejercicio("sentadillas", "Sentadillas", "Piernas", 10,
                "Pies al ancho de los hombros. Baja como si te sentaras y sube sin levantar los talones.");
        ejercicio("zancadas", "Zancadas", "Piernas", 10,
                "Da un paso largo al frente y baja la rodilla de atrás casi hasta el piso. Alterna las piernas.");
        ejercicio("sumo", "Sentadilla sumo", "Piernas", 8,
                "Pies bien abiertos y puntas hacia afuera. Baja con la espalda recta y sube apretando los glúteos.");
        ejercicio("talones", "Elevación de talones", "Piernas", 6,
                "De pie, sube sobre las puntas de los pies, aguanta un segundo y baja despacio.");
        ejercicio("puente", "Puente de glúteos", "Glúteos", 8,
                "Acostado boca arriba con las rodillas dobladas, sube la cadera y aprieta los glúteos arriba.");
        ejercicio("plancha", "Plancha", "Core", 5,
                "Apoya antebrazos y puntas de los pies con el cuerpo en línea recta. Mantén 30 segundos.");
        ejercicio("abdominales", "Abdominales", "Core", 8,
                "Acostado con las rodillas dobladas, sube el torso sin jalar el cuello con las manos.");
        ejercicio("bicicleta", "Bicicleta abdominal", "Core", 6,
                "Acostado, lleva el codo hacia la rodilla contraria, alternando como si pedalearas.");
        ejercicio("caminata", "Caminata rápida", "Cardio", 20,
                "Camina a paso rápido con la espalda recta y moviendo los brazos.");
        ejercicio("trote", "Trote suave", "Cardio", 20,
                "Trota a un ritmo en el que puedas hablar sin ahogarte.");
        ejercicio("tijera", "Saltos de tijera", "Cardio", 10,
                "Salta abriendo piernas y brazos a la vez y vuelve a la posición inicial.");
        ejercicio("marcha", "Marcha en el sitio", "Cardio", 10,
                "Camina en el sitio subiendo bien las rodillas y moviendo los brazos. Es de bajo impacto.");
        ejercicio("estiramiento", "Estiramiento general", "Flexibilidad", 8,
                "Estira piernas, espalda, brazos y cuello, 20 segundos por zona, sin rebotes.");

        semana("Sobrepeso", 3, 12);
        semana("Peso normal", 3, 12);
        semana("Bajo peso", 4, 8);
    }

    private void semana(String nivel, int series, int reps) {
        boolean sobrepeso = nivel.equals("Sobrepeso");
        boolean bajo = nivel.equals("Bajo peso");

        Rutina lunes = rut(nivel, 1, "Pecho y brazos", "Día de empuje: pecho, hombros y tríceps.");
        if (sobrepeso) tiempo(lunes, "caminata");
        fuerza(lunes, "flexiones", series, reps);
        fuerza(lunes, "flexionesJuntas", series, reps);
        fuerza(lunes, "fondos", series, reps);
        fuerza(lunes, "curl", series, reps);
        tiempo(lunes, "estiramiento");

        Rutina martes = rut(nivel, 2, "Espalda y core", "Día de tirón: espalda y abdomen.");
        if (sobrepeso) tiempo(martes, "caminata");
        fuerza(martes, "remo", series, reps);
        fuerza(martes, "remoUna", series, reps);
        fuerza(martes, "superman", series, reps);
        fuerza(martes, "bicicleta", series, reps);
        tiempo(martes, "plancha");

        Rutina miercoles = rut(nivel, 3, "Piernas y glúteos", "Día de piernas: la parte más grande del cuerpo.");
        if (sobrepeso) tiempo(miercoles, "caminata");
        fuerza(miercoles, "sentadillas", series, reps);
        fuerza(miercoles, "zancadas", series, reps);
        fuerza(miercoles, "sumo", series, reps);
        fuerza(miercoles, "puente", series, reps);
        fuerza(miercoles, "talones", series, reps);
        tiempo(miercoles, "estiramiento");

        Rutina jueves;
        if (sobrepeso) {
            jueves = rut(nivel, 4, "Cardio suave y core", "Cardio de bajo impacto para cuidar las articulaciones.");
            tiempo(jueves, "caminata");
            tiempo(jueves, "marcha");
            fuerza(jueves, "abdominales", series, reps);
            tiempo(jueves, "plancha");
        } else if (bajo) {
            jueves = rut(nivel, 4, "Core y movilidad", "Día suave: abdomen y estiramientos para recuperar.");
            fuerza(jueves, "abdominales", series, reps);
            fuerza(jueves, "bicicleta", series, reps);
            tiempo(jueves, "plancha");
            tiempo(jueves, "estiramiento");
        } else {
            jueves = rut(nivel, 4, "Cardio y core", "Cardio para el corazón y trabajo de abdomen.");
            tiempo(jueves, "trote");
            tiempo(jueves, "tijera");
            fuerza(jueves, "abdominales", series, reps);
            tiempo(jueves, "plancha");
        }

        Rutina viernes = rut(nivel, 5, "Cuerpo completo", "Un poco de todo para cerrar la semana.");
        fuerza(viernes, "sentadillas", series, reps);
        fuerza(viernes, "flexiones", series, reps);
        fuerza(viernes, "remo", series, reps);
        tiempo(viernes, "plancha");
        tiempo(viernes, "estiramiento");
    }

    private void ejercicio(String clave, String nombre, String grupo, int minutos, String descripcion) {
        Ejercicio e = new Ejercicio();
        e.setNombre(nombre);
        e.setGrupoMuscular(grupo);
        e.setDuracionMin(minutos);
        e.setDescripcion(descripcion);
        ej.put(clave, ejercicios.save(e));
    }

    private Rutina rut(String nivel, int dia, String nombre, String descripcion) {
        Rutina r = new Rutina();
        r.setNivel(nivel);
        r.setDiaSemana(dia);
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        return rutinas.save(r);
    }

    private void fuerza(Rutina r, String clave, int series, int repeticiones) {
        asignar(r, ej.get(clave), series, repeticiones);
    }

    private void tiempo(Rutina r, String clave) {
        asignar(r, ej.get(clave), null, null);
    }

    private void asignar(Rutina r, Ejercicio e, Integer series, Integer repeticiones) {
        RutinaEjercicio re = new RutinaEjercicio();
        re.setIdRutina(r.getIdRutina());
        re.setIdEjercicio(e.getIdEjercicio());
        re.setSeries(series);
        re.setRepeticiones(repeticiones);
        detalle.save(re);
    }
}