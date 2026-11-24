package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {
    List<Rutina> findByNivelOrderByDiaSemanaAsc(String nivel);
}