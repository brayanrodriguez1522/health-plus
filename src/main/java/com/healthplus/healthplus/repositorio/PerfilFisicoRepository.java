package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilFisicoRepository extends JpaRepository<PerfilFisico, Long> {
    Optional<PerfilFisico> findByIdUsuario(Long idUsuario);
}