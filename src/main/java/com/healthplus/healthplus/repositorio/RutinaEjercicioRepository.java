package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutinaEjercicioRepository extends JpaRepository<RutinaEjercicio, RutinaEjercicio.Clave> {
    List<RutinaEjercicio> findByIdRutina(Long idRutina);
}