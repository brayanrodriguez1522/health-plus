package com.healthplus.healthplus.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro "pendiente": alguien llenó el formulario de registro pero todavía
 * no ha escrito el código de 6 dígitos que le llegó al correo.
 *
 * Mientras no verifique, no existe en la tabla usuarios. Cuando escribe el código
 * correcto, se crea el Usuario y esta fila se borra.
 */
@Entity
@Table(name = "verificacion_correo")
public class VerificacionCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVerificacion;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 100)
    private String nombre;

    // La contraseña ya va cifrada con BCrypt (nunca se guarda en texto plano)
    @Column(nullable = false)
    private String contrasenaHash;

    // El código también se guarda cifrado: si alguien abre la BD no lo puede leer
    @Column(nullable = false)
    private String codigoHash;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private LocalDateTime enviadoEn;

    @Column(nullable = false)
    private int intentos = 0;

    public Long getIdVerificacion() {
        return idVerificacion;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    public void setCodigoHash(String codigoHash) {
        this.codigoHash = codigoHash;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public LocalDateTime getEnviadoEn() {
        return enviadoEn;
    }

    public void setEnviadoEn(LocalDateTime enviadoEn) {
        this.enviadoEn = enviadoEn;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }
}
