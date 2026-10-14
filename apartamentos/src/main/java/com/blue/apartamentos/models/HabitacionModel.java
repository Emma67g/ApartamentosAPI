package com.blue.apartamentos.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "habitaciones", uniqueConstraints = @UniqueConstraint(name = "uk_habitacion_propiedad_numero", columnNames = {
        "id_propiedad", "numero" }))
public class HabitacionModel {

    public enum TipoHabitacion {
        INDIVIDUAL,
        DOBLE,
        SUITE,
        COMPARTIDA
    }

    public enum EstadoHabitacion {
        DISPONIBLE,
        NO_DISPONIBLE,
        MANTENIMIENTO,
        OCUPADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habitacion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false)
    @NotNull(message = "La propiedad es obligatoria (propiedad.id)")
    private PropiedadModel propiedad;

    @Column(name = "numero", nullable = false, length = 20)
    @NotBlank(message = "El número de habitación es obligatorio")
    @Size(max = 20, message = "El número admite máximo 20 caracteres")
    private String numero;

    @Column(name = "nombre", length = 60)
    @Size(max = 60, message = "El nombre admite máximo 60 caracteres")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_habitacion", nullable = false, length = 20)
    @NotNull(message = "El tipo de habitación es obligatorio (INDIVIDUAL, DOBLE, SUITE o COMPARTIDA)")
    private TipoHabitacion tipo;

    @Column(name = "capacidad", nullable = false)
    @Min(value = 1, message = "La capacidad mínima es 1 huésped")
    private int capacidad;

    @Column(name = "precio_noche", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio por noche es obligatorio")
    @Positive(message = "El precio por noche debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    private BigDecimal precioNoche;

    @Column(name = "num_banos", nullable = false)
    @Min(value = 0, message = "El número de baños no puede ser negativo")
    private int numBanos;

    @Column(name = "metros_cuadrados")
    @Positive(message = "Los metros cuadrados deben ser mayores a cero")
    private Integer metrosCuadrados;

    @Column(name = "comodidades", length = 300)
    @Size(max = 300, message = "Las comodidades admite máximo 300 caracteres")
    private String comodidades;

    @Column(name = "descripcion", length = 500)
    @Size(max = 500, message = "La descripción admite máximo 500 caracteres")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onPrePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoHabitacion.DISPONIBLE;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PropiedadModel getPropiedad() {
        return propiedad;
    }

    public void setPropiedad(PropiedadModel propiedad) {
        this.propiedad = propiedad;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoHabitacion tipo) {
        this.tipo = tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public int getNumBanos() {
        return numBanos;
    }

    public void setNumBanos(int numBanos) {
        this.numBanos = numBanos;
    }

    public Integer getMetrosCuadrados() {
        return metrosCuadrados;
    }

    public void setMetrosCuadrados(Integer metrosCuadrados) {
        this.metrosCuadrados = metrosCuadrados;
    }

    public String getComodidades() {
        return comodidades;
    }

    public void setComodidades(String comodidades) {
        this.comodidades = comodidades;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoHabitacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoHabitacion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
