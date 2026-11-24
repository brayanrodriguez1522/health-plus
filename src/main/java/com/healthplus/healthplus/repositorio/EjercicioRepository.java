package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
    List<Ejercicio> findByGrupoMuscular(String grupoMuscular);
}