package com.healthplus.healthplus.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "rutina_ejercicio")
@IdClass(RutinaEjercicio.Clave.class)
public class RutinaEjercicio {

    @Id
    private Long idRutina;

    @Id
    private Long idEjercicio;

    private Integer series;

    private Integer repeticiones;

    public Long getIdRutina() {
        return idRutina;
    }

    public void setIdRutina(Long idRutina) {
        this.idRutina = idRutina;
    }

    public Long getIdEjercicio() {
        return idEjercicio;
    }

    public void setIdEjercicio(Long idEjercicio) {
        this.idEjercicio = idEjercicio;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public Integer getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(Integer repeticiones) {
        this.repeticiones = repeticiones;
    }

    public static class Clave implements Serializable {
        private Long idRutina;
        private Long idEjercicio;

        public Clave() {
        }

        public Clave(Long idRutina, Long idEjercicio) {
            this.idRutina = idRutina;
            this.idEjercicio = idEjercicio;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Clave c)) return false;
            return Objects.equals(idRutina, c.idRutina) && Objects.equals(idEjercicio, c.idEjercicio);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idRutina, idEjercicio);
        }
    }
}