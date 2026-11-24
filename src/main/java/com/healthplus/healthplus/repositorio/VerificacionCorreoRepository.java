package com.healthplus.healthplus.repositorio;

import com.healthplus.healthplus.modelo.VerificacionCorreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionCorreoRepository extends JpaRepository<VerificacionCorreo, Long> {
    Optional<VerificacionCorreo> findByCorreo(String correo);
}
