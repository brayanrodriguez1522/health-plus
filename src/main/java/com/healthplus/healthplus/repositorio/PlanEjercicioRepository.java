package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PlanEjercicioRepository extends JpaRepository<PlanEjercicio, Long> {
    List<PlanEjercicio> findByIdUsuarioAndFecha(Long idUsuario, LocalDate fecha);

    List<PlanEjercicio> findByIdUsuarioAndFechaGreaterThanEqual(Long idUsuario, LocalDate fecha);

    List<PlanEjercicio> findByIdUsuarioAndFechaBetween(Long idUsuario, LocalDate desde, LocalDate hasta);

    // Añadido para la página de inicio (racha) y para borrar la cuenta
    List<PlanEjercicio> findByIdUsuarioAndCompletadoTrue(Long idUsuario);

    List<PlanEjercicio> findByIdUsuario(Long idUsuario);
}