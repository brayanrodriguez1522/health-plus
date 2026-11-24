package com.healthplus.healthplus.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false)
    private String contrasenaHash;

    @Column(nullable = false)
    private LocalDate fechaRegistro = LocalDate.now();

    // ---- Añadido: cómo creó la cuenta ("Correo", "Google" o "Facebook") ----
    // Es nullable para que las filas viejas (creadas antes de este cambio) no den error.
    @Column(length = 20)
    private String proveedor;

    // Foto de perfil que manda Google/Facebook (puede quedar vacía)
    @Column(length = 500)
    private String fotoUrl;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getProveedor() {
        return proveedor == null ? "Correo" : proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    /** true si la cuenta se creó con correo y contraseña (y no con Google/Facebook). */
    public boolean isCuentaLocal() {
        return "Correo".equals(getProveedor());
    }

    /** Iniciales para el avatar: "Jonathan Hernández" -> "JH". */
    public String getIniciales() {
        String[] partes = nombre == null ? new String[0] : nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length && sb.length() < 2; i++) {
            if (!partes[i].isEmpty()) {
                sb.append(Character.toUpperCase(partes[i].charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
