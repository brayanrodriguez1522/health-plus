package com.healthplus.healthplus.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfil_fisico")
public class PerfilFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPerfil;

    @Column(nullable = false, unique = true)
    private Long idUsuario;

    @Column(nullable = false)
    private double pesoKg;

    @Column(nullable = false)
    private double estaturaCm;

    @Column(nullable = false)
    private int edad;

    @Column(nullable = false, length = 20)
    private String sexo;

    public Long getIdPerfil() {
        return idPerfil;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public double getEstaturaCm() {
        return estaturaCm;
    }

    public void setEstaturaCm(double estaturaCm) {
        this.estaturaCm = estaturaCm;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    // RF-07: IMC = peso / estatura(m)^2
    public double getImc() {
        double m = estaturaCm / 100.0;
        return Math.round(pesoKg / (m * m) * 10.0) / 10.0;
    }

    // Añadido para la pantalla de perfil: categoría según la OMS y posición en la barra de colores
    public String getCategoriaImc() {
        double imc = getImc();
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25) return "Peso normal";
        if (imc < 30) return "Sobrepeso";
        return "Obesidad";
    }

    /** Posición (0 a 100 %) del IMC en una escala de 15 a 40. */
    public double getPosicionImc() {
        return Math.max(0, Math.min(100, (getImc() - 15) * 4));
    }
}
